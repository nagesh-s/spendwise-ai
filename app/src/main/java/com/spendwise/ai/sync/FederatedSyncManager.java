package com.spendwise.ai.sync;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages peer-to-peer connection via Sockets (simulating Wi-Fi Direct).
 * Used to exchange fine-tuned TFLite models completely offline.
 */
public class FederatedSyncManager {
    private static final String TAG = "FederatedSyncManager";
    public static final int SYNC_PORT = 8988;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final File modelDirectory;

    public interface SyncListener {
        void onSyncComplete(File mergedModelFile);
        void onSyncFailed(String reason);
    }

    public FederatedSyncManager(File modelDirectory) {
        this.modelDirectory = modelDirectory;
        if (!modelDirectory.exists()) {
            modelDirectory.mkdirs();
        }
    }

    /**
     * Start as a Host (listening for incoming models to average).
     */
    public void startHostMode(SyncListener listener) {
        executor.execute(() -> {
            try (ServerSocket serverSocket = new ServerSocket(SYNC_PORT)) {
                Log.i(TAG, "Host listening on port " + SYNC_PORT);
                Socket clientSocket = serverSocket.accept();
                Log.i(TAG, "Client connected: " + clientSocket.getInetAddress());

                File incomingFile = new File(modelDirectory, "incoming_peer_model.json");
                try (InputStream is = clientSocket.getInputStream();
                     FileOutputStream fos = new FileOutputStream(incomingFile)) {
                    
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }
                
                Log.i(TAG, "Received peer model. Starting Federated Averaging...");
                File myModelFile = new File(modelDirectory, "my_model_weights.json");
                File mergedFile = FederatedAveragingEngine.averageWeights(myModelFile, incomingFile, modelDirectory);
                
                if (mergedFile != null) {
                    listener.onSyncComplete(mergedFile);
                } else {
                    listener.onSyncFailed("Averaging failed.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Host error", e);
                listener.onSyncFailed(e.getMessage());
            }
        });
    }

    /**
     * Start as a Client (connecting to Host to send my model).
     */
    public void startClientMode(String hostIpAddress, SyncListener listener) {
        executor.execute(() -> {
            try (Socket socket = new Socket(hostIpAddress, SYNC_PORT)) {
                Log.i(TAG, "Connected to host: " + hostIpAddress);
                
                File myModelFile = new File(modelDirectory, "my_model_weights.json");
                if (!myModelFile.exists()) {
                    // Create dummy weights to send
                    FederatedAveragingEngine.createDummyWeights(myModelFile, new float[]{0.1f, 0.2f, 0.3f, 0.4f});
                }

                try (FileInputStream fis = new FileInputStream(myModelFile);
                     OutputStream os = socket.getOutputStream()) {
                    
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                }
                
                Log.i(TAG, "Sent model to host successfully.");
                listener.onSyncComplete(null);

            } catch (Exception e) {
                Log.e(TAG, "Client error", e);
                listener.onSyncFailed(e.getMessage());
            }
        });
    }
}
