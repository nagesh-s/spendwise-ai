package com.spendwise.ai.voice;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VoiceUnderstandingEngineTest {

    private final VoiceExpenseParser parser = new VoiceExpenseParser();
    private final List<String> categories = Arrays.asList(
            "Food & Dining",
            "Transportation",
            "Bills & Utilities",
            "Shopping",
            "Other");

    @Test
    public void understandSync_highConfidenceParser_doesNotUseFallbackExtractor() {
        AtomicBoolean fallbackCalled = new AtomicBoolean(false);
        VoiceUnderstandingEngine engine = new VoiceUnderstandingEngine(
                parser,
                new VoiceUnderstandingEngine.FallbackExtractor() {
                    @Override
                    public boolean isAvailable() {
                        return true;
                    }

                    @Override
                    public VoiceUnderstandingEngine.FallbackExtraction extract(String transcript, List<String> categories) {
                        fallbackCalled.set(true);
                        return null;
                    }
                },
                Runnable::run);

        VoiceUnderstandingEngine.UnderstandingResult result = engine.understandSync(
                "Spent Rs 250 for Food & Dining at Starbucks",
                categories);

        assertNotNull(result);
        assertTrue(result.parsedExpense.isReadyForAutoSave());
        assertFalse(result.fallbackAttempted);
        assertFalse(fallbackCalled.get());
    }

    @Test
    public void understandSync_lowConfidenceCategory_fallbackFillsCategory() {
        VoiceUnderstandingEngine engine = new VoiceUnderstandingEngine(
                parser,
                new VoiceUnderstandingEngine.FallbackExtractor() {
                    @Override
                    public boolean isAvailable() {
                        return true;
                    }

                    @Override
                    public VoiceUnderstandingEngine.FallbackExtraction extract(String transcript, List<String> categories) {
                        VoiceUnderstandingEngine.FallbackExtraction extraction = new VoiceUnderstandingEngine.FallbackExtraction();
                        extraction.category = "Food & Dining";
                        extraction.categoryConfidence = 0.92;
                        return extraction;
                    }
                },
                Runnable::run);

        VoiceUnderstandingEngine.UnderstandingResult result = engine.understandSync(
                "Spent Rs 250 at Starbucks",
                categories);

        assertTrue(result.fallbackAttempted);
        assertTrue(result.fallbackImprovedResult);
        assertEquals("Food & Dining", result.parsedExpense.category);
        assertTrue(result.parsedExpense.categorySure);
        assertTrue(result.parsedExpense.isReadyForAutoSave());
    }

    @Test
    public void understandSync_lowConfidenceFallback_doesNotOverrideParser() {
        VoiceUnderstandingEngine engine = new VoiceUnderstandingEngine(
                parser,
                new VoiceUnderstandingEngine.FallbackExtractor() {
                    @Override
                    public boolean isAvailable() {
                        return true;
                    }

                    @Override
                    public VoiceUnderstandingEngine.FallbackExtraction extract(String transcript, List<String> categories) {
                        VoiceUnderstandingEngine.FallbackExtraction extraction = new VoiceUnderstandingEngine.FallbackExtraction();
                        extraction.amountCents = 9999L;
                        extraction.amountConfidence = 0.40;
                        extraction.category = "Shopping";
                        extraction.categoryConfidence = 0.45;
                        return extraction;
                    }
                },
                Runnable::run);

        VoiceUnderstandingEngine.UnderstandingResult result = engine.understandSync(
                "groceries 120",
                categories);

        assertTrue(result.fallbackAttempted);
        assertFalse(result.fallbackImprovedResult);
        assertEquals(12000L, result.parsedExpense.amountCents);
        assertFalse(result.parsedExpense.amountSure);
        assertFalse(result.parsedExpense.categorySure);
    }
}
