package com.spendwise.ai.sms;

import com.spendwise.ai.data.entity.Category;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SmsExpenseClassifierTest {

    @Test
    public void resolveDescription_prefersMerchantWhenPresent() {
        ParsedTransaction transaction = transaction("Airtel", "recharge successful");

        String description = SmsExpenseClassifier.resolveDescription(transaction, "SMS Import");

        assertEquals("Airtel", description);
    }

    @Test
    public void resolveDescription_usesFallbackWhenMerchantMissing() {
        ParsedTransaction transaction = transaction(null, "upi debit");

        String description = SmsExpenseClassifier.resolveDescription(transaction, "SMS Import");

        assertEquals("SMS Import", description);
    }

    @Test
    public void resolveCategoryId_mapsBillsFromRechargeContext() {
        List<Category> categories = defaultCategories();
        ParsedTransaction transaction = transaction("Airtel", "Rs 399 recharge debited from account");

        long categoryId = SmsExpenseClassifier.resolveCategoryId(categories, transaction);

        assertEquals(2L, categoryId);
    }

    @Test
    public void resolveCategoryId_mapsTransportFromCabContext() {
        List<Category> categories = defaultCategories();
        ParsedTransaction transaction = transaction("Uber", "INR 210 paid for ride");

        long categoryId = SmsExpenseClassifier.resolveCategoryId(categories, transaction);

        assertEquals(3L, categoryId);
    }

    @Test
    public void resolveCategoryId_fallsBackToOtherWhenNoKeywordMatch() {
        List<Category> categories = defaultCategories();
        ParsedTransaction transaction = transaction("UnknownMerchant", "Random transfer ref 123");

        long categoryId = SmsExpenseClassifier.resolveCategoryId(categories, transaction);

        assertEquals(4L, categoryId);
    }

    @Test
    public void resolveCategoryId_fallsBackToFirstCategoryWhenOtherMissing() {
        List<Category> categories = Collections.singletonList(category(10L, "General"));
        ParsedTransaction transaction = transaction("UnknownMerchant", "Random transfer ref 123");

        long categoryId = SmsExpenseClassifier.resolveCategoryId(categories, transaction);

        assertEquals(10L, categoryId);
    }

    private static ParsedTransaction transaction(String merchant, String message) {
        return new ParsedTransaction(
                ParsedTransaction.TransactionType.DEBIT,
                1000L,
                merchant,
                null,
                message,
                System.currentTimeMillis());
    }

    private static List<Category> defaultCategories() {
        return Arrays.asList(
                category(1L, "Food & Dining"),
                category(2L, "Bills & Utilities"),
                category(3L, "Transportation"),
                category(4L, "Other"));
    }

    private static Category category(long id, String name) {
        Category category = new Category(name, 0, null, true);
        category.setId(id);
        return category;
    }
}
