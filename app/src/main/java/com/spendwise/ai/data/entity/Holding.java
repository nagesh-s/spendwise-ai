package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "holdings", foreignKeys = {
        @ForeignKey(entity = Bucket.class, parentColumns = "id", childColumns = "bucketId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Provider.class, parentColumns = "id", childColumns = "providerId",
                onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index("bucketId"),
        @Index("providerId"),
        @Index(value = { "canonicalInstrumentKey" }, unique = true)
})
public class Holding {

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
    @Nullable private Long providerId;
    @NonNull private String name;
    @NonNull private String normalizedName;
    @Nullable private String jobTitle;
    @Nullable private String platform;
    @NonNull private String assetType;
    @Nullable private String folioAccountHint;
    private long totalContributionsCents;
    private long totalWithdrawalsCents;
    private long totalIncomeCents;
    private long totalFeesCents;
    private long totalTaxesCents;
    private long netCashInvestedCents;
    private long currentValueCents;
    private long absolutePnlCents;
    @Nullable private Long maturityDateEpochDay;
    @Nullable private Double interestRate;
    private long lastUpdatedEpochDay;
    @Nullable private String notes;
    private boolean isAutoFetched;
    private boolean isSip;
    @Nullable private Long sipAmountCents;
    @Nullable private String primaryCorrelationKey;
    @Nullable private String canonicalInstrumentKey;

    public Holding(long bucketId, @NonNull String name, @NonNull String normalizedName,
            @NonNull String assetType, long lastUpdatedEpochDay) {
        this.bucketId = bucketId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.assetType = assetType;
        this.lastUpdatedEpochDay = lastUpdatedEpochDay;
    }

    public Holding(long bucketId, @NonNull String name, @NonNull String assetType,
            long investedAmountCents, long currentValueCents, long lastUpdatedEpochDay) {
        this(bucketId, name, normalize(name), assetType, lastUpdatedEpochDay);
        this.netCashInvestedCents = Math.max(0L, investedAmountCents);
        this.currentValueCents = Math.max(0L, currentValueCents);
        this.absolutePnlCents = this.currentValueCents - this.netCashInvestedCents;
    }

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public long getBucketId() { return bucketId; }
    public void setBucketId(long value) { bucketId = value; }
    @Nullable public Long getProviderId() { return providerId; }
    public void setProviderId(@Nullable Long value) { providerId = value; }
    @NonNull public String getName() { return name; }
    public void setName(@NonNull String value) { name = value; }
    @NonNull public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(@NonNull String value) { normalizedName = value; }
    @Nullable public String getJobTitle() { return jobTitle; }
    public void setJobTitle(@Nullable String value) { jobTitle = value; }
    @Nullable public String getPlatform() { return platform; }
    public void setPlatform(@Nullable String value) { platform = value; }
    @NonNull public String getAssetType() { return assetType; }
    public void setAssetType(@NonNull String value) { assetType = value; }
    @Nullable public String getFolioAccountHint() { return folioAccountHint; }
    public void setFolioAccountHint(@Nullable String value) { folioAccountHint = value; }
    public long getTotalContributionsCents() { return totalContributionsCents; }
    public void setTotalContributionsCents(long value) { totalContributionsCents = value; }
    public long getTotalWithdrawalsCents() { return totalWithdrawalsCents; }
    public void setTotalWithdrawalsCents(long value) { totalWithdrawalsCents = value; }
    public long getTotalIncomeCents() { return totalIncomeCents; }
    public void setTotalIncomeCents(long value) { totalIncomeCents = value; }
    public long getTotalFeesCents() { return totalFeesCents; }
    public void setTotalFeesCents(long value) { totalFeesCents = value; }
    public long getTotalTaxesCents() { return totalTaxesCents; }
    public void setTotalTaxesCents(long value) { totalTaxesCents = value; }
    public long getNetCashInvestedCents() { return netCashInvestedCents; }
    public void setNetCashInvestedCents(long value) { netCashInvestedCents = value; }
    public long getCurrentValueCents() { return currentValueCents; }
    public void setCurrentValueCents(long value) { currentValueCents = value; }
    public long getAbsolutePnlCents() { return absolutePnlCents; }
    public void setAbsolutePnlCents(long value) { absolutePnlCents = value; }
    @Nullable public Long getMaturityDateEpochDay() { return maturityDateEpochDay; }
    public void setMaturityDateEpochDay(@Nullable Long value) { maturityDateEpochDay = value; }
    @Nullable public Double getInterestRate() { return interestRate; }
    public void setInterestRate(@Nullable Double value) { interestRate = value; }
    public long getLastUpdatedEpochDay() { return lastUpdatedEpochDay; }
    public void setLastUpdatedEpochDay(long value) { lastUpdatedEpochDay = value; }
    @Nullable public String getNotes() { return notes; }
    public void setNotes(@Nullable String value) { notes = value; }
    public boolean isAutoFetched() { return isAutoFetched; }
    public void setAutoFetched(boolean value) { isAutoFetched = value; }
    public boolean isSip() { return isSip; }
    public void setSip(boolean value) { isSip = value; }
    @Nullable public Long getSipAmountCents() { return sipAmountCents; }
    public void setSipAmountCents(@Nullable Long value) { sipAmountCents = value; }
    @Nullable public String getPrimaryCorrelationKey() { return primaryCorrelationKey; }
    public void setPrimaryCorrelationKey(@Nullable String value) { primaryCorrelationKey = value; }
    @Nullable public String getCanonicalInstrumentKey() { return canonicalInstrumentKey; }
    public void setCanonicalInstrumentKey(@Nullable String value) { canonicalInstrumentKey = value; }

    // Transitional read aliases used by existing bucket summary code.
    public long getInvestedAmountCents() { return netCashInvestedCents; }
    public void setInvestedAmountCents(long value) { netCashInvestedCents = value; }
    @Nullable public Double getReturnPercent() {
        return netCashInvestedCents == 0 ? null
                : (absolutePnlCents * 100d) / netCashInvestedCents;
    }
    public void setReturnPercent(@Nullable Double ignored) { }
    public long getProfitLossCents() { return absolutePnlCents; }
    public double calculateReturnPercent() {
        return netCashInvestedCents == 0 ? 0d : absolutePnlCents * 100d / netCashInvestedCents;
    }
    @Nullable public String getLastSourceType() { return null; }
    public void setLastSourceType(@Nullable String ignored) { }
    @Nullable public String getLastSourceKey() { return null; }
    public void setLastSourceKey(@Nullable String ignored) { }

    @NonNull
    private static String normalize(@NonNull String value) {
        String normalized = value.toLowerCase(java.util.Locale.US)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
        return normalized.isEmpty() ? "holding" : normalized;
    }
}
