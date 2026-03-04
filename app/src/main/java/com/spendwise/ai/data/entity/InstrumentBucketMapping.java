package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Learn-once mapping memory for bucket assignment.
 */
@Entity(tableName = "instrument_bucket_mappings", foreignKeys = {
        @ForeignKey(entity = Bucket.class, parentColumns = "id", childColumns = "bucketId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("bucketId"),
        @Index(value = { "instrumentPattern", "platformPattern", "assetType" }, unique = true)
})
public class InstrumentBucketMapping {

    public static final String SOURCE_USER = "USER";
    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_RULE = "RULE";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String instrumentPattern;

    @Nullable
    private String platformPattern;

    @NonNull
    private String assetType;

    private long bucketId;

    private double confidenceBoost;

    private long updatedAtMillis;

    @NonNull
    private String source;

    public InstrumentBucketMapping(
            @NonNull String instrumentPattern,
            @Nullable String platformPattern,
            @NonNull String assetType,
            long bucketId,
            double confidenceBoost,
            long updatedAtMillis,
            @NonNull String source) {
        this.instrumentPattern = instrumentPattern;
        this.platformPattern = platformPattern;
        this.assetType = assetType;
        this.bucketId = bucketId;
        this.confidenceBoost = confidenceBoost;
        this.updatedAtMillis = updatedAtMillis;
        this.source = source;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getInstrumentPattern() {
        return instrumentPattern;
    }

    public void setInstrumentPattern(@NonNull String instrumentPattern) {
        this.instrumentPattern = instrumentPattern;
    }

    @Nullable
    public String getPlatformPattern() {
        return platformPattern;
    }

    public void setPlatformPattern(@Nullable String platformPattern) {
        this.platformPattern = platformPattern;
    }

    @NonNull
    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(@NonNull String assetType) {
        this.assetType = assetType;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public double getConfidenceBoost() {
        return confidenceBoost;
    }

    public void setConfidenceBoost(double confidenceBoost) {
        this.confidenceBoost = confidenceBoost;
    }

    public long getUpdatedAtMillis() {
        return updatedAtMillis;
    }

    public void setUpdatedAtMillis(long updatedAtMillis) {
        this.updatedAtMillis = updatedAtMillis;
    }

    @NonNull
    public String getSource() {
        return source;
    }

    public void setSource(@NonNull String source) {
        this.source = source;
    }
}
