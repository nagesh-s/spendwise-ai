package com.spendwise.ai.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.spendwise.ai.MainActivity;
import com.spendwise.ai.R;

/**
 * Helper class for creating and managing app notifications.
 */
public class NotificationHelper {

    public static final String CHANNEL_REMINDERS = "bill_reminders";
    public static final String CHANNEL_GENERAL = "general";

    private static final int NOTIFICATION_ID_REMINDER_BASE = 1000;
    private static final int NOTIFICATION_ID_INVESTMENT_DIGEST = 2200;

    /**
     * Create notification channels. Should be called on app startup.
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Bill Reminders channel (high priority)
            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Bill Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            remindersChannel.setDescription("Notifications for upcoming bills and recurring expenses");
            remindersChannel.enableVibration(true);
            manager.createNotificationChannel(remindersChannel);

            // General channel
            NotificationChannel generalChannel = new NotificationChannel(
                    CHANNEL_GENERAL,
                    "General",
                    NotificationManager.IMPORTANCE_DEFAULT);
            generalChannel.setDescription("General app notifications");
            manager.createNotificationChannel(generalChannel);
        }
    }

    /**
     * Show a bill reminder notification.
     */
    public static void showBillReminderNotification(Context context, long expenseId,
            String title, String description, long amountCents, int daysUntilDue) {

        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("action", "view_recurring");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) expenseId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String amountStr = String.format("₹%.2f", amountCents / 100.0);
        String contentText;
        if (daysUntilDue == 0) {
            contentText = title + " (" + amountStr + ") is due today";
        } else if (daysUntilDue == 1) {
            contentText = title + " (" + amountStr + ") is due tomorrow";
        } else {
            contentText = title + " (" + amountStr + ") is due in " + daysUntilDue + " days";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Bill Reminder")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        // Check notification permission (Android 13+)
        try {
            notificationManager.notify(
                    NOTIFICATION_ID_REMINDER_BASE + (int) expenseId,
                    builder.build());
        } catch (SecurityException e) {
            // Notification permission not granted
        }
    }

    /**
     * Cancel a specific reminder notification.
     */
    public static void cancelReminderNotification(Context context, long expenseId) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(NOTIFICATION_ID_REMINDER_BASE + (int) expenseId);
    }

    /**
     * Show one compact investment automation digest notification.
     */
    public static void showInvestmentDigestNotification(Context context, String title, String body) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("action", "view_buckets");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID_INVESTMENT_DIGEST,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_GENERAL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        try {
            notificationManager.notify(NOTIFICATION_ID_INVESTMENT_DIGEST, builder.build());
        } catch (SecurityException ignore) {
            // Notification permission not granted.
        }
    }
}
