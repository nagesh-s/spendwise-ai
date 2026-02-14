package com.spendwise.ai.insights;

import android.content.Context;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.BudgetDao;
import com.spendwise.ai.data.dao.ExpenseDao;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * On-device prediction engine.
 * Calculates end-of-month projections, category forecasts, and budget risk.
 * All logic runs locally — no cloud APIs.
 */
public class PredictionEngine {

    private final ExpenseDao expenseDao;
    private final BudgetDao budgetDao;

    public PredictionEngine(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.expenseDao = db.expenseDao();
        this.budgetDao = db.budgetDao();
    }

    /**
     * Generate all predictions. Must be called from a background thread.
     */
    public List<Prediction> generatePredictions() {
        List<Prediction> predictions = new ArrayList<>();

        try {
            generateMonthEndProjection(predictions);
            generateTopCategoryProjections(predictions);
            generateBudgetRiskPredictions(predictions);
        } catch (Exception e) {
            // Safety — never crash for a prediction failure
        }

        return predictions;
    }

    /**
     * Project total spending for end of current month.
     * Uses daily run-rate × remaining days.
     */
    private void generateMonthEndProjection(List<Prediction> predictions) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        int dayOfMonth = today.getDayOfMonth();
        if (dayOfMonth < 3)
            return; // Need at least a few days of data

        long monthStart = DateUtils.getMonthStartEpochDay(thisMonth);
        long todayEpoch = today.toEpochDay();

        long currentTotal = expenseDao.getTotalForRange(monthStart, todayEpoch);
        if (currentTotal == 0)
            return;

        // Daily run-rate
        double dailyRate = (double) currentTotal / dayOfMonth;
        int daysInMonth = thisMonth.lengthOfMonth();
        long projectedTotal = (long) (dailyRate * daysInMonth);

        // Confidence based on how far into the month we are
        int confidence = Math.min(95, (dayOfMonth * 100) / daysInMonth);

        int daysRemaining = daysInMonth - dayOfMonth;
        long remainingProjected = projectedTotal - currentTotal;

        predictions.add(new Prediction(
                Prediction.PredictionType.MONTH_END_TOTAL,
                "Projected Month-End Total",
                projectedTotal,
                confidence,
                String.format(Locale.getDefault(),
                        "At your current pace, you'll spend about ₹%,.0f by month-end (₹%,.0f more over %d days).",
                        projectedTotal / 100.0, remainingProjected / 100.0, daysRemaining)));
    }

    /**
     * Project top categories' spending for end of month.
     */
    private void generateTopCategoryProjections(List<Prediction> predictions) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        int dayOfMonth = today.getDayOfMonth();
        if (dayOfMonth < 3)
            return;

        long monthStart = DateUtils.getMonthStartEpochDay(thisMonth);
        long todayEpoch = today.toEpochDay();
        int daysInMonth = thisMonth.lengthOfMonth();

        List<CategoryTotal> totals = expenseDao.getCategoryTotalsForRangeSync(monthStart, todayEpoch);
        if (totals == null || totals.isEmpty())
            return;

        // Show projection for top 2 categories only
        int count = Math.min(2, totals.size());
        for (int i = 0; i < count; i++) {
            CategoryTotal cat = totals.get(i);
            double dailyRate = (double) cat.getTotal() / dayOfMonth;
            long projected = (long) (dailyRate * daysInMonth);
            int confidence = Math.min(90, (dayOfMonth * 100) / daysInMonth);

            predictions.add(new Prediction(
                    Prediction.PredictionType.CATEGORY_PROJECTION,
                    cat.getName() + " Forecast",
                    projected,
                    confidence,
                    String.format(Locale.getDefault(),
                            "%s is on track for ₹%,.0f this month.",
                            cat.getName(), projected / 100.0)));
        }
    }

    /**
     * Predict which budgets are at risk of being exceeded.
     */
    private void generateBudgetRiskPredictions(List<Prediction> predictions) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();

        int dayOfMonth = today.getDayOfMonth();
        if (dayOfMonth < 3)
            return;

        String monthKey = DateUtils.getMonthKey(thisMonth);
        long monthStart = DateUtils.getMonthStartEpochDay(thisMonth);
        long monthEnd = DateUtils.getMonthEndEpochDay(thisMonth);
        int daysInMonth = thisMonth.lengthOfMonth();

        List<BudgetWithSpend> budgets = budgetDao.getBudgetsWithSpendSync(monthKey, monthStart, monthEnd);
        if (budgets == null)
            return;

        for (BudgetWithSpend budget : budgets) {
            if (budget.getLimitCents() <= 0)
                continue;

            // Project category spending to month-end
            double dailyRate = (double) budget.getSpentCents() / dayOfMonth;
            long projectedSpend = (long) (dailyRate * daysInMonth);

            if (projectedSpend > budget.getLimitCents() && budget.getSpentCents() < budget.getLimitCents()) {
                // Not yet over, but projected to go over
                long overBy = projectedSpend - budget.getLimitCents();
                int confidence = Math.min(85, (dayOfMonth * 100) / daysInMonth);

                predictions.add(new Prediction(
                        Prediction.PredictionType.BUDGET_RISK,
                        "⚠️ " + budget.getCategoryName() + " Budget Risk",
                        projectedSpend,
                        confidence,
                        String.format(Locale.getDefault(),
                                "At current pace, %s will exceed budget by ₹%,.0f.",
                                budget.getCategoryName(), overBy / 100.0)));
            }
        }
    }
}
