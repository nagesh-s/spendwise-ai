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
        private static final String SALARY_AUTOMATION_WORK = "salary_automation_work";
        private static final String INVESTMENT_SCHEDULE_WORK = "investment_schedule_work";
        private static final String INVESTMENT_DIGEST_WORK = "investment_digest_work";

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
         * Schedule salary fallback automation (daily).
         */
        public static void scheduleSalaryAutomationWork(Context context) {
                Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .setRequiresBatteryNotLow(false)
                                .build();

                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                                SalaryAutomationWorker.class,
                                24, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .setInitialDelay(12, TimeUnit.MINUTES)
                                .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                SALARY_AUTOMATION_WORK,
                                ExistingPeriodicWorkPolicy.KEEP,
                                workRequest);
        }

        /**
         * Schedule expected SIP materialization + reconciliation checks (daily).
         */
        public static void scheduleInvestmentScheduleWork(Context context) {
                Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .setRequiresBatteryNotLow(false)
                                .build();

                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                                InvestmentScheduleWorker.class,
                                24, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .setInitialDelay(14, TimeUnit.MINUTES)
                                .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                INVESTMENT_SCHEDULE_WORK,
                                ExistingPeriodicWorkPolicy.KEEP,
                                workRequest);
        }

        /**
         * Schedule one compact daily investment digest notification.
         */
        public static void scheduleInvestmentDigestWork(Context context) {
                Constraints constraints = new Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                                .setRequiresBatteryNotLow(false)
                                .build();

                PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                                InvestmentDigestWorker.class,
                                24, TimeUnit.HOURS)
                                .setConstraints(constraints)
                                .setInitialDelay(20, TimeUnit.MINUTES)
                                .build();

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                INVESTMENT_DIGEST_WORK,
                                ExistingPeriodicWorkPolicy.KEEP,
                                workRequest);
        }

        /**
         * Cancel periodic SMS auto-import work.
         */
        public static void cancelSmsAutoImportWork(Context context) {
                WorkManager.getInstance(context).cancelUniqueWork(SMS_AUTO_IMPORT_WORK);
        }

        /**
         * Enable/disable SMS auto-import schedule.
         */
        public static void setSmsAutoImportEnabled(Context context, boolean enabled) {
                if (enabled) {
                        scheduleSmsAutoImportWork(context);
                        runSmsAutoImportNow(context);
                } else {
                        cancelSmsAutoImportWork(context);
                }
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
                                .setInputData(SmsAutoImportWorker.buildInputData(true, true, 0))
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }

        /**
         * Run a manual holdings repair pass that only scans investments and
         * backfills a wider SMS window.
         */
        public static void runInvestmentSmsRepairNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                SmsAutoImportWorker.class)
                                .setInputData(SmsAutoImportWorker.buildInputData(
                                                false,
                                                true,
                                                SmsAutoImportWorker.defaultRepairLookbackDays()))
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }

        /**
         * Re-scan a full year of SMS to backfill sender/body on existing
         * ingestion events that were created before migration added those columns.
         */
        public static final String SOURCE_REFRESH_WORK = "source_refresh_work";

        public static void runSourceRefreshNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                SmsAutoImportWorker.class)
                                .setInputData(SmsAutoImportWorker.buildInputData(true, true, 365))
                                .build();
                WorkManager.getInstance(context).enqueueUniqueWork(
                                SOURCE_REFRESH_WORK,
                                androidx.work.ExistingWorkPolicy.REPLACE,
                                oneTimeRequest);
        }

        /**
         * Run salary automation worker immediately (one-time).
         */
        public static void runSalaryAutomationNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                SalaryAutomationWorker.class)
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }

        /**
         * Run investment schedule materialization immediately (one-time).
         */
        public static void runInvestmentScheduleNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                InvestmentScheduleWorker.class)
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }

        /**
         * Run investment digest immediately (one-time).
         */
        public static void runInvestmentDigestNow(Context context) {
                OneTimeWorkRequest oneTimeRequest = new OneTimeWorkRequest.Builder(
                                InvestmentDigestWorker.class)
                                .build();
                WorkManager.getInstance(context).enqueue(oneTimeRequest);
        }
}
