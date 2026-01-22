package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.data.pojo.MerchantAgg;

import java.util.List;

/**
 * Data Access Object for Merchant entity.
 * Provides CRUD operations and analytics queries for merchants.
 */
@Dao
public interface MerchantDao {

    // ==================== CRUD Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Merchant merchant);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIfNotExists(Merchant merchant);

    @Update
    void update(Merchant merchant);

    @Delete
    void delete(Merchant merchant);

    @Query("DELETE FROM merchants WHERE id = :merchantId")
    void deleteById(long merchantId);

    // ==================== Query Operations ====================

    @Query("SELECT * FROM merchants WHERE id = :id")
    Merchant getById(long id);

    @Query("SELECT * FROM merchants ORDER BY name ASC")
    LiveData<List<Merchant>> getAllMerchants();

    @Query("SELECT * FROM merchants ORDER BY name ASC")
    List<Merchant> getAllMerchantsSync();

    /**
     * Search merchants by name (for autocomplete).
     */
    @Query("SELECT * FROM merchants WHERE name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT :limit")
    List<Merchant> searchByName(String query, int limit);

    /**
     * Find a merchant by exact name (case-insensitive).
     */
    @Query("SELECT * FROM merchants WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    Merchant findByName(String name);

    // ==================== Analytics Queries ====================

    /**
     * Get top merchants by total spending for a date range.
     * Returns merchant name, transaction count, total, and average.
     */
    @Query("SELECT m.name AS name, " +
           "COUNT(e.id) AS transactionCount, " +
           "IFNULL(SUM(e.amountCents), 0) AS totalCents, " +
           "IFNULL(AVG(e.amountCents), 0) AS avgCents " +
           "FROM merchants m " +
           "JOIN expenses e ON e.merchantId = m.id " +
           "WHERE e.dateEpochDay BETWEEN :startDay AND :endDay " +
           "GROUP BY m.id " +
           "ORDER BY totalCents DESC " +
           "LIMIT :limit")
    LiveData<List<MerchantAgg>> getTopMerchants(long startDay, long endDay, int limit);

    /**
     * Get analytics for a specific merchant.
     */
    @Query("SELECT m.name AS name, " +
           "COUNT(e.id) AS transactionCount, " +
           "IFNULL(SUM(e.amountCents), 0) AS totalCents, " +
           "IFNULL(AVG(e.amountCents), 0) AS avgCents " +
           "FROM merchants m " +
           "LEFT JOIN expenses e ON e.merchantId = m.id " +
           "WHERE m.id = :merchantId " +
           "GROUP BY m.id")
    MerchantAgg getMerchantStats(long merchantId);

    /**
     * Get the count of all merchants.
     */
    @Query("SELECT COUNT(*) FROM merchants")
    int getMerchantCount();
}
