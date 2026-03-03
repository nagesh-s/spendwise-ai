package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a money bucket in the timeline-based allocation system.
 * Each bucket corresponds to a life stage (Emergency, Present, Medium-term,
 * Long-term, Retirement) and holds investments + linked expense categories.
 *
 * Solves the "mental accounting" problem by giving every rupee a purpose,
 * so users can spend guilt-free from short-term buckets while long-term
 * wealth compounds untouched.
 */
@Entity(tableName = "buckets")
public class Bucket {

    // Timeline-based bucket types
    public static final String TYPE_EMERGENCY = "EMERGENCY"; // Bucket 0 — protected baseline
    public static final String TYPE_SHORT_TERM = "SHORT_TERM"; // Now → 2 years
    public static final String TYPE_MEDIUM_TERM = "MEDIUM_TERM"; // 2 → 5 years
    public static final String TYPE_LONG_TERM = "LONG_TERM"; // 5 → 15 years
    public static final String TYPE_RETIREMENT = "RETIREMENT"; // 15+ years
    public static final String TYPE_CUSTOM = "CUSTOM"; // User-created

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String type;

    /**
     * Target allocation percentage from salary (e.g., 20 means 20%).
     * All buckets should sum to 100%.
     */
    private int targetPercent;

    /**
     * Running balance in cents for this bucket.
     * Updated when salary is allocated or investments are added/removed.
     */
    private long currentBalanceCents;

    private int colorArgb;

    @Nullable
    private String iconKey;

    /**
     * Display order in the bucket list (0 = first).
     */
    private int sortOrder;

    /**
     * True for the 5 seeded default buckets. Default buckets cannot be deleted.
     */
    private boolean isDefault;

    @Nullable
    private String description;

    /**
     * Human-readable timeline label shown in the UI card.
     * E.g., "Now – 2 Years", "2 – 5 Years", "15+ Years".
     */
    @Nullable
    private String timelineLabel;

    /**
     * Per-bucket investment guidance shown in the UI.
     * E.g., "No stocks. Use FDs, Liquid Funds, or savings account."
     * Reinforces which instrument types belong in each time horizon.
     */
    @Nullable
    private String investmentAdvice;

    public Bucket(@NonNull String name, @NonNull String type, int targetPercent,
            int colorArgb, int sortOrder, boolean isDefault) {
        this.name = name;
        this.type = type;
        this.targetPercent = targetPercent;
        this.colorArgb = colorArgb;
        this.sortOrder = sortOrder;
        this.isDefault = isDefault;
        this.currentBalanceCents = 0;
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

    public int getTargetPercent() {
        return targetPercent;
    }

    public void setTargetPercent(int targetPercent) {
        this.targetPercent = targetPercent;
    }

    public long getCurrentBalanceCents() {
        return currentBalanceCents;
    }

    public void setCurrentBalanceCents(long currentBalanceCents) {
        this.currentBalanceCents = currentBalanceCents;
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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(@Nullable String description) {
        this.description = description;
    }

    @Nullable
    public String getTimelineLabel() {
        return timelineLabel;
    }

    public void setTimelineLabel(@Nullable String timelineLabel) {
        this.timelineLabel = timelineLabel;
    }

    @Nullable
    public String getInvestmentAdvice() {
        return investmentAdvice;
    }

    public void setInvestmentAdvice(@Nullable String investmentAdvice) {
        this.investmentAdvice = investmentAdvice;
    }
}
