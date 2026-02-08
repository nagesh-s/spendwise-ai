package com.spendwise.ai.recurring;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.RecurringExpenseDao;
import com.spendwise.ai.data.entity.RecurringExpense;

import java.util.List;

/**
 * ViewModel for RecurringExpenses management.
 */
public class RecurringExpensesViewModel extends AndroidViewModel {

    private final RecurringExpenseDao dao;
    private final LiveData<List<RecurringExpense>> allRecurringExpenses;

    public RecurringExpensesViewModel(@NonNull Application application) {
        super(application);
        AppDatabase db = AppDatabase.getInstance(application);
        dao = db.recurringExpenseDao();
        allRecurringExpenses = dao.getAllRecurringExpenses();
    }

    public LiveData<List<RecurringExpense>> getAllRecurringExpenses() {
        return allRecurringExpenses;
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
}
