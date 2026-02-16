package com.spendwise.ai.voice;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses voice transcripts to extract expense details.
 * Extracts: Amount, Merchant, Category, Description.
 */
public class VoiceExpenseParser {

    public static class ParsedExpense {
        public long amountCents;
        public boolean amountSure;
        public String merchant;
        public boolean merchantSure;
        public String category;
        public boolean categorySure;
        public String description;

        public boolean isValid() {
            return amountCents > 0;
        }

        public boolean isReadyForAutoSave() {
            return amountCents > 0
                    && amountSure
                    && category != null
                    && categorySure
                    && (merchant == null || merchantSure);
        }
    }

    private static final Pattern CURRENCY_PREFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr|rupees?)\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)");
    private static final Pattern CURRENCY_SUFFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(?:₹|rs\\.?|inr|rupees?)\\b");
    private static final Pattern ACTION_AMOUNT_PATTERN = Pattern.compile(
            "(?i)\\b(?:spent|spend|paid|pay|add|expense|cost|for)\\s+(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\b");
    private static final Pattern GENERIC_AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)(?!\\d)");

    // "at Starbucks", "from Amazon", "in Uber", "via Paytm"
    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?i)\\b(?:at|from|to|in|via)\\s+([\\p{L}0-9][\\p{L}0-9 '&.\\-]{1,60})");
    private static final Pattern PAID_TO_MERCHANT_PATTERN = Pattern.compile(
            "(?i)\\b(?:paid|pay)\\b(?:\\s+\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)?\\s+to\\s+([\\p{L}0-9][\\p{L}0-9 '&.\\-]{1,60})");

    private static final String[] AMOUNT_CONTEXT_HINTS = {
            "spent", "spend", "paid", "pay", "for", "expense", "cost", "amount", "rs", "inr", "rupee"
    };

    private static final String[] MERCHANT_STOP_WORDS = {
            "for", "on", "using", "with", "category", "under", "towards", "today", "yesterday", "tomorrow"
    };

    private static final int AMOUNT_SURE_THRESHOLD_GENERIC = 4;
    private static final int CATEGORY_SURE_THRESHOLD = 4;

    private static final List<CategoryHintRule> CATEGORY_HINT_RULES = Arrays.asList(
            new CategoryHintRule(
                    new String[] { "food", "dining", "grocery", "groceries", "restaurant", "meal" },
                    new String[] { "food", "lunch", "dinner", "breakfast", "snack", "coffee", "tea", "cafe",
                            "restaurant", "grocery", "groceries", "milk", "pizza", "burger" }),
            new CategoryHintRule(
                    new String[] { "transport", "travel", "fuel", "commute", "car", "vehicle" },
                    new String[] { "uber", "ola", "taxi", "cab", "bus", "metro", "train", "fuel", "petrol",
                            "diesel", "parking", "toll" }),
            new CategoryHintRule(
                    new String[] { "bill", "utilities", "utility", "recharge", "internet", "electricity", "rent" },
                    new String[] { "bill", "electricity", "water", "wifi", "internet", "broadband", "recharge",
                            "phone", "mobile", "rent", "maintenance", "gas" }),
            new CategoryHintRule(
                    new String[] { "entertainment", "fun", "movie", "subscription" },
                    new String[] { "movie", "netflix", "spotify", "prime", "game", "gaming", "concert",
                            "entertainment", "subscription" }),
            new CategoryHintRule(
                    new String[] { "shopping", "personal", "care", "fashion" },
                    new String[] { "amazon", "flipkart", "shopping", "clothes", "fashion", "shoes", "cosmetic",
                            "makeup", "skincare" }),
            new CategoryHintRule(
                    new String[] { "health", "fitness", "medical", "pharmacy" },
                    new String[] { "doctor", "medicine", "hospital", "clinic", "pharmacy", "gym", "health" }),
            new CategoryHintRule(
                    new String[] { "education", "learning", "course" },
                    new String[] { "school", "college", "course", "tuition", "class", "book", "education" }));

    /**
     * Parse the spoken text into structured expense data.
     * 
     * @param text       The recognized speech text
     * @param categories List of available category names to match against
     */
    public ParsedExpense parse(String text, List<String> categories) {
        ParsedExpense result = new ParsedExpense();
        String spokenText = text == null ? "" : text.trim();
        List<String> categoryList = categories != null ? categories : Collections.emptyList();

        AmountExtraction amountExtraction = extractAmount(spokenText);
        result.amountCents = amountExtraction.amountCents;
        result.amountSure = amountExtraction.sure;

        CategoryExtraction categoryExtraction = extractCategory(spokenText, categoryList);
        result.category = categoryExtraction.category;
        result.categorySure = categoryExtraction.sure;

        MerchantExtraction merchantExtraction = extractMerchant(spokenText);
        result.merchant = merchantExtraction.merchant;
        result.merchantSure = merchantExtraction.sure;
        result.description = spokenText;

        return result;
    }

    private AmountExtraction extractAmount(String text) {
        AmountCandidate candidate = extractAmountByPattern(CURRENCY_PREFIX_AMOUNT_PATTERN, text);
        if (candidate != null) {
            return AmountExtraction.sure(candidate);
        }

        candidate = extractAmountByPattern(CURRENCY_SUFFIX_AMOUNT_PATTERN, text);
        if (candidate != null) {
            return AmountExtraction.sure(candidate);
        }

        candidate = extractAmountByPattern(ACTION_AMOUNT_PATTERN, text);
        if (candidate != null) {
            return AmountExtraction.sure(candidate);
        }

        AmountCandidate genericCandidate = extractBestGenericAmount(text);
        if (genericCandidate == null || genericCandidate.amount <= 0) {
            return AmountExtraction.empty();
        }
        return new AmountExtraction(
                Math.round(genericCandidate.amount * 100d),
                genericCandidate.score >= AMOUNT_SURE_THRESHOLD_GENERIC);
    }

    private AmountCandidate extractAmountByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        Double amount = parseAmount(matcher.group(1));
        if (amount == null || amount <= 0) {
            return null;
        }
        return new AmountCandidate(amount, 10);
    }

    private AmountCandidate extractBestGenericAmount(String text) {
        Matcher matcher = GENERIC_AMOUNT_PATTERN.matcher(text);
        AmountCandidate bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;

        while (matcher.find()) {
            String rawAmount = matcher.group(1);
            Double amount = parseAmount(rawAmount);
            if (amount == null || amount <= 0) {
                continue;
            }

            int score = 0;
            String context = getMatchContext(text, matcher.start(), matcher.end());
            for (String hint : AMOUNT_CONTEXT_HINTS) {
                if (containsWord(context, hint)) {
                    score += 2;
                }
            }
            if (rawAmount.contains(".")) {
                score += 1;
            }
            if (amount < 1000000) {
                score += 1;
            }

            if (score > bestScore) {
                bestScore = score;
                bestCandidate = new AmountCandidate(amount, score);
            }
        }
        return bestCandidate;
    }

    private Double parseAmount(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(raw.replace(",", ""));
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private CategoryExtraction extractCategory(String text, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return CategoryExtraction.empty();
        }
        String normalizedText = normalize(text);
        if (normalizedText.isEmpty()) {
            return CategoryExtraction.empty();
        }

        // Exact phrase match gets highest priority.
        for (String category : categories) {
            String normalizedCategory = normalize(category);
            if (!normalizedCategory.isEmpty() && containsPhrase(normalizedText, normalizedCategory)) {
                return CategoryExtraction.sure(category);
            }
        }

        String bestCategory = null;
        int bestScore = 0;

        for (String category : categories) {
            String normalizedCategory = normalize(category);
            if (normalizedCategory.isEmpty()) {
                continue;
            }

            int score = 0;
            String[] categoryTokens = normalizedCategory.split(" ");
            for (String token : categoryTokens) {
                if (token.length() >= 3 && containsWord(normalizedText, token)) {
                    score += 2;
                }
            }

            for (CategoryHintRule rule : CATEGORY_HINT_RULES) {
                if (rule.matchesCategory(normalizedCategory)) {
                    score += rule.countSpeechHits(normalizedText) * 3;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestCategory = category;
            }
        }

        if (bestCategory == null || bestScore <= 0) {
            return CategoryExtraction.empty();
        }
        return new CategoryExtraction(bestCategory, bestScore >= CATEGORY_SURE_THRESHOLD);
    }

    private MerchantExtraction extractMerchant(String text) {
        String merchant = extractMerchantByPattern(PAID_TO_MERCHANT_PATTERN, text);
        if (merchant != null) {
            return MerchantExtraction.sure(merchant);
        }
        merchant = extractMerchantByPattern(MERCHANT_PATTERN, text);
        if (merchant != null) {
            return MerchantExtraction.sure(merchant);
        }
        return MerchantExtraction.empty();
    }

    private String extractMerchantByPattern(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return cleanMerchantName(matcher.group(1));
    }

    private String cleanMerchantName(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.trim().replaceAll("^[\\s,.:;\\-]+|[\\s,.:;\\-]+$", "");
        if (cleaned.isEmpty()) {
            return null;
        }

        String lower = cleaned.toLowerCase();
        int cutIndex = cleaned.length();
        for (String word : MERCHANT_STOP_WORDS) {
            int idx = indexOfWord(lower, word);
            if (idx != -1 && idx < cutIndex) {
                cutIndex = idx;
            }
        }
        cleaned = cleaned.substring(0, cutIndex).trim().replaceAll("[,.:;\\-]+$", "");

        return cleaned.length() >= 2 ? cleaned : null;
    }

    private int indexOfWord(String text, String word) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private String getMatchContext(String text, int start, int end) {
        int contextStart = Math.max(0, start - 20);
        int contextEnd = Math.min(text.length(), end + 20);
        return normalize(text.substring(contextStart, contextEnd));
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase()
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private boolean containsWord(String text, String word) {
        return (" " + text + " ").contains(" " + word + " ");
    }

    private static final class CategoryHintRule {
        private final String[] categoryTokens;
        private final String[] speechKeywords;

        CategoryHintRule(String[] categoryTokens, String[] speechKeywords) {
            this.categoryTokens = categoryTokens;
            this.speechKeywords = speechKeywords;
        }

        boolean matchesCategory(String normalizedCategory) {
            for (String token : categoryTokens) {
                if (normalizedCategory.contains(token)) {
                    return true;
                }
            }
            return false;
        }

        int countSpeechHits(String normalizedSpeechText) {
            int hits = 0;
            for (String keyword : speechKeywords) {
                if ((" " + normalizedSpeechText + " ").contains(" " + keyword + " ")) {
                    hits++;
                }
            }
            return hits;
        }
    }

    private static final class AmountCandidate {
        private final double amount;
        private final int score;

        AmountCandidate(double amount, int score) {
            this.amount = amount;
            this.score = score;
        }
    }

    private static final class AmountExtraction {
        private final long amountCents;
        private final boolean sure;

        AmountExtraction(long amountCents, boolean sure) {
            this.amountCents = amountCents;
            this.sure = sure;
        }

        static AmountExtraction sure(AmountCandidate candidate) {
            return new AmountExtraction(Math.round(candidate.amount * 100d), true);
        }

        static AmountExtraction empty() {
            return new AmountExtraction(0L, false);
        }
    }

    private static final class CategoryExtraction {
        private final String category;
        private final boolean sure;

        CategoryExtraction(String category, boolean sure) {
            this.category = category;
            this.sure = sure;
        }

        static CategoryExtraction sure(String category) {
            return new CategoryExtraction(category, true);
        }

        static CategoryExtraction empty() {
            return new CategoryExtraction(null, false);
        }
    }

    private static final class MerchantExtraction {
        private final String merchant;
        private final boolean sure;

        MerchantExtraction(String merchant, boolean sure) {
            this.merchant = merchant;
            this.sure = sure;
        }

        static MerchantExtraction sure(String merchant) {
            return new MerchantExtraction(merchant, true);
        }

        static MerchantExtraction empty() {
            return new MerchantExtraction(null, false);
        }
    }
}
