package com.spendwise.ai.insights;

import androidx.annotation.DrawableRes;

/**
 * A single AI-generated insight about the user's spending.
 * Displayed as a dismissible card on the Dashboard.
 */
public class Insight {

    private final InsightType type;
    private final String title;
    private final String description;
    private final Severity severity;
    @DrawableRes
    private final int iconRes;
    private final String actionLabel; // nullable — e.g. "View Budget"

    public enum Severity {
        INFO, // neutral observation
        WARNING, // caution, approaching limit
        POSITIVE // good news, streak, savings
    }

    public Insight(InsightType type, String title, String description,
            Severity severity, @DrawableRes int iconRes, String actionLabel) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.iconRes = iconRes;
        this.actionLabel = actionLabel;
    }

    public InsightType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    @DrawableRes
    public int getIconRes() {
        return iconRes;
    }

    public String getActionLabel() {
        return actionLabel;
    }
}
