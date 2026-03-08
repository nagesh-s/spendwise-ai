package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a single expense record in the database.
 * 
 * Each expense has an amount (stored in cents to avoid floating point issues),
 * a date (stored as epoch day for efficient date-based queries),
 * an optional description, and links to a category and optionally a merchant.
 */
@Entity(tableName = "expenses", foreignKeys = {
        @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.RESTRICT),
        @ForeignKey(entity = Merchant.class, parentColumns = "id", childColumns = "merchantId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index(value = "dateEpochDay"),
        @Index(value = "categoryId"),
        @Index(value = "merchantId"),
        @Index(value = "isInvestmentTransfer"),
        @Index(value = "recoveryStatus"),
        @Index(value = "canonicalSourceKey", unique = true)
})
public class Expense {

    public static final String RECOVERY_STATUS_NONE = "NONE";
    public static final String RECOVERY_STATUS_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String RECOVERY_STATUS_IGNORED = "IGNORED";
    public static final String RECOVERY_STATUS_CONVERTED = "CONVERTED";

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Date stored as epoch day (days since 1970-01-01).
     * Use LocalDate.toEpochDay() and LocalDate.ofEpochDay() for conversion.
     */
    private long dateEpochDay;

    /**
     * Amount in cents (e.g., ₹123.45 is stored as 12345).
     * Using cents avoids floating-point precision issues.
     */
    private long amountCents;

    @Nullable
    private String description;

    private long categoryId;

    @Nullable
    private Long merchantId;

    /**
     * Path to attached photo/receipt image.
     * Stored as relative path within app's private storage.
     */
    @Nullable
    private String photoPath;

    /**
     * Optional link to the account this expense was made from.
     * Null means no account is assigned (backwards compatible).
     */
    @Nullable
    private Long accountId;

    /**
     * True when this row represents an investment transfer/outflow rather than
     * lifestyle spending. These rows stay visible in history but are excluded
     * from budgeting and spend analytics.
     */
    private boolean isInvestmentTransfer;

    /**
     * Linked holding created or updated from this expense row.
     */
    @Nullable
    private Long linkedInvestmentId;

    /**
     * Tracks the recovery lifecycle for expense -> investment conversion.
     */
    @NonNull
    private String recoveryStatus;

    /**
     * Canonical source key used to keep expense-derived recoveries idempotent.
     */
    @Nullable
    private String canonicalSourceKey;

    // Constructor
    public Expense(long dateEpochDay, long amountCents, @Nullable String description,
            long categoryId, @Nullable Long merchantId) {
        this.dateEpochDay = dateEpochDay;
        this.amountCents = amountCents;
        this.description = description;
        this.categoryId = categoryId;
        this.merchantId = merchantId;
        this.isInvestmentTransfer = false;
        this.recoveryStatus = RECOVERY_STATUS_NONE;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getDateEpochDay() {
        return dateEpochDay;
    }

    public void setDateEpochDay(long dateEpochDay) {
        this.dateEpochDay = dateEpochDay;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    @Nullable
    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(@Nullable Long merchantId) {
        this.merchantId = merchantId;
    }

    @Nullable
    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(@Nullable String photoPath) {
        this.photoPath = photoPath;
    }

    @Nullable
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(@Nullable Long accountId) {
        this.accountId = accountId;
    }

    public boolean isInvestmentTransfer() {
        return isInvestmentTransfer;
    }

    public void setInvestmentTransfer(boolean investmentTransfer) {
        isInvestmentTransfer = investmentTransfer;
    }

    @Nullable
    public Long getLinkedInvestmentId() {
        return linkedInvestmentId;
    }

    public void setLinkedInvestmentId(@Nullable Long linkedInvestmentId) {
        this.linkedInvestmentId = linkedInvestmentId;
    }

    @NonNull
    public String getRecoveryStatus() {
        return recoveryStatus;
    }

    public void setRecoveryStatus(@NonNull String recoveryStatus) {
        this.recoveryStatus = recoveryStatus;
    }

    @Nullable
    public String getCanonicalSourceKey() {
        return canonicalSourceKey;
    }

    public void setCanonicalSourceKey(@Nullable String canonicalSourceKey) {
        this.canonicalSourceKey = canonicalSourceKey;
    }
}
