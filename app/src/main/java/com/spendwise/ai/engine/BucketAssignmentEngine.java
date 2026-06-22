package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.InstrumentBucketMapping;
import com.spendwise.ai.data.entity.Holding;
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
    private final SemanticBucketClassifier classifier;

    public BucketAssignmentEngine(android.content.Context context) {
        this.classifier = new SemanticBucketClassifier(context);
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

        OnDeviceInference inference = inferOnDevice(signal, buckets);
        if (inference.confidence >= 0.60d) {
            long bucketId = resolveBucketIdByType(buckets, inference.bucketType, fallbackBucketId(buckets));
            return new AssignmentResult(bucketId, inference.bucketType, inference.confidence, "LITE_ML", false);
        }

        long unassignedId = getOrCreateUnassignedBucketId(db);
        return new AssignmentResult(unassignedId, Bucket.TYPE_UNASSIGNED, inference.confidence, "UNSURE", true);
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

    public void learnFromHolding(
            @NonNull AppDatabase db,
            @NonNull com.spendwise.ai.data.entity.Holding holding,
            long bucketId,
            @NonNull String source) {
        String instrumentPattern = normalizeOrEmpty(holding.getName());
        if (instrumentPattern.isEmpty()) {
            instrumentPattern = normalizeOrEmpty(holding.getPlatform());
        }
        if (instrumentPattern.isEmpty()) {
            instrumentPattern = "investment";
        }
        InstrumentBucketMapping mapping = new InstrumentBucketMapping(
                instrumentPattern,
                normalizeOrNull(holding.getPlatform()),
                holding.getAssetType(),
                bucketId,
                0.9, // High confidence for manual sorts
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

    private long getOrCreateUnassignedBucketId(AppDatabase db) {
        Bucket unassigned = db.bucketDao().getByType("UNASSIGNED");
        if (unassigned == null) {
            unassigned = new Bucket("Unassigned", "UNASSIGNED", 0, android.graphics.Color.GRAY, 99, false);
            long id = db.bucketDao().insert(unassigned);
            return id;
        }
        return unassigned.getId();
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
        if (Holding.ASSET_FD.equals(assetType) || Holding.ASSET_RD.equals(assetType)) {
            return Bucket.TYPE_MEDIUM_TERM;
        }
        if (Holding.ASSET_PPF.equals(assetType) || Holding.ASSET_NPS.equals(assetType)) {
            return Bucket.TYPE_RETIREMENT;
        }
        if (Holding.ASSET_SAVINGS.equals(assetType)) {
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
    private OnDeviceInference inferOnDevice(@NonNull ParsedInvestmentSignal signal, @NonNull List<Bucket> buckets) {
        String label = (normalizeOrEmpty(signal.getInvestmentName()) + " " + normalizeOrEmpty(signal.getPlatform()))
                .trim();
        String assetType = signal.getAssetType();

        double bestScore = -1.0;
        String bestType = Bucket.TYPE_LONG_TERM;

        // Try cosine similarity for all buckets
        for (Bucket bucket : buckets) {
            String bucketDesc = bucket.getName() + " " + bucket.getType() + " ";
            // Enrich with regex keywords to "train" the zero-shot model
            if (Bucket.TYPE_RETIREMENT.equals(bucket.getType())) {
                bucketDesc += "nps pension retire provident fund ppf";
            } else if (Bucket.TYPE_EMERGENCY.equals(bucket.getType())) {
                bucketDesc += "emergency liquid savings account reserve";
            } else if (Bucket.TYPE_MEDIUM_TERM.equals(bucket.getType())) {
                bucketDesc += "car wedding education fixed recurring deposit fd rd";
            } else if (Bucket.TYPE_SHORT_TERM.equals(bucket.getType())) {
                bucketDesc += "travel vacation short liquid";
            } else if (Bucket.TYPE_LONG_TERM.equals(bucket.getType())) {
                bucketDesc += "mutual fund etf stock shares equity growth wealth";
            }

            double score = classifier.computeSimilarity(label + " " + assetType, bucketDesc.trim());
            if (score > bestScore) {
                bestScore = score;
                bestType = bucket.getType();
            }
        }

        // If the model fails or returns nonsense, fallback to legacy rules
        if (bestScore < 0) {
            if (label.contains("retire") || label.contains("nps") || label.contains("pension") || Holding.ASSET_PPF.equals(assetType) || Holding.ASSET_NPS.equals(assetType)) {
                return new OnDeviceInference(Bucket.TYPE_RETIREMENT, 0.9d);
            }
            if (label.contains("emergency") || label.contains("liquid") || Holding.ASSET_SAVINGS.equals(assetType)) {
                return new OnDeviceInference(Bucket.TYPE_EMERGENCY, 0.85d);
            }
            if (label.contains("car") || label.contains("wedding") || label.contains("education") || Holding.ASSET_FD.equals(assetType) || Holding.ASSET_RD.equals(assetType)) {
                return new OnDeviceInference(Bucket.TYPE_MEDIUM_TERM, 0.8d);
            }
            if (label.contains("travel") || label.contains("vacation")) {
                return new OnDeviceInference(Bucket.TYPE_SHORT_TERM, 0.75d);
            }
            if (Holding.ASSET_MF.equals(assetType) || Holding.ASSET_ETF.equals(assetType) || Holding.ASSET_STOCK.equals(assetType)) {
                return new OnDeviceInference(Bucket.TYPE_LONG_TERM, 0.6d);
            }
            return new OnDeviceInference(Bucket.TYPE_LONG_TERM, 0.45d);
        }

        return new OnDeviceInference(bestType, bestScore);
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
