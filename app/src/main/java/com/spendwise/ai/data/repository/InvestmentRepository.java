package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.InvestmentDao;
import com.spendwise.ai.data.entity.Holding;

import java.util.List;

/**
 * Repository for Holding operations.
 */
public class InvestmentRepository {

    private final InvestmentDao investmentDao;

    public InvestmentRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        investmentDao = db.investmentDao();
    }

    public void insert(Holding investment) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentDao.insert(investment));
    }

    public void update(Holding investment) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentDao.update(investment));
    }

    public void delete(Holding investment) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentDao.delete(investment));
    }

    public LiveData<List<Holding>> getInvestmentsForBucket(long bucketId) {
        return investmentDao.getInvestmentsForBucket(bucketId);
    }

    public LiveData<List<com.spendwise.ai.data.pojo.HoldingWithProvider>> getHoldingsWithProviderForBucket(long bucketId) {
        return investmentDao.getHoldingsWithProviderForBucket(bucketId);
    }

    public LiveData<List<Holding>> getAllInvestments() {
        return investmentDao.getAllInvestments();
    }

    public LiveData<Long> getTotalPortfolioValue() {
        return investmentDao.getTotalPortfolioValue();
    }

    public LiveData<Long> getTotalInvestedAmount() {
        return investmentDao.getTotalInvestedAmount();
    }

    public LiveData<List<Holding>> getByPlatform(String platform) {
        return investmentDao.getByPlatform(platform);
    }
}
