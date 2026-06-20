package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.BucketCategory;
import com.spendwise.ai.data.pojo.BucketWithTotal;

import java.util.List;

/**
 * Data Access Object for Bucket and BucketCategory entities.
 */
@Dao
public interface BucketDao {

    // --- Bucket CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Bucket bucket);

    @Update
    void update(Bucket bucket);

    @Delete
    void delete(Bucket bucket);

    @Query("SELECT * FROM buckets ORDER BY sortOrder ASC")
    LiveData<List<Bucket>> getAllBuckets();

    @Query("SELECT * FROM buckets ORDER BY sortOrder ASC")
    List<Bucket> getAllBucketsSync();

    @Query("SELECT * FROM buckets WHERE id = :id")
    Bucket getById(long id);

    @Query("SELECT * FROM buckets WHERE type = :type LIMIT 1")
    Bucket getByType(String type);

    /**
     * Get all buckets with their total investment values via LEFT JOIN.
     * Each BucketWithTotal includes the bucket and aggregate investment totals.
     */
    @Query("SELECT b.*, " +
            "COALESCE(SUM(i.currentValueCents), 0) AS totalValueCents, " +
            "COALESCE(SUM(i.netCashInvestedCents), 0) AS totalInvestedCents " +
            "FROM buckets b LEFT JOIN holdings i ON i.bucketId = b.id " +
            "GROUP BY b.id ORDER BY b.sortOrder ASC")
    LiveData<List<BucketWithTotal>> getAllBucketsWithTotals();

    /**
     * Returns the sum of all bucket target percentages.
     * Should equal 100 when properly configured.
     */
    @Query("SELECT COALESCE(SUM(targetPercent), 0) FROM buckets")
    int getTotalAllocatedPercent();

    @Query("SELECT COUNT(*) FROM buckets")
    int getBucketCount();

    @Query("UPDATE buckets SET currentBalanceCents = :balanceCents WHERE id = :bucketId")
    void updateBalance(long bucketId, long balanceCents);

    // --- BucketCategory CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBucketCategory(BucketCategory bc);

    @Delete
    void deleteBucketCategory(BucketCategory bc);

    @Query("SELECT * FROM bucket_categories WHERE bucketId = :bucketId")
    LiveData<List<BucketCategory>> getCategoriesForBucket(long bucketId);

    @Query("SELECT * FROM bucket_categories WHERE bucketId = :bucketId")
    List<BucketCategory> getCategoriesForBucketSync(long bucketId);

    @Query("DELETE FROM bucket_categories WHERE bucketId = :bucketId AND categoryId = :categoryId")
    void removeCategoryFromBucket(long bucketId, long categoryId);

    @Query("DELETE FROM bucket_categories WHERE bucketId = :bucketId")
    void removeAllCategoriesFromBucket(long bucketId);
}
