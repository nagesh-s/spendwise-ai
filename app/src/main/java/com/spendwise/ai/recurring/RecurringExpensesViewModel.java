package com.spendwise.ai.recurring;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.BucketDao;
import com.spendwise.ai.data.dao.InvestmentScheduleDao;
import com.spendwise.ai.data.dao.RecurringExpenseDao;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.InvestmentSchedule;
import com.spendwise.ai.data.entity.RecurringExpense;
import com.spendwise.ai.engine.InvestmentScheduleEngine;

import org.threeten.bp.LocalDate;

import java.util.List;

/**
 * ViewModel for RecurringExpenses management.
 */
public class RecurringExpensesViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final RecurringExpenseDao dao;
    private final InvestmentScheduleDao investmentScheduleDao;
    private final BucketDao bucketDao;
    private final LiveData<List<RecurringExpense>> allRecurringExpenses;
    private final LiveData<List<InvestmentSchedule>> allInvestmentSchedules;

    public RecurringExpensesViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        dao = db.recurringExpenseDao();
        investmentScheduleDao = db.investmentScheduleDao();
        bucketDao = db.bucketDao();
        allRecurringExpenses = dao.getAllRecurringExpenses();
        allInvestmentSchedules = investmentScheduleDao.getAllSchedules();
    }

    public LiveData<List<RecurringExpense>> getAllRecurringExpenses() {
        return allRecurringExpenses;
    }

    public LiveData<List<InvestmentSchedule>> getAllInvestmentSchedules() {
        return allInvestmentSchedules;
    }

    public void insert(RecurringExpense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.insert(expense));
    }

    public void update(RecurringExpense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.update(expense));
    }

    public void delete(RecurringExpense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.delete(expense));
    }

    public void toggleActive(RecurringExpense expense) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.setActive(expense.getId(), !expense.isActive()));
    }

    public RecurringExpense getById(long id) {
        return dao.getById(id);
    }

    public void insertInvestmentSchedule(@NonNull InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long id = investmentScheduleDao.insert(schedule);
            schedule.setId(id);
            if (schedule.isActive()) {
                new InvestmentScheduleEngine().generateDueOccurrencesForSchedule(
                        db,
                        schedule,
                        LocalDate.now().toEpochDay());
            }
        });
    }

    public void updateInvestmentSchedule(@NonNull InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            investmentScheduleDao.update(schedule);
            if (schedule.isActive()) {
                new InvestmentScheduleEngine().generateDueOccurrencesForSchedule(
                        db,
                        schedule,
                        LocalDate.now().toEpochDay());
            }
        });
    }

    public void deleteInvestmentSchedule(@NonNull InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentScheduleDao.delete(schedule));
    }

    public void toggleInvestmentScheduleActive(@NonNull InvestmentSchedule schedule) {
        AppDatabase.databaseWriteExecutor.execute(() -> investmentScheduleDao
                .setActive(schedule.getId(), !schedule.isActive()));
    }

    public InvestmentSchedule getInvestmentScheduleById(long id) {
        return investmentScheduleDao.getById(id);
    }

    public List<Bucket> getAllBucketsSync() {
        return bucketDao.getAllBucketsSync();
    }
}
