package com.spendwise.ai.sms;

import androidx.annotation.Nullable;

import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tensorflow.lite.Interpreter;

/**
 * Lightweight semantic fallback model.
 * Optionally uses a TensorFlow Lite model if available, otherwise falls back
 * to strict NLP-like heuristics.
 */
public class OnDeviceSemanticInvestmentExtractor {

    private static final Pattern SEMANTIC_AMOUNT = Pattern.compile(
            "(?i)(?:invested|bought|purchased|sip|lumpsum)\\s+(?:of\\s+)?(?:rs\\.?|inr|₹)?\\s*(\\d+(?:,\\d{3})*(?:\\.\\d{1,2})?)");

    private static final Pattern SEMANTIC_PLATFORM = Pattern.compile(
            "(?i)\\b(?:via|through|on)\\s+(groww|zerodha|upstox|coin|angel\\s*one|paytm\\s*money)\\b");

    private static final Pattern SEMANTIC_NAME = Pattern.compile(
            "(?i)(?:in|for)\\s+([A-Za-z][A-Za-z0-9&()'./\\-\\s]{3,50}?)\\s+(?:fund|mutual\\s*fund|etf|stock|shares)");

    private Interpreter tfliteInterpreter;
    private static final String TAG = "OnDeviceSemanticExtr";

    public OnDeviceSemanticInvestmentExtractor() {
        this(null);
    }

    public OnDeviceSemanticInvestmentExtractor(@Nullable File modelFile) {
        if (modelFile != null && modelFile.exists()) {
            try {
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(1);
                tfliteInterpreter = new Interpreter(modelFile, options);
                Log.i(TAG, "Successfully loaded TFLite model: " + modelFile.getName());
            } catch (Exception e) {
                Log.e(TAG, "Failed to load TFLite model, falling back to heuristics.", e);
            }
        }
    }

    @Nullable
    public ParsedInvestmentSignal extract(@Nullable String sender, @Nullable String body, long timestampMillis) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        if (tfliteInterpreter != null) {
            // Prototype TFLite Inference
            // Normally we would tokenize the body, feed it to a ByteBuffer, and run inference
            try {
                float[][] input = new float[1][256]; // Dummy embedding
                float[][] output = new float[1][5];  // Dummy confidence/category output
                tfliteInterpreter.run(input, output);
                Log.i(TAG, "TFLite inference ran successfully (prototype mode).");
            } catch (Exception e) {
                Log.e(TAG, "TFLite inference failed", e);
            }
        }
        
        // This acts as a semantic heuristic parser.
        // E.g., "Invested Rs 500 in Parag Parikh Flexi Cap Fund via Groww"
        
        String normalized = body.toLowerCase(Locale.US);
        
        long amountCents = -1;
        Matcher amountMatcher = SEMANTIC_AMOUNT.matcher(body);
        if (amountMatcher.find()) {
            try {
                String amtStr = amountMatcher.group(1).replaceAll(",", "");
                amountCents = (long) (Double.parseDouble(amtStr) * 100);
            } catch (Exception ignored) {}
        }
        
        String platform = null;
        Matcher platMatcher = SEMANTIC_PLATFORM.matcher(body);
        if (platMatcher.find()) {
            platform = platMatcher.group(1);
        }
        
        String name = null;
        Matcher nameMatcher = SEMANTIC_NAME.matcher(body);
        if (nameMatcher.find()) {
            name = nameMatcher.group(1).trim();
        }
        
        String assetType = "MUTUAL_FUND"; // Default guess if fund/mutual fund matched
        if (normalized.contains("stock") || normalized.contains("shares")) {
            assetType = "STOCK";
        } else if (normalized.contains("etf")) {
            assetType = "ETF";
        }
        
        if (amountCents > 0 && name != null) {
            String sanitizePlatform = platform == null ? "unknown" : platform.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
            String sanitizeName = name.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
            
            String correlationSeed = sanitizePlatform + "|" + sanitizeName + "|" + amountCents;
            String correlationKey = "corr_" + correlationSeed;
            
            String sourceSeed = correlationSeed + "|UNIT_ALLOTTED|" + assetType + "|" + (timestampMillis / (24L * 60 * 60 * 1000));
            String sourceKey = "src_" + sourceSeed;
                    
            return new ParsedInvestmentSignal(
                    ParsedInvestmentSignal.SignalType.UNIT_ALLOTTED,
                    sender,
                    platform,
                    name,
                    assetType,
                    amountCents,
                    timestampMillis,
                    sourceKey,
                    correlationKey,
                    body,
                    false,
                    null,
                    null,
                    0.65d, // Lower confidence than deterministic
                    "semantic_fallback"
            );
        }
        
        return null;
    }
}
