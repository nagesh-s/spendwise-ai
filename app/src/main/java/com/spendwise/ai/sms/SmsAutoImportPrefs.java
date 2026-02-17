package com.spendwise.ai.sms;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Persisted user preference for SMS auto-import background worker.
 */
public final class SmsAutoImportPrefs {

    private static final String PREF_NAME = "sms_auto_import_prefs";
    private static final String KEY_ENABLED = "sms_auto_import_enabled";

    private SmsAutoImportPrefs() {
    }

    public static boolean isEnabled(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(@NonNull Context context, boolean enabled) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ENABLED, enabled)
                .apply();
    }
}
