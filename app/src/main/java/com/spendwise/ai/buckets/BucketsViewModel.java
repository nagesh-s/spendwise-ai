package com.spendwise.ai.buckets;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.pojo.BucketWithTotal;
import com.spendwise.ai.data.repository.BucketRepository;
import com.spendwise.ai.data.repository.InvestmentRepository;
import com.spendwise.ai.engine.InvestmentDeduplicationEngine;

import java.util.List;

/**
 * ViewModel for the main Buckets dashboard screen.
 */
public class BucketsViewModel extends AndroidViewModel {

    private final BucketRepository bucketRepository;
    private final InvestmentRepository investmentRepository;
    private final LiveData<List<BucketWithTotal>> bucketsWithTotals;
    private final LiveData<Long> totalPortfolioValue;
    private final MutableLiveData<InvestmentDeduplicationEngine.RepairResult> repairResult = new MutableLiveData<>();

    public BucketsViewModel(@NonNull Application application) {
        super(application);
        bucketRepository = new BucketRepository(application);
        investmentRepository = new InvestmentRepository(application);
        bucketsWithTotals = bucketRepository.getAllBucketsWithTotals();
        totalPortfolioValue = investmentRepository.getTotalPortfolioValue();
        repairHoldings();
    }

    public LiveData<List<BucketWithTotal>> getBucketsWithTotals() {
        return bucketsWithTotals;
    }

    public LiveData<Long> getTotalPortfolioValue() {
        return totalPortfolioValue;
    }

    public LiveData<InvestmentDeduplicationEngine.RepairResult> getRepairResult() {
        return repairResult;
    }

    public void repairHoldings() {
        Application application = getApplication();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            InvestmentDeduplicationEngine.RepairResult result = new InvestmentDeduplicationEngine()
                    .repair(AppDatabase.getInstance(application));
            repairResult.postValue(result);
        });
    }

    public void addBucket(Bucket bucket) {
        bucketRepository.insert(bucket);
    }

    public void updateBucket(Bucket bucket) {
        bucketRepository.update(bucket);
    }

    public void deleteBucket(Bucket bucket) {
        bucketRepository.delete(bucket);
    }

    public void updateBalance(long bucketId, long balanceCents) {
        bucketRepository.updateBalance(bucketId, balanceCents);
    }
}
