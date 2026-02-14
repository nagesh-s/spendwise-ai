package com.spendwise.ai.analytics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.data.pojo.DailyTotal;
import com.spendwise.ai.data.repository.ExpenseRepository;

import org.threeten.bp.LocalDate;

import java.util.List;

/**
 * ViewModel for the Analytics screen.
 * Manages time period selection and provides expense summary data.
 */
public class AnalyticsViewModel extends AndroidViewModel {

    public enum Period {
        WEEK, MONTH, QUARTER, YEAR
    }

    private final ExpenseRepository expenseRepository;
    private final MutableLiveData<Period> selectedPeriod = new MutableLiveData<>(Period.MONTH);

    // Computed date range
    private LiveData<Long> totalSpending;
    private LiveData<List<CategoryTotal>> categoryTotals;
    private LiveData<List<DailyTotal>> dailyTotals;

    public AnalyticsViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        refreshData();
    }

    public MutableLiveData<Period> getSelectedPeriod() {
        return selectedPeriod;
    }

    public void setPeriod(Period period) {
        selectedPeriod.setValue(period);
        refreshData();
    }

    public LiveData<Long> getTotalSpending() {
        return totalSpending;
    }

    public LiveData<List<CategoryTotal>> getCategoryTotals() {
        return categoryTotals;
    }

    public LiveData<List<DailyTotal>> getDailyTotals() {
        return dailyTotals;
    }

    /**
     * Get the start date for the current period.
     */
    public long getStartDay() {
        LocalDate now = LocalDate.now();
        Period period = selectedPeriod.getValue();
        if (period == null)
            period = Period.MONTH;

        switch (period) {
            case WEEK:
                return now.minusDays(7).toEpochDay();
            case QUARTER:
                return now.minusMonths(3).toEpochDay();
            case YEAR:
                return now.minusYears(1).toEpochDay();
            case MONTH:
            default:
                return now.withDayOfMonth(1).toEpochDay();
        }
    }

    public long getEndDay() {
        return LocalDate.now().toEpochDay();
    }

    /**
     * Get number of days in the current period for daily average calculation.
     */
    public int getDaysInPeriod() {
        Period period = selectedPeriod.getValue();
        if (period == null)
            period = Period.MONTH;

        switch (period) {
            case WEEK:
                return 7;
            case QUARTER:
                return 90;
            case YEAR:
                return 365;
            case MONTH:
            default:
                return LocalDate.now().getDayOfMonth();
        }
    }

    private void refreshData() {
        long start = getStartDay();
        long end = getEndDay();
        totalSpending = expenseRepository.getTotalForRangeLive(start, end);
        categoryTotals = expenseRepository.getCategoryTotalsForRange(start, end);
        dailyTotals = expenseRepository.getDailyTotalsForRange(start, end);
    }
}
