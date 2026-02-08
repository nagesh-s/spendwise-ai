package com.spendwise.ai.data;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.spendwise.ai.data.dao.BudgetDao;
import com.spendwise.ai.data.dao.CategoryDao;
import com.spendwise.ai.data.dao.ExpenseDao;
import com.spendwise.ai.data.dao.MerchantDao;
import com.spendwise.ai.data.dao.QuickTileDao;
import com.spendwise.ai.data.dao.RecurringExpenseDao;
import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.data.entity.RecurringExpense;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Room database for SpendWise AI.
 * 
 * Contains all entities and provides access to DAOs.
 * Uses singleton pattern to ensure only one database instance exists.
 */
@Database(entities = {
        Expense.class,
        Category.class,
        Merchant.class,
        Budget.class,
        QuickTile.class,
        RecurringExpense.class
}, version = 2, exportSchema = true)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "spendwise_db";

    // Singleton instance
    private static volatile AppDatabase INSTANCE;

    // Executor for background database operations
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    // Abstract methods to get DAOs
    public abstract ExpenseDao expenseDao();

    public abstract CategoryDao categoryDao();

    public abstract MerchantDao merchantDao();

    public abstract BudgetDao budgetDao();

    public abstract QuickTileDao quickTileDao();

    public abstract RecurringExpenseDao recurringExpenseDao();

    /**
     * Get the singleton database instance.
     * Creates the database on first call, with default categories prepopulated.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME)
                            .addCallback(prepopulateCallback)
                            .fallbackToDestructiveMigration() // For development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback to prepopulate default categories on first database creation.
     */
    private static final RoomDatabase.Callback prepopulateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);

            // Prepopulate default categories in background
            databaseWriteExecutor.execute(() -> {
                CategoryDao categoryDao = INSTANCE.categoryDao();
                categoryDao.insertAll(getDefaultCategories());
            });
        }
    };

    /**
     * Returns the list of default expense categories.
     * These categories cannot be deleted by the user (isDefault = true).
     */
    private static List<Category> getDefaultCategories() {
        List<Category> categories = new ArrayList<>();

        // Food & Dining
        categories.add(new Category("Food & Dining", Color.parseColor("#FF5722"), "fa_utensils", true));

        // Transportation
        categories.add(new Category("Transportation", Color.parseColor("#2196F3"), "fa_car", true));

        // Shopping
        categories.add(new Category("Shopping", Color.parseColor("#9C27B0"), "fa_shopping_bag", true));

        // Entertainment
        categories.add(new Category("Entertainment", Color.parseColor("#E91E63"), "fa_film", true));

        // Bills & Utilities
        categories.add(new Category("Bills & Utilities", Color.parseColor("#607D8B"), "fa_file_invoice", true));

        // Health & Fitness
        categories.add(new Category("Health & Fitness", Color.parseColor("#4CAF50"), "fa_heartbeat", true));

        // Travel
        categories.add(new Category("Travel", Color.parseColor("#00BCD4"), "fa_plane", true));

        // Education
        categories.add(new Category("Education", Color.parseColor("#3F51B5"), "fa_graduation_cap", true));

        // Personal Care
        categories.add(new Category("Personal Care", Color.parseColor("#FF9800"), "fa_spa", true));

        // Other
        categories.add(new Category("Other", Color.parseColor("#795548"), "fa_ellipsis_h", true));

        return categories;
    }

    /**
     * Close the database instance.
     * Should only be called when the app is terminating.
     */
    public static void closeDatabase() {
        if (INSTANCE != null && INSTANCE.isOpen()) {
            INSTANCE.close();
            INSTANCE = null;
        }
    }
}
