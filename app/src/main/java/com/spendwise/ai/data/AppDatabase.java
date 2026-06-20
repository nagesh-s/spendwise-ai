package com.spendwise.ai.data;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.spendwise.ai.data.dao.AccountDao;
import com.spendwise.ai.data.dao.BucketDao;
import com.spendwise.ai.data.dao.BudgetDao;
import com.spendwise.ai.data.dao.CategoryDao;
import com.spendwise.ai.data.dao.ExpenseDao;
import com.spendwise.ai.data.dao.FinancialGoalDao;
import com.spendwise.ai.data.dao.IngestionEventDao;
import com.spendwise.ai.data.dao.InvestmentDao;
import com.spendwise.ai.data.dao.InvestmentEventLinkDao;
import com.spendwise.ai.data.dao.InvestmentOccurrenceDao;
import com.spendwise.ai.data.dao.InvestmentScheduleDao;
import com.spendwise.ai.data.dao.InvestmentTransactionDao;
import com.spendwise.ai.data.dao.InstrumentBucketMappingDao;
import com.spendwise.ai.data.dao.MerchantDao;
import com.spendwise.ai.data.dao.QuickTileDao;
import com.spendwise.ai.data.dao.RecurringExpenseDao;
import com.spendwise.ai.data.dao.TagDao;
import com.spendwise.ai.data.dao.ProviderDao;
import com.spendwise.ai.data.dao.ReviewCandidateDao;
import com.spendwise.ai.data.dao.UserCorrectionDao;
import com.spendwise.ai.data.dao.ValuationSnapshotDao;
import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.BucketCategory;
import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.FinancialGoal;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.InstrumentBucketMapping;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.InvestmentEventLink;
import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.data.entity.InvestmentSchedule;
import com.spendwise.ai.data.entity.InvestmentTransaction;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.data.entity.RecurringExpense;
import com.spendwise.ai.data.entity.ExpenseTag;
import com.spendwise.ai.data.entity.Tag;
import com.spendwise.ai.data.entity.Provider;
import com.spendwise.ai.data.entity.ReviewCandidate;
import com.spendwise.ai.data.entity.UserCorrection;
import com.spendwise.ai.data.entity.ValuationSnapshot;
import com.spendwise.ai.BuildConfig;

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
                FinancialGoal.class,
                Bucket.class,
                BucketCategory.class,
                Provider.class,
                Holding.class,
                InvestmentTransaction.class,
                InvestmentEventLink.class,
                ValuationSnapshot.class,
                UserCorrection.class,
                ReviewCandidate.class,
                InvestmentSchedule.class,
                InvestmentOccurrence.class,
                IngestionEvent.class,
                InstrumentBucketMapping.class
}, version = 10, exportSchema = true)
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

        public abstract BucketDao bucketDao();

        public abstract InvestmentDao investmentDao();

        public abstract ProviderDao providerDao();

        public abstract InvestmentTransactionDao investmentTransactionDao();

        public abstract InvestmentEventLinkDao investmentEventLinkDao();

        public abstract ValuationSnapshotDao valuationSnapshotDao();

        public abstract UserCorrectionDao userCorrectionDao();

        public abstract ReviewCandidateDao reviewCandidateDao();

        public abstract InvestmentScheduleDao investmentScheduleDao();

        public abstract InvestmentOccurrenceDao investmentOccurrenceDao();

        public abstract IngestionEventDao ingestionEventDao();

        public abstract InstrumentBucketMappingDao instrumentBucketMappingDao();

        /**
         * Get the singleton database instance.
         * Creates the database on first call, with default categories prepopulated.
         */
        public static AppDatabase getInstance(Context context) {
                if (INSTANCE == null) {
                        synchronized (AppDatabase.class) {
                                if (INSTANCE == null) {
                        RoomDatabase.Builder<AppDatabase> builder = Room.databaseBuilder(
                                                        context.getApplicationContext(),
                                                        AppDatabase.class,
                                                        DATABASE_NAME)
                                                        .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                                                                        MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                                                        .addCallback(prepopulateCallback);
                        if (BuildConfig.ALLOW_DESTRUCTIVE_DATABASE_MIGRATION) {
                                builder.fallbackToDestructiveMigration();
                        }
                        INSTANCE = builder.build();
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
                        seedDefaultBuckets();
                }

                @Override
                public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);
                        // Also check on open in case migration cleared the data
                        seedDefaultCategories();
                        seedDefaultAccounts();
                        seedDefaultBuckets();
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
                accounts.add(new Account("UPI", Account.TYPE_UPI, 0, Color.parseColor("#00897B"), "fa_mobile_alt",
                                false));
                accounts.add(new Account("Bank", Account.TYPE_BANK, 0, Color.parseColor("#1E88E5"), "fa_university",
                                false));
                accounts.add(new Account("Debit Card", Account.TYPE_DEBIT_CARD, 0, Color.parseColor("#5E35B1"),
                                "fa_credit_card", false));
                accounts.add(new Account("Credit Card", Account.TYPE_CREDIT_CARD, 0, Color.parseColor("#E53935"),
                                "fa_credit_card", false));
                accounts.add(new Account("Wallet", Account.TYPE_WALLET, 0, Color.parseColor("#FB8C00"), "fa_wallet",
                                false));
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
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_expense_tags_tagId` ON `expense_tags` (`tagId`)");

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
                                        "INSERT INTO `accounts` (`name`, `type`, `balanceCents`, `colorArgb`, `iconKey`, `isDefault`) "
                                                        +
                                                        "VALUES ('Cash', 'CASH', 0, " + Color.parseColor("#4CAF50")
                                                        + ", 'fa_wallet', 1)");

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
         * Migration from version 4 to 5:
         * - Creates 'buckets' table
         * - Creates 'bucket_categories' join table
         * - Creates 'investments' table
         * - Seeds 5 default buckets (Emergency, Short-term, Medium-term, Long-term,
         * Retirement)
         * All existing data is preserved.
         */
        static final Migration MIGRATION_4_5 = new Migration(4, 5) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                        // Create buckets table
                        database.execSQL("CREATE TABLE IF NOT EXISTS `buckets` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`name` TEXT NOT NULL, " +
                                        "`type` TEXT NOT NULL, " +
                                        "`targetPercent` INTEGER NOT NULL DEFAULT 0, " +
                                        "`currentBalanceCents` INTEGER NOT NULL DEFAULT 0, " +
                                        "`colorArgb` INTEGER NOT NULL, " +
                                        "`iconKey` TEXT, " +
                                        "`sortOrder` INTEGER NOT NULL DEFAULT 0, " +
                                        "`isDefault` INTEGER NOT NULL DEFAULT 0, " +
                                        "`description` TEXT, " +
                                        "`timelineLabel` TEXT, " +
                                        "`investmentAdvice` TEXT)");

                        // Create bucket_categories join table
                        database.execSQL("CREATE TABLE IF NOT EXISTS `bucket_categories` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`bucketId` INTEGER NOT NULL, " +
                                        "`categoryId` INTEGER NOT NULL, " +
                                        "FOREIGN KEY(`bucketId`) REFERENCES `buckets`(`id`) ON DELETE CASCADE, " +
                                        "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE CASCADE)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_bucket_categories_bucketId` ON `bucket_categories` (`bucketId`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_bucket_categories_categoryId` ON `bucket_categories` (`categoryId`)");

                        // Create investments table
                        database.execSQL("CREATE TABLE IF NOT EXISTS `investments` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`bucketId` INTEGER NOT NULL, " +
                                        "`name` TEXT NOT NULL, " +
                                        "`jobTitle` TEXT, " +
                                        "`platform` TEXT, " +
                                        "`assetType` TEXT NOT NULL, " +
                                        "`investedAmountCents` INTEGER NOT NULL DEFAULT 0, " +
                                        "`currentValueCents` INTEGER NOT NULL DEFAULT 0, " +
                                        "`returnPercent` REAL, " +
                                        "`maturityDateEpochDay` INTEGER, " +
                                        "`interestRate` REAL, " +
                                        "`lastUpdatedEpochDay` INTEGER NOT NULL, " +
                                        "`notes` TEXT, " +
                                        "`isAutoFetched` INTEGER NOT NULL DEFAULT 0, " +
                                        "`isSip` INTEGER NOT NULL DEFAULT 0, " +
                                        "`sipAmountCents` INTEGER, " +
                                        "FOREIGN KEY(`bucketId`) REFERENCES `buckets`(`id`) ON DELETE CASCADE)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investments_bucketId` ON `investments` (`bucketId`)");

                        // Seed 5 default buckets
                        seedDefaultBucketsInMigration(database);
                }
        };

        /**
         * Migration from version 5 to 6:
         * - Adds dual-ledger + automation data tables
         * - Adds reconciliation linkage columns to investments
         */
        static final Migration MIGRATION_5_6 = new Migration(5, 6) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL("ALTER TABLE `investments` ADD COLUMN `primaryCorrelationKey` TEXT");
                        database.execSQL("ALTER TABLE `investments` ADD COLUMN `lastSourceType` TEXT");
                        database.execSQL("ALTER TABLE `investments` ADD COLUMN `lastSourceKey` TEXT");

                        database.execSQL("CREATE TABLE IF NOT EXISTS `investment_schedules` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`name` TEXT NOT NULL, " +
                                        "`bucketId` INTEGER NOT NULL, " +
                                        "`assetType` TEXT NOT NULL, " +
                                        "`platform` TEXT, " +
                                        "`amountCents` INTEGER NOT NULL, " +
                                        "`frequency` TEXT NOT NULL, " +
                                        "`dayOfPeriod` INTEGER NOT NULL, " +
                                        "`startEpochDay` INTEGER NOT NULL, " +
                                        "`endEpochDay` INTEGER, " +
                                        "`isActive` INTEGER NOT NULL, " +
                                        "`source` TEXT, " +
                                        "`notes` TEXT, " +
                                        "FOREIGN KEY(`bucketId`) REFERENCES `buckets`(`id`) ON DELETE CASCADE)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_schedules_bucketId` ON `investment_schedules` (`bucketId`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_schedules_isActive` ON `investment_schedules` (`isActive`)");

                        database.execSQL("CREATE TABLE IF NOT EXISTS `investment_occurrences` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`scheduleId` INTEGER NOT NULL, " +
                                        "`dueEpochDay` INTEGER NOT NULL, " +
                                        "`expectedAmountCents` INTEGER NOT NULL, " +
                                        "`status` TEXT NOT NULL, " +
                                        "`confirmedAmountCents` INTEGER, " +
                                        "`confirmedAtMillis` INTEGER, " +
                                        "`linkedInvestmentId` INTEGER, " +
                                        "`correlationKey` TEXT, " +
                                        "`sourceType` TEXT, " +
                                        "FOREIGN KEY(`scheduleId`) REFERENCES `investment_schedules`(`id`) ON DELETE CASCADE, "
                                        +
                                        "FOREIGN KEY(`linkedInvestmentId`) REFERENCES `investments`(`id`) ON DELETE SET NULL)");
                        database.execSQL(
                                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_investment_occurrences_scheduleId_dueEpochDay` ON `investment_occurrences` (`scheduleId`, `dueEpochDay`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_occurrences_status` ON `investment_occurrences` (`status`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_occurrences_correlationKey` ON `investment_occurrences` (`correlationKey`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_occurrences_linkedInvestmentId` ON `investment_occurrences` (`linkedInvestmentId`)");

                        database.execSQL("CREATE TABLE IF NOT EXISTS `ingestion_events` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`domain` TEXT NOT NULL, " +
                                        "`sourceType` TEXT NOT NULL, " +
                                        "`sourceKey` TEXT NOT NULL, " +
                                        "`correlationKey` TEXT, " +
                                        "`eventType` TEXT NOT NULL, " +
                                        "`amountCents` INTEGER NOT NULL, " +
                                        "`eventTimestampMillis` INTEGER NOT NULL, " +
                                        "`payloadHash` TEXT, " +
                                        "`processedAtMillis` INTEGER, " +
                                        "`status` TEXT NOT NULL)");
                        database.execSQL(
                                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_ingestion_events_sourceKey` ON `ingestion_events` (`sourceKey`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_ingestion_events_correlationKey` ON `ingestion_events` (`correlationKey`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_ingestion_events_eventTimestampMillis` ON `ingestion_events` (`eventTimestampMillis`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_ingestion_events_status` ON `ingestion_events` (`status`)");

                        database.execSQL("CREATE TABLE IF NOT EXISTS `instrument_bucket_mappings` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`instrumentPattern` TEXT NOT NULL, " +
                                        "`platformPattern` TEXT, " +
                                        "`assetType` TEXT NOT NULL, " +
                                        "`bucketId` INTEGER NOT NULL, " +
                                        "`confidenceBoost` REAL NOT NULL, " +
                                        "`updatedAtMillis` INTEGER NOT NULL, " +
                                        "`source` TEXT NOT NULL, " +
                                        "FOREIGN KEY(`bucketId`) REFERENCES `buckets`(`id`) ON DELETE CASCADE)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_instrument_bucket_mappings_bucketId` ON `instrument_bucket_mappings` (`bucketId`)");
                        database.execSQL(
                                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_instrument_bucket_mappings_instrumentPattern_platformPattern_assetType` ON `instrument_bucket_mappings` (`instrumentPattern`, `platformPattern`, `assetType`)");
                }
        };

        /**
         * Migration from version 6 to 7:
         * - Tracks expense rows that are really investment transfers
         * - Adds linkage metadata for expense -> holding recovery
         */
        static final Migration MIGRATION_6_7 = new Migration(6, 7) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL(
                                        "ALTER TABLE `expenses` ADD COLUMN `isInvestmentTransfer` INTEGER NOT NULL DEFAULT 0");
                        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `linkedInvestmentId` INTEGER");
                        database.execSQL(
                                        "ALTER TABLE `expenses` ADD COLUMN `recoveryStatus` TEXT NOT NULL DEFAULT 'NONE'");
                        database.execSQL("ALTER TABLE `expenses` ADD COLUMN `canonicalSourceKey` TEXT");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_expenses_isInvestmentTransfer` ON `expenses` (`isInvestmentTransfer`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_expenses_recoveryStatus` ON `expenses` (`recoveryStatus`)");
                        database.execSQL(
                                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_expenses_canonicalSourceKey` ON `expenses` (`canonicalSourceKey`)");
                }
        };

        /**
         * Migration from version 7 to 8:
         * - Adds sourceSender and sourceBody columns to IngestionEvent for source
         * traceability
         */
        static final Migration MIGRATION_7_8 = new Migration(7, 8) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL("ALTER TABLE `ingestion_events` ADD COLUMN `sourceSender` TEXT");
                        database.execSQL("ALTER TABLE `ingestion_events` ADD COLUMN `sourceBody` TEXT");
                }
        };

        /**
         * Migration from version 8 to 9:
         * - Adds canonical investment identity for duplicate-safe holding merges
         * - Adds source links so multiple SMS/notification/repair events can point
         * to one holding
         */
        static final Migration MIGRATION_8_9 = new Migration(8, 9) {
                @Override
                public void migrate(@NonNull SupportSQLiteDatabase database) {
                        database.execSQL("ALTER TABLE `investments` ADD COLUMN `canonicalInstrumentKey` TEXT");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investments_canonicalInstrumentKey` ON `investments` (`canonicalInstrumentKey`)");

                        database.execSQL("CREATE TABLE IF NOT EXISTS `investment_sources` (" +
                                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                        "`investmentId` INTEGER NOT NULL, " +
                                        "`sourceType` TEXT NOT NULL, " +
                                        "`sourceKey` TEXT NOT NULL, " +
                                        "`correlationKey` TEXT, " +
                                        "`canonicalInstrumentKey` TEXT, " +
                                        "`eventType` TEXT NOT NULL, " +
                                        "`amountCents` INTEGER NOT NULL, " +
                                        "`eventTimestampMillis` INTEGER NOT NULL, " +
                                        "FOREIGN KEY(`investmentId`) REFERENCES `investments`(`id`) ON DELETE CASCADE)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_sources_investmentId` ON `investment_sources` (`investmentId`)");
                        database.execSQL(
                                        "CREATE UNIQUE INDEX IF NOT EXISTS `index_investment_sources_sourceKey` ON `investment_sources` (`sourceKey`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_sources_correlationKey` ON `investment_sources` (`correlationKey`)");
                        database.execSQL(
                                        "CREATE INDEX IF NOT EXISTS `index_investment_sources_canonicalInstrumentKey` ON `investment_sources` (`canonicalInstrumentKey`)");
                }
        };

        /**
         * Seeds the 5 default buckets during migration v4→v5.
         * Uses direct SQL since the DAO is not available during migration.
         */
        private static void seedDefaultBucketsInMigration(SupportSQLiteDatabase database) {
                String insertSql = "INSERT INTO `buckets` (`name`, `type`, `targetPercent`, " +
                                "`currentBalanceCents`, `colorArgb`, `iconKey`, `sortOrder`, `isDefault`, " +
                                "`description`, `timelineLabel`, `investmentAdvice`) VALUES (?, ?, ?, 0, ?, ?, ?, 1, ?, ?, ?)";

                // Bucket 0 — Emergency Fund
                database.execSQL(insertSql, new Object[] {
                                "Emergency Fund", Bucket.TYPE_EMERGENCY, 10,
                                Color.parseColor("#E53935"), "fa_shield", 0,
                                "Protected baseline for unexpected events",
                                "Protected",
                                "High liquidity only. Savings account or liquid mutual funds."
                });

                // Bucket 1 — Enjoy the Present
                database.execSQL(insertSql, new Object[] {
                                "Enjoy the Present", Bucket.TYPE_SHORT_TERM, 20,
                                Color.parseColor("#1E88E5"), "fa_shopping_cart", 1,
                                "Guilt-free spending for immediate desires",
                                "Now \u2013 2 Years",
                                "No stocks. Use FDs, Liquid Funds, or savings account."
                });

                // Bucket 2 — Build the Next
                database.execSQL(insertSql, new Object[] {
                                "Build the Next", Bucket.TYPE_MEDIUM_TERM, 20,
                                Color.parseColor("#FB8C00"), "fa_car", 2,
                                "Upgrading your lifestyle \u2014 car, education, wedding",
                                "2 \u2013 5 Years",
                                "Moderate risk. Arbitrage, Hybrid Funds, Gold."
                });

                // Bucket 3 — Secure the Future
                database.execSQL(insertSql, new Object[] {
                                "Secure the Future", Bucket.TYPE_LONG_TERM, 30,
                                Color.parseColor("#43A047"), "fa_home", 3,
                                "Major life anchors \u2014 house, business, family",
                                "5 \u2013 15 Years",
                                "Growth priority. Flexi-Cap, Mid-Cap, strong stocks."
                });

                // Bucket 4 — Freedom Fund
                database.execSQL(insertSql, new Object[] {
                                "Freedom Fund", Bucket.TYPE_RETIREMENT, 20,
                                Color.parseColor("#7E57C2"), "fa_chart_line", 4,
                                "Financial independence \u2014 working becomes optional",
                                "15+ Years",
                                "Aggressive compounding. Index, Small-Cap, PPF/NPS."
                });
        }

        /**
         * Seed default buckets via DAO (used in Callback, not in migration).
         */
        private static void seedDefaultBuckets() {
                databaseWriteExecutor.execute(() -> {
                        if (INSTANCE == null)
                                return;
                        BucketDao bucketDao = INSTANCE.bucketDao();
                        int count = bucketDao.getBucketCount();
                        if (count == 0) {
                                Bucket emergency = new Bucket("Emergency Fund", Bucket.TYPE_EMERGENCY, 10,
                                                Color.parseColor("#E53935"), 0, true);
                                emergency.setIconKey("fa_shield");
                                emergency.setDescription("Protected baseline for unexpected events");
                                emergency.setTimelineLabel("Protected");
                                emergency.setInvestmentAdvice(
                                                "High liquidity only. Savings account or liquid mutual funds.");
                                bucketDao.insert(emergency);

                                Bucket shortTerm = new Bucket("Enjoy the Present", Bucket.TYPE_SHORT_TERM, 20,
                                                Color.parseColor("#1E88E5"), 1, true);
                                shortTerm.setIconKey("fa_shopping_cart");
                                shortTerm.setDescription("Guilt-free spending for immediate desires");
                                shortTerm.setTimelineLabel("Now \u2013 2 Years");
                                shortTerm.setInvestmentAdvice("No stocks. Use FDs, Liquid Funds, or savings account.");
                                bucketDao.insert(shortTerm);

                                Bucket mediumTerm = new Bucket("Build the Next", Bucket.TYPE_MEDIUM_TERM, 20,
                                                Color.parseColor("#FB8C00"), 2, true);
                                mediumTerm.setIconKey("fa_car");
                                mediumTerm.setDescription("Upgrading your lifestyle \u2014 car, education, wedding");
                                mediumTerm.setTimelineLabel("2 \u2013 5 Years");
                                mediumTerm.setInvestmentAdvice("Moderate risk. Arbitrage, Hybrid Funds, Gold.");
                                bucketDao.insert(mediumTerm);

                                Bucket longTerm = new Bucket("Secure the Future", Bucket.TYPE_LONG_TERM, 30,
                                                Color.parseColor("#43A047"), 3, true);
                                longTerm.setIconKey("fa_home");
                                longTerm.setDescription("Major life anchors \u2014 house, business, family");
                                longTerm.setTimelineLabel("5 \u2013 15 Years");
                                longTerm.setInvestmentAdvice("Growth priority. Flexi-Cap, Mid-Cap, strong stocks.");
                                bucketDao.insert(longTerm);

                                Bucket retirement = new Bucket("Freedom Fund", Bucket.TYPE_RETIREMENT, 20,
                                                Color.parseColor("#7E57C2"), 4, true);
                                retirement.setIconKey("fa_chart_line");
                                retirement.setDescription("Financial independence \u2014 working becomes optional");
                                retirement.setTimelineLabel("15+ Years");
                                retirement.setInvestmentAdvice("Aggressive compounding. Index, Small-Cap, PPF/NPS.");
                                bucketDao.insert(retirement);
                        }
                });
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
