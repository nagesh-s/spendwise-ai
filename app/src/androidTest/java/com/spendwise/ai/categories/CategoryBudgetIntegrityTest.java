package com.spendwise.ai.categories;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.util.DateUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CategoryBudgetIntegrityTest {

    private static final String APP_DB_NAME = "spendwise_db";

    private Context context;
    private Application application;
    private AppDatabase db;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        application = (Application) context.getApplicationContext();
        AppDatabase.closeDatabase();
        context.deleteDatabase(APP_DB_NAME);
        db = AppDatabase.getInstance(application);
    }

    @After
    public void tearDown() {
        AppDatabase.closeDatabase();
        context.deleteDatabase(APP_DB_NAME);
    }

    @Test
    public void categoryDeleteProtection_andBudgetSpendLinking_areConsistent() throws Exception {
        waitForCondition(() -> db.categoryDao().getDefaultCategories().size() > 0, 5, TimeUnit.SECONDS);
        List<Category> defaults = db.categoryDao().getDefaultCategories();
        assertTrue("Expected seeded default categories", defaults.size() > 0);

        Category defaultCategory = defaults.get(0);
        int deletedDefault = db.categoryDao().deleteByIdIfNotDefault(defaultCategory.getId());
        assertEquals(0, deletedDefault);
        assertNotNull(db.categoryDao().getById(defaultCategory.getId()));

        String suffix = String.valueOf(System.currentTimeMillis());
        long customCategoryId = db.categoryDao().insert(
                new Category("Budget Test " + suffix, Color.parseColor("#03A9F4"), "fa_tag", false));
        assertNotNull(db.categoryDao().getById(customCategoryId));

        int deletedCustom = db.categoryDao().deleteByIdIfNotDefault(customCategoryId);
        assertEquals(1, deletedCustom);
        assertNull(db.categoryDao().getById(customCategoryId));

        long budgetCategoryId = db.categoryDao().insert(
                new Category("Budget Link " + suffix, Color.parseColor("#E91E63"), "fa_tag", false));

        YearMonth month = YearMonth.of(2026, 2);
        String monthKey = DateUtils.getMonthKey(month);
        long start = DateUtils.getMonthStartEpochDay(month);
        long end = DateUtils.getMonthEndEpochDay(month);

        db.budgetDao().insert(new Budget(monthKey, budgetCategoryId, 10000L));
        db.expenseDao().insert(new Expense(LocalDate.of(2026, 2, 10).toEpochDay(), 2500L, "inside", budgetCategoryId, null));
        db.expenseDao().insert(new Expense(LocalDate.of(2026, 2, 20).toEpochDay(), 1500L, "inside2", budgetCategoryId, null));
        db.expenseDao().insert(new Expense(LocalDate.of(2026, 3, 1).toEpochDay(), 9999L, "outside", budgetCategoryId, null));

        List<BudgetWithSpend> withSpend = db.budgetDao().getBudgetsWithSpendSync(monthKey, start, end);
        assertEquals(1, withSpend.size());
        BudgetWithSpend row = withSpend.get(0);
        assertEquals(budgetCategoryId, row.getCategoryId());
        assertEquals(10000L, row.getLimitCents());
        assertEquals(4000L, row.getSpentCents());
        assertEquals(40, row.getUsagePercent());
    }

    private void waitForCondition(Condition condition, long timeout, TimeUnit unit) throws Exception {
        long timeoutMillis = unit.toMillis(timeout);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMillis) {
            if (condition.evaluate()) {
                return;
            }
            Thread.sleep(50);
        }
        assertTrue("Timed out waiting for async category seeding", condition.evaluate());
    }

    private interface Condition {
        boolean evaluate();
    }
}
