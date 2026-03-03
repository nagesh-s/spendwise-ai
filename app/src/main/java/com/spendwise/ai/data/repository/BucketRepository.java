package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.BucketDao;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.BucketCategory;
import com.spendwise.ai.data.pojo.BucketWithTotal;

import java.util.List;

/**
 * Repository for Bucket and BucketCategory operations.
 */
public class BucketRepository {

    private final BucketDao bucketDao;

    public BucketRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        bucketDao = db.bucketDao();
    }

    // --- Bucket operations ---

    public void insert(Bucket bucket) {
        AppDatabase.databaseWriteExecutor.execute(() -> bucketDao.insert(bucket));
    }

    public void update(Bucket bucket) {
        AppDatabase.databaseWriteExecutor.execute(() -> bucketDao.update(bucket));
    }

    public void delete(Bucket bucket) {
        AppDatabase.databaseWriteExecutor.execute(() -> bucketDao.delete(bucket));
    }

    public LiveData<List<Bucket>> getAllBuckets() {
        return bucketDao.getAllBuckets();
    }

    public LiveData<List<BucketWithTotal>> getAllBucketsWithTotals() {
        return bucketDao.getAllBucketsWithTotals();
    }

    public void updateBalance(long bucketId, long balanceCents) {
        AppDatabase.databaseWriteExecutor.execute(() -> bucketDao.updateBalance(bucketId, balanceCents));
    }

    // --- BucketCategory operations ---

    public void addCategoryToBucket(long bucketId, long categoryId) {
        AppDatabase.databaseWriteExecutor
                .execute(() -> bucketDao.insertBucketCategory(new BucketCategory(bucketId, categoryId)));
    }

    public void removeCategoryFromBucket(long bucketId, long categoryId) {
        AppDatabase.databaseWriteExecutor.execute(() -> bucketDao.removeCategoryFromBucket(bucketId, categoryId));
    }

    public LiveData<List<BucketCategory>> getCategoriesForBucket(long bucketId) {
        return bucketDao.getCategoriesForBucket(bucketId);
    }
}
