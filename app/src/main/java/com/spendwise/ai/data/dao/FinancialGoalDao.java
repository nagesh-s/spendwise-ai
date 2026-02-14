package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.FinancialGoal;

import java.util.List;

/**
 * Data Access Object for FinancialGoal entity.
 */
@Dao
public interface FinancialGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(FinancialGoal goal);

    @Update
    void update(FinancialGoal goal);

    @Delete
    void delete(FinancialGoal goal);

    @Query("SELECT * FROM financial_goals ORDER BY status ASC, endDateEpochDay ASC")
    LiveData<List<FinancialGoal>> getAllGoals();

    @Query("SELECT * FROM financial_goals WHERE status = :status ORDER BY endDateEpochDay ASC")
    LiveData<List<FinancialGoal>> getGoalsByStatus(String status);

    @Query("SELECT * FROM financial_goals WHERE status = 'ACTIVE' ORDER BY endDateEpochDay ASC")
    LiveData<List<FinancialGoal>> getActiveGoals();

    @Query("SELECT * FROM financial_goals WHERE id = :id")
    FinancialGoal getById(long id);

    @Query("UPDATE financial_goals SET currentAmountCents = :amountCents WHERE id = :goalId")
    void updateProgress(long goalId, long amountCents);

    @Query("UPDATE financial_goals SET status = :status WHERE id = :goalId")
    void updateStatus(long goalId, String status);
}
