package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.data.pojo.DailyTotal;

import java.util.List;

/**
 * Data Access Object for Expense entity.
 * Provides CRUD operations and aggregate queries for expense data.
 */
@Dao
public interface ExpenseDao {

       // ==================== CRUD Operations ====================

       @Insert(onConflict = OnConflictStrategy.REPLACE)
       long insert(Expense expense);

       @Update
       void update(Expense expense);

       @Delete
       void delete(Expense expense);

       @Query("DELETE FROM expenses WHERE id = :expenseId")
       void deleteById(long expenseId);

       @Query("SELECT * FROM expenses WHERE id = :id")
       Expense getById(long id);

       @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id DESC")
       LiveData<List<Expense>> getAllExpenses();

       @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id DESC")
       List<Expense> getAllExpensesSync();

       @Query("SELECT * FROM expenses WHERE isInvestmentTransfer = 0 ORDER BY dateEpochDay DESC, id DESC")
       LiveData<List<Expense>> getAllConsumptionExpenses();

       @Query("SELECT * FROM expenses WHERE isInvestmentTransfer = 0 ORDER BY dateEpochDay DESC, id DESC")
       List<Expense> getAllConsumptionExpensesSync();

       // ==================== Date-based Queries ====================

       /**
        * Get all expenses for a specific date.
        * 
        * @param dateEpochDay The date as epoch day
        */
       @Query("SELECT * FROM expenses WHERE dateEpochDay = :dateEpochDay ORDER BY id DESC")
       LiveData<List<Expense>> getExpensesForDate(long dateEpochDay);

       /**
        * Get expenses within a date range (inclusive).
        */
       @Query("SELECT * FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay ORDER BY dateEpochDay DESC, id DESC")
       LiveData<List<Expense>> getExpensesForRange(long startDay, long endDay);

       // ==================== Aggregate Queries ====================

       /**
        * Get total spending for a date range.
        * Returns 0 if no expenses exist.
        */
       @Query("SELECT IFNULL(SUM(amountCents), 0) FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay")
       long getTotalForRange(long startDay, long endDay);

       /**
        * Get total spending for a date range as LiveData (for UI observation).
        */
       @Query("SELECT IFNULL(SUM(amountCents), 0) FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay")
       LiveData<Long> getTotalForRangeLive(long startDay, long endDay);

       @Query("SELECT IFNULL(SUM(amountCents), 0) FROM expenses " +
                     "WHERE isInvestmentTransfer = 0 AND dateEpochDay BETWEEN :startDay AND :endDay")
       long getConsumptionTotalForRange(long startDay, long endDay);

       @Query("SELECT IFNULL(SUM(amountCents), 0) FROM expenses " +
                     "WHERE isInvestmentTransfer = 0 AND dateEpochDay BETWEEN :startDay AND :endDay")
       LiveData<Long> getConsumptionTotalForRangeLive(long startDay, long endDay);

       @Query("SELECT IFNULL(SUM(amountCents), 0) FROM expenses " +
                     "WHERE isInvestmentTransfer = 1 AND dateEpochDay BETWEEN :startDay AND :endDay")
       LiveData<Long> getInvestmentTransferTotalForRangeLive(long startDay, long endDay);

       /**
        * Get spending totals grouped by category for a date range.
        * Used for Dashboard pie chart.
        */
       @Query("SELECT c.name AS name, IFNULL(SUM(e.amountCents), 0) AS total, c.colorArgb AS colorArgb " +
                     "FROM expenses e " +
                     "JOIN categories c ON e.categoryId = c.id " +
                     "WHERE e.dateEpochDay BETWEEN :startDay AND :endDay " +
                     "GROUP BY c.id " +
                     "ORDER BY total DESC")
       LiveData<List<CategoryTotal>> getCategoryTotalsForRange(long startDay, long endDay);

       /**
        * Get spending totals grouped by category for a date range (sync version).
        * Used by InsightsEngine on a background thread.
        */
       @Query("SELECT c.name AS name, IFNULL(SUM(e.amountCents), 0) AS total, c.colorArgb AS colorArgb " +
                     "FROM expenses e " +
                     "JOIN categories c ON e.categoryId = c.id " +
                     "WHERE e.dateEpochDay BETWEEN :startDay AND :endDay " +
                     "GROUP BY c.id " +
                     "ORDER BY total DESC")
       List<CategoryTotal> getCategoryTotalsForRangeSync(long startDay, long endDay);

       @Query("SELECT c.name AS name, IFNULL(SUM(e.amountCents), 0) AS total, c.colorArgb AS colorArgb " +
                     "FROM expenses e " +
                     "JOIN categories c ON e.categoryId = c.id " +
                     "WHERE e.isInvestmentTransfer = 0 " +
                     "AND e.dateEpochDay BETWEEN :startDay AND :endDay " +
                     "GROUP BY c.id " +
                     "ORDER BY total DESC")
       LiveData<List<CategoryTotal>> getConsumptionCategoryTotalsForRange(long startDay, long endDay);

       @Query("SELECT c.name AS name, IFNULL(SUM(e.amountCents), 0) AS total, c.colorArgb AS colorArgb " +
                     "FROM expenses e " +
                     "JOIN categories c ON e.categoryId = c.id " +
                     "WHERE e.isInvestmentTransfer = 0 " +
                     "AND e.dateEpochDay BETWEEN :startDay AND :endDay " +
                     "GROUP BY c.id " +
                     "ORDER BY total DESC")
       List<CategoryTotal> getConsumptionCategoryTotalsForRangeSync(long startDay, long endDay);

       /**
        * Get daily totals for a date range.
        * Used for calendar decorators.
        */
       @Query("SELECT dateEpochDay, SUM(amountCents) AS totalCents " +
                     "FROM expenses " +
                     "WHERE dateEpochDay BETWEEN :startDay AND :endDay " +
                     "GROUP BY dateEpochDay")
       LiveData<List<DailyTotal>> getDailyTotalsForRange(long startDay, long endDay);

       @Query("SELECT dateEpochDay, SUM(amountCents) AS totalCents " +
                     "FROM expenses " +
                     "WHERE isInvestmentTransfer = 0 AND dateEpochDay BETWEEN :startDay AND :endDay " +
                     "GROUP BY dateEpochDay")
       LiveData<List<DailyTotal>> getConsumptionDailyTotalsForRange(long startDay, long endDay);

       /**
        * Get total spending for a specific category in a date range.
        * Used for budget progress calculation.
        */
       @Query("SELECT IFNULL(SUM(amountCents), 0) FROM expenses " +
                     "WHERE categoryId = :categoryId AND dateEpochDay BETWEEN :startDay AND :endDay")
       long getTotalForCategoryInRange(long categoryId, long startDay, long endDay);

       // ==================== Category-based Queries ====================

       /**
        * Get expenses for a specific category.
        */
       @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY dateEpochDay DESC, id DESC")
       LiveData<List<Expense>> getExpensesByCategory(long categoryId);

       @Query("SELECT * FROM expenses WHERE dateEpochDay BETWEEN :startDay AND :endDay " +
                     "AND isInvestmentTransfer = 0 " +
                     "AND recoveryStatus != 'CONVERTED' " +
                     "AND recoveryStatus != 'IGNORED' " +
                     "ORDER BY dateEpochDay DESC, id DESC")
       List<Expense> getExpenseCandidatesForInvestmentRecoverySync(long startDay, long endDay);

       @Query("UPDATE expenses SET linkedInvestmentId = :targetInvestmentId WHERE linkedInvestmentId = :sourceInvestmentId")
       void relinkRecoveredInvestment(long sourceInvestmentId, long targetInvestmentId);

       // ==================== Merchant-based Queries ====================

       /**
        * Get expenses for a specific merchant.
        */
       @Query("SELECT * FROM expenses WHERE merchantId = :merchantId ORDER BY dateEpochDay DESC, id DESC LIMIT :limit")
       LiveData<List<Expense>> getRecentExpensesByMerchant(long merchantId, int limit);

       /**
        * Count expenses (for statistics).
        */
       @Query("SELECT COUNT(*) FROM expenses")
       int getExpenseCount();

       /**
        * Check if an expense already exists with the same key fields.
        * Used to avoid duplicate imports from auto transaction reader.
        */
       @Query("SELECT COUNT(*) FROM expenses " +
                     "WHERE dateEpochDay = :dateEpochDay " +
                     "AND amountCents = :amountCents " +
                     "AND categoryId = :categoryId " +
                     "AND IFNULL(description, '') = IFNULL(:description, '')")
       int countExactMatch(long dateEpochDay, long amountCents, long categoryId, String description);

       @Query("SELECT COUNT(*) FROM expenses " +
                     "WHERE dateEpochDay = :dateEpochDay " +
                     "AND amountCents = :amountCents " +
                     "AND IFNULL(description, '') = IFNULL(:description, '')")
       int countByDateAmountDescription(long dateEpochDay, long amountCents, String description);

       @Query("SELECT * FROM expenses " +
                     "WHERE dateEpochDay = :dateEpochDay " +
                     "AND amountCents = :amountCents " +
                     "AND IFNULL(description, '') = IFNULL(:description, '') " +
                     "LIMIT 1")
       Expense getByDateAmountDescription(long dateEpochDay, long amountCents, String description);
}
