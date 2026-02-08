package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.ExpenseDao;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.data.pojo.DailyTotal;

import java.util.List;

/**
 * Repository for Expense data access.
 * Provides a clean API for UI to interact with expense data.
 * All database operations run on background threads.
 */
public class ExpenseRepository {

    private final ExpenseDao expenseDao;

    public ExpenseRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        expenseDao = db.expenseDao();
    }

    // ==================== Insert/Update/Delete ====================

    public void insert(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> expenseDao.insert(expense));
    }

    public void update(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> expenseDao.update(expense));
    }

    public void delete(Expense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> expenseDao.delete(expense));
    }

    public void deleteById(long expenseId) {
        AppDatabase.databaseWriteExecutor.execute(() -> expenseDao.deleteById(expenseId));
    }

    // ==================== Queries ====================

    public LiveData<List<Expense>> getAllExpenses() {
        return expenseDao.getAllExpenses();
    }

    public Expense getExpenseByIdSync(long expenseId) {
        return expenseDao.getById(expenseId);
    }

    public LiveData<List<Expense>> getExpensesForDate(long dateEpochDay) {
        return expenseDao.getExpensesForDate(dateEpochDay);
    }

    public LiveData<List<Expense>> getExpensesForRange(long startDay, long endDay) {
        return expenseDao.getExpensesForRange(startDay, endDay);
    }

    // ==================== Aggregates ====================

    public LiveData<Long> getTotalForRangeLive(long startDay, long endDay) {
        return expenseDao.getTotalForRangeLive(startDay, endDay);
    }

    public LiveData<List<CategoryTotal>> getCategoryTotalsForRange(long startDay, long endDay) {
        return expenseDao.getCategoryTotalsForRange(startDay, endDay);
    }

    public LiveData<List<DailyTotal>> getDailyTotalsForRange(long startDay, long endDay) {
        return expenseDao.getDailyTotalsForRange(startDay, endDay);
    }

    public LiveData<List<Expense>> getExpensesByCategory(long categoryId) {
        return expenseDao.getExpensesByCategory(categoryId);
    }

    public LiveData<List<Expense>> getRecentExpensesByMerchant(long merchantId, int limit) {
        return expenseDao.getRecentExpensesByMerchant(merchantId, limit);
    }
}
