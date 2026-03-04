package com.spendwise.ai.notifications;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.engine.InvestmentAutoIngestionEngine;
import com.spendwise.ai.sms.InvestmentSmsParser;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Notification listener for parsing investment signals from broker app and email
 * notifications.
 *
 * Signals are fed to the same ingestion engine as SMS import so duplicate debit
 * + allotment flows remain de-duplicated.
 */
public class InvestmentNotificationListenerService extends NotificationListenerService {

    private static volatile InvestmentNotificationListenerService activeInstance;

    public static void requestManualSync(@NonNull Context context) {
        InvestmentNotificationListenerService instance = activeInstance;
        if (instance != null) {
            instance.ingestActiveNotifications();
            return;
        }
        try {
            requestRebind(new ComponentName(context, InvestmentNotificationListenerService.class));
        } catch (Throwable ignored) {
            // Listener access may still be disabled. Caller handles UX.
        }
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        activeInstance = this;
        ingestActiveNotifications();
    }

    @Override
    public void onListenerDisconnected() {
        if (activeInstance == this) {
            activeInstance = null;
        }
        super.onListenerDisconnected();
    }

    @Override
    public void onNotificationPosted(@NonNull StatusBarNotification sbn) {
        ParsedInvestmentSignal signal = parseNotificationSignal(sbn);
        if (signal == null) {
            return;
        }
        List<ParsedInvestmentSignal> singleSignal = new ArrayList<>();
        singleSignal.add(signal);
        ingestSignals(singleSignal);
    }

    private void ingestActiveNotifications() {
        StatusBarNotification[] notifications;
        try {
            notifications = getActiveNotifications();
        } catch (Throwable ignored) {
            notifications = null;
        }
        if (notifications == null || notifications.length == 0) {
            return;
        }

        List<ParsedInvestmentSignal> parsedSignals = new ArrayList<>();
        for (StatusBarNotification notification : notifications) {
            ParsedInvestmentSignal signal = parseNotificationSignal(notification);
            if (signal != null) {
                parsedSignals.add(signal);
            }
        }
        ingestSignals(parsedSignals);
    }

    private void ingestSignals(@NonNull List<ParsedInvestmentSignal> signals) {
        if (signals.isEmpty()) {
            return;
        }
        Context appContext = getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            new InvestmentAutoIngestionEngine().ingest(db, signals);
        });
    }

    @Nullable
    private ParsedInvestmentSignal parseNotificationSignal(@Nullable StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return null;
        }
        String sourcePackage = sbn.getPackageName();
        if (sourcePackage == null || sourcePackage.equals(getPackageName())) {
            return null;
        }

        String message = extractNotificationMessage(sbn.getNotification());
        if (message.isEmpty()) {
            return null;
        }

        String sender = resolveSenderLabel(sourcePackage);
        long timestamp = sbn.getPostTime() > 0L ? sbn.getPostTime() : System.currentTimeMillis();
        return InvestmentSmsParser.parse(sender, message, timestamp);
    }

    @NonNull
    private String resolveSenderLabel(@NonNull String packageName) {
        try {
            PackageManager packageManager = getPackageManager();
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(appInfo);
            if (label != null && label.toString().trim().length() > 0) {
                return label.toString();
            }
        } catch (PackageManager.NameNotFoundException ignored) {
            // Fall through to package name.
        }
        return packageName;
    }

    @NonNull
    private String extractNotificationMessage(@NonNull Notification notification) {
        Bundle extras = notification.extras;
        if (extras == null) {
            return "";
        }

        LinkedHashSet<String> parts = new LinkedHashSet<>();
        addPart(parts, extras.getCharSequence(Notification.EXTRA_TITLE));
        addPart(parts, extras.getCharSequence(Notification.EXTRA_TEXT));
        addPart(parts, extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        addPart(parts, extras.getCharSequence(Notification.EXTRA_SUB_TEXT));
        addPart(parts, extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));
        addPart(parts, notification.tickerText);

        Object linesObj = extras.get(Notification.EXTRA_TEXT_LINES);
        if (linesObj instanceof CharSequence[]) {
            CharSequence[] lines = (CharSequence[]) linesObj;
            for (CharSequence line : lines) {
                addPart(parts, line);
            }
        }

        if (parts.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (builder.length() > 0) {
                builder.append(". ");
            }
            builder.append(part);
        }
        return builder.toString();
    }

    private void addPart(@NonNull LinkedHashSet<String> parts, @Nullable CharSequence text) {
        if (text == null) {
            return;
        }
        String value = text.toString().trim();
        if (!value.isEmpty()) {
            parts.add(value);
        }
    }
}
