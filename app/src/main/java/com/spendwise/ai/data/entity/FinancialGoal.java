package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a financial goal (savings target, spending limit, etc.)
 * Users can track progress toward becoming/staying financially disciplined.
 */
@Entity(tableName = "financial_goals")
public class FinancialGoal {

    public static final String TYPE_SAVINGS = "SAVINGS"; // Save X amount
    public static final String TYPE_SPENDING_LIMIT = "LIMIT"; // Don't spend more than X
    public static final String TYPE_DEBT_PAYOFF = "DEBT"; // Pay off X debt

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_PAUSED = "PAUSED";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String type; // SAVINGS, LIMIT, DEBT

    /**
     * Target amount in cents (e.g., save ₹50,000 = 5000000 cents).
     */
    private long targetAmountCents;

    /**
     * Current progress in cents (manually updated or computed).
     */
    private long currentAmountCents;

    /**
     * Start date as epoch day.
     */
    private long startDateEpochDay;

    /**
     * Target end date as epoch day. Null means no deadline.
     */
    @Nullable
    private Long endDateEpochDay;

    @NonNull
    private String status;

    private int colorArgb;

    @Nullable
    private String iconKey;

    /**
     * Optional category ID for spending limit goals.
     * Null means applies to all categories.
     */
    @Nullable
    private Long linkedCategoryId;

    public FinancialGoal(@NonNull String name, @NonNull String type,
            long targetAmountCents, long startDateEpochDay,
            @Nullable Long endDateEpochDay, int colorArgb) {
        this.name = name;
        this.type = type;
        this.targetAmountCents = targetAmountCents;
        this.startDateEpochDay = startDateEpochDay;
        this.endDateEpochDay = endDateEpochDay;
        this.colorArgb = colorArgb;
        this.status = STATUS_ACTIVE;
        this.currentAmountCents = 0;
    }

    // Getters and Setters
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

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public long getTargetAmountCents() {
        return targetAmountCents;
    }

    public void setTargetAmountCents(long targetAmountCents) {
        this.targetAmountCents = targetAmountCents;
    }

    public long getCurrentAmountCents() {
        return currentAmountCents;
    }

    public void setCurrentAmountCents(long currentAmountCents) {
        this.currentAmountCents = currentAmountCents;
    }

    public long getStartDateEpochDay() {
        return startDateEpochDay;
    }

    public void setStartDateEpochDay(long startDateEpochDay) {
        this.startDateEpochDay = startDateEpochDay;
    }

    @Nullable
    public Long getEndDateEpochDay() {
        return endDateEpochDay;
    }

    public void setEndDateEpochDay(@Nullable Long endDateEpochDay) {
        this.endDateEpochDay = endDateEpochDay;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    public int getColorArgb() {
        return colorArgb;
    }

    public void setColorArgb(int colorArgb) {
        this.colorArgb = colorArgb;
    }

    @Nullable
    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(@Nullable String iconKey) {
        this.iconKey = iconKey;
    }

    @Nullable
    public Long getLinkedCategoryId() {
        return linkedCategoryId;
    }

    public void setLinkedCategoryId(@Nullable Long linkedCategoryId) {
        this.linkedCategoryId = linkedCategoryId;
    }

    /**
     * Get progress as a percentage (0-100).
     */
    public int getProgressPercent() {
        if (targetAmountCents <= 0)
            return 0;
        return (int) Math.min(100, (currentAmountCents * 100) / targetAmountCents);
    }

    /**
     * Check if the goal is complete.
     */
    public boolean isComplete() {
        return currentAmountCents >= targetAmountCents;
    }
}
