package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.InvestmentDao;
import com.spendwise.ai.data.entity.Investment;

import java.util.List;

/**
 * Repository for Investment operations.
 */
public class InvestmentRepository {

    private final InvestmentDao investmentDao;

    public InvestmentRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        investmentDao = db.investmentDao();
    }

    public void insert(Investment investment) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentDao.insert(investment));
    }

    public void update(Investment investment) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentDao.update(investment));
    }

    public void delete(Investment investment) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentDao.delete(investment));
    }

    public LiveData<List<Investment>> getInvestmentsForBucket(long bucketId) {
        return investmentDao.getInvestmentsForBucket(bucketId);
    }

    public LiveData<List<Investment>> getAllInvestments() {
        return investmentDao.getAllInvestments();
    }

    public LiveData<Long> getTotalPortfolioValue() {
        return investmentDao.getTotalPortfolioValue();
    }

    public LiveData<Long> getTotalInvestedAmount() {
        return investmentDao.getTotalInvestedAmount();
    }

    public LiveData<List<Investment>> getByPlatform(String platform) {
        return investmentDao.getByPlatform(platform);
    }
}
