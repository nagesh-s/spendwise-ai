package com.spendwise.ai.voice;

import java.util.ArrayList;
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
        public String merchant;
        public String category;
        public String description;

        public boolean isValid() {
            return amountCents > 0;
        }
    }

    // "250" or "250.50"
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");

    // "at Starbucks", "from Amazon", "in Uber"
    private static final Pattern MERCHANT_PATTERN = Pattern.compile("\\b(at|from|in)\\s+([A-Za-z0-9\\s']+)");

    /**
     * Parse the spoken text into structured expense data.
     * 
     * @param text       The recognized speech text
     * @param categories List of available category names to match against
     */
    public ParsedExpense parse(String text, List<String> categories) {
        ParsedExpense result = new ParsedExpense();
        String lowerText = text.toLowerCase();

        // 1. Extract Amount
        // Look for numbers. If multiple, take the first one that looks like a price.
        Matcher amountMatcher = AMOUNT_PATTERN.matcher(text);
        if (amountMatcher.find()) {
            try {
                double amount = Double.parseDouble(amountMatcher.group(1));
                result.amountCents = (long) (amount * 100);
            } catch (NumberFormatException e) {
                // Ignore invalid numbers
            }
        }

        // 2. Extract Category
        // Simple keyword matching against provided category names
        for (String cat : categories) {
            if (lowerText.contains(cat.toLowerCase())) {
                result.category = cat;
                break; // Take first match
            }
        }

        // If no direct category match, try to infer from common keywords?
        // For now, let's stick to direct category name matching for simplicity &
        // accuracy.

        // 3. Extract Merchant
        Matcher merchantMatcher = MERCHANT_PATTERN.matcher(text);
        if (merchantMatcher.find()) {
            // Group 2 is the merchant name
            String rawMerchant = merchantMatcher.group(2).trim();
            // Cleanup: stop at common prepositions or end of sentence
            result.merchant = cleanMerchantName(rawMerchant);
        }

        // 4. Description is the original text (cleaned up)
        result.description = text;

        return result;
    }

    private String cleanMerchantName(String raw) {
        // Stop at "for", "on", "using", "with"
        String[] stopWords = { " for ", " on ", " using ", " with ", " category " };
        String lower = raw.toLowerCase();

        int cutIndex = raw.length();
        for (String word : stopWords) {
            int idx = lower.indexOf(word);
            if (idx != -1 && idx < cutIndex) {
                cutIndex = idx;
            }
        }
        return raw.substring(0, cutIndex).trim();
    }
}
