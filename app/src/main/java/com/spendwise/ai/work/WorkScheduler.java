package com.spendwise.ai.work;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to schedule and manage WorkManager jobs.
 */
public class WorkScheduler {

        private static final String RECURRING_EXPENSE_WORK = "recurring_expense_work";
        private static final String REMINDER_WORK = "reminder_work";
        private static final String SMS_AUTO_IMPORT_WORK = "sms_auto_import_work";

        /**
         * Schedule the recurring expense worker to run daily.
         * This should be called from Application.onCreate().
         */
        public static void scheduleRecurringExpenseWork(Context context) {
                Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .setRequiresBatteryNotLow(false)
                                .build();

                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                                RecurringExpenseWorker.class,
                                24, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .setInitialDelay(1, TimeUnit.MINUTES)
                                .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                RECURRING_EXPENSE_WORK,
                                ExistingPeriodicWorkPolicy.KEEP,
                                workRequest);
        }

        /**
         * Schedule the reminder worker to run daily.
         */
        public static void scheduleReminderWork(Context context) {
                Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .build();

                // Run at 9 AM ideally, but WorkManager will handle scheduling
                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                                ReminderWorker.class,
                                24, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .setInitialDelay(5, TimeUnit.MINUTES)
                                .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                REMINDER_WORK,
                                ExistingPeriodicWorkPolicy.KEEP,
                                workRequest);
        }

        /**
         * Schedule auto SMS import worker to run periodically.
         */
        public static void scheduleSmsAutoImportWork(Context context) {
                Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .setRequiresBatteryNotLow(false)
                                .build();

                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                                SmsAutoImportWorker.class,
                                6, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .setInitialDelay(10, TimeUnit.MINUTES)
                                .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                SMS_AUTO_IMPORT_WORK,
                                ExistingPeriodicWorkPolicy.KEEP,
                                workRequest);
        }

        /**
         * Run the recurring expense worker immediately (one-time).
         */
        public static void runRecurringExpenseWorkNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                RecurringExpenseWorker.class)
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }

        /**
         * Run the reminder worker immediately (one-time).
         */
        public static void runReminderWorkNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                ReminderWorker.class)
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }

        /**
         * Run SMS auto import worker immediately (one-time).
         */
        public static void runSmsAutoImportNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                SmsAutoImportWorker.class)
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }
}
