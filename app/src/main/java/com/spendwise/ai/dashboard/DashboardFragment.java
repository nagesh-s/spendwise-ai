package com.spendwise.ai.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.databinding.FragmentDashboardBinding;
import com.spendwise.ai.insights.Insight;
import com.spendwise.ai.insights.InsightsAdapter;
import com.spendwise.ai.insights.Prediction;
import com.spendwise.ai.insights.PredictionAdapter;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;

import java.util.ArrayList;
import java.util.List;

/**
 * Dashboard fragment showing monthly overview with charts and category
 * breakdown.
 */
public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private DashboardViewModel viewModel;
    private CategoryTotalAdapter categoryAdapter;
    private InsightsAdapter insightsAdapter;
    private PredictionAdapter predictionAdapter;
    private YearMonth currentMonth = YearMonth.now();
    private long monthlyTotalCents = 0L;
    private List<BudgetWithSpend> latestBudgets = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        setupCharts();
        setupRecyclerView();
        setupInsights();
        setupPredictions();
        setupMonthNavigation();
        setupFab();
        observeData();
    }

    private void setupFab() {
        binding.fabAddExpense.setOnClickListener(v -> {
            // Navigate to add expense dialog
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.expenseEditorDialogFragment);
        });

        // Animate FAB entrance
        android.view.animation.Animation fabAnim = android.view.animation.AnimationUtils.loadAnimation(
                requireContext(), com.spendwise.ai.R.anim.fab_pop_in);
        fabAnim.setStartOffset(500);
        binding.fabAddExpense.startAnimation(fabAnim);

        // Animate cards with stagger
        animateCard(binding.cardMonthlyTotal, 0);
        animateCard(binding.cardBudgetHealth, 120);
    }

    private void animateCard(View card, int delay) {
        android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(
                requireContext(), com.spendwise.ai.R.anim.card_slide_up);
        anim.setStartOffset(delay);
        card.startAnimation(anim);
    }

    private void setupCharts() {
        // Get theme-aware text color
        int textColor = com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurface,
                Color.BLACK);
        int textSecondaryColor = com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                Color.DKGRAY);

        // Configure Pie Chart
        PieChart pieChart = binding.pieChartCategories;
        pieChart.setUsePercentValues(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setDrawCenterText(false);
        pieChart.setRotationAngle(0f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setDrawEntryLabels(false);
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setEntryLabelColor(textSecondaryColor);
        pieChart.setMinAngleForSlices(12f);
        pieChart.setExtraBottomOffset(8f);

        Legend pieLegend = pieChart.getLegend();
        pieLegend.setEnabled(true);
        pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieLegend.setDrawInside(false);
        pieLegend.setWordWrapEnabled(true);
        pieLegend.setTextColor(textColor);
        pieLegend.setTextSize(11f);

        // Configure Bar Chart
        HorizontalBarChart barChart = binding.barChartBudgets;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.setMaxVisibleValueCount(10);
        barChart.setPinchZoom(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setFitBars(true);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(textColor);

        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setEnabled(false);

        YAxis rightAxis = barChart.getAxisRight();
        rightAxis.setEnabled(false);

        Legend barLegend = barChart.getLegend();
        barLegend.setEnabled(false);
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryTotalAdapter();
        binding.recyclerTopCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTopCategories.setAdapter(categoryAdapter);
    }

    private void setupInsights() {
        insightsAdapter = new InsightsAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerInsights.setLayoutManager(layoutManager);
        binding.recyclerInsights.setAdapter(insightsAdapter);

        insightsAdapter.setActionListener(new InsightsAdapter.OnInsightActionListener() {
            @Override
            public void onActionClicked(Insight insight) {
                String action = insight.getActionLabel();
                if (action == null)
                    return;
                View v = binding.getRoot();
                if (action.equals("View Analytics")) {
                    androidx.navigation.Navigation.findNavController(v)
                            .navigate(com.spendwise.ai.R.id.navigation_analytics);
                } else if (action.equals("View Goals")) {
                    androidx.navigation.Navigation.findNavController(v)
                            .navigate(com.spendwise.ai.R.id.navigation_goals);
                }
            }

            @Override
            public void onDismissed(Insight insight, int position) {
                // Card already removed by adapter
                if (insightsAdapter.getItemCount() == 0) {
                    binding.insightsSection.setVisibility(View.GONE);
                }
            }
        });
    }

    private void setupPredictions() {
        predictionAdapter = new PredictionAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false);
        binding.recyclerPredictions.setLayoutManager(layoutManager);
        binding.recyclerPredictions.setAdapter(predictionAdapter);
    }

    private void setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
    }

    private void observeData() {
        // Observe current month
        viewModel.getCurrentMonth().observe(getViewLifecycleOwner(), month -> {
            currentMonth = month;
            binding.textCurrentMonth.setText(DateUtils.getDisplayMonth(month));
            updateSpendingPace();
        });

        // Observe monthly total
        viewModel.getMonthlyTotal().observe(getViewLifecycleOwner(), total -> {
            monthlyTotalCents = total != null ? total : 0L;
            binding.textMonthlyTotal.setText(DateUtils.formatAmountCents(monthlyTotalCents));
            updateSpendingPace();
            updateBudgetHealth();
        });

        // Observe today's total
        viewModel.getTodayTotal().observe(getViewLifecycleOwner(), total -> {
            binding.textTodayTotal.setText(DateUtils.formatAmountCents(total != null ? total : 0));
        });

        // Observe this week's total
        viewModel.getWeekTotal().observe(getViewLifecycleOwner(), total -> {
            binding.textWeekTotal.setText(DateUtils.formatAmountCents(total != null ? total : 0));
        });

        // Observe category totals for pie chart and list
        viewModel.getCategoryTotals().observe(getViewLifecycleOwner(), this::updateCategoryData);

        // Observe budget progress
        viewModel.getBudgetsWithSpend().observe(getViewLifecycleOwner(), budgets -> {
            latestBudgets = budgets != null ? budgets : new ArrayList<>();
            updateBudgetChart(budgets);
            updateBudgetHealth();
        });

        // Observe AI insights
        viewModel.getInsights().observe(getViewLifecycleOwner(), insights -> {
            if (insights != null && !insights.isEmpty()) {
                insightsAdapter.setInsights(insights);
                binding.insightsSection.setVisibility(View.VISIBLE);
            } else {
                binding.insightsSection.setVisibility(View.GONE);
            }
        });

        // Observe Smart Predictions
        viewModel.getPredictions().observe(getViewLifecycleOwner(), predictions -> {
            if (predictions != null && !predictions.isEmpty()) {
                predictionAdapter.setPredictions(predictions);
                binding.predictionsSection.setVisibility(View.VISIBLE);
            } else {
                binding.predictionsSection.setVisibility(View.GONE);
            }
        });
    }

    private void updateCategoryData(List<CategoryTotal> categoryTotals) {
        if (categoryTotals == null || categoryTotals.isEmpty()) {
            // Show empty states
            binding.pieChartCategories.setVisibility(View.GONE);
            binding.textPieChartEmpty.setVisibility(View.VISIBLE);
            binding.recyclerTopCategories.setVisibility(View.GONE);
            binding.textCategoriesEmpty.setVisibility(View.VISIBLE);
            return;
        }

        // Show charts
        binding.pieChartCategories.setVisibility(View.VISIBLE);
        binding.textPieChartEmpty.setVisibility(View.GONE);
        binding.recyclerTopCategories.setVisibility(View.VISIBLE);
        binding.textCategoriesEmpty.setVisibility(View.GONE);

        // Update pie chart
        updatePieChart(categoryTotals);

        // Update list (top 5)
        List<CategoryTotal> top5 = categoryTotals.size() > 5
                ? categoryTotals.subList(0, 5)
                : categoryTotals;
        categoryAdapter.submitList(new ArrayList<>(top5));
    }

    private void updatePieChart(List<CategoryTotal> categoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (CategoryTotal category : categoryTotals) {
            entries.add(new PieEntry(category.getTotal(), category.getName()));
            colors.add(category.getColorArgb());
        }

        PieDataSet dataSet = new PieDataSet(entries, getString(com.spendwise.ai.R.string.category));
        dataSet.setColors(colors);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(6f);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.TRANSPARENT);

        PieData data = new PieData(dataSet);
        data.setDrawValues(false);

        binding.pieChartCategories.setData(data);
        binding.pieChartCategories.animateY(1000, Easing.EaseInOutQuad);
        binding.pieChartCategories.invalidate();
    }

    private void updateBudgetChart(List<BudgetWithSpend> budgets) {
        if (budgets == null || budgets.isEmpty()) {
            binding.barChartBudgets.setVisibility(View.GONE);
            binding.textBudgetChartEmpty.setVisibility(View.VISIBLE);
            return;
        }

        binding.barChartBudgets.setVisibility(View.VISIBLE);
        binding.textBudgetChartEmpty.setVisibility(View.GONE);

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int colorGreen = Color.parseColor("#4CAF50");
        int colorOrange = Color.parseColor("#FF9800");
        int colorRed = Color.parseColor("#F44336");

        for (int i = 0; i < budgets.size(); i++) {
            BudgetWithSpend budget = budgets.get(i);
            float usagePercent = budget.getUsagePercent();
            entries.add(new BarEntry(i, Math.min(usagePercent, 120f))); // Cap at 120%
            labels.add(budget.getCategoryName());

            // Color based on usage
            if (usagePercent >= 100) {
                colors.add(colorRed);
            } else if (usagePercent >= 80) {
                colors.add(colorOrange);
            } else {
                colors.add(colorGreen);
            }
        }

        BarDataSet dataSet = new BarDataSet(entries, "Budget Usage");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new PercentFormatter());

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.6f);

        binding.barChartBudgets.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.barChartBudgets.getXAxis().setLabelCount(labels.size());
        binding.barChartBudgets.setData(data);
        binding.barChartBudgets.animateY(800);
        binding.barChartBudgets.invalidate();
    }

    private void updateSpendingPace() {
        int elapsedDays = getElapsedDays(currentMonth);
        int totalDays = currentMonth.lengthOfMonth();
        long dailyAverage = elapsedDays > 0 ? (monthlyTotalCents / elapsedDays) : 0L;

        long projectedTotal;
        if (currentMonth.equals(YearMonth.now())) {
            projectedTotal = dailyAverage * totalDays;
        } else {
            projectedTotal = monthlyTotalCents;
        }

        binding.textDailyAverage.setText(DateUtils.formatAmountCents(dailyAverage));
        binding.textProjectedTotal.setText(DateUtils.formatAmountCents(projectedTotal));
    }

    private int getElapsedDays(YearMonth month) {
        if (!month.equals(YearMonth.now())) {
            return month.lengthOfMonth();
        }
        return LocalDate.now().getDayOfMonth();
    }

    private void updateBudgetHealth() {
        if (latestBudgets == null || latestBudgets.isEmpty()) {
            binding.textBudgetHealthPrimary.setText(getString(com.spendwise.ai.R.string.no_budgets_set));
            binding.textBudgetHealthMeta.setText(getString(com.spendwise.ai.R.string.no_budgets_summary_meta));
            binding.progressBudgetHealth.setProgress(0);
            return;
        }

        long totalLimit = 0L;
        long totalSpent = 0L;
        int overBudgetCount = 0;
        for (BudgetWithSpend budget : latestBudgets) {
            totalLimit += budget.getLimitCents();
            totalSpent += budget.getSpentCents();
            if (budget.isOverBudget()) {
                overBudgetCount++;
            }
        }

        int usagePercent = totalLimit > 0 ? (int) ((totalSpent * 100) / totalLimit) : 0;
        binding.progressBudgetHealth.setProgress(Math.min(usagePercent, 100));
        binding.progressBudgetHealth.setIndicatorColor(getBudgetIndicatorColor(usagePercent));

        String primaryText = getString(
                com.spendwise.ai.R.string.budget_usage_format,
                DateUtils.formatAmountCents(totalSpent),
                DateUtils.formatAmountCents(totalLimit));
        binding.textBudgetHealthPrimary.setText(primaryText);

        long remaining = totalLimit - totalSpent;
        String remainingText = remaining >= 0
                ? getString(com.spendwise.ai.R.string.remaining_budget_format, DateUtils.formatAmountCents(remaining))
                : getString(com.spendwise.ai.R.string.over_budget_amount_format,
                        DateUtils.formatAmountCents(Math.abs(remaining)));
        String overBudgetText = getResources().getQuantityString(
                com.spendwise.ai.R.plurals.over_budget_categories_count,
                overBudgetCount,
                overBudgetCount);
        binding.textBudgetHealthMeta.setText(remainingText + " • " + overBudgetText);
    }

    private int getBudgetIndicatorColor(int usagePercent) {
        if (usagePercent >= 100) {
            return com.google.android.material.color.MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorError,
                    Color.RED);
        }
        if (usagePercent >= 80) {
            return com.google.android.material.color.MaterialColors.getColor(
                    requireContext(),
                    com.google.android.material.R.attr.colorSecondary,
                    Color.YELLOW);
        }
        return com.google.android.material.color.MaterialColors.getColor(
                requireContext(),
                com.google.android.material.R.attr.colorPrimary,
                Color.GREEN);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
