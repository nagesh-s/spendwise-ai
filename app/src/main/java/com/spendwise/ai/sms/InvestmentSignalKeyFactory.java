package com.spendwise.ai.sms;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Canonical key helper for deduping investment ingestion events across sources.
 */
public final class InvestmentSignalKeyFactory {

    private InvestmentSignalKeyFactory() {
    }

    @NonNull
    public static String buildIngestionSourceKey(@NonNull ParsedInvestmentSignal signal) {
        String base = signal.getSourceKey();
        if (!signal.isCorrection()) {
            return base;
        }
        return String.format(
                Locale.US,
                "%s_corr_%d",
                base,
                Math.max(0L, signal.getAmountCents()));
    }
}
