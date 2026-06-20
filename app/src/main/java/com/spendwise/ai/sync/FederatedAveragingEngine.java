package com.spendwise.ai.sync;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Mocks Federated Averaging (FedAvg).
 * Takes two weight vectors (represented as JSON for this prototype) and calculates their mean.
 * In a true TFLite implementation, this would manipulate FlatBuffers or rely on TensorFlow Federated.
 */
public class FederatedAveragingEngine {
    private static final String TAG = "FederatedAveragingEngine";

    public static void createDummyWeights(File file, float[] weights) {
        try {
            JSONObject obj = new JSONObject();
            JSONArray arr = new JSONArray();
            for (float w : weights) {
                arr.put(w);
            }
            obj.put("weights", arr);
            
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(obj.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to create dummy weights", e);
        }
    }

    public static File averageWeights(File modelA, File modelB, File outputDir) {
        try {
            float[] weightsA = readWeights(modelA);
            float[] weightsB = readWeights(modelB);

            if (weightsA == null && weightsB == null) return null;
            if (weightsA == null) return modelB;
            if (weightsB == null) return modelA;

            int len = Math.min(weightsA.length, weightsB.length);
            float[] averaged = new float[len];

            for (int i = 0; i < len; i++) {
                // The actual Federated Union: simple average
                averaged[i] = (weightsA[i] + weightsB[i]) / 2.0f;
            }

            File mergedFile = new File(outputDir, "merged_model_weights.json");
            createDummyWeights(mergedFile, averaged);
            Log.i(TAG, "Successfully averaged " + len + " weights.");
            return mergedFile;

        } catch (Exception e) {
            Log.e(TAG, "Failed to average weights", e);
            return null;
        }
    }

    private static float[] readWeights(File file) {
        if (!file.exists()) return null;
        try {
            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(bytes);
            }
            String jsonStr = new String(bytes, StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(jsonStr);
            JSONArray arr = obj.getJSONArray("weights");
            
            float[] weights = new float[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                weights[i] = (float) arr.getDouble(i);
            }
            return weights;
        } catch (Exception e) {
            Log.e(TAG, "Error reading weights from " + file.getName(), e);
            return null;
        }
    }
}
