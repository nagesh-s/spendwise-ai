package com.spendwise.ai.data;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.spendwise.ai.data.dao.AccountDao;
import com.spendwise.ai.data.dao.BudgetDao;
import com.spendwise.ai.data.dao.CategoryDao;
import com.spendwise.ai.data.dao.ExpenseDao;
import com.spendwise.ai.data.dao.FinancialGoalDao;
import com.spendwise.ai.data.dao.MerchantDao;
import com.spendwise.ai.data.dao.QuickTileDao;
import com.spendwise.ai.data.dao.RecurringExpenseDao;
import com.spendwise.ai.data.dao.TagDao;
import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.data.entity.RecurringExpense;
import com.spendwise.ai.data.entity.Tag;
import com.spendwise.ai.data.entity.ExpenseTag;
import com.spendwise.ai.data.entity.FinancialGoal;

import androidx.room.migration.Migration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        RecurringExpense.class,
        Tag.class,
        ExpenseTag.class,
        Account.class,
        FinancialGoal.class
}, version = 4, exportSchema = false)
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

    public abstract TagDao tagDao();

    public abstract AccountDao accountDao();

    public abstract FinancialGoalDao financialGoalDao();

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
                            .addMigrations(MIGRATION_3_4)
                            .addCallback(prepopulateCallback)
                            .fallbackToDestructiveMigration() // For development only
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Callback to prepopulate default categories on first database creation
     * or when categories are missing after a migration.
     */
    private static final RoomDatabase.Callback prepopulateCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            seedDefaultCategories();
            seedDefaultAccounts();
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Also check on open in case migration cleared the data
            seedDefaultCategories();
            seedDefaultAccounts();
        }
    };

    private static void seedDefaultCategories() {
        databaseWriteExecutor.execute(() -> {
            if (INSTANCE == null)
                return;
            CategoryDao categoryDao = INSTANCE.categoryDao();
            // Only seed if no categories exist
            int count = categoryDao.getCategoryCount();
            if (count == 0) {
                categoryDao.insertAll(getDefaultCategories());
            }
        });
    }

    /**
     * Seed default accounts and repair missing types for existing users.
     */
    private static void seedDefaultAccounts() {
        databaseWriteExecutor.execute(() -> {
            if (INSTANCE == null)
                return;
            AccountDao accountDao = INSTANCE.accountDao();
            List<Account> existingAccounts = accountDao.getAllAccountsSync();
            Set<String> existingTypes = new HashSet<>();
            if (existingAccounts != null) {
                for (Account account : existingAccounts) {
                    if (account.getType() != null) {
                        existingTypes.add(account.getType().toUpperCase(Locale.US));
                    }
                }
            }

            for (Account account : getDefaultAccounts()) {
                if (!existingTypes.contains(account.getType().toUpperCase(Locale.US))) {
                    accountDao.insert(account);
                }
            }
        });
    }

    private static List<Account> getDefaultAccounts() {
        List<Account> accounts = new ArrayList<>();
        accounts.add(new Account("Cash", Account.TYPE_CASH, 0, Color.parseColor("#4CAF50"), "fa_wallet", true));
        accounts.add(new Account("UPI", Account.TYPE_UPI, 0, Color.parseColor("#00897B"), "fa_mobile_alt", false));
        accounts.add(new Account("Bank", Account.TYPE_BANK, 0, Color.parseColor("#1E88E5"), "fa_university", false));
        accounts.add(new Account("Debit Card", Account.TYPE_DEBIT_CARD, 0, Color.parseColor("#5E35B1"), "fa_credit_card", false));
        accounts.add(new Account("Credit Card", Account.TYPE_CREDIT_CARD, 0, Color.parseColor("#E53935"), "fa_credit_card", false));
        accounts.add(new Account("Wallet", Account.TYPE_WALLET, 0, Color.parseColor("#FB8C00"), "fa_wallet", false));
        return accounts;
    }

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
     * Migration from version 3 to 4:
     * - Creates 'tags' table
     * - Creates 'expense_tags' join table
     * All existing data is preserved.
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Create tags table
            database.execSQL("CREATE TABLE IF NOT EXISTS `tags` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`colorArgb` INTEGER NOT NULL)");

            // Create expense_tags join table
            database.execSQL("CREATE TABLE IF NOT EXISTS `expense_tags` (" +
                    "`expenseId` INTEGER NOT NULL, " +
                    "`tagId` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`expenseId`, `tagId`), " +
                    "FOREIGN KEY(`expenseId`) REFERENCES `expenses`(`id`) ON DELETE CASCADE, " +
                    "FOREIGN KEY(`tagId`) REFERENCES `tags`(`id`) ON DELETE CASCADE)");

            // Create indices for expense_tags
            database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expense_tags_expenseId` ON `expense_tags` (`expenseId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_expense_tags_tagId` ON `expense_tags` (`tagId`)");

            // Create accounts table
            database.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`balanceCents` INTEGER NOT NULL, " +
                    "`colorArgb` INTEGER NOT NULL, " +
                    "`iconKey` TEXT, " +
                    "`isDefault` INTEGER NOT NULL)");

            // Add accountId column to expenses table
            database.execSQL("ALTER TABLE `expenses` ADD COLUMN `accountId` INTEGER");

            // Seed default Cash account
            database.execSQL(
                    "INSERT INTO `accounts` (`name`, `type`, `balanceCents`, `colorArgb`, `iconKey`, `isDefault`) " +
                            "VALUES ('Cash', 'CASH', 0, " + Color.parseColor("#4CAF50") + ", 'fa_wallet', 1)");

            // Create financial_goals table
            database.execSQL("CREATE TABLE IF NOT EXISTS `financial_goals` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`targetAmountCents` INTEGER NOT NULL, " +
                    "`currentAmountCents` INTEGER NOT NULL, " +
                    "`startDateEpochDay` INTEGER NOT NULL, " +
                    "`endDateEpochDay` INTEGER, " +
                    "`status` TEXT NOT NULL, " +
                    "`colorArgb` INTEGER NOT NULL, " +
                    "`iconKey` TEXT, " +
                    "`linkedCategoryId` INTEGER)");
        }
    };

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
