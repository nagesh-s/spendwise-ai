package com.spendwise.ai.sms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.entity.Category;

import java.util.List;
import java.util.Locale;

/**
 * Maps parsed SMS transactions to the best expense category and description.
 */
public final class SmsExpenseClassifier {

    private SmsExpenseClassifier() {
    }

    public static String resolveDescription(@NonNull ParsedTransaction transaction, @NonNull String fallback) {
        String merchant = transaction.getMerchant();
        if (merchant != null && !merchant.trim().isEmpty()) {
            return merchant.trim();
        }
        return fallback;
    }

    public static long resolveCategoryId(@NonNull List<Category> categories, @NonNull ParsedTransaction transaction) {
        if (categories.isEmpty()) {
            return 1L;
        }

        String context = normalize(
                (transaction.getMerchant() == null ? "" : transaction.getMerchant()) + " "
                        + (transaction.getOriginalMessage() == null ? "" : transaction.getOriginalMessage()));

        if (containsAny(context, "uber", "ola", "taxi", "cab", "metro", "train", "bus", "fuel", "petrol", "diesel",
                "parking", "toll")) {
            Long id = findCategoryId(categories, "transportation", "transport", "travel");
            if (id != null) {
                return id;
            }
        }

        if (containsAny(context, "electricity", "water", "internet", "wifi", "broadband", "bill", "recharge", "rent",
                "postpaid", "prepaid", "gas")) {
            Long id = findCategoryId(categories, "bills", "utilities", "bill");
            if (id != null) {
                return id;
            }
        }

        if (containsAny(context, "amazon", "flipkart", "myntra", "shopping", "store", "mart", "retail", "purchase")) {
            Long id = findCategoryId(categories, "shopping");
            if (id != null) {
                return id;
            }
        }

        if (containsAny(context, "movie", "netflix", "spotify", "prime", "hotstar", "subscription", "game",
                "entertainment")) {
            Long id = findCategoryId(categories, "entertainment");
            if (id != null) {
                return id;
            }
        }

        if (containsAny(context, "pharmacy", "hospital", "doctor", "clinic", "medicine", "health", "fitness", "gym")) {
            Long id = findCategoryId(categories, "health", "fitness", "medical");
            if (id != null) {
                return id;
            }
        }

        if (containsAny(context, "school", "college", "course", "tuition", "class", "education", "book")) {
            Long id = findCategoryId(categories, "education", "learning");
            if (id != null) {
                return id;
            }
        }

        if (containsAny(context, "restaurant", "food", "dining", "swiggy", "zomato", "cafe", "coffee", "milk",
                "grocery", "groceries")) {
            Long id = findCategoryId(categories, "food", "dining", "grocery");
            if (id != null) {
                return id;
            }
        }

        Long otherId = findCategoryId(categories, "other", "misc");
        if (otherId != null) {
            return otherId;
        }
        return categories.get(0).getId();
    }

    @Nullable
    private static Long findCategoryId(@NonNull List<Category> categories, String... nameHints) {
        for (Category category : categories) {
            String name = normalize(category.getName());
            for (String hint : nameHints) {
                if (name.contains(normalize(hint))) {
                    return category.getId();
                }
            }
        }
        return null;
    }

    private static boolean containsAny(String text, String... keywords) {
        String normalized = normalize(text);
        for (String keyword : keywords) {
            if (normalized.contains(normalize(keyword))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        return input.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", " ").trim();
    }
}
