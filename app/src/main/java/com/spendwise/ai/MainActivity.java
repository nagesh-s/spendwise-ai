package com.spendwise.ai;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.databinding.ActivityMainBinding;
import com.spendwise.ai.quicktiles.QuickTilesFragment;
import com.spendwise.ai.widget.QuickTilesWidgetProvider;

import org.threeten.bp.LocalDate;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private boolean skipNextLockCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        applySystemBarAppearance();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        BottomNavigationView bottomNavView = binding.bottomNavView;
        setupBottomNavigation(bottomNavView);

        consumeSkipLockFlag(getIntent());

        // Check for first run
        android.content.SharedPreferences prefs = getSharedPreferences("spendwise_prefs", MODE_PRIVATE);
        if (prefs.getBoolean("first_run", true)) {
            startActivity(new Intent(this, com.spendwise.ai.onboarding.OnboardingActivity.class));
            finish();
            return;
        }

        // Handle intent from widget
        handleWidgetIntent(getIntent());
    }

    private void setupBottomNavigation(BottomNavigationView bottomNavView) {
        NavigationUI.setupWithNavController(bottomNavView, navController);
        bottomNavView.setOnItemReselectedListener(item -> {
            navController.popBackStack(item.getItemId(), false);
        });
    }

    private void applySystemBarAppearance() {
        int surface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF);
        getWindow().setStatusBarColor(surface);
        getWindow().setNavigationBarColor(surface);

        int nightModeMask = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkTheme = nightModeMask == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDarkTheme);
            controller.setAppearanceLightNavigationBars(!isDarkTheme);
        }
    }

    private void consumeSkipLockFlag(Intent intent) {
        if (intent != null && intent.getBooleanExtra("skip_lock_check", false)) {
            skipNextLockCheck = true;
            intent.removeExtra("skip_lock_check");
        }
    }

    private void checkBiometricLock() {
        if (skipNextLockCheck) {
            skipNextLockCheck = false;
            return;
        }

        if (com.spendwise.ai.security.BiometricHelper.isBiometricEnabled(this) &&
                com.spendwise.ai.security.BiometricHelper.isLockRequired(this)) {
            // Launch lock screen
            Intent lockIntent = new Intent(this, com.spendwise.ai.security.LockScreenActivity.class);
            startActivity(lockIntent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        applySystemBarAppearance();
        if (isFinishing()) {
            return;
        }
        checkBiometricLock();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            com.spendwise.ai.security.BiometricHelper.markAppBackgrounded(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        consumeSkipLockFlag(intent);
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
            intent.removeExtra("action");
            intent.removeExtra("tile_id");
        } else if ("view_buckets".equals(action)) {
            navigateToDestination(R.id.navigation_buckets);
            intent.removeExtra("action");
        } else if ("view_recurring".equals(action)) {
            navigateToDestination(R.id.navigation_recurring);
            intent.removeExtra("action");
        } else if ("quick_tiles_add".equals(intent.getStringExtra("navigate_to"))) {
            getSupportFragmentManager().setFragmentResult(
                    QuickTilesFragment.RESULT_KEY_OPEN_EDITOR,
                    Bundle.EMPTY);
            navigateToDestination(R.id.navigation_quick_tiles);
            intent.removeExtra("navigate_to");
        } else if ("quick_tiles_manager".equals(intent.getStringExtra("navigate_to"))) {
            // Navigate to Quick Tiles management screen
            navigateToDestination(R.id.navigation_quick_tiles);
            intent.removeExtra("navigate_to");
        }
    }

    private void navigateToDestination(int destinationId) {
        if (navController == null) {
            return;
        }
        NavDestination currentDestination = navController.getCurrentDestination();
        if (currentDestination != null && currentDestination.getId() == destinationId) {
            return;
        }
        try {
            navController.navigate(destinationId);
        } catch (IllegalArgumentException ignored) {
            // Ignore invalid destination from stale intents.
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
