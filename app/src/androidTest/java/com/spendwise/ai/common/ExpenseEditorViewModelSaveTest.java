package com.spendwise.ai.common;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.entity.Tag;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.threeten.bp.LocalDate;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ExpenseEditorViewModelSaveTest {

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
    public void saveExpense_persistsExpenseWithMerchantAccountAndTags() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        long categoryId = db.categoryDao().insert(
                new Category("JUnit Category " + suffix, Color.parseColor("#03A9F4"), "fa_tag", false));
        long accountId = db.accountDao().insert(
                new Account("JUnit Wallet " + suffix, Account.TYPE_WALLET, 0L,
                        Color.parseColor("#4CAF50"), "fa_wallet", false));
        long tagId = db.tagDao().insert(new Tag("JUnit Tag " + suffix, Color.parseColor("#FF9800")));

        ExpenseEditorViewModel viewModel = new ExpenseEditorViewModel(application);
        long dateEpochDay = LocalDate.of(2026, 2, 14).toEpochDay();
        int beforeCount = db.expenseDao().getExpenseCount();

        viewModel.initForNewExpense(dateEpochDay);
        viewModel.setAmountCents(12345L);
        viewModel.setDescription("JUnit save flow");
        viewModel.setSelectedCategoryId(categoryId);
        viewModel.setMerchantName("JUnit Merchant " + suffix);
        viewModel.setSelectedAccountId(accountId);
        viewModel.toggleTag(tagId);

        viewModel.saveExpense();

        waitForCondition(() -> db.expenseDao().getExpenseCount() == beforeCount + 1, 5, TimeUnit.SECONDS);

        List<Expense> expenses = db.expenseDao().getAllExpensesSync();
        assertEquals(1, expenses.size());
        Expense saved = expenses.get(0);
        assertEquals(12345L, saved.getAmountCents());
        assertEquals("JUnit save flow", saved.getDescription());
        assertEquals(categoryId, saved.getCategoryId());
        assertNotNull(saved.getMerchantId());
        assertNotNull(saved.getAccountId());
        assertEquals(accountId, saved.getAccountId().longValue());

        Merchant merchant = db.merchantDao().findByName("JUnit Merchant " + suffix);
        assertNotNull("Expected merchant to be inserted via save flow", merchant);
        assertEquals(merchant.getId(), saved.getMerchantId().longValue());

        waitForCondition(
                () -> db.tagDao().getTagsForExpenseSync(saved.getId()).size() == 1,
                5,
                TimeUnit.SECONDS);
        List<Tag> savedTags = db.tagDao().getTagsForExpenseSync(saved.getId());
        assertEquals(1, savedTags.size());
        assertEquals(tagId, savedTags.get(0).getId());
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
        assertTrue("Timed out waiting for asynchronous save operation", condition.evaluate());
    }

    private interface Condition {
        boolean evaluate();
    }
}
