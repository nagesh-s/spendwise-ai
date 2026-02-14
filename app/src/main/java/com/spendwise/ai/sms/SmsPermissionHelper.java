package com.spendwise.ai.sms;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for SMS permission management and reading SMS messages.
 */
public class SmsPermissionHelper {

    public static final int SMS_PERMISSION_REQUEST_CODE = 1001;

    /**
     * Check if the app has READ_SMS permission.
     */
    public static boolean hasReadSmsPermission(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request READ_SMS permission from the user.
     */
    public static void requestSmsPermission(@NonNull Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[] { Manifest.permission.READ_SMS },
                SMS_PERMISSION_REQUEST_CODE);
    }

    /**
     * Check if permission rationale should be shown.
     */
    public static boolean shouldShowRationale(@NonNull Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_SMS);
    }

    /**
     * Open app settings screen for manual permission management.
     */
    public static void openAppSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", context.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Read recent SMS messages and parse for transactions.
     *
     * @param context  Application context
     * @param dayLimit Number of days back to scan
     * @return List of parsed transactions (debit only by default)
     */
    public static List<ParsedTransaction> readRecentTransactions(@NonNull Context context, int dayLimit) {
        List<ParsedTransaction> transactions = new ArrayList<>();

        if (!hasReadSmsPermission(context)) {
            return transactions;
        }

        long cutoffTime = System.currentTimeMillis() - (dayLimit * 24L * 60 * 60 * 1000);

        ContentResolver contentResolver = context.getContentResolver();
        Uri smsUri = Uri.parse("content://sms/inbox");

        String[] projection = { "address", "body", "date" };
        String selection = "date > ?";
        String[] selectionArgs = { String.valueOf(cutoffTime) };
        String sortOrder = "date DESC";

        Cursor cursor = null;
        try {
            cursor = contentResolver.query(smsUri, projection, selection, selectionArgs, sortOrder);
            if (cursor != null && cursor.moveToFirst()) {
                int addressIdx = cursor.getColumnIndex("address");
                int bodyIdx = cursor.getColumnIndex("body");
                int dateIdx = cursor.getColumnIndex("date");

                do {
                    String sender = cursor.getString(addressIdx);
                    String body = cursor.getString(bodyIdx);
                    long timestamp = cursor.getLong(dateIdx);

                    if (TransactionParser.isTransactionSms(sender, body)) {
                        ParsedTransaction parsed = TransactionParser.parse(body, timestamp);
                        if (parsed != null) {
                            transactions.add(parsed);
                        }
                    }
                } while (cursor.moveToNext());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return transactions;
    }
}
