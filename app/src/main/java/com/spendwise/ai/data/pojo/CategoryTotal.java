package com.spendwise.ai.data.pojo;

import androidx.annotation.NonNull;

/**
 * POJO for category spending totals query result.
 * Used to display category-wise breakdown in Dashboard pie chart.
 */
public class CategoryTotal {

    @NonNull
    private String name;

    private long total;

    private int colorArgb;

    public CategoryTotal(@NonNull String name, long total, int colorArgb) {
        this.name = name;
        this.total = total;
        this.colorArgb = colorArgb;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public long getTotal() {
        return total;
    }

    public int getColorArgb() {
        return colorArgb;
    }

    /**
     * Returns the total as a formatted currency string.
     * @return Formatted amount (e.g., "₹1,234.56")
     */
    public String getFormattedTotal() {
        return String.format("₹%,.2f", total / 100.0);
    }
}
