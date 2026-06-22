package com.spendwise.ai.engine;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder;
import com.google.mediapipe.tasks.text.textembedder.TextEmbedderResult;

import java.io.IOException;

/**
 * Lite ML Model classifier for semantic bucket assignment.
 * Uses Universal Sentence Encoder or MobileBERT text embedder via TFLite Task API.
 */
public class SemanticBucketClassifier {

    private static final String TAG = "SemanticBucketClassifier";
    private static final String MODEL_PATH = "models/text_embedder.tflite";
    
    private TextEmbedder textEmbedder;

    public SemanticBucketClassifier(Context context) {
        try {
            BaseOptions baseOptions = BaseOptions.builder().setModelAssetPath(MODEL_PATH).build();
            TextEmbedder.TextEmbedderOptions options = TextEmbedder.TextEmbedderOptions.builder()
                    .setBaseOptions(baseOptions)
                    .build();
            textEmbedder = TextEmbedder.createFromOptions(context, options);
            Log.i(TAG, "Successfully loaded text_embedder.tflite via MediaPipe");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing TextEmbedder", e);
        }
    }

    /**
     * Compute cosine similarity between two text strings.
     * Returns -1 if model is not available or extraction fails.
     */
    public double computeSimilarity(@NonNull String text1, @NonNull String text2) {
        if (textEmbedder == null) {
            return -1.0;
        }

        try {
            TextEmbedderResult result1 = textEmbedder.embed(text1);
            TextEmbedderResult result2 = textEmbedder.embed(text2);
            
            if (result1.embeddingResult().embeddings().isEmpty() || result2.embeddingResult().embeddings().isEmpty()) {
                return -1.0;
            }
            
            return TextEmbedder.cosineSimilarity(
                    result1.embeddingResult().embeddings().get(0),
                    result2.embeddingResult().embeddings().get(0)
            );
        } catch (Exception e) {
            Log.e(TAG, "Error computing similarity", e);
            return -1.0;
        }
    }

    public void close() {
        if (textEmbedder != null) {
            textEmbedder.close();
            textEmbedder = null;
        }
    }
}
