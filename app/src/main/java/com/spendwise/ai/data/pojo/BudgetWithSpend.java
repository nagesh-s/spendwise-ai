package com.spendwise.ai.data.pojo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * POJO for budget with actual spending.
 * Combines budget limit with current spend for progress display.
 */
public class BudgetWithSpend {

    private long budgetId;
    private long categoryId;

    @NonNull
    private String categoryName;

    private int categoryColorArgb;

    private long limitCents;

    private long spentCents;

    public BudgetWithSpend(long budgetId, long categoryId, @NonNull String categoryName, int categoryColorArgb,
                           long limitCents, long spentCents) {
        this.budgetId = budgetId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.categoryColorArgb = categoryColorArgb;
        this.limitCents = limitCents;
        this.spentCents = spentCents;
    }

    public long getBudgetId() {
        return budgetId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    @NonNull
    public String getCategoryName() {
        return categoryName;
    }

    public int getCategoryColorArgb() {
        return categoryColorArgb;
    }

    public long getLimitCents() {
        return limitCents;
    }

    public long getSpentCents() {
        return spentCents;
    }

    /**
     * Returns the usage percentage (0-100+).
     * Can exceed 100 if over budget.
     */
    public int getUsagePercent() {
        if (limitCents <= 0) return 0;
        return (int) ((spentCents * 100) / limitCents);
    }

    /**
     * Returns true if spending exceeds the budget limit.
     */
    public boolean isOverBudget() {
        return spentCents > limitCents;
    }

    public String getFormattedLimit() {
        return String.format("₹%,.2f", limitCents / 100.0);
    }

    public String getFormattedSpent() {
        return String.format("₹%,.2f", spentCents / 100.0);
    }
}
