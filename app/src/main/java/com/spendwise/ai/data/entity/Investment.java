package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tracks an individual investment holding within a bucket.
 * Each investment has a human-readable "job title" (e.g., "Car Upgrade Fund")
 * and a platform label (e.g., "Upstox", "Groww") to solve the "Upstox Effect" —
 * where all investments in a single app look like one undifferentiated number.
 *
 * Supports manual entry and future auto-fetch capabilities.
 */
@Entity(tableName = "investments", foreignKeys = {
        @ForeignKey(entity = Bucket.class, parentColumns = "id", childColumns = "bucketId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("bucketId")
})
public class Investment {

    // Asset type constants
    public static final String ASSET_STOCK = "STOCK";
    public static final String ASSET_MF = "MUTUAL_FUND";
    public static final String ASSET_ETF = "ETF";
    public static final String ASSET_FD = "FIXED_DEPOSIT";
    public static final String ASSET_RD = "RECURRING_DEPOSIT";
    public static final String ASSET_PPF = "PPF";
    public static final String ASSET_NPS = "NPS";
    public static final String ASSET_GOLD = "GOLD";
    public static final String ASSET_SAVINGS = "SAVINGS_ACCOUNT";
    public static final String ASSET_OTHER = "OTHER";

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long bucketId;

    @NonNull
    private String name; // "HDFC Nifty 50 Index Fund"

    /**
     * Human-readable purpose label for this investment.
     * E.g., "Retirement Fund", "Car Upgrade Fund", "Europe Trip Savings".
     * Solves the Upstox Effect — makes each investment feel purposeful.
     */
    @Nullable
    private String jobTitle;

    /**
     * Which platform/broker this investment lives on.
     * E.g., "Upstox", "Groww", "Zerodha", "ICICI Direct", "Bank FD", "Manual".
     * Helps users reconcile with external apps.
     */
    @Nullable
    private String platform;

    @NonNull
    private String assetType; // ASSET_MF, ASSET_FD, etc.

    /**
     * Principal amount invested in cents.
     */
    private long investedAmountCents;

    /**
     * Current market value in cents (manually updated or auto-fetched).
     */
    private long currentValueCents;

    /**
     * Profit/Loss percentage. Null if not computed yet.
     */
    @Nullable
    private Double returnPercent;

    /**
     * Maturity date for FD/RD as epoch day. Null for non-maturing assets.
     */
    @Nullable
    private Long maturityDateEpochDay;

    /**
     * Interest rate for FD/RD (e.g., 7.5 for 7.5%). Null for market assets.
     */
    @Nullable
    private Double interestRate;

    /**
     * When the current value was last refreshed, as epoch day.
     */
    private long lastUpdatedEpochDay;

    @Nullable
    private String notes;

    /**
     * False = manual entry, True = auto-fetched from an external source.
     */
    private boolean isAutoFetched;

    /**
     * True if this is a recurring SIP (Systematic Investment Plan).
     */
    private boolean isSip;

    /**
     * Monthly SIP amount in cents. Null if not a SIP.
     */
    @Nullable
    private Long sipAmountCents;

    /**
     * Primary reconciliation key linking this holding to ingestion events and
     * expected SIP occurrences.
     */
    @Nullable
    private String primaryCorrelationKey;

    /**
     * Last source type that updated this holding (SMS/NOTIFICATION/MANUAL/SCHEDULE).
     */
    @Nullable
    private String lastSourceType;

    /**
     * Last source event key used for idempotent updates.
     */
    @Nullable
    private String lastSourceKey;

    public Investment(long bucketId, @NonNull String name, @NonNull String assetType,
            long investedAmountCents, long currentValueCents, long lastUpdatedEpochDay) {
        this.bucketId = bucketId;
        this.name = name;
        this.assetType = assetType;
        this.investedAmountCents = investedAmountCents;
        this.currentValueCents = currentValueCents;
        this.lastUpdatedEpochDay = lastUpdatedEpochDay;
        this.isAutoFetched = false;
        this.isSip = false;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    @Nullable
    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(@Nullable String jobTitle) {
        this.jobTitle = jobTitle;
    }

    @Nullable
    public String getPlatform() {
        return platform;
    }

    public void setPlatform(@Nullable String platform) {
        this.platform = platform;
    }

    @NonNull
    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(@NonNull String assetType) {
        this.assetType = assetType;
    }

    public long getInvestedAmountCents() {
        return investedAmountCents;
    }

    public void setInvestedAmountCents(long investedAmountCents) {
        this.investedAmountCents = investedAmountCents;
    }

    public long getCurrentValueCents() {
        return currentValueCents;
    }

    public void setCurrentValueCents(long currentValueCents) {
        this.currentValueCents = currentValueCents;
    }

    @Nullable
    public Double getReturnPercent() {
        return returnPercent;
    }

    public void setReturnPercent(@Nullable Double returnPercent) {
        this.returnPercent = returnPercent;
    }

    @Nullable
    public Long getMaturityDateEpochDay() {
        return maturityDateEpochDay;
    }

    public void setMaturityDateEpochDay(@Nullable Long maturityDateEpochDay) {
        this.maturityDateEpochDay = maturityDateEpochDay;
    }

    @Nullable
    public Double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(@Nullable Double interestRate) {
        this.interestRate = interestRate;
    }

    public long getLastUpdatedEpochDay() {
        return lastUpdatedEpochDay;
    }

    public void setLastUpdatedEpochDay(long lastUpdatedEpochDay) {
        this.lastUpdatedEpochDay = lastUpdatedEpochDay;
    }

    @Nullable
    public String getNotes() {
        return notes;
    }

    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    public boolean isAutoFetched() {
        return isAutoFetched;
    }

    public void setAutoFetched(boolean autoFetched) {
        isAutoFetched = autoFetched;
    }

    public boolean isSip() {
        return isSip;
    }

    public void setSip(boolean sip) {
        isSip = sip;
    }

    @Nullable
    public Long getSipAmountCents() {
        return sipAmountCents;
    }

    public void setSipAmountCents(@Nullable Long sipAmountCents) {
        this.sipAmountCents = sipAmountCents;
    }

    @Nullable
    public String getPrimaryCorrelationKey() {
        return primaryCorrelationKey;
    }

    public void setPrimaryCorrelationKey(@Nullable String primaryCorrelationKey) {
        this.primaryCorrelationKey = primaryCorrelationKey;
    }

    @Nullable
    public String getLastSourceType() {
        return lastSourceType;
    }

    public void setLastSourceType(@Nullable String lastSourceType) {
        this.lastSourceType = lastSourceType;
    }

    @Nullable
    public String getLastSourceKey() {
        return lastSourceKey;
    }

    public void setLastSourceKey(@Nullable String lastSourceKey) {
        this.lastSourceKey = lastSourceKey;
    }

    /**
     * Calculate P&L amount in cents.
     */
    public long getProfitLossCents() {
        return currentValueCents - investedAmountCents;
    }

    /**
     * Calculate P&L as a percentage.
     */
    public double calculateReturnPercent() {
        if (investedAmountCents <= 0)
            return 0.0;
        return ((double) (currentValueCents - investedAmountCents) / investedAmountCents) * 100.0;
    }
}
