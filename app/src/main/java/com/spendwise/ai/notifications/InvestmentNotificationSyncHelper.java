package com.spendwise.ai.notifications;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;

/**
 * Utility methods for notification listener access and manual sync actions.
 */
public final class InvestmentNotificationSyncHelper {

    private InvestmentNotificationSyncHelper() {
    }

    private static final String ENABLED_NOTIFICATION_LISTENERS_KEY = "enabled_notification_listeners";

    public static boolean isNotificationAccessEnabled(@NonNull Context context) {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS_KEY);
        if (enabledListeners == null || enabledListeners.trim().isEmpty()) {
            return false;
        }

        ComponentName expected = new ComponentName(context, InvestmentNotificationListenerService.class);
        String[] listeners = enabledListeners.split(":");
        for (String listener : listeners) {
            ComponentName enabled = ComponentName.unflattenFromString(listener);
            if (expected.equals(enabled)) {
                return true;
            }
        }
        return false;
    }

    public static void openNotificationAccessSettings(@NonNull Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return;
        }
        Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(fallbackIntent);
    }

    public static void requestSyncNow(@NonNull Context context) {
        InvestmentNotificationListenerService.requestManualSync(context);
    }
}
