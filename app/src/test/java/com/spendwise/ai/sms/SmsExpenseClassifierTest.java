package com.spendwise.ai.sms;

import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Category;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SmsExpenseClassifierTest {

    @Test
    public void resolveDescription_prefersContextLabelOverMerchant() {
        ParsedTransaction transaction = transaction("Airtel", "Rs 399 recharge debited from account");

        String description = SmsExpenseClassifier.resolveDescription(transaction, "SMS Import");

        assertEquals("Recharge", description);
    }

    @Test
    public void resolveDescription_returnsUtilityBillForBillsContext() {
        ParsedTransaction transaction = transaction("Airtel", "INR 999 paid towards internet bill");

        String description = SmsExpenseClassifier.resolveDescription(transaction, "SMS Import");

        assertEquals("Utility Bill", description);
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

    @Test
    public void resolveAccountType_identifiesUpi() {
        ParsedTransaction transaction = transaction("Any", "Rs 250 debited via UPI to abc@okaxis");
        assertEquals(Account.TYPE_UPI, SmsExpenseClassifier.resolveAccountType(transaction));
    }

    @Test
    public void resolveAccountType_identifiesCreditCard() {
        ParsedTransaction transaction = transaction("Any", "INR 500 spent on credit card ending 1234");
        assertEquals(Account.TYPE_CREDIT_CARD, SmsExpenseClassifier.resolveAccountType(transaction));
    }

    @Test
    public void resolveAccountType_identifiesDebitCard() {
        ParsedTransaction transaction = transaction("Any", "Rs 200 debited on debit card at POS terminal");
        assertEquals(Account.TYPE_DEBIT_CARD, SmsExpenseClassifier.resolveAccountType(transaction));
    }

    private static ParsedTransaction transaction(String merchant, String message) {
        return new ParsedTransaction(
                ParsedTransaction.TransactionType.DEBIT,
                "Bank",
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
