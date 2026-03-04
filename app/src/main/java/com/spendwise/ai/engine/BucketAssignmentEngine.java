package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.InstrumentBucketMapping;
import com.spendwise.ai.data.entity.Investment;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

import java.util.List;
import java.util.Locale;

/**
 * AI-primary (on-device heuristic) assignment with mapping memory and rules fallback.
 */
public class BucketAssignmentEngine {

    public static class AssignmentResult {
        public final long bucketId;
        @NonNull
        public final String bucketType;
        public final double confidence;
        @NonNull
        public final String strategy;
        public final boolean needsUserConfirmation;

        AssignmentResult(long bucketId, @NonNull String bucketType, double confidence,
                @NonNull String strategy, boolean needsUserConfirmation) {
            this.bucketId = bucketId;
            this.bucketType = bucketType;
            this.confidence = confidence;
            this.strategy = strategy;
            this.needsUserConfirmation = needsUserConfirmation;
        }
    }

    @NonNull
    public AssignmentResult assign(
            @NonNull AppDatabase db,
            @NonNull ParsedInvestmentSignal signal,
            @NonNull List<Bucket> buckets) {
        AssignmentResult mapped = matchLearnedMapping(db, signal, buckets);
        if (mapped != null) {
            return mapped;
        }

        OnDeviceInference inference = inferOnDevice(signal);
        if (inference.confidence >= 0.65d) {
            long bucketId = resolveBucketIdByType(buckets, inference.bucketType, fallbackBucketId(buckets));
            return new AssignmentResult(bucketId, inference.bucketType, inference.confidence, "AI", false);
        }

        String fallbackType = resolveByRules(signal.getAssetType());
        long fallbackId = resolveBucketIdByType(buckets, fallbackType, fallbackBucketId(buckets));
        return new AssignmentResult(fallbackId, fallbackType, inference.confidence, "RULE_FALLBACK", true);
    }

    public void learnMapping(
            @NonNull AppDatabase db,
            @NonNull ParsedInvestmentSignal signal,
            long bucketId,
            @NonNull String source) {
        String instrumentPattern = normalizeOrEmpty(signal.getInvestmentName());
        if (instrumentPattern.isEmpty()) {
            instrumentPattern = normalizeOrEmpty(signal.getPlatform());
        }
        if (instrumentPattern.isEmpty()) {
            instrumentPattern = "investment";
        }
        InstrumentBucketMapping mapping = new InstrumentBucketMapping(
                instrumentPattern,
                normalizeOrNull(signal.getPlatform()),
                signal.getAssetType(),
                bucketId,
                0.8,
                System.currentTimeMillis(),
                source);
        db.instrumentBucketMappingDao().upsert(mapping);
    }

    @Nullable
    private AssignmentResult matchLearnedMapping(
            @NonNull AppDatabase db,
            @NonNull ParsedInvestmentSignal signal,
            @NonNull List<Bucket> buckets) {
        List<InstrumentBucketMapping> candidates = db.instrumentBucketMappingDao()
                .getByAssetTypeSync(signal.getAssetType());
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        String signalName = normalizeOrEmpty(signal.getInvestmentName());
        String signalPlatform = normalizeOrEmpty(signal.getPlatform());

        for (InstrumentBucketMapping mapping : candidates) {
            String namePattern = normalizeOrEmpty(mapping.getInstrumentPattern());
            String platformPattern = normalizeOrEmpty(mapping.getPlatformPattern());
            boolean nameMatch = !namePattern.isEmpty() && signalName.contains(namePattern);
            boolean platformMatch = platformPattern.isEmpty() || signalPlatform.contains(platformPattern);
            if (nameMatch && platformMatch) {
                String bucketType = resolveBucketType(buckets, mapping.getBucketId(), Bucket.TYPE_LONG_TERM);
                return new AssignmentResult(mapping.getBucketId(), bucketType, 0.95d, "LEARNED", false);
            }
        }
        return null;
    }

    private long fallbackBucketId(@NonNull List<Bucket> buckets) {
        return buckets.isEmpty() ? -1L : buckets.get(0).getId();
    }

    private long resolveBucketIdByType(@NonNull List<Bucket> buckets, @NonNull String type, long fallback) {
        for (Bucket bucket : buckets) {
            if (type.equals(bucket.getType())) {
                return bucket.getId();
            }
        }
        return fallback;
    }

    @NonNull
    private String resolveBucketType(@NonNull List<Bucket> buckets, long bucketId, @NonNull String fallbackType) {
        for (Bucket bucket : buckets) {
            if (bucket.getId() == bucketId) {
                return bucket.getType();
            }
        }
        return fallbackType;
    }

    @NonNull
    private String resolveByRules(@NonNull String assetType) {
        if (Investment.ASSET_FD.equals(assetType) || Investment.ASSET_RD.equals(assetType)) {
            return Bucket.TYPE_MEDIUM_TERM;
        }
        if (Investment.ASSET_PPF.equals(assetType) || Investment.ASSET_NPS.equals(assetType)) {
            return Bucket.TYPE_RETIREMENT;
        }
        if (Investment.ASSET_SAVINGS.equals(assetType)) {
            return Bucket.TYPE_EMERGENCY;
        }
        return Bucket.TYPE_LONG_TERM;
    }

    private static class OnDeviceInference {
        @NonNull
        final String bucketType;
        final double confidence;

        OnDeviceInference(@NonNull String bucketType, double confidence) {
            this.bucketType = bucketType;
            this.confidence = confidence;
        }
    }

    @NonNull
    private OnDeviceInference inferOnDevice(@NonNull ParsedInvestmentSignal signal) {
        String label = (normalizeOrEmpty(signal.getInvestmentName()) + " " + normalizeOrEmpty(signal.getPlatform()))
                .trim();
        String assetType = signal.getAssetType();

        if (label.contains("retire") || label.contains("nps") || label.contains("pension")) {
            return new OnDeviceInference(Bucket.TYPE_RETIREMENT, 0.9d);
        }
        if (label.contains("emergency") || label.contains("liquid") || Investment.ASSET_SAVINGS.equals(assetType)) {
            return new OnDeviceInference(Bucket.TYPE_EMERGENCY, 0.85d);
        }
        if (label.contains("car") || label.contains("wedding") || label.contains("education")
                || Investment.ASSET_FD.equals(assetType) || Investment.ASSET_RD.equals(assetType)) {
            return new OnDeviceInference(Bucket.TYPE_MEDIUM_TERM, 0.8d);
        }
        if (label.contains("travel") || label.contains("vacation")) {
            return new OnDeviceInference(Bucket.TYPE_SHORT_TERM, 0.75d);
        }
        if (Investment.ASSET_PPF.equals(assetType) || Investment.ASSET_NPS.equals(assetType)) {
            return new OnDeviceInference(Bucket.TYPE_RETIREMENT, 0.8d);
        }
        if (Investment.ASSET_MF.equals(assetType) || Investment.ASSET_ETF.equals(assetType)
                || Investment.ASSET_STOCK.equals(assetType)) {
            return new OnDeviceInference(Bucket.TYPE_LONG_TERM, 0.6d);
        }
        return new OnDeviceInference(Bucket.TYPE_LONG_TERM, 0.45d);
    }

    @NonNull
    private String normalizeOrEmpty(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("\\s+", " ").trim();
    }

    @Nullable
    private String normalizeOrNull(@Nullable String value) {
        String normalized = normalizeOrEmpty(value);
        return normalized.isEmpty() ? null : normalized;
    }
}
