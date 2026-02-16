package com.spendwise.ai;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.spendwise.ai.notifications.NotificationHelper;
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

        // Schedule periodic SMS auto-import
        WorkScheduler.scheduleSmsAutoImportWork(this);

        // Run immediately to catch up on any missed work
        WorkScheduler.runRecurringExpenseWorkNow(this);
        WorkScheduler.runReminderWorkNow(this);
        WorkScheduler.runSmsAutoImportNow(this);
    }
}
