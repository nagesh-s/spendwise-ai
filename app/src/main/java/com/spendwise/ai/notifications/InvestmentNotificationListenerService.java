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
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.engine.EventIngestionPipeline;
import com.spendwise.ai.engine.InvestmentAutoIngestionEngine;
import com.spendwise.ai.engine.InvestmentGamificationEngine;
import com.spendwise.ai.engine.OccurrenceReconciliationEngine;
import com.spendwise.ai.engine.SalaryAllocationOrchestrator;
import com.spendwise.ai.engine.SalaryDetector;
import com.spendwise.ai.sms.InvestmentSmsParser;
import com.spendwise.ai.sms.InvestmentSignalKeyFactory;
import com.spendwise.ai.sms.ParsedInvestmentSignal;
import com.spendwise.ai.sms.ParsedTransaction;
import com.spendwise.ai.sms.TransactionParser;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Notification listener for parsing investment signals from broker app and
 * email
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
        processNotification(sbn);
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

        for (StatusBarNotification notification : notifications) {
            processNotification(notification);
        }
    }

    private void processNotification(@Nullable StatusBarNotification sbn) {
        NotificationPayload payload = parseNotificationPayload(sbn);
        if (payload == null) {
            return;
        }

        Context appContext = getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            if (payload.salaryDetection != null) {
                new SalaryAllocationOrchestrator(appContext).onSalaryDetected(
                        payload.salaryDetection.getAmountCents(),
                        payload.salaryDetection.getTimestampMillis(),
                        IngestionEvent.SOURCE_NOTIFICATION,
                        buildSalarySourceKey(payload.transaction, payload),
                        payload.message);
            }

            if (payload.investmentSignal != null) {
                EventIngestionPipeline pipeline = new EventIngestionPipeline();
                EventIngestionPipeline.IngestResult event = pipeline.ingest(
                        db,
                        IngestionEvent.DOMAIN_INVESTMENT,
                        IngestionEvent.SOURCE_NOTIFICATION,
                        InvestmentSignalKeyFactory.buildIngestionSourceKey(payload.investmentSignal),
                        payload.investmentSignal.getCorrelationKey(),
                        payload.investmentSignal.getSignalType().name(),
                        payload.investmentSignal.getAmountCents(),
                        payload.investmentSignal.getTimestampMillis(),
                        payload.sender,
                        payload.investmentSignal.getRawMessage(),
                        payload.investmentSignal.getRawMessage());
                if (!event.duplicate) {
                    List<ParsedInvestmentSignal> oneSignal = new ArrayList<>();
                    oneSignal.add(payload.investmentSignal);
                    InvestmentAutoIngestionEngine.IngestResult ingestResult = new InvestmentAutoIngestionEngine()
                            .ingest(db, oneSignal, IngestionEvent.SOURCE_NOTIFICATION);
                    OccurrenceReconciliationEngine.ReconcileResult reconcileResult = new OccurrenceReconciliationEngine()
                            .reconcileSignals(
                                    db,
                                    oneSignal,
                                    ingestResult.investmentIdBySourceKey,
                                    IngestionEvent.SOURCE_NOTIFICATION);
                    if (reconcileResult.confirmed + reconcileResult.adjusted > 0) {
                        new InvestmentGamificationEngine(appContext).awardSipReconcileXp();
                    }
                    pipeline.markProcessed(db, event.eventId, IngestionEvent.STATUS_PROCESSED);
                }
            }
        });
    }

    @Nullable
    private NotificationPayload parseNotificationPayload(@Nullable StatusBarNotification sbn) {
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
        ParsedInvestmentSignal investmentSignal = InvestmentSmsParser.parse(sender, message, timestamp);

        ParsedTransaction parsedTransaction = null;
        SalaryDetector.SalaryDetectionResult salaryDetection = null;
        if (TransactionParser.isTransactionSms(sender, message)) {
            parsedTransaction = TransactionParser.parse(sender, message, timestamp);
            if (parsedTransaction != null) {
                salaryDetection = new SalaryDetector().detect(parsedTransaction);
            }
        }

        if (investmentSignal == null && salaryDetection == null) {
            return null;
        }
        NotificationPayload payload = new NotificationPayload();
        payload.packageName = sourcePackage;
        payload.sender = sender;
        payload.timestampMillis = timestamp;
        payload.message = message;
        payload.investmentSignal = investmentSignal;
        payload.salaryDetection = salaryDetection;
        payload.transaction = parsedTransaction;
        return payload;
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

    @NonNull
    private String buildSalarySourceKey(
            @Nullable ParsedTransaction transaction,
            @NonNull NotificationPayload payload) {
        long day = payload.timestampMillis / (24L * 60L * 60L * 1000L);
        long amount = payload.salaryDetection == null ? 0L : payload.salaryDetection.getAmountCents();
        String account = transaction == null || transaction.getAccountInfo() == null
                ? "na"
                : transaction.getAccountInfo();
        return String.format(
                Locale.US,
                "salary_notif_%s_%s_%d_%d",
                sanitize(payload.packageName),
                sanitize(account),
                amount,
                day);
    }

    @NonNull
    private String sanitize(@Nullable String value) {
        if (value == null) {
            return "na";
        }
        String cleaned = value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "na" : cleaned;
    }

    private static class NotificationPayload {
        String packageName;
        String sender;
        long timestampMillis;
        String message;
        ParsedInvestmentSignal investmentSignal;
        SalaryDetector.SalaryDetectionResult salaryDetection;
        ParsedTransaction transaction;
    }
}
