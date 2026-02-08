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
import com.spendwise.ai.util.DateUtils;

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

        // Configure Pie Chart
        PieChart pieChart = binding.pieChartCategories;
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setDrawCenterText(false);
        pieChart.setRotationAngle(0f);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);
        pieChart.setEntryLabelTextSize(12f);
        pieChart.setEntryLabelColor(Color.WHITE);

        Legend pieLegend = pieChart.getLegend();
        pieLegend.setEnabled(true);
        pieLegend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        pieLegend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        pieLegend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        pieLegend.setDrawInside(false);
        pieLegend.setWordWrapEnabled(true);
        pieLegend.setTextColor(textColor);

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

    private void setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
    }

    private void observeData() {
        // Observe current month
        viewModel.getCurrentMonth().observe(getViewLifecycleOwner(), month -> {
            binding.textCurrentMonth.setText(DateUtils.getDisplayMonth(month));
        });

        // Observe monthly total
        viewModel.getMonthlyTotal().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                binding.textMonthlyTotal.setText(DateUtils.formatAmountCents(total));
            } else {
                binding.textMonthlyTotal.setText(DateUtils.formatAmountCents(0));
            }
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
        viewModel.getBudgetsWithSpend().observe(getViewLifecycleOwner(), this::updateBudgetChart);
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

        PieDataSet dataSet = new PieDataSet(entries, "Categories");
        dataSet.setColors(colors);
        dataSet.setSliceSpace(2f);
        dataSet.setValueLinePart1OffsetPercentage(80f);
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.BLACK);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChartCategories));

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
