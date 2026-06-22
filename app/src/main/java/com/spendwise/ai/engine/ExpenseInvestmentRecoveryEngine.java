package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.sms.InvestmentSignalKeyFactory;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Recovers likely investment transfers that were previously imported into the
 * expenses ledger. Recovered rows stay visible in history but stop inflating
 * spending analytics and can seed placeholder holdings in Buckets.
 */
public class ExpenseInvestmentRecoveryEngine {

    private static final double AUTO_CONVERT_THRESHOLD = 0.75d;
    private static final double PENDING_REVIEW_THRESHOLD = 0.50d;

    public static class RecoveryResult {
        public int scanned;
        public int converted;
        public int linkedExisting;
        public int pendingReview;
        public int ignored;
    }

    @NonNull
    public RecoveryResult recover(
            @NonNull android.content.Context context,
            @NonNull AppDatabase db,
            long startEpochDay,
            long endEpochDay) {
        RecoveryResult result = new RecoveryResult();
        List<Expense> candidates = db.expenseDao()
                .getExpenseCandidatesForInvestmentRecoverySync(startEpochDay, endEpochDay);
        if (candidates == null || candidates.isEmpty()) {
            return result;
        }

        EventIngestionPipeline pipeline = new EventIngestionPipeline();
        InvestmentAutoIngestionEngine ingestionEngine = new InvestmentAutoIngestionEngine();
        OccurrenceReconciliationEngine reconciliationEngine = new OccurrenceReconciliationEngine();
        result.scanned = candidates.size();

        for (Expense expense : candidates) {
            Candidate candidate = evaluateCandidate(db, expense);
            if (candidate.score < PENDING_REVIEW_THRESHOLD) {
                expense.setRecoveryStatus(Expense.RECOVERY_STATUS_IGNORED);
                db.expenseDao().update(expense);
                result.ignored++;
                continue;
            }

            if (candidate.score < AUTO_CONVERT_THRESHOLD) {
                expense.setRecoveryStatus(Expense.RECOVERY_STATUS_PENDING_REVIEW);
                db.expenseDao().update(expense);
                result.pendingReview++;
                continue;
            }

            ParsedInvestmentSignal signal = buildSignal(expense, candidate);
            EventIngestionPipeline.IngestResult event = pipeline.ingest(
                    db,
                    IngestionEvent.DOMAIN_INVESTMENT,
                    IngestionEvent.SOURCE_EXPENSE_RECOVERY,
                    InvestmentSignalKeyFactory.buildIngestionSourceKey(signal),
                    signal.getCorrelationKey(),
                    signal.getSignalType().name(),
                    signal.getAmountCents(),
                    signal.getTimestampMillis(),
                    null,
                    null,
                    signal.getRawMessage());

            long linkedInvestmentId;
            if (event.duplicate) {
                linkedInvestmentId = resolveLinkedInvestmentId(db, signal, expense);
                if (linkedInvestmentId > 0) {
                    result.linkedExisting++;
                }
            } else {
                List<ParsedInvestmentSignal> signals = new ArrayList<>();
                signals.add(signal);
                InvestmentAutoIngestionEngine.IngestResult ingestResult = ingestionEngine.ingest(
                        context,
                        db,
                        signals,
                        IngestionEvent.SOURCE_EXPENSE_RECOVERY);
                reconciliationEngine.reconcileSignals(
                        db,
                        signals,
                        ingestResult.investmentIdBySourceKey,
                        ingestResult.transactionIdBySourceKey,
                        IngestionEvent.SOURCE_EXPENSE_RECOVERY);
                pipeline.markProcessed(db, event.eventId, IngestionEvent.STATUS_PROCESSED);
                String sourceKey = InvestmentSignalKeyFactory.buildIngestionSourceKey(signal);
                linkedInvestmentId = ingestResult.investmentIdBySourceKey.containsKey(sourceKey)
                        ? ingestResult.investmentIdBySourceKey.get(sourceKey)
                        : resolveLinkedInvestmentId(db, signal, expense);
            }

            if (linkedInvestmentId > 0) {
                expense.setInvestmentTransfer(true);
                expense.setLinkedInvestmentId(linkedInvestmentId);
                expense.setRecoveryStatus(Expense.RECOVERY_STATUS_CONVERTED);
                expense.setCanonicalSourceKey(InvestmentSignalKeyFactory.buildIngestionSourceKey(signal));
                db.expenseDao().update(expense);
                
                // If we recovered a broker expense, overwrite the existing holding's platform 
                // (which is likely the AMC) with the broker name so it groups correctly.
                if (candidate.platform != null) {
                    Holding holding = db.investmentDao().getById(linkedInvestmentId);
                    if (holding != null && !candidate.platform.equals(holding.getPlatform())) {
                        holding.setPlatform(candidate.platform);
                        
                        String normalizedBroker = normalize(candidate.platform);
                        com.spendwise.ai.data.entity.Provider provider = db.providerDao().getByNormalizedName(normalizedBroker);
                        if (provider == null) {
                            provider = new com.spendwise.ai.data.entity.Provider(
                                    normalizedBroker, 
                                    candidate.platform, 
                                    com.spendwise.ai.data.entity.Provider.TYPE_BROKER, 
                                    null, 
                                    System.currentTimeMillis()
                            );
                            long pid = db.providerDao().insertIgnore(provider);
                            if (pid > 0) {
                                holding.setProviderId(pid);
                            } else {
                                provider = db.providerDao().getByNormalizedName(normalizedBroker);
                                if (provider != null) holding.setProviderId(provider.getId());
                            }
                        } else {
                            holding.setProviderId(provider.getId());
                        }
                        db.investmentDao().update(holding);
                    }
                }
                
                result.converted++;
            } else {
                expense.setRecoveryStatus(Expense.RECOVERY_STATUS_PENDING_REVIEW);
                db.expenseDao().update(expense);
                result.pendingReview++;
            }
        }
        return result;
    }

    private long resolveLinkedInvestmentId(
            @NonNull AppDatabase db,
            @NonNull ParsedInvestmentSignal signal,
            @NonNull Expense expense) {
        String sourceKey = InvestmentSignalKeyFactory.buildIngestionSourceKey(signal);
        Holding bySource = db.investmentDao().getByLastSourceKey(sourceKey);
        if (bySource != null) {
            return bySource.getId();
        }
        Holding byCorrelation = db.investmentDao().getByPrimaryCorrelationKey(signal.getCorrelationKey());
        if (byCorrelation != null) {
            return byCorrelation.getId();
        }

        // Fuzzy match: same amount, within -3 to +7 days
        long dayMillis = expense.getDateEpochDay() * 86400000L;
        List<com.spendwise.ai.data.entity.InvestmentTransaction> recentTxs = 
            db.investmentTransactionDao().findGlobalCandidates(
                expense.getAmountCents(),
                dayMillis - 3L * 86400000L, 
                dayMillis + 7L * 86400000L
            );
        if (recentTxs != null && !recentTxs.isEmpty()) {
            return recentTxs.get(0).getHoldingId();
        }
        
        return -1L;
    }

    @NonNull
    private ParsedInvestmentSignal buildSignal(@NonNull Expense expense, @NonNull Candidate candidate) {
        long timestampMillis = LocalDate.ofEpochDay(expense.getDateEpochDay())
                .atStartOfDay(ZoneId.systemDefault())
                .plusHours(12)
                .toInstant()
                .toEpochMilli();
        String platform = candidate.platform;
        String investmentName = candidate.investmentName;
        String sourceKey = "expense_recovery_" + expense.getId();
        String correlationKey = buildCorrelationKey(candidate, expense);
        String payload = expense.getDescription() != null && !expense.getDescription().trim().isEmpty()
                ? expense.getDescription()
                : String.format(
                        Locale.US,
                        "Expense recovery Rs %.2f to %s for %s",
                        expense.getAmountCents() / 100.0,
                        platform == null ? "investment" : platform,
                        investmentName);
        return new ParsedInvestmentSignal(
                candidate.signalType,
                null, // sender not available
                candidate.platform,
                candidate.investmentName,
                candidate.assetType,
                expense.getAmountCents(),
                timestampMillis,
                sourceKey,
                correlationKey,
                payload,
                false);
    }

    @NonNull
    private String buildCorrelationKey(@NonNull Candidate candidate, @NonNull Expense expense) {
        String seed = String.format(
                Locale.US,
                "%s_%s_%d_%d",
                sanitize(candidate.platform == null ? candidate.investmentName : candidate.platform),
                sanitize(candidate.assetType),
                expense.getAmountCents(),
                expense.getDateEpochDay() / 30L);
        return "corr_expense_" + seed;
    }

    @NonNull
    private Candidate evaluateCandidate(@NonNull AppDatabase db, @NonNull Expense expense) {
        Candidate candidate = new Candidate();
        candidate.score = 0d;

        Merchant merchant = null;
        if (expense.getMerchantId() != null && expense.getMerchantId() > 0) {
            merchant = db.merchantDao().getById(expense.getMerchantId());
        }

        String description = expense.getDescription() == null ? "" : expense.getDescription();
        String merchantName = merchant == null ? "" : merchant.getName();
        String context = normalize(description + " " + merchantName);

        candidate.platform = resolvePlatform(context);
        candidate.assetType = resolveAssetType(context, candidate.platform);
        candidate.signalType = resolveSignalType(candidate.assetType);
        candidate.investmentName = resolveInvestmentName(candidate.platform, candidate.assetType, merchantName);

        if (candidate.platform != null) {
            candidate.score += 0.70d; // Boosted from 0.55 to ensure known brokers meet auto-convert threshold
        }
        if (containsAny(context, "sip", "mutual fund", "fund", "folio", "stock", "share", "equity",
                "etf", "demat", "investment", "fixed deposit", "fd", "recurring deposit", "rd", "ppf", "nps")) {
            candidate.score += 0.30d;
        }
        if (containsAny(context, "gold", "silver", "liquid")) {
            candidate.score += 0.10d;
        }
        if (expense.getAmountCents() >= 500_00L) {
            candidate.score += 0.05d;
        }
        if (containsAny(normalize(description), "shopping", "food", "transport", "utility", "rent")) {
            candidate.score -= 0.25d;
        }
        if (candidate.platform == null && Holding.ASSET_OTHER.equals(candidate.assetType)) {
            candidate.score -= 0.25d;
        }
        return candidate;
    }

    @Nullable
    private String resolvePlatform(@NonNull String context) {
        if (containsAny(context, "upstox")) {
            return "Upstox";
        }
        if (containsAny(context, "groww")) {
            return "Groww";
        }
        if (containsAny(context, "zerodha", "kite", "coin")) {
            return "Zerodha";
        }
        if (containsAny(context, "angel one")) {
            return "Angel One";
        }
        if (containsAny(context, "paytm money")) {
            return "Paytm Money";
        }
        if (containsAny(context, "et money")) {
            return "ET Money";
        }
        if (containsAny(context, "mf central", "mfcentral")) {
            return "MF Central";
        }
        return null;
    }

    @NonNull
    private String resolveAssetType(@NonNull String context, @Nullable String platform) {
        if (containsAny(context, "fixed deposit", " fd ", "fd")) {
            return Holding.ASSET_FD;
        }
        if (containsAny(context, "recurring deposit", " rd ", "rd")) {
            return Holding.ASSET_RD;
        }
        if (containsAny(context, "ppf")) {
            return Holding.ASSET_PPF;
        }
        if (containsAny(context, "nps", "pension")) {
            return Holding.ASSET_NPS;
        }
        if (containsAny(context, "etf")) {
            return Holding.ASSET_ETF;
        }
        if (containsAny(context, "mutual fund", "sip", "folio", "amc")) {
            return Holding.ASSET_MF;
        }
        if (containsAny(context, "stock", "share", "equity", "demat")) {
            return Holding.ASSET_STOCK;
        }
        if (containsAny(context, "gold")) {
            return Holding.ASSET_GOLD;
        }
        return Holding.ASSET_OTHER;
    }

    @NonNull
    private ParsedInvestmentSignal.SignalType resolveSignalType(@NonNull String assetType) {
        if (Holding.ASSET_FD.equals(assetType)) {
            return ParsedInvestmentSignal.SignalType.FD_BOOKED;
        }
        if (Holding.ASSET_RD.equals(assetType)) {
            return ParsedInvestmentSignal.SignalType.RD_BOOKED;
        }
        return ParsedInvestmentSignal.SignalType.INVESTMENT_DEBIT;
    }

    @NonNull
    private String resolveInvestmentName(
            @Nullable String platform,
            @NonNull String assetType,
            @NonNull String merchantName) {
        if (Holding.ASSET_FD.equals(assetType)) {
            return platform == null ? "Fixed Deposit" : platform + " Fixed Deposit";
        }
        if (Holding.ASSET_RD.equals(assetType)) {
            return platform == null ? "Recurring Deposit" : platform + " Recurring Deposit";
        }
        if (Holding.ASSET_OTHER.equals(assetType)) {
            return "Holding Allocation";
        }
        if (platform != null) {
            return platform + " Holding";
        }
        if (!merchantName.trim().isEmpty()) {
            return merchantName.trim() + " Holding";
        }
        return "Holding Allocation";
    }

    private boolean containsAny(@NonNull String text, String... tokens) {
        String haystack = " " + normalize(text) + " ";
        for (String token : tokens) {
            String needle = " " + normalize(token) + " ";
            if (haystack.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    @NonNull
    private String sanitize(@Nullable String value) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            return "unknown";
        }
        return normalized.replace(' ', '_');
    }

    private static class Candidate {
        double score;
        String platform;
        String assetType;
        String investmentName;
        ParsedInvestmentSignal.SignalType signalType;
    }
}
