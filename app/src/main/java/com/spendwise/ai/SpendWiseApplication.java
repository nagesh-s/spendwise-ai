package com.spendwise.ai;

import android.app.Application;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.spendwise.ai.work.WorkScheduler;

public class SpendWiseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AndroidThreeTen.init(this);

        // Apply saved dark mode preference on app startup
        com.spendwise.ai.settings.SettingsViewModel.applySavedDarkMode(this);

        // Schedule recurring expense processing
        WorkScheduler.scheduleRecurringExpenseWork(this);

        // Also run immediately to catch up on any missed expenses
        WorkScheduler.runRecurringExpenseWorkNow(this);
    }
}
