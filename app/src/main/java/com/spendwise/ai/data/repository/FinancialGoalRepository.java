package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.FinancialGoalDao;
import com.spendwise.ai.data.entity.FinancialGoal;

import java.util.List;

/**
 * Repository for FinancialGoal operations.
 */
public class FinancialGoalRepository {

    private final FinancialGoalDao goalDao;

    public FinancialGoalRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        goalDao = db.financialGoalDao();
    }

    public void insert(FinancialGoal goal) {
        AppDatabase.databaseWriteExecutor.execute(() -> goalDao.insert(goal));
    }

    public void update(FinancialGoal goal) {
        AppDatabase.databaseWriteExecutor.execute(() -> goalDao.update(goal));
    }

    public void delete(FinancialGoal goal) {
        AppDatabase.databaseWriteExecutor.execute(() -> goalDao.delete(goal));
    }

    public LiveData<List<FinancialGoal>> getAllGoals() {
        return goalDao.getAllGoals();
    }

    public LiveData<List<FinancialGoal>> getActiveGoals() {
        return goalDao.getActiveGoals();
    }

    public void updateProgress(long goalId, long amountCents) {
        AppDatabase.databaseWriteExecutor.execute(() -> goalDao.updateProgress(goalId, amountCents));
    }

    public void updateStatus(long goalId, String status) {
        AppDatabase.databaseWriteExecutor.execute(() -> goalDao.updateStatus(goalId, status));
    }
}
