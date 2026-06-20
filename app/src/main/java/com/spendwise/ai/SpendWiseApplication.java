package com.spendwise.ai;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.spendwise.ai.notifications.NotificationHelper;
import com.spendwise.ai.sms.SmsAutoImportPrefs;
import com.spendwise.ai.work.WorkScheduler;

public class SpendWiseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        // Create notification channels
        NotificationHelper.createNotificationChannels(this);

        // Apply saved dark mode preference on app startup
        com.spendwise.ai.settings.SettingsViewModel.applySavedDarkMode(this);

        // Schedule recurring expense processing
        WorkScheduler.scheduleRecurringExpenseWork(this);

        // Schedule reminder notifications
        WorkScheduler.scheduleReminderWork(this);

        // Schedule periodic SMS auto-import (user controlled).
        if (SmsAutoImportPrefs.isEnabled(this)) {
            WorkScheduler.scheduleSmsAutoImportWork(this);
        } else {
            WorkScheduler.cancelSmsAutoImportWork(this);
        }
        WorkScheduler.scheduleSalaryAutomationWork(this);
        WorkScheduler.scheduleInvestmentScheduleWork(this);
        WorkScheduler.scheduleInvestmentDigestWork(this);
        WorkScheduler.scheduleInvestmentMaintenanceWork(this);

        // Run immediately to catch up on any missed work
        WorkScheduler.runRecurringExpenseWorkNow(this);
        WorkScheduler.runReminderWorkNow(this);
        if (SmsAutoImportPrefs.isEnabled(this)) {
            WorkScheduler.runSmsAutoImportNow(this);
        }
        WorkScheduler.runSalaryAutomationNow(this);
        WorkScheduler.runInvestmentScheduleNow(this);
    }
}
