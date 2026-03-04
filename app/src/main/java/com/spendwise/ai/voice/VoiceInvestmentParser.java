package com.spendwise.ai.voice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.entity.Investment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight parser for investment voice commands.
 *
 * Example: "Add 5000 in Parag Parikh flexi cap on Upstox"
 */
public class VoiceInvestmentParser {

    public static class ParsedInvestment {
        public long amountCents;
        @Nullable
        public String investmentName;
        @Nullable
        public String platform;
        @NonNull
        public String assetType = Investment.ASSET_OTHER;

        public boolean isValid() {
            return amountCents > 0;
        }
    }

    private static final Pattern CURRENCY_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr|rupees?)\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)");
    private static final Pattern ACTION_AMOUNT_PATTERN = Pattern.compile(
            "(?i)\\b(?:add|invest|buy|sip|put)\\s+(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\b");
    private static final Pattern GENERIC_AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)(?!\\d)");

    private static final Pattern NAME_PATTERN = Pattern.compile(
            "(?i)(?:in|into|for|of)\\s+([A-Za-z][A-Za-z0-9 '&.\\-/]{2,80}?)(?:\\s+(?:on|via|using|at)\\b|$)");

    private static final Pattern PLATFORM_PATTERN = Pattern.compile(
            "(?i)(?:on|via|using|at)\\s+([A-Za-z][A-Za-z0-9 .&\\-]{1,30})$");

    public ParsedInvestment parse(@Nullable String transcript) {
        ParsedInvestment parsed = new ParsedInvestment();
        if (transcript == null || transcript.trim().isEmpty()) {
            return parsed;
        }
        String text = transcript.trim();

        parsed.amountCents = extractAmountCents(text);
        parsed.platform = extractPlatform(text);
        parsed.investmentName = extractName(text);
        parsed.assetType = inferAssetType(text);
        return parsed;
    }

    private long extractAmountCents(String text) {
        Matcher currencyMatcher = CURRENCY_AMOUNT_PATTERN.matcher(text);
        if (currencyMatcher.find()) {
            Long amount = parseAmount(currencyMatcher.group(1));
            return amount == null ? 0L : amount;
        }
        Matcher actionMatcher = ACTION_AMOUNT_PATTERN.matcher(text);
        if (actionMatcher.find()) {
            Long amount = parseAmount(actionMatcher.group(1));
            return amount == null ? 0L : amount;
        }
        Matcher genericMatcher = GENERIC_AMOUNT_PATTERN.matcher(text);
        if (genericMatcher.find()) {
            Long amount = parseAmount(genericMatcher.group(1));
            return amount == null ? 0L : amount;
        }
        return 0L;
    }

    @Nullable
    private Long parseAmount(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        try {
            double amount = Double.parseDouble(raw.replace(",", ""));
            if (amount <= 0) {
                return null;
            }
            return Math.round(amount * 100d);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private String extractName(String text) {
        Matcher matcher = NAME_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String cleaned = matcher.group(1);
        if (cleaned == null) {
            return null;
        }
        cleaned = cleaned.trim()
                .replaceAll("(?i)\\b(?:rupees?|rs\\.?|inr|today|now)\\b", "")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    @Nullable
    private String extractPlatform(String text) {
        Matcher matcher = PLATFORM_PATTERN.matcher(text.trim());
        if (!matcher.find()) {
            return null;
        }
        String platform = matcher.group(1);
        if (platform == null) {
            return null;
        }
        platform = platform.trim().replaceAll("\\s{2,}", " ");
        if (platform.isEmpty()) {
            return null;
        }
        return toTitle(platform);
    }

    @NonNull
    private String inferAssetType(String text) {
        String normalized = text.toLowerCase(Locale.US);
        if (normalized.contains("etf")) {
            return Investment.ASSET_ETF;
        }
        if (normalized.contains("mutual fund") || normalized.contains("sip") || normalized.contains("mf")) {
            return Investment.ASSET_MF;
        }
        if (normalized.contains("stock") || normalized.contains("share") || normalized.contains("equity")) {
            return Investment.ASSET_STOCK;
        }
        if (normalized.contains("fixed deposit") || normalized.contains("fd")) {
            return Investment.ASSET_FD;
        }
        if (normalized.contains("recurring deposit") || normalized.contains("rd")) {
            return Investment.ASSET_RD;
        }
        if (normalized.contains("gold")) {
            return Investment.ASSET_GOLD;
        }
        if (normalized.contains("ppf")) {
            return Investment.ASSET_PPF;
        }
        if (normalized.contains("nps")) {
            return Investment.ASSET_NPS;
        }
        return Investment.ASSET_OTHER;
    }

    @NonNull
    private String toTitle(@NonNull String input) {
        String[] words = input.toLowerCase(Locale.US).split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString().trim();
    }
}
