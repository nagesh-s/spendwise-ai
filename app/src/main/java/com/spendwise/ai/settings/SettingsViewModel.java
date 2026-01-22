package com.spendwise.ai.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel for Settings screen.
 * Manages dark mode preference and import/export operations.
 */
public class SettingsViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "spendwise_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";

    private final SharedPreferences sharedPreferences;
    private final MutableLiveData<Boolean> darkModeEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> exportStatus = new MutableLiveData<>();
    private final MutableLiveData<String> importStatus = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Load dark mode preference
        boolean isDarkMode = sharedPreferences.getBoolean(KEY_DARK_MODE, false);
        darkModeEnabled.setValue(isDarkMode);
    }

    // ==================== Dark Mode ====================

    public LiveData<Boolean> getDarkModeEnabled() {
        return darkModeEnabled;
    }

    public void setDarkModeEnabled(boolean enabled) {
        darkModeEnabled.setValue(enabled);

        // Save to preferences
        sharedPreferences.edit()
                .putBoolean(KEY_DARK_MODE, enabled)
                .apply();

        // Apply theme
        AppCompatDelegate.setDefaultNightMode(
                enabled ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public void toggleDarkMode() {
        Boolean current = darkModeEnabled.getValue();
        setDarkModeEnabled(current == null || !current);
    }

    /**
     * Apply saved dark mode preference on app startup.
     * Call this from MainActivity or Application class.
     */
    public static void applySavedDarkMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    // ==================== Export/Import Status ====================

    public LiveData<String> getExportStatus() {
        return exportStatus;
    }

    public void setExportStatus(String status) {
        exportStatus.setValue(status);
    }

    public LiveData<String> getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(String status) {
        importStatus.setValue(status);
    }

    public void clearStatus() {
        exportStatus.setValue(null);
        importStatus.setValue(null);
    }
}
