package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Recurring investment schedule (SIP or similar).
 */
@Entity(tableName = "investment_schedules", foreignKeys = {
        @ForeignKey(entity = Bucket.class, parentColumns = "id", childColumns = "bucketId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Holding.class, parentColumns = "id", childColumns = "holdingId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index("bucketId"),
        @Index("holdingId"),
        @Index("isActive")
})
public class InvestmentSchedule {

    public static final String FREQUENCY_DAILY = "DAILY";
    public static final String FREQUENCY_EVERY_2_DAYS = "EVERY_2_DAYS";
    public static final String FREQUENCY_EVERY_3_DAYS = "EVERY_3_DAYS";
    public static final String FREQUENCY_WEEKLY = "WEEKLY";
    public static final String FREQUENCY_MONTHLY = "MONTHLY";
    public static final String FREQUENCY_YEARLY = "YEARLY";

    public static final String SOURCE_USER = "USER";
    public static final String SOURCE_IMPORTED = "IMPORTED";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    private long bucketId;

    @Nullable
    private Long holdingId;

    @NonNull
    private String assetType;

    @Nullable
    private String platform;

    private long amountCents;

    @NonNull
    private String frequency;

    private int dayOfPeriod;

    private long startEpochDay;

    @Nullable
    private Long endEpochDay;

    private boolean isActive;

    @Nullable
    private String source;

    @Nullable
    private String notes;

    public InvestmentSchedule(
            @NonNull String name,
            long bucketId,
            @NonNull String assetType,
            @Nullable String platform,
            long amountCents,
            @NonNull String frequency,
            int dayOfPeriod,
            long startEpochDay) {
        this.name = name;
        this.bucketId = bucketId;
        this.assetType = assetType;
        this.platform = platform;
        this.amountCents = amountCents;
        this.frequency = frequency;
        this.dayOfPeriod = dayOfPeriod;
        this.startEpochDay = startEpochDay;
        this.isActive = true;
        this.source = SOURCE_USER;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    @Nullable
    public Long getHoldingId() {
        return holdingId;
    }

    public void setHoldingId(@Nullable Long holdingId) {
        this.holdingId = holdingId;
    }

    @NonNull
    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(@NonNull String assetType) {
        this.assetType = assetType;
    }

    @Nullable
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(@Nullable String platform) {
        this.platform = platform;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    @NonNull
    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(@NonNull String frequency) {
        this.frequency = frequency;
    }

    public int getDayOfPeriod() {
        return dayOfPeriod;
    }

    public void setDayOfPeriod(int dayOfPeriod) {
        this.dayOfPeriod = dayOfPeriod;
    }

    public long getStartEpochDay() {
        return startEpochDay;
    }

    public void setStartEpochDay(long startEpochDay) {
        this.startEpochDay = startEpochDay;
    }

    @Nullable
    public Long getEndEpochDay() {
        return endEpochDay;
    }

    public void setEndEpochDay(@Nullable Long endEpochDay) {
        this.endEpochDay = endEpochDay;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Nullable
    public String getSource() {
        return source;
    }

    public void setSource(@Nullable String source) {
        this.source = source;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }
}
