package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.InvestmentSource;
import com.spendwise.ai.sms.InvestmentSignalKeyFactory;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ingests investment SMS signals into the investments table with duplicate
 * protection and two-phase message merging (debit -> allotment/update).
 */
public class InvestmentAutoIngestionEngine {

    private static final Pattern SOURCE_TOKEN_PATTERN = Pattern.compile("\\[auto-source:([^\\]]+)]");
    private static final Pattern CORR_TOKEN_PATTERN = Pattern.compile("\\[auto-corr:([^\\]]+)]");
    private static final long FUZZY_AMOUNT_TOLERANCE_CENTS = 50_00L;
    private static final int FUZZY_DAY_WINDOW = 7;

    public static class IngestResult {
        public int inserted;
        public int updated;
        public int skipped;
        public final Map<String, Long> investmentIdBySourceKey = new HashMap<>();
    }

    @NonNull
    public IngestResult ingest(
            @NonNull AppDatabase db,
            @NonNull List<ParsedInvestmentSignal> signals) {
        return ingest(db, signals, IngestionEvent.SOURCE_SMS);
    }

    @NonNull
    public IngestResult ingest(
            @NonNull AppDatabase db,
            @NonNull List<ParsedInvestmentSignal> signals,
            @NonNull String sourceType) {
        IngestResult result = new IngestResult();
        if (signals.isEmpty()) {
            return result;
        }

        List<ParsedInvestmentSignal> orderedSignals = new ArrayList<>(signals);
        orderedSignals.sort(Comparator.comparingLong(ParsedInvestmentSignal::getTimestampMillis));

        List<Bucket> buckets = db.bucketDao().getAllBucketsSync();
        if (buckets.isEmpty()) {
            result.skipped += orderedSignals.size();
            return result;
        }
        new InvestmentDeduplicationEngine().repair(db);

        Map<String, Long> bucketIdByType = new HashMap<>();
        long fallbackBucketId = buckets.get(0).getId();
        for (Bucket bucket : buckets) {
            bucketIdByType.put(bucket.getType(), bucket.getId());
        }
        BucketAssignmentEngine assignmentEngine = new BucketAssignmentEngine();

        List<Holding> allInvestments = db.investmentDao().getAllInvestmentsSync();
        Map<String, Holding> bySource = new HashMap<>();
        Map<String, Holding> byCorrelation = new HashMap<>();
        Map<String, Holding> byCanonicalKey = new HashMap<>();
        indexExistingInvestments(allInvestments, bySource, byCorrelation, byCanonicalKey);

        EventIngestionPipeline pipeline = new EventIngestionPipeline();

        for (ParsedInvestmentSignal signal : orderedSignals) {
            if (signal.getAmountCents() <= 0
                    && signal.getSignalType() != ParsedInvestmentSignal.SignalType.MATURITY_CREDIT) {
                result.skipped++;
                continue;
            }

            String sourceKey = InvestmentSignalKeyFactory.buildIngestionSourceKey(signal);
            String canonicalInstrumentKey = InvestmentSignalKeyFactory.buildCanonicalInstrumentKey(signal);
            EventIngestionPipeline.IngestResult event = pipeline.ingest(
                    db,
                    IngestionEvent.DOMAIN_INVESTMENT,
                    sourceType,
                    sourceKey,
                    signal.getCorrelationKey(),
                    signal.getSignalType().name(),
                    signal.getAmountCents(),
                    signal.getTimestampMillis(),
                    signal.getSender(),
                    signal.getRawMessage(), // payloadHashBase
                    signal.getRawMessage()); // rawMessage

            Holding matched = findByLinkedSource(db, sourceKey);
            if (matched == null) {
                matched = bySource.get(sourceKey);
            }
            if (matched == null) {
                matched = byCorrelation.get(signal.getCorrelationKey());
            }
            if (matched == null) {
                matched = byCanonicalKey.get(canonicalInstrumentKey);
            }
            if (matched == null) {
                matched = findFuzzyMatch(allInvestments, signal);
            }

            if (matched != null) {
                if (applyUpdate(matched, signal, sourceKey, canonicalInstrumentKey)) {
                    db.investmentDao().update(matched);
                    result.updated++;
                } else {
                    result.skipped++;
                }
                linkSource(db, matched.getId(), sourceType, sourceKey, canonicalInstrumentKey, signal);
                pipeline.markProcessed(db, event.eventId, IngestionEvent.STATUS_PROCESSED);
                indexInvestment(matched, bySource, byCorrelation, byCanonicalKey);
                rememberLinkedInvestment(result, signal, sourceKey, matched.getId());
                continue;
            }

            BucketAssignmentEngine.AssignmentResult assignment = assignmentEngine.assign(db, signal, buckets);
            long targetBucketId = assignment.bucketId > 0
                    ? assignment.bucketId
                    : resolveBucketId(bucketIdByType, fallbackBucketId, signal.getAssetType());
            long epochDay = Instant.ofEpochMilli(signal.getTimestampMillis())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay();

            long baseAmount = Math.max(0L, signal.getAmountCents());
            Holding investment = new Holding(
                    targetBucketId,
                    chooseInvestmentName(signal),
                    signal.getAssetType(),
                    baseAmount,
                    baseAmount,
                    epochDay);
            investment.setPlatform(signal.getPlatform());
            investment.setAutoFetched(true);
            investment.setNotes(mergeAutoTokens(investment.getNotes(), signal));
            investment.setPrimaryCorrelationKey(signal.getCorrelationKey());
            investment.setLastSourceType("AUTO_INGEST");
            investment.setLastSourceKey(sourceKey);
            investment.setCanonicalInstrumentKey(canonicalInstrumentKey);
            if (signal.getSignalType() == ParsedInvestmentSignal.SignalType.FD_BOOKED) {
                investment.setJobTitle("FD Allocation");
            } else if (signal.getSignalType() == ParsedInvestmentSignal.SignalType.RD_BOOKED) {
                investment.setJobTitle("RD Allocation");
            }
            if (assignment.needsUserConfirmation) {
                investment.setNotes(appendToken(investment.getNotes(), "[auto-map-pending:true]"));
            }

            long insertedId = db.investmentDao().insert(investment);
            investment.setId(insertedId);
            linkSource(db, insertedId, sourceType, sourceKey, canonicalInstrumentKey, signal);
            pipeline.markProcessed(db, event.eventId, IngestionEvent.STATUS_PROCESSED);
            allInvestments.add(investment);
            indexInvestment(investment, bySource, byCorrelation, byCanonicalKey);
            result.inserted++;
            rememberLinkedInvestment(result, signal, sourceKey, insertedId);
        }
        return result;
    }

    private void rememberLinkedInvestment(
            @NonNull IngestResult result,
            @NonNull ParsedInvestmentSignal signal,
            @NonNull String sourceKey,
            long investmentId) {
        result.investmentIdBySourceKey.put(sourceKey, investmentId);
        result.investmentIdBySourceKey.put(signal.getSourceKey(), investmentId);
    }

    private void indexExistingInvestments(
            @NonNull List<Holding> investments,
            @NonNull Map<String, Holding> bySource,
            @NonNull Map<String, Holding> byCorrelation,
            @NonNull Map<String, Holding> byCanonicalKey) {
        for (Holding investment : investments) {
            indexInvestment(investment, bySource, byCorrelation, byCanonicalKey);
        }
    }

    private void indexInvestment(
            @NonNull Holding investment,
            @NonNull Map<String, Holding> bySource,
            @NonNull Map<String, Holding> byCorrelation,
            @NonNull Map<String, Holding> byCanonicalKey) {
        if (investment.getCanonicalInstrumentKey() != null && !investment.getCanonicalInstrumentKey().isEmpty()) {
            byCanonicalKey.put(investment.getCanonicalInstrumentKey(), investment);
        }
        if (investment.getLastSourceKey() != null && !investment.getLastSourceKey().isEmpty()) {
            bySource.put(investment.getLastSourceKey(), investment);
        }
        if (investment.getPrimaryCorrelationKey() != null && !investment.getPrimaryCorrelationKey().isEmpty()) {
            byCorrelation.put(investment.getPrimaryCorrelationKey(), investment);
        }
        String notes = investment.getNotes();
        if (notes == null || notes.isEmpty()) {
            return;
        }
        String source = extractToken(SOURCE_TOKEN_PATTERN, notes);
        if (source != null) {
            bySource.put(source, investment);
        }
        String corr = extractToken(CORR_TOKEN_PATTERN, notes);
        if (corr != null) {
            byCorrelation.put(corr, investment);
        }
    }

    @Nullable
    private Holding findFuzzyMatch(
            @NonNull List<Holding> allInvestments,
            @NonNull ParsedInvestmentSignal signal) {
        long signalDay = epochDay(signal.getTimestampMillis());
        String normalizedSignalName = normalize(signal.getInvestmentName());
        String normalizedPlatform = normalize(signal.getPlatform());

        for (Holding candidate : allInvestments) {
            boolean assetMatches = normalize(candidate.getAssetType()).equals(normalize(signal.getAssetType()))
                    || Holding.ASSET_OTHER.equals(candidate.getAssetType())
                    || Holding.ASSET_OTHER.equals(signal.getAssetType());
            if (!assetMatches) {
                continue;
            }
            if (!normalizedSignalName.isEmpty()
                    && !isWeakName(signal.getInvestmentName())
                    && !isWeakName(candidate.getName())
                    && !normalize(candidate.getName()).equals(normalizedSignalName)) {
                continue;
            }
            if (!normalizedPlatform.isEmpty()
                    && !normalize(candidate.getPlatform()).equals(normalizedPlatform)) {
                continue;
            }
            boolean isFixedDeposit = Holding.ASSET_FD.equals(candidate.getAssetType())
                    || Holding.ASSET_RD.equals(candidate.getAssetType());
            long dayDifference = Math.abs(candidate.getLastUpdatedEpochDay() - signalDay);

            if (dayDifference > FUZZY_DAY_WINDOW) {
                // If it's not a rigid asset type like FD/RD, respect the narrow 7-day window
                if (!isFixedDeposit) {
                    continue;
                }
                // For FD/RD, allow matching over a longer timeframe (e.g. 10 years maturity)
                // but ONLY if the platform and amount are very close.
                if (dayDifference > 3650) {
                    continue; // Extremely far apart
                }
            }

            long base = Math.max(1L, candidate.getInvestedAmountCents());
            // For FD/RD updates over long periods, interest can be up to 40%
            double allowedVariance = isFixedDeposit ? 0.40d : 0.15d;
            long tolerance = Math.max(FUZZY_AMOUNT_TOLERANCE_CENTS, Math.round(base * allowedVariance));
            if (Math.abs(candidate.getInvestedAmountCents() - signal.getAmountCents()) <= tolerance) {
                return candidate;
            }
        }
        return null;
    }

    private boolean applyUpdate(
            @NonNull Holding existing,
            @NonNull ParsedInvestmentSignal signal,
            @NonNull String sourceKey,
            @NonNull String canonicalInstrumentKey) {
        boolean changed = false;
        long amountCents = Math.max(0L, signal.getAmountCents());

        if (existing.getPlatform() == null && signal.getPlatform() != null) {
            existing.setPlatform(signal.getPlatform());
            changed = true;
        }
        if (!equalsSafe(existing.getPrimaryCorrelationKey(), signal.getCorrelationKey())) {
            existing.setPrimaryCorrelationKey(signal.getCorrelationKey());
            changed = true;
        }
        if (!equalsSafe(existing.getLastSourceKey(), sourceKey)) {
            existing.setLastSourceKey(sourceKey);
            changed = true;
        }
        if (!equalsSafe(existing.getCanonicalInstrumentKey(), canonicalInstrumentKey)) {
            existing.setCanonicalInstrumentKey(canonicalInstrumentKey);
            changed = true;
        }
        if (!equalsSafe(existing.getLastSourceType(), "AUTO_INGEST")) {
            existing.setLastSourceType("AUTO_INGEST");
            changed = true;
        }
        if (isWeakName(existing.getName()) && signal.getInvestmentName() != null) {
            existing.setName(signal.getInvestmentName());
            changed = true;
        }
        if (Holding.ASSET_OTHER.equals(existing.getAssetType())
                && !Holding.ASSET_OTHER.equals(signal.getAssetType())) {
            existing.setAssetType(signal.getAssetType());
            changed = true;
        }

        if (signal.getSignalType() == ParsedInvestmentSignal.SignalType.MATURITY_CREDIT) {
            if (amountCents > 0 && existing.getCurrentValueCents() != amountCents) {
                existing.setCurrentValueCents(amountCents);
                changed = true;
            }
        } else if (amountCents > 0) {
            if (existing.getInvestedAmountCents() == 0
                    || signal.isCorrection()
                    || signal.getSignalType() != ParsedInvestmentSignal.SignalType.INVESTMENT_DEBIT) {
                if (existing.getInvestedAmountCents() != amountCents) {
                    existing.setInvestedAmountCents(amountCents);
                    changed = true;
                }
            }
            long nextCurrent = Math.max(existing.getCurrentValueCents(), amountCents);
            if (signal.isCorrection()) {
                nextCurrent = amountCents;
            }
            if (existing.getCurrentValueCents() != nextCurrent) {
                existing.setCurrentValueCents(nextCurrent);
                changed = true;
            }
        }

        long signalDay = epochDay(signal.getTimestampMillis());
        if (existing.getLastUpdatedEpochDay() < signalDay) {
            existing.setLastUpdatedEpochDay(signalDay);
            changed = true;
        }
        String updatedNotes = mergeAutoTokens(existing.getNotes(), signal);
        if (!equalsSafe(existing.getNotes(), updatedNotes)) {
            existing.setNotes(updatedNotes);
            changed = true;
        }
        return changed;
    }

    @Nullable
    private Holding findByLinkedSource(@NonNull AppDatabase db, @NonNull String sourceKey) {
        return null;
    }

    private void linkSource(
            @NonNull AppDatabase db,
            long investmentId,
            @NonNull String sourceType,
            @NonNull String sourceKey,
            @NonNull String canonicalInstrumentKey,
            @NonNull ParsedInvestmentSignal signal) {
        if (investmentId <= 0) {
            return;
        }
    }

    @NonNull
    private String mergeAutoTokens(@Nullable String currentNotes, @NonNull ParsedInvestmentSignal signal) {
        String notes = currentNotes == null ? "" : currentNotes.trim();
        notes = appendToken(notes, "[auto-source:" + signal.getSourceKey() + "]");
        notes = appendToken(notes, "[auto-corr:" + signal.getCorrelationKey() + "]");
        notes = appendToken(notes, "[auto-signal:" + signal.getSignalType().name() + "]");
        if (signal.isCorrection()) {
            notes = appendToken(notes, "[auto-correction:true]");
        }
        return notes.trim();
    }

    @NonNull
    private String appendToken(@NonNull String notes, @NonNull String token) {
        if (notes.contains(token)) {
            return notes;
        }
        if (notes.isEmpty()) {
            return token;
        }
        return notes + " " + token;
    }

    private long resolveBucketId(
            @NonNull Map<String, Long> bucketIdByType,
            long fallbackBucketId,
            @NonNull String assetType) {
        if (Holding.ASSET_FD.equals(assetType) || Holding.ASSET_RD.equals(assetType)) {
            return getBucketId(bucketIdByType, Bucket.TYPE_MEDIUM_TERM, fallbackBucketId);
        }
        if (Holding.ASSET_PPF.equals(assetType) || Holding.ASSET_NPS.equals(assetType)) {
            return getBucketId(bucketIdByType, Bucket.TYPE_RETIREMENT, fallbackBucketId);
        }
        if (Holding.ASSET_SAVINGS.equals(assetType)) {
            return getBucketId(bucketIdByType, Bucket.TYPE_EMERGENCY, fallbackBucketId);
        }
        return getBucketId(bucketIdByType, Bucket.TYPE_LONG_TERM, fallbackBucketId);
    }

    private long getBucketId(@NonNull Map<String, Long> bucketIdByType, @NonNull String type, long fallback) {
        Long id = bucketIdByType.get(type);
        return id == null ? fallback : id;
    }

    @NonNull
    private String chooseInvestmentName(@NonNull ParsedInvestmentSignal signal) {
        if (signal.getInvestmentName() != null && !signal.getInvestmentName().trim().isEmpty()) {
            return signal.getInvestmentName().trim();
        }
        if (signal.getPlatform() != null && !signal.getPlatform().trim().isEmpty()) {
            return signal.getPlatform().trim() + " Holding";
        }
        return "Holding";
    }

    private boolean isWeakName(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        String normalized = value.trim().toLowerCase(Locale.US);
        return normalized.equals("investment")
                || normalized.equals("investment allocation")
                || normalized.equals("investment units")
                || normalized.equals("fixed deposit")
                || normalized.equals("recurring deposit")
                || normalized.equals("fd allocation")
                || normalized.equals("rd allocation")
                || normalized.endsWith(" fixed deposit")
                || normalized.endsWith(" recurring deposit");
    }

    private long epochDay(long timestampMillis) {
        return Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay();
    }

    @Nullable
    private String extractToken(@NonNull Pattern pattern, @NonNull String notes) {
        Matcher matcher = pattern.matcher(notes);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @NonNull
    private String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("\\s+", " ").trim();
    }

    private boolean equalsSafe(@Nullable String a, @Nullable String b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
}
