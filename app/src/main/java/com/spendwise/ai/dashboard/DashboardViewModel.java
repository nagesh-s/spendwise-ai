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
import com.spendwise.ai.insights.Insight;
import com.spendwise.ai.insights.InsightsEngine;
import com.spendwise.ai.insights.Prediction;
import com.spendwise.ai.insights.PredictionEngine;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.YearMonth;
import java.util.List;

/**
 * ViewModel for Dashboard screen.
 * Manages current month selection and provides data for charts and totals.
 */
public class DashboardViewModel extends AndroidViewModel {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final com.spendwise.ai.data.repository.CategoryRepository categoryRepository;

    // Current selected month
    private final MutableLiveData<YearMonth> currentMonth = new MutableLiveData<>();

    // Derived data based on current month
    private final LiveData<Long> monthlyTotal;
    private final LiveData<List<CategoryTotal>> categoryTotals;
    private final LiveData<List<BudgetWithSpend>> budgetsWithSpend;

    // Today and this week totals (always current)
    private final LiveData<Long> todayTotal;
    private final LiveData<Long> weekTotal;

    // AI Insights & Predictions
    private final MutableLiveData<List<Insight>> insights = new MutableLiveData<>();
    private final MutableLiveData<List<Prediction>> predictions = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        expenseRepository = new ExpenseRepository(application);
        budgetRepository = new BudgetRepository(application);
        categoryRepository = new com.spendwise.ai.data.repository.CategoryRepository(application);

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

        // Today's total
        long today = org.threeten.bp.LocalDate.now().toEpochDay();
        todayTotal = expenseRepository.getTotalForRangeLive(today, today);

        // This week's total (Monday to today)
        org.threeten.bp.LocalDate now = org.threeten.bp.LocalDate.now();
        // Find Monday of current week using threeten.bp
        org.threeten.bp.LocalDate weekStart = now
                .with(org.threeten.bp.temporal.TemporalAdjusters.previousOrSame(org.threeten.bp.DayOfWeek.MONDAY));
        weekTotal = expenseRepository.getTotalForRangeLive(weekStart.toEpochDay(), today);

        // Generate AI insights and predictions on background thread
        refreshInsights();
    }

    /**
     * Regenerate AI insights and predictions from current data.
     */
    public void refreshInsights() {
        com.spendwise.ai.data.AppDatabase.databaseWriteExecutor.execute(() -> {
            // Insights
            InsightsEngine insightsEngine = new InsightsEngine(getApplication());
            List<Insight> generatedInsights = insightsEngine.generateInsights();
            insights.postValue(generatedInsights);

            // Predictions
            PredictionEngine predictionEngine = new PredictionEngine(getApplication());
            List<Prediction> generatedPredictions = predictionEngine.generatePredictions();
            predictions.postValue(generatedPredictions);
        });
    }

    public LiveData<List<Insight>> getInsights() {
        return insights;
    }

    public LiveData<List<Prediction>> getPredictions() {
        return predictions;
    }

    public LiveData<List<com.spendwise.ai.data.entity.Category>> getAllCategories() {
        return categoryRepository.getAllCategories();
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

    public LiveData<Long> getTodayTotal() {
        return todayTotal;
    }

    public LiveData<Long> getWeekTotal() {
        return weekTotal;
    }

    /**
     * Get formatted display string for current month.
     */
    public String getCurrentMonthDisplay() {
        YearMonth month = currentMonth.getValue();
        return month != null ? DateUtils.getDisplayMonth(month) : "";
    }
}
