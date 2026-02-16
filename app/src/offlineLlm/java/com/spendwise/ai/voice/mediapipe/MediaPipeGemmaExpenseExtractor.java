package com.spendwise.ai.voice.mediapipe;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;
import com.spendwise.ai.voice.VoiceUnderstandingEngine;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Offline on-device LLM extractor backed by MediaPipe LLM Inference.
 *
 * Model choice for SpendWise: Gemma 3 1B int4 task bundle.
 */
public class MediaPipeGemmaExpenseExtractor implements VoiceUnderstandingEngine.FallbackExtractor {

    private static final String TAG = "GemmaExpenseExtractor";
    private static final String DEFAULT_MODEL_RELATIVE_PATH = "models/spendwise_gemma3_1b.task";

    private final Context appContext;
    private final String modelPath;
    private final String modelId;

    @Nullable
    private volatile LlmInference llmInference;
    private volatile boolean initAttempted;

    public MediaPipeGemmaExpenseExtractor(Context context, String modelPath, String modelId) {
        this.appContext = context.getApplicationContext();
        this.modelPath = modelPath != null ? modelPath.trim() : "";
        this.modelId = modelId != null ? modelId.trim() : "gemma-3-1b-it-int4";
    }

    @Override
    public boolean isAvailable() {
        return getLlmInference() != null;
    }

    @Override
    public VoiceUnderstandingEngine.FallbackExtraction extract(String transcript, List<String> categories) {
        LlmInference engine = getLlmInference();
        if (engine == null || transcript == null || transcript.trim().isEmpty()) {
            return null;
        }

        try {
            String prompt = buildPrompt(transcript, categories != null ? categories : Collections.emptyList());
            String response = engine.generateResponse(prompt);
            return parseResponse(response);
        } catch (Throwable error) {
            safeLogWarn("Gemma extraction failed: " + error.getMessage(), null);
            return null;
        }
    }

    @Nullable
    private LlmInference getLlmInference() {
        if (llmInference != null) {
            return llmInference;
        }
        if (initAttempted) {
            return null;
        }

        synchronized (this) {
            if (llmInference != null) {
                return llmInference;
            }
            if (initAttempted) {
                return null;
            }
            initAttempted = true;

            File taskFile = resolveExistingModelFile();
            if (taskFile == null) {
                taskFile = provisionBundledModel();
            }
            if (taskFile == null) {
                safeLogInfo("Gemma model file missing. Checked configured path '" + modelPath
                        + "' and default app paths (" + modelId + ")");
                return null;
            }

            try {
                String resolvedModelPath = taskFile.getAbsolutePath();
                LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(resolvedModelPath)
                        .setMaxTokens(128)
                        .setMaxTopK(40)
                        .build();

                llmInference = LlmInference.createFromOptions(appContext, options);
                safeLogInfo("Gemma extractor initialized with model " + modelId + " at " + resolvedModelPath);
            } catch (Throwable error) {
                safeLogWarn("Gemma init failed: " + error.getMessage(), null);
                llmInference = null;
            }

            return llmInference;
        }
    }

    private String buildPrompt(String transcript, List<String> categories) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are an expense extraction engine. ")
                .append("Return ONLY compact JSON with keys: ")
                .append("amount_cents, amount_confidence, category, category_confidence, merchant, merchant_confidence. ")
                .append("Confidence values must be in [0,1]. Use null for unknown values.\n")
                .append("Rules: infer amount in INR cents, choose category only from allowed list, ")
                .append("merchant should be concise and cleaned.\n")
                .append("Allowed categories: ");

        if (categories == null || categories.isEmpty()) {
            builder.append("[]");
        } else {
            builder.append('[');
            for (int i = 0; i < categories.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append('"').append(categories.get(i)).append('"');
            }
            builder.append(']');
        }

        builder.append("\nTranscript: \"")
                .append(transcript.trim())
                .append("\"\n")
                .append("JSON:");

        return builder.toString();
    }

    @Nullable
    private VoiceUnderstandingEngine.FallbackExtraction parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return null;
        }

        try {
            String jsonText = extractJson(rawResponse);
            if (jsonText == null) {
                return null;
            }

            JSONObject json = new JSONObject(jsonText);
            VoiceUnderstandingEngine.FallbackExtraction extraction = new VoiceUnderstandingEngine.FallbackExtraction();

            extraction.amountCents = readLong(json, "amount_cents");
            extraction.amountConfidence = clamp(readDouble(json, "amount_confidence"));
            extraction.category = readString(json, "category");
            extraction.categoryConfidence = clamp(readDouble(json, "category_confidence"));
            extraction.merchant = readString(json, "merchant");
            extraction.merchantConfidence = clamp(readDouble(json, "merchant_confidence"));

            if (extraction.amountCents == null
                    && extraction.category == null
                    && extraction.merchant == null) {
                return null;
            }
            return extraction;
        } catch (Throwable error) {
            safeLogWarn("Gemma response parse failed: " + error.getMessage(), null);
            return null;
        }
    }

    @Nullable
    private String extractJson(String text) {
        String sanitized = text.trim();
        if (sanitized.startsWith("```")) {
            sanitized = sanitized.replaceFirst("^```[a-zA-Z]*\\s*", "");
            sanitized = sanitized.replaceFirst("\\s*```$", "");
            sanitized = sanitized.trim();
        }

        int start = sanitized.indexOf('{');
        int end = sanitized.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }
        return sanitized.substring(start, end + 1);
    }

    @Nullable
    private Long readLong(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        try {
            return json.getLong(key);
        } catch (Exception ignored) {
            try {
                return Long.parseLong(String.valueOf(json.get(key)).trim());
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    @Nullable
    private Double readDouble(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        try {
            return json.getDouble(key);
        } catch (Exception ignored) {
            try {
                return Double.parseDouble(String.valueOf(json.get(key)).trim());
            } catch (Exception ignoredAgain) {
                return null;
            }
        }
    }

    @Nullable
    private String readString(JSONObject json, String key) {
        if (!json.has(key) || json.isNull(key)) {
            return null;
        }
        String value = json.optString(key, "").trim();
        if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    @Nullable
    private Double clamp(@Nullable Double value) {
        if (value == null || value.isNaN()) {
            return null;
        }
        if (value < 0d) {
            return 0d;
        }
        if (value > 1d) {
            return 1d;
        }
        return value;
    }

    @Nullable
    private File resolveExistingModelFile() {
        List<File> candidates = new ArrayList<>();

        if (!modelPath.isEmpty()) {
            candidates.add(resolveConfiguredPath(modelPath));
        }

        File defaultInternal = new File(appContext.getFilesDir(), DEFAULT_MODEL_RELATIVE_PATH);
        candidates.add(defaultInternal);

        File externalRoot = appContext.getExternalFilesDir(null);
        if (externalRoot != null) {
            candidates.add(new File(externalRoot, DEFAULT_MODEL_RELATIVE_PATH));
        }

        for (File candidate : candidates) {
            if (candidate != null && candidate.exists() && candidate.isFile()) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    private File provisionBundledModel() {
        String assetPath = resolveBundledAssetPath();
        File targetFile = resolveManagedInternalModelFile();
        if (targetFile == null) {
            return null;
        }

        if (targetFile.exists() && targetFile.isFile()) {
            return targetFile;
        }

        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            safeLogWarn("Unable to create model directory: " + parent.getAbsolutePath(), null);
            return null;
        }

        byte[] buffer = new byte[8192];
        try (InputStream inputStream = appContext.getAssets().open(assetPath);
                FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.getFD().sync();
            if (targetFile.length() <= 0L) {
                // Corrupted copy attempt.
                //noinspection ResultOfMethodCallIgnored
                targetFile.delete();
                return null;
            }
            safeLogInfo("Copied bundled Gemma model asset '" + assetPath + "' to " + targetFile.getAbsolutePath());
            return targetFile;
        } catch (Throwable error) {
            safeLogWarn("Bundled Gemma model asset unavailable at '" + assetPath + "'", null);
            return null;
        }
    }

    private String resolveBundledAssetPath() {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            return DEFAULT_MODEL_RELATIVE_PATH;
        }
        String trimmed = modelPath.trim();
        if (trimmed.startsWith("/")) {
            return DEFAULT_MODEL_RELATIVE_PATH;
        }
        return trimmed;
    }

    @Nullable
    private File resolveManagedInternalModelFile() {
        if (modelPath != null) {
            String trimmed = modelPath.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("/")) {
                return new File(appContext.getFilesDir(), trimmed);
            }
        }
        return new File(appContext.getFilesDir(), DEFAULT_MODEL_RELATIVE_PATH);
    }

    private File resolveConfiguredPath(String configuredPath) {
        if (configuredPath.startsWith("/")) {
            return new File(configuredPath);
        }
        return new File(appContext.getFilesDir(), configuredPath);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "MediaPipeGemmaExpenseExtractor(modelId=%s)", modelId);
    }

    private void safeLogInfo(String message) {
        try {
            Log.i(TAG, message);
        } catch (Throwable ignored) {
            // Unit tests do not provide android.util.Log implementation.
        }
    }

    private void safeLogWarn(String message, @Nullable Throwable error) {
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
}
