package com.spendwise.ai.sms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.entity.Holding;

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
            "(?i)(?:₹|\\brs\\.?|\\binr\\b)\\s*([\\d,]+\\.?\\d*)");

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

    public static boolean isPromotionalSms(@Nullable String body) {
        if (body == null || body.trim().isEmpty()) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.US);
        return containsAny(normalized,
                "you are eligible for", "pre-approved", "pre approved",
                "apply now", "avail now", "limited offer", "special offer",
                "congratulations! you", "congrats!",
                "click here", "bit.ly/", "apply.link",
                "call to opt", "sms stop", "unsubscribe",
                "eligible for", "instant loan", "loan offer", "upgrade", "personal loan",
                "will be converted", "expires", "caution",
                "loan is credited", "loan disbursed", "loan approved",
                "credit limit", "credit line", "emi starts",
                "redeem now", "claim now", "activate now",
                "download the app", "download app",
                "tinyurl", "cutt.ly", "short.io", "rebrand.ly",
                "t.co/", "goo.gl/", "is.gd/",
                "missed call", "give a missed call",
                "dial ", "call us", "contact us",
                "exclusively for you", "just for you",
                "hurry", "limited time", "offer ends",
                "win ", "won ", "winner",
                "free ", "cashback of", "reward",
                "insurance", "policy premium");
    }

    private static boolean isLikelyLoanSms(String normalized) {
        boolean hasLoanKeyword = containsAny(normalized,
                "loan", "emi", "credit line", "credit limit",
                "disbursed", "disbursal", "repayment");
        boolean hasAmountAndCredited = normalized.contains("credited")
                && !containsAny(normalized, "units", "allotted", "folio", "nav");
        return hasLoanKeyword && hasAmountAndCredited;
    }

    public static boolean isInvestmentSms(@Nullable String sender, @Nullable String body) {
        if (body == null || body.trim().isEmpty() || isPromotionalSms(body)) {
            return false;
        }
        String normalized = body.toLowerCase(Locale.US);
        if (isLikelyLoanSms(normalized)) {
            return false;
        }
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
                && sender.toLowerCase(Locale.US)
                        .matches(".*(upstox|groww|zerodha|nse|bse|amc|mf|bank|hdfc|icici|axis|kotak).*");
        return hasAmount || hasRef || senderHint;
    }

    @Nullable
    public static ParsedInvestmentSignal parse(
            @Nullable String sender,
            @Nullable String body,
            long timestampMillis) {
        if (isPromotionalSms(body) || !isInvestmentSms(sender, body) || body == null) {
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
        long monthStr = timestampMillis / (30L * 86400000L); // Roughly a month
        if (correlationSeed != null && correlationSeed.length() >= 4) {
            correlationSeed = sanitizeKey(correlationSeed) + "_" + amountCents + "_" + monthStr;
        } else {
            correlationSeed = sanitizeKey(String.format(Locale.US, "%s|%s|%d|%d",
                    platform == null ? "unknown" : platform,
                    investmentName == null ? "investment" : investmentName,
                    amountCents,
                    monthStr));
        }

        String sourceSeed = String.format(Locale.US, "%s|%s|%s|%d",
                correlationSeed,
                signalType.name(),
                assetType,
                roundToDay(timestampMillis));
        String sourceKey = "src_" + sanitizeKey(sourceSeed);
        String correlationKey = "corr_" + sanitizeKey(correlationSeed);
        String fingerprint = normalizedFingerprint(body);
        double baseConfidence = reference != null ? 0.95d
                : (platform != null && investmentName != null && amountCents > 0 ? 0.85d : 0.65d);

        double confidence = baseConfidence;
        if (investmentName != null && body.toLowerCase(Locale.US).contains("for " + investmentName.toLowerCase(Locale.US)) && body.length() - body.toLowerCase(Locale.US).indexOf("for " + investmentName.toLowerCase(Locale.US)) < 50) {
            confidence -= 0.3; // likely a footer
        }
        if (containsAny(normalized, "bit.ly", "apply.link", "cutt.ly", "goo.gl", "tinyurl", "short.io")) {
            confidence -= 0.4;
        }
        if (normalized.contains("credited") && !containsAny(normalized, "units", "allotted", "nav", "maturity", "redemption", "folio", "sip")) {
            confidence -= 0.25;
        }
        int kwCount = 0;
        String[] kws = {"mutual fund", "sip", "folio", "allotment", "units allotted", "nav", "amc", "demat", "units", "shares", "scheme", "coin", "investment", "stock"};
        for (String kw : kws) {
            if (normalized.contains(kw)) kwCount++;
        }
        if (kwCount <= 1) {
            confidence -= 0.15;
        }

        return new ParsedInvestmentSignal(
                signalType,
                sender,
                platform,
                investmentName,
                assetType,
                amountCents,
                timestampMillis,
                sourceKey,
                correlationKey,
                body,
                correction,
                reference,
                fingerprint,
                confidence,
                reference != null ? "strong_reference"
                        : confidence >= 0.8d ? "provider_instrument_amount" : "partial_extraction");
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
            return Holding.ASSET_FD;
        }
        if (containsRdKeyword(normalized) || signalType == ParsedInvestmentSignal.SignalType.RD_BOOKED) {
            return Holding.ASSET_RD;
        }
        if (containsAny(normalized, "etf")) {
            return Holding.ASSET_ETF;
        }
        if (containsAny(normalized, "mutual fund", "mf", "sip", "folio", "amc")) {
            return Holding.ASSET_MF;
        }
        if (containsAny(normalized, "share", "stock", "equity", "demat", "cdsl", "nsdl")) {
            return Holding.ASSET_STOCK;
        }
        if (containsAny(normalized, "ppf")) {
            return Holding.ASSET_PPF;
        }
        if (containsAny(normalized, "nps")) {
            return Holding.ASSET_NPS;
        }
        if (containsAny(normalized, "gold")) {
            return Holding.ASSET_GOLD;
        }
        return Holding.ASSET_OTHER;
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
        long bestAmount = 0L;
        int bestScore = -1;

        while (matcher.find()) {
            String amountRaw = matcher.group(1);
            if (amountRaw == null) {
                continue;
            }
            try {
                long parsed = Math.round(Double.parseDouble(amountRaw.replace(",", "")) * 100d);
                if (parsed <= 0) continue;

                int matchStart = matcher.start();
                String preceding = body.substring(Math.max(0, matchStart - 30), matchStart).toLowerCase(Locale.US);
                int score = 1;
                if (containsAny(preceding, "debited", "invested", "purchased", "bought",
                        "sip of", "allotted", "amount")) {
                    score = 3;
                } else if (containsAny(preceding, "rs", "inr", "₹")) {
                    score = 2;
                }
                if (containsAny(preceding, "balance", "avl bal", "limit", "available", "value", "current", "total", "nav")) {
                    score = 0;
                }

                if (score > bestScore || (score == bestScore && parsed > bestAmount)) {
                    bestAmount = parsed;
                    bestScore = score;
                }
            } catch (NumberFormatException ignore) {
                // Skip malformed amounts.
            }
        }
        return bestAmount;
    }

    @Nullable
    private static String extractInvestmentName(
            @NonNull String body,
            @NonNull ParsedInvestmentSignal.SignalType signalType,
            @Nullable String platform,
            @NonNull String assetType) {
        String bodyForName = body.replaceAll("(?i)(?:For queries|For more details|For assistance|For support).*$", "");
        Matcher matcher = INVESTMENT_NAME_PATTERN.matcher(bodyForName);
        if (matcher.find()) {
            String value = cleanLabel(matcher.group(1));
            // Remove any accidental currency/amount captures
            if (value != null) {
                value = value.replaceAll("(?i)^(?:rs\\.?|inr|₹)\\s*[\\d,]+\\.?\\d*\\s*(?:in|for)?\\s*", "").trim();
                
                String lowerName = value.toLowerCase(Locale.US);
                boolean blacklisted = false;
                for (String bl : new String[]{"queries", "query", "details", "information", "customer care", "helpline", "support", "assistance", "more info", "questions", "grievance", "complaints"}) {
                    if (lowerName.contains(bl)) {
                        blacklisted = true;
                        break;
                    }
                }

                if (!blacklisted && value.length() >= 3 && !value.matches("^[\\d.,]+$")) {
                    return value;
                }
            }
        }

        if (Holding.ASSET_FD.equals(assetType)) {
            return platform == null ? "Fixed Deposit" : platform + " Fixed Deposit";
        }
        if (Holding.ASSET_RD.equals(assetType)) {
            return platform == null ? "Recurring Deposit" : platform + " Recurring Deposit";
        }
        if (signalType == ParsedInvestmentSignal.SignalType.UNIT_ALLOTTED) {
            return platform == null ? "Holding Units" : platform + " Holding Units";
        }
        return platform == null ? "Holding Allocation" : platform + " Holding";
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

    public static boolean containsInvestmentKeyword(String normalized) {
        boolean hasStrong = containsAny(normalized,
                "mutual fund", "mf", "sip", "folio", "allotment",
                "units allotted", "nav", "amc", "demat", "buy order");
        boolean hasWeak = containsAny(normalized,
                "units", "shares", "scheme", "coin", "investment",
                "stock", "upstox", "groww", "zerodha", "kite",
                "angel one", "icici direct", "paytm money", "et money",
                "mf central", "broker");

        // Consider it an investment keyword match if there's a strong keyword,
        // or if it has a weak keyword but isn't just a generic word without context.
        if (hasStrong) return true;
        if (hasWeak) {
            // Require at least two weak keywords if no strong ones
            int weakCount = 0;
            String[] weaks = {"units", "shares", "scheme", "coin", "investment", "stock", "upstox", "groww", "zerodha", "kite", "angel one", "icici direct", "paytm money", "et money", "mf central", "broker"};
            for (String w : weaks) {
                if (normalized.contains(w)) weakCount++;
            }
            return weakCount >= 2;
        }
        return false;
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

    @NonNull
    private static String normalizedFingerprint(@NonNull String value) {
        return sanitizeKey(value.replaceAll("\\b\\d{10,}\\b", " timestamp "));
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
