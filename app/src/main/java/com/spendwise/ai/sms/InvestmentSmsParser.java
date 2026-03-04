package com.spendwise.ai.sms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.entity.Investment;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses investment-related SMS messages (MF/ETF/stock/FD/RD).
 *
 * Supports both "debit initiated" and "units/holding confirmed" phases so the
 * ingestion layer can upsert and avoid duplicate entries.
 */
public final class InvestmentSmsParser {

    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr)\\s*([\\d,]+\\.?\\d*)");

    private static final Pattern REF_PATTERN = Pattern.compile(
            "(?i)(?:ref(?:erence)?|utr|arn|order(?:\\s*(?:id|no|number))?|folio(?:\\s*(?:no|number))?)\\s*[:#-]?\\s*([A-Za-z0-9\\-]{4,30})");

    private static final Pattern PLATFORM_PATTERN = Pattern.compile(
            "(?i)\\b(upstox|groww|zerodha|kite|coin|angel\\s*one|icici\\s*direct|hdfc\\s*sec|paytm\\s*money|et\\s*money|mf\\s*central)\\b");

    private static final Pattern FD_NUMBER_PATTERN = Pattern.compile(
            "(?i)\\bfd\\s*(?:no\\.?|number)?\\s*[:#-]?\\s*([A-Za-z0-9\\-]{4,30})");

    private static final Pattern RD_NUMBER_PATTERN = Pattern.compile(
            "(?i)\\brd\\s*(?:no\\.?|number)?\\s*[:#-]?\\s*([A-Za-z0-9\\-]{4,30})");

    private static final Pattern INVESTMENT_NAME_PATTERN = Pattern.compile(
            "(?i)(?:of|in|for)\\s+([A-Za-z][A-Za-z0-9&()'./\\-\\s]{3,80}?)(?:\\s+(?:units?|shares?|allotted|allotment|credited|buy|bought|purchased|executed|scheme|fund|etf|sip)\\b|[.,]|$)");

    private InvestmentSmsParser() {
    }

    public static boolean isInvestmentSms(@Nullable String sender, @Nullable String body) {
        if (body == null || body.trim().isEmpty()) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.US);
        boolean hasInvestmentKeyword = containsInvestmentKeyword(normalized)
                || containsFdKeyword(normalized)
                || containsRdKeyword(normalized);
        if (!hasInvestmentKeyword) {
            return false;
        }
        boolean hasAmount = AMOUNT_PATTERN.matcher(body).find();
        boolean hasRef = REF_PATTERN.matcher(body).find()
                || FD_NUMBER_PATTERN.matcher(body).find()
                || RD_NUMBER_PATTERN.matcher(body).find();
        boolean senderHint = sender != null
                && sender.toLowerCase(Locale.US).matches(".*(upstox|groww|zerodha|nse|bse|amc|mf|bank|hdfc|icici|axis|kotak).*");
        return hasAmount || hasRef || senderHint;
    }

    @Nullable
    public static ParsedInvestmentSignal parse(
            @Nullable String sender,
            @Nullable String body,
            long timestampMillis) {
        if (!isInvestmentSms(sender, body) || body == null) {
            return null;
        }

        String normalized = body.toLowerCase(Locale.US);
        ParsedInvestmentSignal.SignalType signalType = resolveSignalType(normalized);
        String assetType = resolveAssetType(normalized, signalType);
        String platform = extractPlatform(sender, body);
        long amountCents = extractPrimaryAmountCents(body);
        String reference = extractFirstMatch(body, REF_PATTERN);
        if (reference == null) {
            reference = extractFirstMatch(body, FD_NUMBER_PATTERN);
        }
        if (reference == null) {
            reference = extractFirstMatch(body, RD_NUMBER_PATTERN);
        }
        String investmentName = extractInvestmentName(body, signalType, platform, assetType);
        boolean correction = normalized.contains("revised")
                || normalized.contains("updated")
                || normalized.contains("corrected")
                || normalized.contains("amended");

        String correlationSeed = reference;
        if (correlationSeed == null || correlationSeed.length() < 4) {
            correlationSeed = sanitizeKey(String.format(Locale.US, "%s|%s|%d",
                    platform == null ? "unknown" : platform,
                    investmentName == null ? "investment" : investmentName,
                    amountCents));
        }

        String sourceSeed = String.format(Locale.US, "%s|%s|%s|%d",
                correlationSeed,
                signalType.name(),
                assetType,
                roundToDay(timestampMillis));
        String sourceKey = "src_" + sanitizeKey(sourceSeed);
        String correlationKey = "corr_" + sanitizeKey(correlationSeed);

        return new ParsedInvestmentSignal(
                signalType,
                platform,
                investmentName,
                assetType,
                amountCents,
                timestampMillis,
                sourceKey,
                correlationKey,
                body,
                correction);
    }

    @NonNull
    private static ParsedInvestmentSignal.SignalType resolveSignalType(String normalized) {
        if (containsFdKeyword(normalized) && containsAny(normalized,
                "booked", "opened", "created", "placed", "issued")) {
            return ParsedInvestmentSignal.SignalType.FD_BOOKED;
        }
        if (containsRdKeyword(normalized) && containsAny(normalized,
                "booked", "opened", "created", "started", "registered")) {
            return ParsedInvestmentSignal.SignalType.RD_BOOKED;
        }
        if (containsAny(normalized,
                "units allotted", "allotted", "allotment",
                "units credited", "shares credited", "buy order executed",
                "purchase executed", "bought", "purchased", "order executed")) {
            return ParsedInvestmentSignal.SignalType.UNIT_ALLOTTED;
        }
        if (containsAny(normalized, "maturity", "matured", "redemption", "redeemed", "closed")) {
            return ParsedInvestmentSignal.SignalType.MATURITY_CREDIT;
        }
        return ParsedInvestmentSignal.SignalType.INVESTMENT_DEBIT;
    }

    @NonNull
    private static String resolveAssetType(String normalized, ParsedInvestmentSignal.SignalType signalType) {
        if (containsFdKeyword(normalized) || signalType == ParsedInvestmentSignal.SignalType.FD_BOOKED) {
            return Investment.ASSET_FD;
        }
        if (containsRdKeyword(normalized) || signalType == ParsedInvestmentSignal.SignalType.RD_BOOKED) {
            return Investment.ASSET_RD;
        }
        if (containsAny(normalized, "etf")) {
            return Investment.ASSET_ETF;
        }
        if (containsAny(normalized, "mutual fund", "mf", "sip", "folio", "amc")) {
            return Investment.ASSET_MF;
        }
        if (containsAny(normalized, "share", "stock", "equity", "demat", "cdsl", "nsdl")) {
            return Investment.ASSET_STOCK;
        }
        if (containsAny(normalized, "ppf")) {
            return Investment.ASSET_PPF;
        }
        if (containsAny(normalized, "nps")) {
            return Investment.ASSET_NPS;
        }
        if (containsAny(normalized, "gold")) {
            return Investment.ASSET_GOLD;
        }
        return Investment.ASSET_OTHER;
    }

    @Nullable
    private static String extractPlatform(@Nullable String sender, @NonNull String body) {
        Matcher bodyMatcher = PLATFORM_PATTERN.matcher(body);
        if (bodyMatcher.find()) {
            return toTitle(bodyMatcher.group(1));
        }
        if (sender == null || sender.trim().isEmpty()) {
            return null;
        }
        String cleaned = sender.replaceAll("^[A-Za-z]{2}-", "")
                .replaceAll("[^A-Za-z0-9 ]", " ")
                .trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return toTitle(cleaned);
    }

    private static long extractPrimaryAmountCents(@NonNull String body) {
        Matcher matcher = AMOUNT_PATTERN.matcher(body);
        long selectedAmount = 0L;
        while (matcher.find()) {
            String amountRaw = matcher.group(1);
            if (amountRaw == null) {
                continue;
            }
            try {
                long parsed = Math.round(Double.parseDouble(amountRaw.replace(",", "")) * 100d);
                selectedAmount = Math.max(selectedAmount, parsed);
            } catch (NumberFormatException ignore) {
                // Skip malformed amounts.
            }
        }
        return selectedAmount;
    }

    @Nullable
    private static String extractInvestmentName(
            @NonNull String body,
            @NonNull ParsedInvestmentSignal.SignalType signalType,
            @Nullable String platform,
            @NonNull String assetType) {
        Matcher matcher = INVESTMENT_NAME_PATTERN.matcher(body);
        if (matcher.find()) {
            String value = cleanLabel(matcher.group(1));
            if (value != null && value.length() >= 3) {
                return value;
            }
        }

        if (Investment.ASSET_FD.equals(assetType)) {
            return platform == null ? "Fixed Deposit" : platform + " Fixed Deposit";
        }
        if (Investment.ASSET_RD.equals(assetType)) {
            return platform == null ? "Recurring Deposit" : platform + " Recurring Deposit";
        }
        if (signalType == ParsedInvestmentSignal.SignalType.UNIT_ALLOTTED) {
            return platform == null ? "Investment Units" : platform + " Investment Units";
        }
        return platform == null ? "Investment Allocation" : platform + " Investment";
    }

    @Nullable
    private static String extractFirstMatch(@NonNull String body, Pattern pattern) {
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static long roundToDay(long timestampMillis) {
        return timestampMillis / (24L * 60 * 60 * 1000);
    }

    private static boolean containsInvestmentKeyword(String normalized) {
        return containsAny(normalized,
                "investment", "mutual fund", "mf", "sip", "folio",
                "etf", "shares", "stock", "demat", "buy order", "allotment",
                "units", "scheme", "amc");
    }

    private static boolean containsFdKeyword(String normalized) {
        return containsAny(normalized, " fixed deposit", " fd ", "fd no", "fd number");
    }

    private static boolean containsRdKeyword(String normalized) {
        return containsAny(normalized, " recurring deposit", " rd ", "rd no", "rd number");
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private static String sanitizeKey(@NonNull String value) {
        String cleaned = value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }

    @Nullable
    private static String cleanLabel(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim()
                .replaceAll("(?i)\\b(?:order|ref|utr|folio|units?|shares?)\\b.*$", "")
                .replaceAll("[\\s,.:;\\-]+$", "")
                .replaceAll("\\s{2,}", " ");
        return cleaned.isEmpty() ? null : cleaned;
    }

    @Nullable
    private static String toTitle(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String[] words = raw.trim().toLowerCase(Locale.US).split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            if (word.length() == 1) {
                builder.append(Character.toUpperCase(word.charAt(0)));
            } else {
                builder.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1));
            }
        }
        return builder.toString();
    }
}
