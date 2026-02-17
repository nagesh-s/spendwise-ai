package com.spendwise.ai.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Entity representing a recurring expense template.
 * 
 * Recurring expenses are automatically generated as actual Expense entries
 * based on the configured frequency. Examples: monthly subscriptions,
 * weekly commute costs, rent, etc.
 */
@Entity(tableName = "recurring_expenses", foreignKeys = {
        @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = Merchant.class, parentColumns = "id", childColumns = "merchantId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index("categoryId"),
        @Index("merchantId"),
        @Index("isActive")
})
public class RecurringExpense {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Amount in cents (e.g., 50000 = ₹500.00)
     */
    private long amountCents;

    /**
     * Description of the recurring expense (e.g., "Netflix Subscription")
     */
    private String description;

    /**
     * Foreign key to Category entity.
     */
    private Long categoryId;

    /**
     * Foreign key to Merchant entity.
     */
    private Long merchantId;

    /**
     * Recurrence frequency: DAILY, EVERY_2_DAYS, EVERY_3_DAYS, WEEKLY, MONTHLY, YEARLY
     */
    private String frequency;

    /**
     * Day of week for WEEKLY frequency (1=Sunday, 7=Saturday)
     * Day of month for MONTHLY/YEARLY frequency (1-31)
     */
    private int dayOfPeriod;

    /**
     * Month for YEARLY frequency (1-12)
     */
    private int monthOfYear;

    /**
     * Start date as epoch day (days since 1970-01-01)
     */
    private long startDateEpochDay;

    /**
     * End date as epoch day. Null means no end date.
     */
    private Long endDateEpochDay;

    /**
     * The last date an expense was generated from this template.
     */
    private long lastGeneratedEpochDay;

    /**
     * Whether this recurring expense is currently active.
     */
    private boolean isActive;

    /**
     * Number of days before the due date to show reminder.
     * 0 = no reminder, 1 = 1 day before, etc.
     */
    private int reminderDaysBefore;

    // ==================== Constructor ====================

    public RecurringExpense(long amountCents, String description, Long categoryId,
            Long merchantId, String frequency, int dayOfPeriod,
            long startDateEpochDay) {
        this.amountCents = amountCents;
        this.description = description;
        this.categoryId = categoryId;
        this.merchantId = merchantId;
        this.frequency = frequency;
        this.dayOfPeriod = dayOfPeriod;
        this.startDateEpochDay = startDateEpochDay;
        this.lastGeneratedEpochDay = startDateEpochDay - 1; // Not yet generated
        this.isActive = true;
        this.reminderDaysBefore = 0;
        this.monthOfYear = 1;
    }

    // ==================== Getters ====================

    public long getId() {
        return id;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getDescription() {
        return description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public String getFrequency() {
        return frequency;
    }

    public int getDayOfPeriod() {
        return dayOfPeriod;
    }

    public int getMonthOfYear() {
        return monthOfYear;
    }

    public long getStartDateEpochDay() {
        return startDateEpochDay;
    }

    public Long getEndDateEpochDay() {
        return endDateEpochDay;
    }

    public long getLastGeneratedEpochDay() {
        return lastGeneratedEpochDay;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getReminderDaysBefore() {
        return reminderDaysBefore;
    }

    // ==================== Setters ====================

    public void setId(long id) {
        this.id = id;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setDayOfPeriod(int dayOfPeriod) {
        this.dayOfPeriod = dayOfPeriod;
    }

    public void setMonthOfYear(int monthOfYear) {
        this.monthOfYear = monthOfYear;
    }

    public void setStartDateEpochDay(long startDateEpochDay) {
        this.startDateEpochDay = startDateEpochDay;
    }

    public void setEndDateEpochDay(Long endDateEpochDay) {
        this.endDateEpochDay = endDateEpochDay;
    }

    public void setLastGeneratedEpochDay(long lastGeneratedEpochDay) {
        this.lastGeneratedEpochDay = lastGeneratedEpochDay;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setReminderDaysBefore(int reminderDaysBefore) {
        this.reminderDaysBefore = reminderDaysBefore;
    }

    // ==================== Frequency Constants ====================

    public static final String FREQUENCY_DAILY = "DAILY";
    public static final String FREQUENCY_EVERY_2_DAYS = "EVERY_2_DAYS";
    public static final String FREQUENCY_EVERY_3_DAYS = "EVERY_3_DAYS";
    public static final String FREQUENCY_WEEKLY = "WEEKLY";
    public static final String FREQUENCY_MONTHLY = "MONTHLY";
    public static final String FREQUENCY_YEARLY = "YEARLY";
}
