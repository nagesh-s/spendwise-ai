package com.spendwise.ai.dashboard;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.data.repository.BudgetRepository;
import com.spendwise.ai.data.repository.ExpenseRepository;
import com.spendwise.ai.util.DateUtils;

import java.time.YearMonth;
import java.util.List;

/**
 * ViewModel for Dashboard screen.
 * Manages current month selection and provides data for charts and totals.
 */
public class DashboardViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    // Current selected month
    private final MutableLiveData<YearMonth> currentMonth = new MutableLiveData<>();

    // Derived data based on current month
    private final LiveData<Long> monthlyTotal;
    private final LiveData<List<CategoryTotal>> categoryTotals;
    private final LiveData<List<BudgetWithSpend>> budgetsWithSpend;

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        budgetRepository = new BudgetRepository(application);

        // Initialize with current month
        currentMonth.setValue(YearMonth.now());

        // Monthly total - updates when month changes
        monthlyTotal = Transformations.switchMap(currentMonth, month -> {
            long startDay = DateUtils.getMonthStartEpochDay(month);
            long endDay = DateUtils.getMonthEndEpochDay(month);
            return expenseRepository.getTotalForRangeLive(startDay, endDay);
        });

        // Category totals for pie chart - updates when month changes
        categoryTotals = Transformations.switchMap(currentMonth, month -> {
            long startDay = DateUtils.getMonthStartEpochDay(month);
            long endDay = DateUtils.getMonthEndEpochDay(month);
            return expenseRepository.getCategoryTotalsForRange(startDay, endDay);
        });

        // Budget progress - updates when month changes
        budgetsWithSpend = Transformations.switchMap(currentMonth, month -> {
            String monthKey = DateUtils.getMonthKey(month);
            long startDay = DateUtils.getMonthStartEpochDay(month);
            long endDay = DateUtils.getMonthEndEpochDay(month);
            return budgetRepository.getBudgetsWithSpend(monthKey, startDay, endDay);
        });
    }

    // ==================== Month Navigation ====================

    public LiveData<YearMonth> getCurrentMonth() {
        return currentMonth;
    }

    public void goToPreviousMonth() {
        YearMonth current = currentMonth.getValue();
        if (current != null) {
            currentMonth.setValue(current.minusMonths(1));
        }
    }

    public void goToNextMonth() {
        YearMonth current = currentMonth.getValue();
        if (current != null) {
            currentMonth.setValue(current.plusMonths(1));
        }
    }

    public void goToCurrentMonth() {
        currentMonth.setValue(YearMonth.now());
    }

    // ==================== Data Accessors ====================

    public LiveData<Long> getMonthlyTotal() {
        return monthlyTotal;
    }

    public LiveData<List<CategoryTotal>> getCategoryTotals() {
        return categoryTotals;
    }

    public LiveData<List<BudgetWithSpend>> getBudgetsWithSpend() {
        return budgetsWithSpend;
    }

    /**
     * Get formatted display string for current month.
     */
    public String getCurrentMonthDisplay() {
        YearMonth month = currentMonth.getValue();
        return month != null ? DateUtils.getDisplayMonth(month) : "";
    }
}
