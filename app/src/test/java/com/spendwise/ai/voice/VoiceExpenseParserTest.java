package com.spendwise.ai.voice;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

public class VoiceExpenseParserTest {

    private final VoiceExpenseParser parser = new VoiceExpenseParser();
    private final List<String> categories = Arrays.asList(
            "Food & Dining",
            "Transportation",
            "Bills & Utilities",
            "Shopping",
            "Entertainment",
            "Other");

    @Test
    public void parse_amountOnly() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("250", categories);
        assertEquals(25000, result.amountCents);
        assertTrue(result.isValid());
        assertFalse(result.amountSure);
        assertFalse(result.isReadyForAutoSave());
    }

    @Test
    public void parse_currencyPrefixAndCommaAmount() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("Spent Rs 1,250.50 on internet bill", categories);
        assertEquals(125050, result.amountCents);
        assertEquals("Bills & Utilities", result.category);
        assertTrue(result.amountSure);
        assertTrue(result.categorySure);
    }

    @Test
    public void parse_amountAndMerchantFromAtClause() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("500 at Starbucks", categories);
        assertEquals(50000, result.amountCents);
        assertEquals("Starbucks", result.merchant);
        assertTrue(result.merchantSure);
    }

    @Test
    public void parse_merchantFromPaidToClause() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("Paid 430 to Airtel for recharge", categories);
        assertEquals(43000, result.amountCents);
        assertEquals("Airtel", result.merchant);
        assertEquals("Bills & Utilities", result.category);
        assertTrue(result.amountSure);
        assertTrue(result.merchantSure);
    }

    @Test
    public void parse_directCategoryPhrase() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("200 for Food & Dining", categories);
        assertEquals(20000, result.amountCents);
        assertEquals("Food & Dining", result.category);
        assertTrue(result.categorySure);
    }

    @Test
    public void parse_keywordCategoryInference() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("paid 300 for uber ride", categories);
        assertEquals(30000, result.amountCents);
        assertEquals("Transportation", result.category);
        assertFalse(result.categorySure);
        assertFalse(result.isReadyForAutoSave());
    }

    @Test
    public void parse_keywordCategoryInferenceForFood() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("Spent 180 on lunch", categories);
        assertEquals(18000, result.amountCents);
        assertEquals("Food & Dining", result.category);
        assertFalse(result.categorySure);
    }

    @Test
    public void parse_cleanMerchantNameWithTrailingContext() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("300 from Amazon for shopping", categories);
        assertEquals(30000, result.amountCents);
        assertEquals("Amazon", result.merchant);
        assertTrue(result.merchantSure);
    }

    @Test
    public void parse_noAmountReturnsInvalid() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("Need to add milk expense", categories);
        assertFalse(result.isValid());
        assertEquals(0, result.amountCents);
        assertFalse(result.amountSure);
    }

    @Test
    public void parse_fullSureResult_isAutoSaveReady() {
        VoiceExpenseParser.ParsedExpense result = parser.parse(
                "Spent Rs 250 for Food & Dining at Starbucks", categories);
        assertTrue(result.amountSure);
        assertTrue(result.categorySure);
        assertTrue(result.merchantSure);
        assertTrue(result.isReadyForAutoSave());
    }
}
