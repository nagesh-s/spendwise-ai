package com.spendwise.ai.buckets;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Persisted preferences for salary detection and Expected Salary feature.
 *
 * Smart fallback logic:
 * - If user's bank sends SMS salary notifications, they are parsed
 * automatically.
 * - If no salary SMS is detected by the expected payday, the app prompts
 * manually.
 * - The user sets their expected salary ONCE and only confirms/edits on payday.
 */
public final class BucketSalaryPrefs {

    private static final String PREF_NAME = "bucket_salary_prefs";
    private static final String KEY_EXPECTED_SALARY_CENTS = "expected_salary_cents";
    private static final String KEY_EXPECTED_PAYDAY = "expected_payday"; // day of month (1-31)
    private static final String KEY_LAST_ALLOCATED_MONTH = "last_allocated_month"; // YYYYMM
    private static final String KEY_MANUAL_SALARY_ENABLED = "manual_salary_enabled";
    private static final String KEY_SHOW_NET_WORTH = "show_net_worth";

    private BucketSalaryPrefs() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- Expected Salary ---

    public static long getExpectedSalaryCents(@NonNull Context context) {
        return prefs(context).getLong(KEY_EXPECTED_SALARY_CENTS, 0L);
    }

    public static void setExpectedSalaryCents(@NonNull Context context, long cents) {
        prefs(context).edit().putLong(KEY_EXPECTED_SALARY_CENTS, cents).apply();
    }

    // --- Expected Payday ---

    public static int getExpectedPayday(@NonNull Context context) {
        return prefs(context).getInt(KEY_EXPECTED_PAYDAY, 1); // Default: 1st of month
    }

    public static void setExpectedPayday(@NonNull Context context, int dayOfMonth) {
        prefs(context).edit().putInt(KEY_EXPECTED_PAYDAY, Math.max(1, Math.min(31, dayOfMonth))).apply();
    }

    // --- Last Allocated Month (to prevent double allocation) ---

    public static String getLastAllocatedMonth(@NonNull Context context) {
        return prefs(context).getString(KEY_LAST_ALLOCATED_MONTH, "");
    }

    public static void setLastAllocatedMonth(@NonNull Context context, String yyyyMm) {
        prefs(context).edit().putString(KEY_LAST_ALLOCATED_MONTH, yyyyMm).apply();
    }

    public static boolean hasAllocatedThisMonth(@NonNull Context context, String currentYyyymm) {
        return currentYyyymm.equals(getLastAllocatedMonth(context));
    }

    // --- Manual Salary Toggle ---
    // Enabled only if user doesn't get salary SMS notifications

    public static boolean isManualSalaryEnabled(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_MANUAL_SALARY_ENABLED, false);
    }

    public static void setManualSalaryEnabled(@NonNull Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_MANUAL_SALARY_ENABLED, enabled).apply();
    }

    // --- Show Net Worth toggle (anti-Upstox UX) ---

    public static boolean isShowNetWorth(@NonNull Context context) {
        return prefs(context).getBoolean(KEY_SHOW_NET_WORTH, false);
    }

    public static void setShowNetWorth(@NonNull Context context, boolean show) {
        prefs(context).edit().putBoolean(KEY_SHOW_NET_WORTH, show).apply();
    }
}
