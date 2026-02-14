package com.spendwise.ai.insights;

/**
 * A smart prediction about future spending.
 * Displayed as cards on the Dashboard predictions section.
 */
public class Prediction {

    public enum PredictionType {
        MONTH_END_TOTAL,
        CATEGORY_PROJECTION,
        BUDGET_RISK,
        BEST_DAY,
        WORST_DAY
    }

    private final PredictionType type;
    private final String label;
    private final long projectedAmountCents;
    private final int confidencePercent; // 0-100
    private final String description;

    public Prediction(PredictionType type, String label, long projectedAmountCents,
            int confidencePercent, String description) {
        this.type = type;
        this.label = label;
        this.projectedAmountCents = projectedAmountCents;
        this.confidencePercent = confidencePercent;
        this.description = description;
    }

    public PredictionType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public long getProjectedAmountCents() {
        return projectedAmountCents;
    }

    public int getConfidencePercent() {
        return confidencePercent;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedAmount() {
        return String.format("₹%,.0f", projectedAmountCents / 100.0);
    }
}
