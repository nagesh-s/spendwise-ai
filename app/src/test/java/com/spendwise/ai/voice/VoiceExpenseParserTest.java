package com.spendwise.ai.voice;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
import java.util.List;

public class VoiceExpenseParserTest {

    private final VoiceExpenseParser parser = new VoiceExpenseParser();
    private final List<String> categories = Arrays.asList("Food", "Travel", "Fuel", "Groceries", "Entertainment");

    @Test
    public void testAmountOnly() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("250", categories);
        assertEquals(25000, result.amountCents);
    }

    @Test
    public void testAmountAndMerchant() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("500 at Starbucks", categories);
        assertEquals(50000, result.amountCents);
        assertEquals("Starbucks", result.merchant);
    }

    @Test
    public void testAmountAndCategory() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("200 for Food", categories);
        assertEquals(20000, result.amountCents);
        assertEquals("Food", result.category);
    }

    @Test
    public void testFullExpense() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("1200 at Shell for Fuel", categories);
        assertEquals(120000, result.amountCents);
        assertEquals("Shell", result.merchant);
        assertEquals("Fuel", result.category);
    }

    @Test
    public void testFloatingPoint() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("250.50", categories);
        assertEquals(25050, result.amountCents);
    }

    @Test
    public void testCleanMerchant() {
        VoiceExpenseParser.ParsedExpense result = parser.parse("300 from Amazon for shopping", categories);
        assertEquals(30000, result.amountCents);
        assertEquals("Amazon", result.merchant);
    }
}
