package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.BudgetDao;
import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.pojo.BudgetWithSpend;

import java.util.List;

/**
 * Repository for Budget data access.
 * Provides a clean API for UI to interact with budget data.
 */
public class BudgetRepository {

    private final BudgetDao budgetDao;

    public BudgetRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        budgetDao = db.budgetDao();
    }

    // ==================== Insert/Update/Delete ====================

    public void insert(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> budgetDao.insert(budget));
    }

    public void update(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> budgetDao.update(budget));
    }

    public void delete(Budget budget) {
        AppDatabase.databaseWriteExecutor.execute(() -> budgetDao.delete(budget));
    }

    /**
     * Insert or update a budget for a category and month.
     */
    public void upsert(long categoryId, String monthKey, long limitCents) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Budget existing = budgetDao.getBudgetForCategoryAndMonth(categoryId, monthKey);
            if (existing != null) {
                existing.setLimitCents(limitCents);
                budgetDao.update(existing);
            } else {
                Budget newBudget = new Budget(monthKey, categoryId, limitCents);
                budgetDao.insert(newBudget);
            }
        });
    }

    // ==================== Queries ====================

    public LiveData<List<Budget>> getBudgetsForMonth(String monthKey) {
        return budgetDao.getBudgetsForMonth(monthKey);
    }

    /**
     * Get budgets with actual spending for progress bars.
     */
    public LiveData<List<BudgetWithSpend>> getBudgetsWithSpend(String monthKey, long startDay, long endDay) {
        return budgetDao.getBudgetsWithSpend(monthKey, startDay, endDay);
    }

    /**
     * Copy budgets from one month to another.
     */
    public void copyBudgetsToNewMonth(String sourceMonthKey, String newMonthKey) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            int existingCount = budgetDao.countBudgetsForMonth(newMonthKey);
            if (existingCount == 0) {
                budgetDao.copyBudgetsToNewMonth(sourceMonthKey, newMonthKey);
            }
        });
    }
}
