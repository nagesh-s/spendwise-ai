package com.spendwise.ai.voice;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;

/**
 * Parser-first voice understanding engine with optional on-device fallback.
 *
 * Flow:
 * 1) Deterministic parser runs first.
 * 2) If confidence is low and fallback extractor is available, attempt extraction.
 * 3) Merge only high-confidence fallback fields.
 */
public class VoiceUnderstandingEngine {

    private static final double MIN_AMOUNT_CONFIDENCE = 0.70d;
    private static final double MIN_CATEGORY_CONFIDENCE = 0.65d;
    private static final double MIN_MERCHANT_CONFIDENCE = 0.60d;

    public interface Callback {
        void onResult(UnderstandingResult result);
    }

    public interface FallbackExtractor {
        boolean isAvailable();

        FallbackExtraction extract(String transcript, List<String> categories);
    }

    public static class UnderstandingResult {
        public final VoiceExpenseParser.ParsedExpense parsedExpense;
        public final boolean fallbackAttempted;
        public final boolean fallbackImprovedResult;

        public UnderstandingResult(VoiceExpenseParser.ParsedExpense parsedExpense, boolean fallbackAttempted,
                boolean fallbackImprovedResult) {
            this.parsedExpense = parsedExpense;
            this.fallbackAttempted = fallbackAttempted;
            this.fallbackImprovedResult = fallbackImprovedResult;
        }
    }

    public static class FallbackExtraction {
        public Long amountCents;
        public Double amountConfidence;
        public String merchant;
        public Double merchantConfidence;
        public String category;
        public Double categoryConfidence;
    }

    private final VoiceExpenseParser parser;
    private final FallbackExtractor fallbackExtractor;
    private final Executor backgroundExecutor;

    public VoiceUnderstandingEngine(VoiceExpenseParser parser, FallbackExtractor fallbackExtractor,
            Executor backgroundExecutor) {
        this.parser = parser;
        this.fallbackExtractor = fallbackExtractor;
        this.backgroundExecutor = backgroundExecutor;
    }

    public void understandAsync(String transcript, List<String> categories, Callback callback) {
        List<String> safeCategories = categories != null ? categories : Collections.emptyList();
        VoiceExpenseParser.ParsedExpense parsed = parser.parse(transcript, safeCategories);

        if (!shouldAttemptFallback(parsed, transcript) || fallbackExtractor == null || !fallbackExtractor.isAvailable()) {
            callback.onResult(new UnderstandingResult(parsed, false, false));
            return;
        }

        backgroundExecutor.execute(() -> {
            FallbackExtraction extraction = fallbackExtractor.extract(transcript, safeCategories);
            if (extraction == null) {
                callback.onResult(new UnderstandingResult(parsed, true, false));
                return;
            }

            VoiceExpenseParser.ParsedExpense merged = merge(parsed, extraction, safeCategories, transcript);
            boolean improved = confidenceScore(merged) > confidenceScore(parsed);
            callback.onResult(new UnderstandingResult(merged, true, improved));
        });
    }

    public UnderstandingResult understandSync(String transcript, List<String> categories) {
        List<String> safeCategories = categories != null ? categories : Collections.emptyList();
        VoiceExpenseParser.ParsedExpense parsed = parser.parse(transcript, safeCategories);

        if (!shouldAttemptFallback(parsed, transcript) || fallbackExtractor == null || !fallbackExtractor.isAvailable()) {
            return new UnderstandingResult(parsed, false, false);
        }

        FallbackExtraction extraction = fallbackExtractor.extract(transcript, safeCategories);
        if (extraction == null) {
            return new UnderstandingResult(parsed, true, false);
        }

        VoiceExpenseParser.ParsedExpense merged = merge(parsed, extraction, safeCategories, transcript);
        boolean improved = confidenceScore(merged) > confidenceScore(parsed);
        return new UnderstandingResult(merged, true, improved);
    }

    private boolean shouldAttemptFallback(VoiceExpenseParser.ParsedExpense parsed, String transcript) {
        return transcript != null && !transcript.trim().isEmpty() && !parsed.isReadyForAutoSave();
    }

    private VoiceExpenseParser.ParsedExpense merge(VoiceExpenseParser.ParsedExpense base, FallbackExtraction fallback,
            List<String> categories, String transcript) {
        VoiceExpenseParser.ParsedExpense merged = copy(base);
        merged.description = !isBlank(base.description)
                ? base.description.trim()
                : compactDescription(transcript);

        if (!merged.amountSure && fallback.amountCents != null && fallback.amountCents > 0
                && normalizeConfidence(fallback.amountConfidence) >= MIN_AMOUNT_CONFIDENCE) {
            merged.amountCents = fallback.amountCents;
            merged.amountSure = true;
        }

        if (!merged.categorySure && fallback.category != null
                && normalizeConfidence(fallback.categoryConfidence) >= MIN_CATEGORY_CONFIDENCE) {
            String resolvedCategory = resolveCategoryName(fallback.category, categories);
            if (resolvedCategory != null) {
                merged.category = resolvedCategory;
                merged.categorySure = true;
            }
        }

        if ((!merged.merchantSure || isBlank(merged.merchant))
                && !isBlank(fallback.merchant)
                && normalizeConfidence(fallback.merchantConfidence) >= MIN_MERCHANT_CONFIDENCE) {
            merged.merchant = fallback.merchant.trim();
            merged.merchantSure = true;
        }

        return merged;
    }

    private int confidenceScore(VoiceExpenseParser.ParsedExpense parsed) {
        int score = 0;
        if (parsed.amountSure) {
            score += 2;
        } else if (parsed.amountCents > 0) {
            score += 1;
        }
        if (parsed.categorySure) {
            score += 2;
        } else if (!isBlank(parsed.category)) {
            score += 1;
        }
        if (parsed.merchantSure) {
            score += 2;
        } else if (!isBlank(parsed.merchant)) {
            score += 1;
        }
        return score;
    }

    private VoiceExpenseParser.ParsedExpense copy(VoiceExpenseParser.ParsedExpense source) {
        VoiceExpenseParser.ParsedExpense copy = new VoiceExpenseParser.ParsedExpense();
        copy.amountCents = source.amountCents;
        copy.amountSure = source.amountSure;
        copy.category = source.category;
        copy.categorySure = source.categorySure;
        copy.merchant = source.merchant;
        copy.merchantSure = source.merchantSure;
        copy.description = source.description;
        return copy;
    }

    private String resolveCategoryName(String fallbackCategory, List<String> categories) {
        if (isBlank(fallbackCategory) || categories == null || categories.isEmpty()) {
            return null;
        }
        for (String category : categories) {
            if (category.equalsIgnoreCase(fallbackCategory.trim())) {
                return category;
            }
        }

        String normalizedFallbackCategory = normalize(fallbackCategory);
        for (String category : categories) {
            String normalizedCategory = normalize(category);
            if (normalizedCategory.equals(normalizedFallbackCategory)) {
                return category;
            }
        }
        return null;
    }

    private double normalizeConfidence(Double confidence) {
        if (confidence == null || confidence.isNaN()) {
            return 0d;
        }
        if (confidence < 0d) {
            return 0d;
        }
        if (confidence > 1d) {
            return 1d;
        }
        return confidence;
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String compactDescription(String transcript) {
        if (transcript == null) {
            return "";
        }
        String compact = transcript.trim().replaceAll("\\s+", " ");
        if (compact.length() <= 72) {
            return compact;
        }
        return compact.substring(0, 72).trim();
    }
}
