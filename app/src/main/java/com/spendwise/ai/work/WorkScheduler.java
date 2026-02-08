package com.spendwise.ai.work;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Helper class to schedule and manage WorkManager jobs.
 */
public class WorkScheduler {

    private static final String RECURRING_EXPENSE_WORK = "recurring_expense_work";

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
                .setInitialDelay(1, TimeUnit.MINUTES) // Small delay on first run
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                RECURRING_EXPENSE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest);
    }

    /**
     * Run the recurring expense worker immediately (one-time).
     * Useful for testing or when app is launched.
     */
    public static void runRecurringExpenseWorkNow(Context context) {
        androidx.work.OneTimeWorkRequest oneTimeRequest = new androidx.work.OneTimeWorkRequest.Builder(
                RecurringExpenseWorker.class)
                .build();

        WorkManager.getInstance(context).enqueue(oneTimeRequest);
    }
}
