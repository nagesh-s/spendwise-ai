package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.Budget;
import com.spendwise.ai.data.pojo.BudgetWithSpend;

import java.util.List;

/**
 * Data Access Object for Budget entity.
 * Provides CRUD operations and budget progress queries.
 */
@Dao
public interface BudgetDao {

    // ==================== CRUD Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Budget budget);

    @Update
    void update(Budget budget);

    @Delete
    void delete(Budget budget);

    @Query("DELETE FROM budgets WHERE id = :budgetId")
    void deleteById(long budgetId);

    // ==================== Query Operations ====================

    @Query("SELECT * FROM budgets WHERE id = :id")
    Budget getById(long id);

    @Query("SELECT * FROM budgets WHERE monthKeyYYYYMM = :monthKey ORDER BY categoryId")
    LiveData<List<Budget>> getBudgetsForMonth(String monthKey);

    @Query("SELECT * FROM budgets WHERE monthKeyYYYYMM = :monthKey ORDER BY categoryId")
    List<Budget> getBudgetsForMonthSync(String monthKey);

    @Query("SELECT * FROM budgets WHERE monthKeyYYYYMM = :monthKey AND categoryId = :categoryId LIMIT 1")
    Budget getBudgetForCategoryAndMonth(long categoryId, String monthKey);

    /**
     * Get budgets with actual spending for a month.
     * This query joins budgets with expenses to calculate current spend.
     * 
     * @param monthKey The month in YYYYMM format
     * @param startDay First day of the month as epoch day
     * @param endDay Last day of the month as epoch day
     */
    @Query("SELECT b.id AS budgetId, " +
           "c.name AS categoryName, " +
           "c.colorArgb AS categoryColorArgb, " +
           "b.limitCents AS limitCents, " +
           "IFNULL(SUM(e.amountCents), 0) AS spentCents " +
           "FROM budgets b " +
           "JOIN categories c ON b.categoryId = c.id " +
           "LEFT JOIN expenses e ON e.categoryId = b.categoryId " +
           "    AND e.dateEpochDay BETWEEN :startDay AND :endDay " +
           "WHERE b.monthKeyYYYYMM = :monthKey " +
           "GROUP BY b.id " +
           "ORDER BY c.name ASC")
    LiveData<List<BudgetWithSpend>> getBudgetsWithSpend(String monthKey, long startDay, long endDay);

    /**
     * Copy budgets from one month to another.
     * Useful for carrying forward budget limits to a new month.
     */
    @Query("INSERT INTO budgets (monthKeyYYYYMM, categoryId, limitCents) " +
           "SELECT :newMonthKey, categoryId, limitCents " +
           "FROM budgets WHERE monthKeyYYYYMM = :sourceMonthKey")
    void copyBudgetsToNewMonth(String sourceMonthKey, String newMonthKey);

    /**
     * Check if budgets exist for a month.
     */
    @Query("SELECT COUNT(*) FROM budgets WHERE monthKeyYYYYMM = :monthKey")
    int countBudgetsForMonth(String monthKey);
}
