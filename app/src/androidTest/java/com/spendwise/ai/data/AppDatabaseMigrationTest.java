package com.spendwise.ai.data;

import android.content.Context;
import android.database.Cursor;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {

    private static final String TEST_DB = "migration-test-v3-v4.db";
    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DB);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(TEST_DB);
    }

    @Test
    public void migration3To4_addsNewTablesAndAccountColumn() {
        SupportSQLiteOpenHelper.Callback callback = new SupportSQLiteOpenHelper.Callback(3) {
            @Override
            public void onCreate(SupportSQLiteDatabase db) {
                createVersion3Schema(db);
            }

            @Override
            public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
                // No-op for test setup.
            }
        };

        SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DB)
                .callback(callback)
                .build();

        SupportSQLiteOpenHelper helper = new FrameworkSQLiteOpenHelperFactory().create(config);
        SupportSQLiteDatabase db = helper.getWritableDatabase();

        // Execute app migration.
        AppDatabase.MIGRATION_3_4.migrate(db);

        assertTableExists(db, "tags");
        assertTableExists(db, "expense_tags");
        assertTableExists(db, "accounts");
        assertTableExists(db, "financial_goals");
        assertExpenseAccountColumnExists(db);
        assertDefaultCashAccountSeeded(db);
        assertExistingExpenseRowsRemainAccessible(db);

        helper.close();
    }

    private void createVersion3Schema(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `colorArgb` INTEGER NOT NULL, `iconKey` TEXT, `isDefault` INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `merchants` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `expenses` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dateEpochDay` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `description` TEXT, " +
                "`categoryId` INTEGER NOT NULL, `merchantId` INTEGER, `photoPath` TEXT)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`monthKey` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, `limitCents` INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `quick_tiles` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`categoryId` INTEGER NOT NULL, `merchantId` INTEGER, `label` TEXT NOT NULL, `amountCents` INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_expenses` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`description` TEXT, `categoryId` INTEGER, `merchantId` INTEGER, `frequency` TEXT, " +
                "`dayOfPeriod` INTEGER NOT NULL, `monthOfYear` INTEGER NOT NULL, `startDateEpochDay` INTEGER NOT NULL, " +
                "`endDateEpochDay` INTEGER, `lastGeneratedEpochDay` INTEGER NOT NULL, " +
                "`isActive` INTEGER NOT NULL, `reminderDaysBefore` INTEGER NOT NULL)");

        db.execSQL("INSERT INTO `categories` (`name`, `colorArgb`, `iconKey`, `isDefault`) " +
                "VALUES ('Test Category', -16776961, 'fa_tag', 0)");
        db.execSQL("INSERT INTO `expenses` (`dateEpochDay`, `amountCents`, `description`, `categoryId`, `merchantId`, `photoPath`) " +
                "VALUES (19700, 12345, 'pre-migration', 1, NULL, NULL)");
    }

    private void assertTableExists(SupportSQLiteDatabase db, String tableName) {
        Cursor cursor = db.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='" + tableName + "'");
        try {
            assertTrue("Expected table " + tableName + " to exist after migration", cursor.moveToFirst());
        } finally {
            cursor.close();
        }
    }

    private void assertExpenseAccountColumnExists(SupportSQLiteDatabase db) {
        Cursor cursor = db.query("PRAGMA table_info(expenses)");
        boolean found = false;
        try {
            int nameColumn = cursor.getColumnIndexOrThrow("name");
            while (cursor.moveToNext()) {
                String columnName = cursor.getString(nameColumn);
                if ("accountId".equals(columnName)) {
                    found = true;
                    break;
                }
            }
        } finally {
            cursor.close();
        }
        assertTrue("Expected accountId column in expenses after migration", found);
    }

    private void assertDefaultCashAccountSeeded(SupportSQLiteDatabase db) {
        Cursor cursor = db.query(
                "SELECT COUNT(*) FROM accounts WHERE name='Cash' AND type='CASH' AND isDefault=1");
        try {
            assertTrue(cursor.moveToFirst());
            int count = cursor.getInt(0);
            assertTrue("Expected default Cash account to be seeded", count >= 1);
        } finally {
            cursor.close();
        }
    }

    private void assertExistingExpenseRowsRemainAccessible(SupportSQLiteDatabase db) {
        Cursor cursor = db.query("SELECT id, amountCents, accountId FROM expenses");
        try {
            assertTrue("Expected existing expense rows after migration", cursor.moveToFirst());
            long amount = cursor.getLong(cursor.getColumnIndexOrThrow("amountCents"));
            assertTrue("Expected existing expense amount to be preserved", amount == 12345L);
        } finally {
            cursor.close();
        }
    }
}
