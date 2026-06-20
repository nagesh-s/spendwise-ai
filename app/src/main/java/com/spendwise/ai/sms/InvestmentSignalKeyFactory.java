package com.spendwise.ai.sms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.entity.Holding;

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

    @NonNull
    public static String buildCanonicalInstrumentKey(@NonNull ParsedInvestmentSignal signal) {
        return buildCanonicalInstrumentKey(
                signal.getAssetType(),
                firstNonBlank(signal.getPlatform(), signal.getSender(), "unknown"),
                signal.getInvestmentName(),
                signal.getAmountCents(),
                signal.getTimestampMillis(),
                signal.getRawMessage(),
                signal.getCorrelationKey());
    }

    @NonNull
    public static String buildCanonicalInstrumentKey(
            @NonNull String rawAssetType,
            @Nullable String rawPlatform,
            @Nullable String rawName,
            long rawAmountCents,
            long timestampMillis,
            @Nullable String rawMessage,
            @Nullable String correlationKey) {
        String assetType = sanitize(rawAssetType);
        String platform = sanitize(firstNonBlank(rawPlatform, null, "unknown"));
        long amountBucket = Math.max(0L, rawAmountCents) / 100L;

        if (Holding.ASSET_FD.equals(rawAssetType) || Holding.ASSET_RD.equals(rawAssetType)) {
            if (hasExplicitReference(rawMessage) && correlationKey != null && !correlationKey.trim().isEmpty()) {
                return String.format(Locale.US, "inst_%s_%s_%s",
                        assetType,
                        platform,
                        sanitize(correlationKey));
            }
            long bookingWindow = epochDay(timestampMillis) / 30L;
            return String.format(Locale.US, "inst_%s_%s_%d_w%d",
                    assetType,
                    platform,
                    amountBucket,
                    bookingWindow);
        }

        String label = sanitize(firstNonBlank(rawName, rawPlatform, "investment"));
        return String.format(Locale.US, "inst_%s_%s_%s_%d",
                assetType,
                platform,
                label,
                amountBucket);
    }

    private static long epochDay(long timestampMillis) {
        return timestampMillis / (24L * 60L * 60L * 1000L);
    }

    @NonNull
    private static String sanitize(@Nullable String value) {
        if (value == null) {
            return "unknown";
        }
        String cleaned = value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    @NonNull
    private static String firstNonBlank(@Nullable String first, @Nullable String second, @NonNull String fallback) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return fallback;
    }

    private static boolean hasExplicitReference(@Nullable String rawMessage) {
        if (rawMessage == null) {
            return false;
        }
        String normalized = rawMessage.toLowerCase(Locale.US);
        return normalized.contains("fd no")
                || normalized.contains("fd number")
                || normalized.contains("rd no")
                || normalized.contains("rd number")
                || normalized.contains("ref")
                || normalized.contains("utr")
                || normalized.contains("arn")
                || normalized.contains("folio")
                || normalized.contains("order id")
                || normalized.contains("order no")
                || normalized.contains("order number");
    }
}
