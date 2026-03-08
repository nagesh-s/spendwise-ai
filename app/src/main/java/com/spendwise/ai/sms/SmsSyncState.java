package com.spendwise.ai.sms;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Shared sync pointer for SMS scans/imports so old messages are not processed repeatedly.
 */
public final class SmsSyncState {

    private static final String PREF_SMS_SYNC_STATE = "sms_sync_state_prefs";
    private static final String KEY_LAST_TXN_SYNC_TIMESTAMP = "last_txn_sync_timestamp";
    private static final String KEY_LAST_INVESTMENT_SYNC_TIMESTAMP = "last_investment_sync_timestamp";

    private SmsSyncState() {
    }

    public static long getLastTransactionSyncTimestamp(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SMS_SYNC_STATE, Context.MODE_PRIVATE);
        long stored = prefs.getLong(KEY_LAST_TXN_SYNC_TIMESTAMP, 0L);
        long now = System.currentTimeMillis();
        if (stored < 0 || stored > now + (24L * 60 * 60 * 1000)) {
            prefs.edit().putLong(KEY_LAST_TXN_SYNC_TIMESTAMP, 0L).apply();
            return 0L;
        }
        return stored;
    }

    public static long getLastInvestmentSyncTimestamp(@NonNull Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_SMS_SYNC_STATE, Context.MODE_PRIVATE);
        long stored = prefs.getLong(KEY_LAST_INVESTMENT_SYNC_TIMESTAMP, 0L);
        long now = System.currentTimeMillis();
        if (stored < 0 || stored > now + (24L * 60 * 60 * 1000)) {
            prefs.edit().putLong(KEY_LAST_INVESTMENT_SYNC_TIMESTAMP, 0L).apply();
            return 0L;
        }
        return stored;
    }

    public static void updateLastTransactionSyncTimestamp(@NonNull Context context, long timestampMillis) {
        if (timestampMillis <= 0) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_SMS_SYNC_STATE, Context.MODE_PRIVATE);
        long current = prefs.getLong(KEY_LAST_TXN_SYNC_TIMESTAMP, 0L);
        if (timestampMillis <= current) {
            return;
        }
        prefs.edit().putLong(KEY_LAST_TXN_SYNC_TIMESTAMP, timestampMillis).apply();
    }

    public static void updateLastInvestmentSyncTimestamp(@NonNull Context context, long timestampMillis) {
        if (timestampMillis <= 0) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_SMS_SYNC_STATE, Context.MODE_PRIVATE);
        long current = prefs.getLong(KEY_LAST_INVESTMENT_SYNC_TIMESTAMP, 0L);
        if (timestampMillis <= current) {
            return;
        }
        prefs.edit().putLong(KEY_LAST_INVESTMENT_SYNC_TIMESTAMP, timestampMillis).apply();
    }

    public static void clearTransactionSync(@NonNull Context context) {
        context.getSharedPreferences(PREF_SMS_SYNC_STATE, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_TXN_SYNC_TIMESTAMP, 0L)
                .apply();
    }

    public static void clearInvestmentSync(@NonNull Context context) {
        context.getSharedPreferences(PREF_SMS_SYNC_STATE, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_LAST_INVESTMENT_SYNC_TIMESTAMP, 0L)
                .apply();
    }
}
