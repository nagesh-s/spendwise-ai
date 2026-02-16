package com.spendwise.ai.voice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class OnDeviceSemanticExpenseExtractorTest {

    private final OnDeviceSemanticExpenseExtractor extractor = new OnDeviceSemanticExpenseExtractor();

    @Test
    public void extract_parsesWordAmountCategoryAndMerchant() {
        List<String> categories = Arrays.asList("Food & Dining", "Bills & Utilities", "Transport");

        VoiceUnderstandingEngine.FallbackExtraction result = extractor.extract(
                "spent two hundred fifty on lunch at cafe coffee day",
                categories);

        assertNotNull(result);
        assertEquals(Long.valueOf(25000L), result.amountCents);
        assertNotNull(result.amountConfidence);
        assertTrue(result.amountConfidence >= 0.70d);
        assertEquals("Food & Dining", result.category);
        assertNotNull(result.categoryConfidence);
        assertTrue(result.categoryConfidence >= 0.65d);
        assertEquals("cafe coffee day", result.merchant.toLowerCase());
    }

    @Test
    public void extract_parsesColloquialHundredsAmount() {
        VoiceUnderstandingEngine.FallbackExtraction result = extractor.extract(
                "paid two fifty for milk",
                Arrays.asList("Food & Dining"));

        assertNotNull(result);
        assertEquals(Long.valueOf(25000L), result.amountCents);
        assertNotNull(result.amountConfidence);
        assertTrue(result.amountConfidence >= 0.70d);
    }

    @Test
    public void extract_detectsBillsCategoryFromKeywords() {
        VoiceUnderstandingEngine.FallbackExtraction result = extractor.extract(
                "paid 999 internet bill to airtel",
                Arrays.asList("Food & Dining", "Bills & Utilities", "Entertainment"));

        assertNotNull(result);
        assertEquals(Long.valueOf(99900L), result.amountCents);
        assertEquals("Bills & Utilities", result.category);
        assertEquals("airtel", result.merchant.toLowerCase());
    }

    @Test
    public void extract_returnsNullForBlankInput() {
        assertNull(extractor.extract("   ", Arrays.asList("Food & Dining")));
    }
}
