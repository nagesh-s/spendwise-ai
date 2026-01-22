package com.spendwise.ai.data.pojo;

/**
 * POJO for daily spending total.
 * Used for calendar decorators to show spending intensity per day.
 */
public class DailyTotal {

    private long dateEpochDay;

    private long totalCents;

    public DailyTotal(long dateEpochDay, long totalCents) {
        this.dateEpochDay = dateEpochDay;
        this.totalCents = totalCents;
    }

    public long getDateEpochDay() {
        return dateEpochDay;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public String getFormattedTotal() {
        return String.format("₹%,.2f", totalCents / 100.0);
    }
}
