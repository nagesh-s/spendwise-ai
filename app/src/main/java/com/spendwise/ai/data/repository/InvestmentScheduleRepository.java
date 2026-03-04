package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.InvestmentScheduleDao;
import com.spendwise.ai.data.entity.InvestmentSchedule;

import java.util.List;

public class InvestmentScheduleRepository {

    private final InvestmentScheduleDao dao;

    public InvestmentScheduleRepository(Application application) {
        dao = AppDatabase.getInstance(application).investmentScheduleDao();
    }

    public LiveData<List<InvestmentSchedule>> getAllSchedules() {
        return dao.getAllSchedules();
    }

    public void insert(InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(schedule));
    }

    public void update(InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(schedule));
    }

    public void delete(InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(schedule));
    }

    public void setActive(long id, boolean isActive) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.setActive(id, isActive));
    }

    public InvestmentSchedule getById(long id) {
        return dao.getById(id);
    }
}
