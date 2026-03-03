package com.spendwise.ai.buckets;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.entity.BucketCategory;
import com.spendwise.ai.data.entity.Investment;
import com.spendwise.ai.data.repository.BucketRepository;
import com.spendwise.ai.data.repository.InvestmentRepository;

import java.util.List;

/**
 * ViewModel for the Bucket Detail drill-down screen.
 */
public class BucketDetailViewModel extends AndroidViewModel {

    private final BucketRepository bucketRepository;
    private final InvestmentRepository investmentRepository;

    public BucketDetailViewModel(@NonNull Application application) {
        super(application);
        bucketRepository = new BucketRepository(application);
        investmentRepository = new InvestmentRepository(application);
    }

    public LiveData<List<Investment>> getInvestmentsForBucket(long bucketId) {
        return investmentRepository.getInvestmentsForBucket(bucketId);
    }

    public LiveData<List<BucketCategory>> getCategoriesForBucket(long bucketId) {
        return bucketRepository.getCategoriesForBucket(bucketId);
    }

    public void addInvestment(Investment investment) {
        investmentRepository.insert(investment);
    }

    public void updateInvestment(Investment investment) {
        investmentRepository.update(investment);
    }

    public void deleteInvestment(Investment investment) {
        investmentRepository.delete(investment);
    }

    public void addCategoryToBucket(long bucketId, long categoryId) {
        bucketRepository.addCategoryToBucket(bucketId, categoryId);
    }

    public void removeCategoryFromBucket(long bucketId, long categoryId) {
        bucketRepository.removeCategoryFromBucket(bucketId, categoryId);
    }
}
