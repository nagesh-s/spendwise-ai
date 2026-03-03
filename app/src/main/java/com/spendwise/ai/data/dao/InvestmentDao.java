package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Investment;

import java.util.List;

/**
 * Data Access Object for Investment entity.
 */
@Dao
public interface InvestmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Investment investment);

    @Update
    void update(Investment investment);

    @Delete
    void delete(Investment investment);

    @Query("SELECT * FROM investments WHERE bucketId = :bucketId ORDER BY name ASC")
    LiveData<List<Investment>> getInvestmentsForBucket(long bucketId);

    @Query("SELECT * FROM investments WHERE bucketId = :bucketId ORDER BY name ASC")
    List<Investment> getInvestmentsForBucketSync(long bucketId);

    @Query("SELECT * FROM investments ORDER BY lastUpdatedEpochDay DESC")
    LiveData<List<Investment>> getAllInvestments();

    @Query("SELECT * FROM investments ORDER BY lastUpdatedEpochDay DESC")
    List<Investment> getAllInvestmentsSync();

    @Query("SELECT * FROM investments WHERE id = :id")
    Investment getById(long id);

    @Query("SELECT COALESCE(SUM(currentValueCents), 0) FROM investments")
    LiveData<Long> getTotalPortfolioValue();

    @Query("SELECT COALESCE(SUM(investedAmountCents), 0) FROM investments")
    LiveData<Long> getTotalInvestedAmount();

    @Query("SELECT COALESCE(SUM(currentValueCents), 0) FROM investments WHERE bucketId = :bucketId")
    long getTotalValueForBucket(long bucketId);

    @Query("SELECT COALESCE(SUM(investedAmountCents), 0) FROM investments WHERE bucketId = :bucketId")
    long getTotalInvestedForBucket(long bucketId);

    @Query("SELECT * FROM investments WHERE platform = :platform ORDER BY name ASC")
    LiveData<List<Investment>> getByPlatform(String platform);

    @Query("SELECT COUNT(*) FROM investments WHERE bucketId = :bucketId")
    int getInvestmentCountForBucket(long bucketId);

    @Query("SELECT * FROM investments WHERE maturityDateEpochDay IS NOT NULL " +
            "AND maturityDateEpochDay <= :epochDay ORDER BY maturityDateEpochDay ASC")
    List<Investment> getInvestmentsMaturingBefore(long epochDay);
}
