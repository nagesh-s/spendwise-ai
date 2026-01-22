package com.spendwise.ai.data.pojo;

import androidx.annotation.NonNull;

/**
 * POJO for merchant analytics query result.
 * Used to display top merchants with aggregated statistics.
 */
public class MerchantAgg {

    @NonNull
    private String name;

    private int transactionCount;

    private long totalCents;

    private long avgCents;

    public MerchantAgg(@NonNull String name, int transactionCount, long totalCents, long avgCents) {
        this.name = name;
        this.transactionCount = transactionCount;
        this.totalCents = totalCents;
        this.avgCents = avgCents;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public long getTotalCents() {
        return totalCents;
    }

    public long getAvgCents() {
        return avgCents;
    }

    public String getFormattedTotal() {
        return String.format("₹%,.2f", totalCents / 100.0);
    }

    public String getFormattedAverage() {
        return String.format("₹%,.2f", avgCents / 100.0);
    }
}
