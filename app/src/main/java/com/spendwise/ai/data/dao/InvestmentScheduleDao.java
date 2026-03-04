package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.InvestmentSchedule;

import java.util.List;

@Dao
public interface InvestmentScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(InvestmentSchedule schedule);

    @Update
    void update(InvestmentSchedule schedule);

    @Delete
    void delete(InvestmentSchedule schedule);

    @Query("SELECT * FROM investment_schedules ORDER BY isActive DESC, id DESC")
    LiveData<List<InvestmentSchedule>> getAllSchedules();

    @Query("SELECT * FROM investment_schedules ORDER BY isActive DESC, id DESC")
    List<InvestmentSchedule> getAllSchedulesSync();

    @Query("SELECT * FROM investment_schedules WHERE isActive = 1 ORDER BY id ASC")
    List<InvestmentSchedule> getActiveSchedulesSync();

    @Query("SELECT * FROM investment_schedules WHERE id = :id")
    InvestmentSchedule getById(long id);

    @Query("UPDATE investment_schedules SET isActive = :isActive WHERE id = :id")
    void setActive(long id, boolean isActive);
}
