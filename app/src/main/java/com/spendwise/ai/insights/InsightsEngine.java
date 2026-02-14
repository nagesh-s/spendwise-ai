package com.spendwise.ai.insights;

import android.content.Context;

import com.spendwise.ai.R;
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
 * On-device AI insights engine.
 * Scans expense and budget data to generate actionable insight cards.
 * All logic runs locally using SQL aggregations — no cloud APIs.
 */
public class InsightsEngine {

    private final ExpenseDao expenseDao;
    private final BudgetDao budgetDao;

    public InsightsEngine(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.expenseDao = db.expenseDao();
        this.budgetDao = db.budgetDao();
    }

    /**
     * Generate all applicable insights based on current data.
     * This must be called from a background thread.
     */
    public List<Insight> generateInsights() {
        List<Insight> insights = new ArrayList<>();

        try {
            generateMonthComparisonInsight(insights);
            generateTopCategoryInsight(insights);
            generateBudgetAlerts(insights);
            generateSpendingSpikeInsights(insights);
            generateStreakInsight(insights);
        } catch (Exception e) {
            // Safety — never crash the Dashboard for an insight failure
        }

        return insights;
    }

    /**
     * MONTH_COMPARISON: "You've spent 20% less than last month so far"
     */
    private void generateMonthComparisonInsight(List<Insight> insights) {
        LocalDate today = LocalDate.now();
        YearMonth thisMonth = YearMonth.now();
        YearMonth lastMonth = thisMonth.minusMonths(1);

        long thisMonthStart = DateUtils.getMonthStartEpochDay(thisMonth);
        long todayEpoch = today.toEpochDay();

        // Current month-to-date total
        long currentTotal = expenseDao.getTotalForRange(thisMonthStart, todayEpoch);

        // Same number of days into last month
        int dayOfMonth = today.getDayOfMonth();
        long lastMonthStart = DateUtils.getMonthStartEpochDay(lastMonth);
        long lastMonthSameDay = lastMonthStart + dayOfMonth - 1;
        long lastTotal = expenseDao.getTotalForRange(lastMonthStart, lastMonthSameDay);

        if (lastTotal <= 0)
            return; // no data to compare

        double changePercent = ((double) (currentTotal - lastTotal) / lastTotal) * 100;
        int absChange = (int) Math.abs(changePercent);

        if (absChange < 5)
            return; // not significant enough

        if (changePercent < 0) {
            // Spending less — positive!
            insights.add(new Insight(
                    InsightType.MONTH_COMPARISON,
                    "Spending Down " + absChange + "% 📉",
                    String.format(Locale.getDefault(),
                            "You've spent %d%% less than the same point last month. Keep it up!",
                            absChange),
                    Insight.Severity.POSITIVE,
                    R.drawable.ic_analytics,
                    null));
        } else if (changePercent > 15) {
            // Spending significantly more — warning
            insights.add(new Insight(
                    InsightType.MONTH_COMPARISON,
                    "Spending Up " + absChange + "%",
                    String.format(Locale.getDefault(),
                            "You've spent %d%% more than the same point last month.",
                            absChange),
                    Insight.Severity.WARNING,
                    R.drawable.ic_analytics,
                    "View Analytics"));
        }
    }

    /**
     * TOP_CATEGORY: "Food & Dining is your #1 expense (₹4,200 this month)"
     */
    private void generateTopCategoryInsight(List<Insight> insights) {
        YearMonth thisMonth = YearMonth.now();
        long start = DateUtils.getMonthStartEpochDay(thisMonth);
        long end = LocalDate.now().toEpochDay();

        List<CategoryTotal> totals = expenseDao.getCategoryTotalsForRangeSync(start, end);
        if (totals == null || totals.isEmpty())
            return;

        CategoryTotal top = totals.get(0);
        long totalAll = 0;
        for (CategoryTotal ct : totals)
            totalAll += ct.getTotal();

        if (totalAll == 0)
            return;

        int percentage = (int) ((top.getTotal() * 100) / totalAll);

        insights.add(new Insight(
                InsightType.TOP_CATEGORY,
                top.getName() + " is #1",
                String.format(Locale.getDefault(),
                        "%s accounts for %d%% of your spending this month (₹%,.0f).",
                        top.getName(), percentage, top.getTotal() / 100.0),
                Insight.Severity.INFO,
                R.drawable.ic_category_black_24dp,
                null));
    }

    /**
     * BUDGET_ALERT: "Shopping is at 85% of budget with 12 days left"
     */
    private void generateBudgetAlerts(List<Insight> insights) {
        YearMonth thisMonth = YearMonth.now();
        String monthKey = DateUtils.getMonthKey(thisMonth);
        long start = DateUtils.getMonthStartEpochDay(thisMonth);
        long end = DateUtils.getMonthEndEpochDay(thisMonth);

        List<BudgetWithSpend> budgets = budgetDao.getBudgetsWithSpendSync(monthKey, start, end);
        if (budgets == null)
            return;

        LocalDate today = LocalDate.now();
        int daysLeft = thisMonth.lengthOfMonth() - today.getDayOfMonth();
        double elapsedFraction = (double) today.getDayOfMonth() / thisMonth.lengthOfMonth();

        for (BudgetWithSpend budget : budgets) {
            if (budget.getLimitCents() <= 0)
                continue;

            double usageFraction = (double) budget.getSpentCents() / budget.getLimitCents();

            if (usageFraction >= 1.0) {
                // Already exceeded
                long overBy = budget.getSpentCents() - budget.getLimitCents();
                insights.add(new Insight(
                        InsightType.BUDGET_ALERT,
                        budget.getCategoryName() + " Budget Exceeded!",
                        String.format(Locale.getDefault(),
                                "You're ₹%,.0f over your %s budget.",
                                overBy / 100.0, budget.getCategoryName()),
                        Insight.Severity.WARNING,
                        R.drawable.ic_currency,
                        "View Goals"));
            } else if (usageFraction >= 0.80 && usageFraction > elapsedFraction) {
                // At risk — spending faster than time elapsed
                int usagePct = (int) (usageFraction * 100);
                insights.add(new Insight(
                        InsightType.BUDGET_ALERT,
                        budget.getCategoryName() + " at " + usagePct + "%",
                        String.format(Locale.getDefault(),
                                "%s budget is %d%% used with %d days remaining.",
                                budget.getCategoryName(), usagePct, daysLeft),
                        Insight.Severity.WARNING,
                        R.drawable.ic_currency,
                        "View Goals"));
            }
        }
    }

    /**
     * SPENDING_SPIKE: "You spent 45% more on Food this week vs your average"
     */
    private void generateSpendingSpikeInsights(List<Insight> insights) {
        LocalDate today = LocalDate.now();
        long weekStart = today.minusDays(6).toEpochDay();
        long todayEpoch = today.toEpochDay();

        // Get this week's total
        long thisWeekTotal = expenseDao.getTotalForRange(weekStart, todayEpoch);
        if (thisWeekTotal == 0)
            return;

        // Get 4-week average (week-2 through week-5 ago)
        long fourWeekStart = today.minusDays(34).toEpochDay(); // 5 weeks ago
        long fourWeekEnd = today.minusDays(7).toEpochDay(); // 1 week ago
        long fourWeekTotal = expenseDao.getTotalForRange(fourWeekStart, fourWeekEnd);

        long weeklyAvg = fourWeekTotal / 4;
        if (weeklyAvg <= 0)
            return;

        double changePercent = ((double) (thisWeekTotal - weeklyAvg) / weeklyAvg) * 100;
        int absChange = (int) Math.abs(changePercent);

        if (changePercent > 30) {
            insights.add(new Insight(
                    InsightType.SPENDING_SPIKE,
                    "Weekly Spike: +" + absChange + "%",
                    String.format(Locale.getDefault(),
                            "This week's spending is %d%% above your 4-week average.",
                            absChange),
                    Insight.Severity.WARNING,
                    R.drawable.ic_analytics,
                    "View Analytics"));
        } else if (changePercent < -20) {
            insights.add(new Insight(
                    InsightType.SPENDING_SPIKE,
                    "Spending Down " + absChange + "% This Week 🎉",
                    String.format(Locale.getDefault(),
                            "You spent %d%% less than your 4-week average this week!",
                            absChange),
                    Insight.Severity.POSITIVE,
                    R.drawable.ic_analytics,
                    null));
        }
    }

    /**
     * STREAK: "🔥 3 days with no spending — keep it up!"
     */
    private void generateStreakInsight(List<Insight> insights) {
        LocalDate today = LocalDate.now();
        int streakDays = 0;

        // Walk backwards from yesterday — today might not be over yet
        for (int i = 1; i <= 30; i++) {
            long day = today.minusDays(i).toEpochDay();
            long total = expenseDao.getTotalForRange(day, day);
            if (total == 0) {
                streakDays++;
            } else {
                break;
            }
        }

        if (streakDays >= 2) {
            insights.add(new Insight(
                    InsightType.STREAK,
                    "🔥 " + streakDays + "-Day No-Spend Streak!",
                    String.format(Locale.getDefault(),
                            "You haven't spent anything in %d days. Great discipline!",
                            streakDays),
                    Insight.Severity.POSITIVE,
                    R.drawable.ic_goals,
                    null));
        }
    }
}
