package com.spendwise.ai.analytics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.spendwise.ai.R;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.data.pojo.DailyTotal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for the Advanced Analytics dashboard.
 * Displays spending trends, category breakdowns, and summary statistics.
 */
public class AnalyticsFragment extends Fragment {

    private AnalyticsViewModel viewModel;
    private LineChart trendChart;
    private PieChart categoryChart;
    private TextView totalSpendingText;
    private TextView dailyAverageText;
    private LinearLayout topCategoriesLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);

        // Bind views
        trendChart = view.findViewById(R.id.chart_trend);
        categoryChart = view.findViewById(R.id.chart_category);
        totalSpendingText = view.findViewById(R.id.text_total_spending);
        dailyAverageText = view.findViewById(R.id.text_daily_average);
        topCategoriesLayout = view.findViewById(R.id.layout_top_categories);

        setupCharts();
        setupPeriodSelector(view);
        observeData();
    }

    private void setupCharts() {
        // Line chart styling
        trendChart.getDescription().setEnabled(false);
        trendChart.setTouchEnabled(true);
        trendChart.setDragEnabled(true);
        trendChart.setScaleEnabled(false);
        trendChart.getLegend().setEnabled(false);
        trendChart.getAxisRight().setEnabled(false);
        trendChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        trendChart.getXAxis().setDrawGridLines(false);
        trendChart.setNoDataText("No spending data");

        // Pie chart styling
        categoryChart.getDescription().setEnabled(false);
        categoryChart.setHoleRadius(50f);
        categoryChart.setTransparentCircleRadius(55f);
        categoryChart.setDrawEntryLabels(false);
        categoryChart.setNoDataText("No category data");
        categoryChart.getLegend().setWordWrapEnabled(true);
    }

    private void setupPeriodSelector(View view) {
        view.findViewById(R.id.chip_week).setOnClickListener(v -> viewModel.setPeriod(AnalyticsViewModel.Period.WEEK));
        view.findViewById(R.id.chip_month)
                .setOnClickListener(v -> viewModel.setPeriod(AnalyticsViewModel.Period.MONTH));
        view.findViewById(R.id.chip_quarter)
                .setOnClickListener(v -> viewModel.setPeriod(AnalyticsViewModel.Period.QUARTER));
        view.findViewById(R.id.chip_year).setOnClickListener(v -> viewModel.setPeriod(AnalyticsViewModel.Period.YEAR));
    }

    private void observeData() {
        // Total spending
        viewModel.getTotalSpending().observe(getViewLifecycleOwner(), total -> {
            if (total != null) {
                totalSpendingText.setText(String.format(Locale.getDefault(), "₹%,.2f", total / 100.0));
                int days = viewModel.getDaysInPeriod();
                double dailyAvg = (total / 100.0) / Math.max(1, days);
                dailyAverageText.setText(String.format(Locale.getDefault(), "Daily avg: ₹%,.2f", dailyAvg));
            } else {
                totalSpendingText.setText("₹0.00");
                dailyAverageText.setText("Daily avg: ₹0.00");
            }
        });

        // Daily totals for trend chart
        viewModel.getDailyTotals().observe(getViewLifecycleOwner(), dailyTotals -> {
            if (dailyTotals != null && !dailyTotals.isEmpty()) {
                renderTrendChart(dailyTotals);
            }
        });

        // Category totals for pie chart
        viewModel.getCategoryTotals().observe(getViewLifecycleOwner(), categoryTotals -> {
            if (categoryTotals != null && !categoryTotals.isEmpty()) {
                renderCategoryChart(categoryTotals);
                renderTopCategories(categoryTotals);
            }
        });
    }

    private void renderTrendChart(List<DailyTotal> dailyTotals) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dailyTotals.size(); i++) {
            DailyTotal dt = dailyTotals.get(i);
            entries.add(new Entry(i, dt.getTotalCents() / 100f));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Spending");
        dataSet.setColor(Color.parseColor("#00897B"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(3f);
        dataSet.setCircleColor(Color.parseColor("#00897B"));
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#4D00897B"));
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        trendChart.setData(lineData);
        trendChart.invalidate();
    }

    private void renderCategoryChart(List<CategoryTotal> categoryTotals) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int[] palette = {
                Color.parseColor("#00897B"), Color.parseColor("#FF6F61"),
                Color.parseColor("#1A237E"), Color.parseColor("#4CAF50"),
                Color.parseColor("#FF9800"), Color.parseColor("#9C27B0"),
                Color.parseColor("#03A9F4"), Color.parseColor("#E91E63")
        };

        for (int i = 0; i < categoryTotals.size(); i++) {
            CategoryTotal ct = categoryTotals.get(i);
            entries.add(new PieEntry(ct.getTotal(), ct.getName() != null ? ct.getName() : "Unknown"));
            colors.add(palette[i % palette.length]);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setSliceSpace(2f);

        PieData pieData = new PieData(dataSet);
        categoryChart.setData(pieData);
        categoryChart.invalidate();
    }

    private void renderTopCategories(List<CategoryTotal> categoryTotals) {
        topCategoriesLayout.removeAllViews();

        // Calculate total for percentage
        long totalCents = 0;
        for (CategoryTotal ct : categoryTotals) {
            totalCents += ct.getTotal();
        }

        // Show top 5 categories
        int limit = Math.min(5, categoryTotals.size());
        for (int i = 0; i < limit; i++) {
            CategoryTotal ct = categoryTotals.get(i);
            double pct = totalCents > 0 ? (ct.getTotal() * 100.0 / totalCents) : 0;

            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 8, 0, 8);

            TextView nameText = new TextView(requireContext());
            nameText.setText(ct.getName() != null ? ct.getName() : "Unknown");
            nameText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView amountText = new TextView(requireContext());
            amountText.setText(String.format(Locale.getDefault(), "₹%,.2f (%.0f%%)", ct.getTotal() / 100.0, pct));

            row.addView(nameText);
            row.addView(amountText);
            topCategoriesLayout.addView(row);
        }
    }
}
