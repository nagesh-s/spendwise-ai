package com.spendwise.ai.voice;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight, fully on-device fallback extractor used when parser confidence is low.
 *
 * This keeps voice understanding offline while improving extraction for natural speech,
 * including number words such as "two hundred fifty".
 */
public class OnDeviceSemanticExpenseExtractor implements VoiceUnderstandingEngine.FallbackExtractor {

    private static final Pattern CURRENCY_PREFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(?:₹|rs\\.?|inr|rupees?)\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)");
    private static final Pattern CURRENCY_SUFFIX_AMOUNT_PATTERN = Pattern.compile(
            "(?i)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\s*(?:₹|rs\\.?|inr|rupees?)\\b");
    private static final Pattern ACTION_AMOUNT_PATTERN = Pattern.compile(
            "(?i)\\b(?:spent|spend|paid|pay|add|expense|cost|for)\\s+(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)\\b");
    private static final Pattern GENERIC_AMOUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)(?!\\d)");

    private static final Pattern MERCHANT_PATTERN = Pattern.compile(
            "(?i)\\b(?:at|from|to|in|via)\\s+([\\p{L}0-9][\\p{L}0-9 '&.\\-]{1,60})");
    private static final Pattern PAID_TO_MERCHANT_PATTERN = Pattern.compile(
            "(?i)\\b(?:paid|pay|spent|spend)\\b(?:\\s+\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)?\\s+to\\s+([\\p{L}0-9][\\p{L}0-9 '&.\\-]{1,60})");

    private static final String[] AMOUNT_CONTEXT_HINTS = {
            "spent", "spend", "paid", "pay", "for", "expense", "cost", "amount", "rs", "inr", "rupee"
    };

    private static final String[] MERCHANT_STOP_WORDS = {
            "for", "on", "using", "with", "category", "under", "towards", "today", "yesterday", "tomorrow"
    };

    private static final String[] NUMBER_JOINER_WORDS = {
            "and"
    };

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

    private static final Map<String, Integer> NUMBER_WORDS;
    private static final Map<String, Integer> TENS_WORDS;
    private static final Map<String, Integer> SCALE_WORDS;

    static {
        Map<String, Integer> numberWords = new HashMap<>();
        numberWords.put("zero", 0);
        numberWords.put("one", 1);
        numberWords.put("two", 2);
        numberWords.put("three", 3);
        numberWords.put("four", 4);
        numberWords.put("five", 5);
        numberWords.put("six", 6);
        numberWords.put("seven", 7);
        numberWords.put("eight", 8);
        numberWords.put("nine", 9);
        numberWords.put("ten", 10);
        numberWords.put("eleven", 11);
        numberWords.put("twelve", 12);
        numberWords.put("thirteen", 13);
        numberWords.put("fourteen", 14);
        numberWords.put("fifteen", 15);
        numberWords.put("sixteen", 16);
        numberWords.put("seventeen", 17);
        numberWords.put("eighteen", 18);
        numberWords.put("nineteen", 19);
        NUMBER_WORDS = Collections.unmodifiableMap(numberWords);

        Map<String, Integer> tensWords = new HashMap<>();
        tensWords.put("twenty", 20);
        tensWords.put("thirty", 30);
        tensWords.put("forty", 40);
        tensWords.put("fifty", 50);
        tensWords.put("sixty", 60);
        tensWords.put("seventy", 70);
        tensWords.put("eighty", 80);
        tensWords.put("ninety", 90);
        TENS_WORDS = Collections.unmodifiableMap(tensWords);

        Map<String, Integer> scaleWords = new HashMap<>();
        scaleWords.put("hundred", 100);
        scaleWords.put("thousand", 1000);
        scaleWords.put("lakh", 100000);
        scaleWords.put("lac", 100000);
        scaleWords.put("million", 1000000);
        SCALE_WORDS = Collections.unmodifiableMap(scaleWords);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public VoiceUnderstandingEngine.FallbackExtraction extract(String transcript, List<String> categories) {
        if (isBlank(transcript)) {
            return null;
        }

        String spokenText = transcript.trim();
        String normalizedText = normalize(spokenText);
        List<String> safeCategories = categories != null ? categories : Collections.emptyList();

        VoiceUnderstandingEngine.FallbackExtraction extraction = new VoiceUnderstandingEngine.FallbackExtraction();

        AmountCandidate amountCandidate = extractAmount(spokenText, normalizedText);
        if (amountCandidate != null) {
            extraction.amountCents = amountCandidate.amountCents;
            extraction.amountConfidence = amountCandidate.confidence;
        }

        CategoryCandidate categoryCandidate = extractCategory(normalizedText, safeCategories);
        if (categoryCandidate != null) {
            extraction.category = categoryCandidate.category;
            extraction.categoryConfidence = categoryCandidate.confidence;
        }

        MerchantCandidate merchantCandidate = extractMerchant(spokenText);
        if (merchantCandidate != null) {
            extraction.merchant = merchantCandidate.merchant;
            extraction.merchantConfidence = merchantCandidate.confidence;
        }

        if (extraction.amountCents == null
                && extraction.category == null
                && extraction.merchant == null) {
            return null;
        }

        return extraction;
    }

    private AmountCandidate extractAmount(String spokenText, String normalizedText) {
        AmountCandidate candidate = extractAmountByPattern(CURRENCY_PREFIX_AMOUNT_PATTERN, spokenText, 0.94d);
        if (candidate != null) {
            return candidate;
        }

        candidate = extractAmountByPattern(CURRENCY_SUFFIX_AMOUNT_PATTERN, spokenText, 0.94d);
        if (candidate != null) {
            return candidate;
        }

        candidate = extractAmountByPattern(ACTION_AMOUNT_PATTERN, spokenText, 0.91d);
        if (candidate != null) {
            return candidate;
        }

        candidate = extractBestGenericNumericAmount(spokenText, normalizedText);
        if (candidate != null) {
            return candidate;
        }

        return extractAmountFromNumberWords(normalizedText);
    }

    private AmountCandidate extractAmountByPattern(Pattern pattern, String text, double confidence) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        Long amountCents = parseAmountCents(matcher.group(1));
        if (amountCents == null || amountCents <= 0) {
            return null;
        }
        return new AmountCandidate(amountCents, confidence);
    }

    private AmountCandidate extractBestGenericNumericAmount(String spokenText, String normalizedText) {
        Matcher matcher = GENERIC_AMOUNT_PATTERN.matcher(spokenText);
        AmountCandidate bestCandidate = null;
        double bestScore = 0d;

        while (matcher.find()) {
            Long amountCents = parseAmountCents(matcher.group(1));
            if (amountCents == null || amountCents <= 0) {
                continue;
            }

            String context = normalize(getMatchContext(spokenText, matcher.start(), matcher.end()));
            boolean contextual = hasAmountContext(context);
            double confidence = contextual ? 0.76d : 0.58d;
            if (amountCents <= 99999999L) {
                confidence += 0.04d;
            }

            if (confidence > bestScore) {
                bestScore = confidence;
                bestCandidate = new AmountCandidate(amountCents, clampConfidence(confidence));
            }
        }

        if (bestCandidate != null && hasAmountContext(normalizedText)) {
            return bestCandidate;
        }
        return bestCandidate != null && bestCandidate.confidence >= 0.62d ? bestCandidate : null;
    }

    private AmountCandidate extractAmountFromNumberWords(String normalizedText) {
        if (isBlank(normalizedText)) {
            return null;
        }

        String[] tokens = normalizedText.split(" ");
        AmountCandidate best = null;

        for (int i = 0; i < tokens.length; i++) {
            NumberWordParse parsed = parseNumberWords(tokens, i);
            if (parsed == null || parsed.consumedTokens <= 0 || parsed.rupees <= 0) {
                continue;
            }

            long amountCents = parsed.rupees * 100L;
            boolean contextual = hasAmountContext(getTokenWindow(tokens, i, parsed.consumedTokens));
            double confidence = contextual ? 0.80d : 0.63d;
            if (parsed.consumedTokens >= 3) {
                confidence += 0.04d;
            }
            if (best == null || confidence > best.confidence) {
                best = new AmountCandidate(amountCents, clampConfidence(confidence));
            }

            i += parsed.consumedTokens - 1;
        }

        return best;
    }

    private NumberWordParse parseNumberWords(String[] tokens, int startIndex) {
        if (startIndex < 0 || startIndex >= tokens.length) {
            return null;
        }

        long total = 0L;
        long current = 0L;
        int consumed = 0;
        boolean sawNumber = false;

        for (int i = startIndex; i < tokens.length; i++) {
            String token = tokens[i];

            if (isJoiner(token) && sawNumber) {
                consumed++;
                continue;
            }

            Integer numberValue = NUMBER_WORDS.get(token);
            if (numberValue != null) {
                current += numberValue;
                consumed++;
                sawNumber = true;
                continue;
            }

            Integer tensValue = TENS_WORDS.get(token);
            if (tensValue != null) {
                current += tensValue;
                consumed++;
                sawNumber = true;
                continue;
            }

            Integer scale = SCALE_WORDS.get(token);
            if (scale != null) {
                if (scale == 100) {
                    if (current == 0) {
                        current = 1;
                    }
                    current *= scale;
                } else {
                    if (current == 0) {
                        current = 1;
                    }
                    total += current * scale;
                    current = 0;
                }
                consumed++;
                sawNumber = true;
                continue;
            }

            break;
        }

        if (!sawNumber || consumed == 0) {
            return null;
        }

        long amount = total + current;
        amount = Math.max(amount, parseColloquialHundreds(tokens, startIndex, consumed));
        if (amount <= 0) {
            return null;
        }
        return new NumberWordParse(amount, consumed);
    }

    private long parseColloquialHundreds(String[] tokens, int startIndex, int consumedTokens) {
        if (consumedTokens < 2) {
            return 0L;
        }

        Integer first = NUMBER_WORDS.get(tokens[startIndex]);
        if (first == null || first <= 0 || first >= 10) {
            return 0L;
        }

        if (consumedTokens == 2) {
            Integer secondTens = TENS_WORDS.get(tokens[startIndex + 1]);
            if (secondTens != null) {
                return (first * 100L) + secondTens;
            }
            Integer secondUnits = NUMBER_WORDS.get(tokens[startIndex + 1]);
            if (secondUnits != null && secondUnits < 100) {
                return (first * 100L) + secondUnits;
            }
            return 0L;
        }

        if (consumedTokens >= 3) {
            Integer secondTens = TENS_WORDS.get(tokens[startIndex + 1]);
            Integer thirdUnits = NUMBER_WORDS.get(tokens[startIndex + 2]);
            if (secondTens != null && thirdUnits != null && thirdUnits < 10) {
                return (first * 100L) + secondTens + thirdUnits;
            }
        }

        return 0L;
    }

    private CategoryCandidate extractCategory(String normalizedText, List<String> categories) {
        if (isBlank(normalizedText) || categories == null || categories.isEmpty()) {
            return null;
        }

        for (String category : categories) {
            String normalizedCategory = normalize(category);
            if (!isBlank(normalizedCategory) && containsPhrase(normalizedText, normalizedCategory)) {
                return new CategoryCandidate(category, 0.95d);
            }
        }

        String bestCategory = null;
        int bestScore = 0;

        for (String category : categories) {
            String normalizedCategory = normalize(category);
            if (isBlank(normalizedCategory)) {
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
            return null;
        }

        double confidence = clampConfidence(0.50d + (bestScore * 0.08d));
        return new CategoryCandidate(bestCategory, confidence);
    }

    private MerchantCandidate extractMerchant(String spokenText) {
        String merchant = extractMerchantByPattern(PAID_TO_MERCHANT_PATTERN, spokenText);
        if (!isBlank(merchant)) {
            return new MerchantCandidate(merchant, 0.80d);
        }

        merchant = extractMerchantByPattern(MERCHANT_PATTERN, spokenText);
        if (!isBlank(merchant)) {
            return new MerchantCandidate(merchant, 0.74d);
        }

        return null;
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

        String lower = cleaned.toLowerCase(Locale.US);
        int cutIndex = cleaned.length();
        for (String stopWord : MERCHANT_STOP_WORDS) {
            int idx = indexOfWord(lower, stopWord);
            if (idx != -1 && idx < cutIndex) {
                cutIndex = idx;
            }
        }

        cleaned = cleaned.substring(0, cutIndex).trim().replaceAll("[,.:;\\-]+$", "");
        if (cleaned.length() < 2 || cleaned.matches("(?i)^(rs|inr|rupees?)$")) {
            return null;
        }
        return cleaned;
    }

    private Long parseAmountCents(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            double amount = Double.parseDouble(raw.replace(",", ""));
            if (amount <= 0) {
                return null;
            }
            return Math.round(amount * 100d);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean hasAmountContext(String normalizedText) {
        if (isBlank(normalizedText)) {
            return false;
        }
        for (String hint : AMOUNT_CONTEXT_HINTS) {
            if (containsWord(normalizedText, hint)) {
                return true;
            }
        }
        return false;
    }

    private String getTokenWindow(String[] tokens, int start, int consumed) {
        int from = Math.max(0, start - 3);
        int to = Math.min(tokens.length, start + consumed + 3);
        StringBuilder builder = new StringBuilder();
        for (int i = from; i < to; i++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens[i]);
        }
        return builder.toString();
    }

    private String getMatchContext(String text, int start, int end) {
        int contextStart = Math.max(0, start - 20);
        int contextEnd = Math.min(text.length(), end + 20);
        return text.substring(contextStart, contextEnd);
    }

    private int indexOfWord(String text, String word) {
        Matcher matcher = Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(text);
        return matcher.find() ? matcher.start() : -1;
    }

    private boolean containsPhrase(String text, String phrase) {
        return (" " + text + " ").contains(" " + phrase + " ");
    }

    private boolean containsWord(String text, String word) {
        return (" " + text + " ").contains(" " + word + " ");
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.US)
                .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private boolean isJoiner(String token) {
        if (token == null) {
            return false;
        }
        for (String joiner : NUMBER_JOINER_WORDS) {
            if (joiner.equals(token)) {
                return true;
            }
        }
        return false;
    }

    private double clampConfidence(double value) {
        if (value < 0d) {
            return 0d;
        }
        if (value > 1d) {
            return 1d;
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class AmountCandidate {
        private final long amountCents;
        private final double confidence;

        AmountCandidate(long amountCents, double confidence) {
            this.amountCents = amountCents;
            this.confidence = confidence;
        }
    }

    private static final class CategoryCandidate {
        private final String category;
        private final double confidence;

        CategoryCandidate(String category, double confidence) {
            this.category = category;
            this.confidence = confidence;
        }
    }

    private static final class MerchantCandidate {
        private final String merchant;
        private final double confidence;

        MerchantCandidate(String merchant, double confidence) {
            this.merchant = merchant;
            this.confidence = confidence;
        }
    }

    private static final class NumberWordParse {
        private final long rupees;
        private final int consumedTokens;

        NumberWordParse(long rupees, int consumedTokens) {
            this.rupees = rupees;
            this.consumedTokens = consumedTokens;
        }
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
}
