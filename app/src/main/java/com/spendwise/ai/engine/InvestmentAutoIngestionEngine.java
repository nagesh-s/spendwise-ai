package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.Investment;
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

        Map<String, Long> bucketIdByType = new HashMap<>();
        long fallbackBucketId = buckets.get(0).getId();
        for (Bucket bucket : buckets) {
            bucketIdByType.put(bucket.getType(), bucket.getId());
        }
        BucketAssignmentEngine assignmentEngine = new BucketAssignmentEngine();

        List<Investment> allInvestments = db.investmentDao().getAllInvestmentsSync();
        Map<String, Investment> bySource = new HashMap<>();
        Map<String, Investment> byCorrelation = new HashMap<>();
        indexExistingInvestments(allInvestments, bySource, byCorrelation);

        EventIngestionPipeline pipeline = new EventIngestionPipeline();

        for (ParsedInvestmentSignal signal : orderedSignals) {
            if (signal.getAmountCents() <= 0
                    && signal.getSignalType() != ParsedInvestmentSignal.SignalType.MATURITY_CREDIT) {
                result.skipped++;
                continue;
            }

            Investment matched = bySource.get(signal.getSourceKey());
            if (matched == null) {
                matched = byCorrelation.get(signal.getCorrelationKey());
            }
            if (matched == null) {
                matched = findFuzzyMatch(allInvestments, signal);
            }

            if (matched != null) {
                if (applyUpdate(matched, signal)) {
                    db.investmentDao().update(matched);
                    result.updated++;
                } else {
                    result.skipped++;
                }
                matched.setNotes(mergeAutoTokens(matched.getNotes(), signal));
                indexInvestment(matched, bySource, byCorrelation);
                result.investmentIdBySourceKey.put(signal.getSourceKey(), matched.getId());
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

            String sourceKey = signal.getSourceKey();
            EventIngestionPipeline.IngestResult event = pipeline.ingest(
                    db,
                    IngestionEvent.DOMAIN_INVESTMENT,
                    IngestionEvent.SOURCE_SMS,
                    sourceKey,
                    signal.getCorrelationKey(),
                    signal.getSignalType().name(),
                    signal.getAmountCents(),
                    signal.getTimestampMillis(),
                    signal.getSender(),
                    signal.getRawMessage(), // payloadHashBase
                    signal.getRawMessage()); // rawMessage
            long baseAmount = Math.max(0L, signal.getAmountCents());
            Investment investment = new Investment(
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
            investment.setLastSourceKey(signal.getSourceKey());
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
            allInvestments.add(investment);
            indexInvestment(investment, bySource, byCorrelation);
            result.inserted++;
            result.investmentIdBySourceKey.put(signal.getSourceKey(), insertedId);
        }
        return result;
    }

    private void indexExistingInvestments(
            @NonNull List<Investment> investments,
            @NonNull Map<String, Investment> bySource,
            @NonNull Map<String, Investment> byCorrelation) {
        for (Investment investment : investments) {
            indexInvestment(investment, bySource, byCorrelation);
        }
    }

    private void indexInvestment(
            @NonNull Investment investment,
            @NonNull Map<String, Investment> bySource,
            @NonNull Map<String, Investment> byCorrelation) {
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
    private Investment findFuzzyMatch(
            @NonNull List<Investment> allInvestments,
            @NonNull ParsedInvestmentSignal signal) {
        long signalDay = epochDay(signal.getTimestampMillis());
        String normalizedSignalName = normalize(signal.getInvestmentName());
        String normalizedPlatform = normalize(signal.getPlatform());

        for (Investment candidate : allInvestments) {
            boolean assetMatches = normalize(candidate.getAssetType()).equals(normalize(signal.getAssetType()))
                    || Investment.ASSET_OTHER.equals(candidate.getAssetType())
                    || Investment.ASSET_OTHER.equals(signal.getAssetType());
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
            boolean isFixedDeposit = Investment.ASSET_FD.equals(candidate.getAssetType())
                    || Investment.ASSET_RD.equals(candidate.getAssetType());
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

    private boolean applyUpdate(@NonNull Investment existing, @NonNull ParsedInvestmentSignal signal) {
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
        if (!equalsSafe(existing.getLastSourceKey(), signal.getSourceKey())) {
            existing.setLastSourceKey(signal.getSourceKey());
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
        if (Investment.ASSET_OTHER.equals(existing.getAssetType())
                && !Investment.ASSET_OTHER.equals(signal.getAssetType())) {
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
        if (Investment.ASSET_FD.equals(assetType) || Investment.ASSET_RD.equals(assetType)) {
            return getBucketId(bucketIdByType, Bucket.TYPE_MEDIUM_TERM, fallbackBucketId);
        }
        if (Investment.ASSET_PPF.equals(assetType) || Investment.ASSET_NPS.equals(assetType)) {
            return getBucketId(bucketIdByType, Bucket.TYPE_RETIREMENT, fallbackBucketId);
        }
        if (Investment.ASSET_SAVINGS.equals(assetType)) {
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
            return signal.getPlatform().trim() + " Investment";
        }
        return "Investment";
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
