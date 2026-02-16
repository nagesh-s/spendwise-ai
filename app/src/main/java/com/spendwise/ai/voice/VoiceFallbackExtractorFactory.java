package com.spendwise.ai.voice;

import android.content.Context;
import android.util.Log;

import com.spendwise.ai.BuildConfig;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Creates the voice fallback extractor per build flavor.
 *
 * standard flavor: semantic rules only (fully offline, lightweight).
 * offlineLlm flavor: MediaPipe Gemma extractor first, semantic extractor as fallback.
 */
public final class VoiceFallbackExtractorFactory {

    private static final String TAG = "VoiceExtractorFactory";
    private static final String MEDIAPIPE_EXTRACTOR_CLASS =
            "com.spendwise.ai.voice.mediapipe.MediaPipeGemmaExpenseExtractor";

    private VoiceFallbackExtractorFactory() {
    }

    public static VoiceUnderstandingEngine.FallbackExtractor create(Context context) {
        VoiceUnderstandingEngine.FallbackExtractor semantic = new OnDeviceSemanticExpenseExtractor();

        if (!BuildConfig.OFFLINE_LLM_ENABLED) {
            return semantic;
        }

        VoiceUnderstandingEngine.FallbackExtractor llm = createMediaPipeExtractor(context);
        if (llm == null) {
            return semantic;
        }

        return new CompositeFallbackExtractor(llm, semantic);
    }

    private static VoiceUnderstandingEngine.FallbackExtractor createMediaPipeExtractor(Context context) {
        try {
            Class<?> extractorClass = Class.forName(MEDIAPIPE_EXTRACTOR_CLASS);
            Constructor<?> constructor = extractorClass.getConstructor(Context.class, String.class, String.class);
            Object instance = constructor.newInstance(
                    context.getApplicationContext(),
                    BuildConfig.OFFLINE_LLM_MODEL_PATH,
                    BuildConfig.OFFLINE_LLM_MODEL_ID);
            if (!(instance instanceof VoiceUnderstandingEngine.FallbackExtractor)) {
                safeLogWarn("MediaPipe extractor class does not implement fallback interface", null);
                return null;
            }
            return (VoiceUnderstandingEngine.FallbackExtractor) instance;
        } catch (Throwable error) {
            safeLogWarn("Offline LLM extractor unavailable, falling back to semantic parser", error);
            return null;
        }
    }

    private static void safeLogWarn(String message, Throwable error) {
        try {
            if (error != null) {
                Log.w(TAG, message, error);
            } else {
                Log.w(TAG, message);
            }
        } catch (Throwable ignored) {
            // Unit tests do not provide android.util.Log implementation.
        }
    }

    private static final class CompositeFallbackExtractor implements VoiceUnderstandingEngine.FallbackExtractor {

        private final VoiceUnderstandingEngine.FallbackExtractor primary;
        private final VoiceUnderstandingEngine.FallbackExtractor secondary;

        CompositeFallbackExtractor(
                VoiceUnderstandingEngine.FallbackExtractor primary,
                VoiceUnderstandingEngine.FallbackExtractor secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public boolean isAvailable() {
            return (primary != null && primary.isAvailable())
                    || (secondary != null && secondary.isAvailable());
        }

        @Override
        public VoiceUnderstandingEngine.FallbackExtraction extract(String transcript, List<String> categories) {
            if (primary != null && primary.isAvailable()) {
                VoiceUnderstandingEngine.FallbackExtraction result = primary.extract(transcript, categories);
                if (result != null) {
                    return result;
                }
            }
            if (secondary != null && secondary.isAvailable()) {
                return secondary.extract(transcript, categories);
            }
            return null;
        }
    }
}
