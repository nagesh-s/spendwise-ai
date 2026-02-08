package com.spendwise.ai.security;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * Helper class for biometric authentication.
 */
public class BiometricHelper {

    private static final String PREFS_NAME = "security_prefs";
    private static final String KEY_BIOMETRIC_ENABLED = "biometric_enabled";
    private static final String KEY_LAST_UNLOCK_TIME = "last_unlock_time";
    private static final long LOCK_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Check if biometric hardware is available.
     */
    public static boolean isBiometricAvailable(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Check if device has biometric capability (even if not enrolled).
     */
    public static boolean hasBiometricCapability(Context context) {
        BiometricManager biometricManager = BiometricManager.from(context);
        int result = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS ||
                result == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED;
    }

    /**
     * Check if biometric lock is enabled in settings.
     */
    public static boolean isBiometricEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    /**
     * Enable or disable biometric lock.
     */
    public static void setBiometricEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    /**
     * Record successful unlock time.
     */
    public static void recordUnlock(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_UNLOCK_TIME, System.currentTimeMillis()).apply();
    }

    /**
     * Check if the app should require unlock (timeout exceeded).
     */
    public static boolean shouldRequireUnlock(Context context) {
        if (!isBiometricEnabled(context)) {
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastUnlock = prefs.getLong(KEY_LAST_UNLOCK_TIME, 0);
        return System.currentTimeMillis() - lastUnlock > LOCK_TIMEOUT_MS;
    }

    /**
     * Show biometric authentication prompt.
     */
    public static void authenticate(FragmentActivity activity, AuthCallback callback) {
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock SpendWise")
                .setSubtitle("Authenticate to access your expenses")
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(
                        BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        recordUnlock(activity);
                        callback.onSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        callback.onError(errString.toString());
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        callback.onFailure();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    /**
     * Callback interface for authentication results.
     */
    public interface AuthCallback {
        void onSuccess();

        void onError(String message);

        void onFailure();
    }
}
