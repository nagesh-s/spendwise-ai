package com.spendwise.ai.calendar;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.pojo.DailyTotal;
import com.spendwise.ai.data.repository.ExpenseRepository;
import com.spendwise.ai.util.DateUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * ViewModel for Calendar screen.
 * Manages selected date and provides daily expenses and monthly totals for decorators.
 */
public class CalendarViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;

    // Currently displayed month (for loading daily totals)
    private final MutableLiveData<YearMonth> displayedMonth = new MutableLiveData<>();

    // Selected date (for showing expenses list)
    private final MutableLiveData<LocalDate> selectedDate = new MutableLiveData<>();

    // Daily totals for calendar decorators
    private final LiveData<List<DailyTotal>> dailyTotals;

    // Expenses for selected date
    private final LiveData<List<Expense>> selectedDateExpenses;

    public CalendarViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);

        // Initialize with current month and today
        displayedMonth.setValue(YearMonth.now());
        selectedDate.setValue(LocalDate.now());

        // Daily totals update when displayed month changes
        dailyTotals = Transformations.switchMap(displayedMonth, month -> {
            long startDay = DateUtils.getMonthStartEpochDay(month);
            long endDay = DateUtils.getMonthEndEpochDay(month);
            return expenseRepository.getDailyTotalsForRange(startDay, endDay);
        });

        // Expenses update when selected date changes
        selectedDateExpenses = Transformations.switchMap(selectedDate, date -> {
            long epochDay = date.toEpochDay();
            return expenseRepository.getExpensesForDate(epochDay);
        });
    }

    // ==================== Month Navigation ====================

    public LiveData<YearMonth> getDisplayedMonth() {
        return displayedMonth;
    }

    public void setDisplayedMonth(YearMonth month) {
        displayedMonth.setValue(month);
    }

    // ==================== Date Selection ====================

    public LiveData<LocalDate> getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate date) {
        selectedDate.setValue(date);
    }

    public void selectToday() {
        selectedDate.setValue(LocalDate.now());
    }

    // ==================== Data Accessors ====================

    public LiveData<List<DailyTotal>> getDailyTotals() {
        return dailyTotals;
    }

    public LiveData<List<Expense>> getSelectedDateExpenses() {
        return selectedDateExpenses;
    }

    /**
     * Get epoch day for the currently selected date.
     */
    public long getSelectedDateEpochDay() {
        LocalDate date = selectedDate.getValue();
        return date != null ? date.toEpochDay() : DateUtils.getTodayEpochDay();
    }
}
