package com.spendwise.ai;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.databinding.ActivityMainBinding;
import com.spendwise.ai.widget.QuickTilesWidgetProvider;

import org.threeten.bp.LocalDate;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        BottomNavigationView bottomNavView = binding.bottomNavView;
        NavigationUI.setupWithNavController(bottomNavView, navController);

        // Check if biometric lock is enabled and required
        checkBiometricLock();

        // Handle intent from widget
        handleWidgetIntent(getIntent());
    }

    private void checkBiometricLock() {
        if (com.spendwise.ai.security.BiometricHelper.isBiometricEnabled(this) &&
                com.spendwise.ai.security.BiometricHelper.isLockRequired(this)) {
            // Launch lock screen
            Intent lockIntent = new Intent(this, com.spendwise.ai.security.LockScreenActivity.class);
            startActivity(lockIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update last active time when app resumes
        com.spendwise.ai.security.BiometricHelper.updateLastActiveTime(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleWidgetIntent(intent);
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent == null)
            return;

        String action = intent.getStringExtra("action");
        if ("create_expense_from_tile".equals(action)) {
            long tileId = intent.getLongExtra("tile_id", -1);
            if (tileId != -1) {
                createExpenseFromTile(tileId);
            }
        } else if ("quick_tiles_manager".equals(intent.getStringExtra("navigate_to"))) {
            // Navigate to Quick Tiles management screen
            navController.navigate(R.id.navigation_quick_tiles);
        }
    }

    private void createExpenseFromTile(long tileId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            QuickTile tile = db.quickTileDao().getById(tileId);

            if (tile != null) {
                // Create expense from tile data
                Expense expense = new Expense(
                        LocalDate.now().toEpochDay(),
                        tile.getAmountCents(),
                        tile.getLabel(),
                        tile.getCategoryId(),
                        tile.getMerchantId());
                db.expenseDao().insert(expense);

                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.expense_added, Toast.LENGTH_SHORT).show();
                    // Refresh the widget
                    QuickTilesWidgetProvider.requestUpdate(this);
                });
            }
        });
    }
}
