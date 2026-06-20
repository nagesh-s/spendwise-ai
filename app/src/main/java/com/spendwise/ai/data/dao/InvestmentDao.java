package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Holding;

import java.util.List;

/**
 * Data Access Object for Holding entity.
 */
@Dao
public interface InvestmentDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Holding investment);

    @Update
    void update(Holding investment);

    @Delete
    void delete(Holding investment);

    @Query("DELETE FROM holdings WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM holdings WHERE bucketId = :bucketId ORDER BY name ASC")
    LiveData<List<Holding>> getInvestmentsForBucket(long bucketId);

    @Query("SELECT * FROM holdings WHERE bucketId = :bucketId ORDER BY name ASC")
    List<Holding> getInvestmentsForBucketSync(long bucketId);

    @androidx.room.Transaction
    @Query("SELECT * FROM holdings WHERE bucketId = :bucketId ORDER BY providerId ASC, name ASC")
    LiveData<List<com.spendwise.ai.data.pojo.HoldingWithProvider>> getHoldingsWithProviderForBucket(long bucketId);

    @Query("SELECT * FROM holdings ORDER BY lastUpdatedEpochDay DESC")
    LiveData<List<Holding>> getAllInvestments();

    @Query("SELECT * FROM holdings ORDER BY lastUpdatedEpochDay DESC")
    List<Holding> getAllInvestmentsSync();

    @Query("SELECT * FROM holdings WHERE id = :id")
    Holding getById(long id);

    @Query("SELECT h.* FROM holdings h "
            + "JOIN investment_event_links l ON l.holdingId = h.id "
            + "JOIN ingestion_events e ON e.id = l.ingestionEventId "
            + "WHERE e.sourceKey = :sourceKey LIMIT 1")
    Holding getByLastSourceKey(String sourceKey);

    @Query("SELECT * FROM holdings WHERE primaryCorrelationKey = :correlationKey LIMIT 1")
    Holding getByPrimaryCorrelationKey(String correlationKey);

    @Query("SELECT * FROM holdings WHERE canonicalInstrumentKey = :canonicalInstrumentKey LIMIT 1")
    Holding getByCanonicalInstrumentKey(String canonicalInstrumentKey);

    @Query("SELECT COALESCE(SUM(currentValueCents), 0) FROM holdings")
    LiveData<Long> getTotalPortfolioValue();

    @Query("SELECT COALESCE(SUM(netCashInvestedCents), 0) FROM holdings")
    LiveData<Long> getTotalInvestedAmount();

    @Query("SELECT COALESCE(SUM(currentValueCents), 0) FROM holdings WHERE bucketId = :bucketId")
    long getTotalValueForBucket(long bucketId);

    @Query("SELECT COALESCE(SUM(netCashInvestedCents), 0) FROM holdings WHERE bucketId = :bucketId")
    long getTotalInvestedForBucket(long bucketId);

    @Query("SELECT * FROM holdings WHERE platform = :platform ORDER BY name ASC")
    LiveData<List<Holding>> getByPlatform(String platform);

    @Query("SELECT COUNT(*) FROM holdings WHERE bucketId = :bucketId")
    int getInvestmentCountForBucket(long bucketId);

    @Query("SELECT * FROM holdings WHERE maturityDateEpochDay IS NOT NULL " +
            "AND maturityDateEpochDay <= :epochDay ORDER BY maturityDateEpochDay ASC")
    List<Holding> getInvestmentsMaturingBefore(long epochDay);
}
