package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.RecurringExpense;

import java.util.List;

/**
 * Data Access Object for RecurringExpense entity.
 */
@Dao
public interface RecurringExpenseDao {

    // ==================== CRUD Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecurringExpense recurringExpense);

    @Update
    void update(RecurringExpense recurringExpense);

    @Delete
    void delete(RecurringExpense recurringExpense);

    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    void deleteById(long id);

    // ==================== Query Operations ====================

    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    RecurringExpense getById(long id);

    @Query("SELECT * FROM recurring_expenses ORDER BY description ASC")
    LiveData<List<RecurringExpense>> getAllRecurringExpenses();

    @Query("SELECT * FROM recurring_expenses ORDER BY description ASC")
    List<RecurringExpense> getAllRecurringExpensesSync();

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY description ASC")
    LiveData<List<RecurringExpense>> getActiveRecurringExpenses();

    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY description ASC")
    List<RecurringExpense> getActiveRecurringExpensesSync();

    /**
     * Get recurring expenses that are due for generation.
     * These are active expenses where lastGeneratedEpochDay is before today.
     */
    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND lastGeneratedEpochDay < :todayEpochDay")
    List<RecurringExpense> getDueRecurringExpenses(long todayEpochDay);

    /**
     * Get recurring expenses with reminders due today.
     * These are active expenses where the next due date minus reminder days equals
     * today.
     */
    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 AND reminderDaysBefore > 0")
    List<RecurringExpense> getRecurringExpensesWithReminders();

    /**
     * Get count of all recurring expenses.
     */
    @Query("SELECT COUNT(*) FROM recurring_expenses")
    int getRecurringExpenseCount();

    /**
     * Get recurring expenses by category.
     */
    @Query("SELECT * FROM recurring_expenses WHERE categoryId = :categoryId ORDER BY description ASC")
    LiveData<List<RecurringExpense>> getRecurringExpensesByCategory(long categoryId);

    /**
     * Update the last generated date for a recurring expense.
     */
    @Query("UPDATE recurring_expenses SET lastGeneratedEpochDay = :epochDay WHERE id = :id")
    void updateLastGeneratedDate(long id, long epochDay);

    /**
     * Toggle active status.
     */
    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    void setActive(long id, boolean isActive);
}
