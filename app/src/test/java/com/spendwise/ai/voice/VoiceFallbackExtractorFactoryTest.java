package com.spendwise.ai.voice;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;

import org.junit.Test;

import java.util.Arrays;

public class VoiceFallbackExtractorFactoryTest {

    @Test
    public void create_returnsWorkingExtractor() {
        Context context = mock(Context.class);
        when(context.getApplicationContext()).thenReturn(context);

        VoiceUnderstandingEngine.FallbackExtractor extractor = VoiceFallbackExtractorFactory.create(context);

        assertNotNull(extractor);
        assertTrue(extractor.isAvailable());

        VoiceUnderstandingEngine.FallbackExtraction extraction = extractor.extract(
                "spent 250 on milk at dairy",
                Arrays.asList("Food & Dining", "Bills & Utilities"));

        assertNotNull(extraction);
        assertNotNull(extraction.amountCents);
        assertTrue(extraction.amountCents > 0L);
    }
}
