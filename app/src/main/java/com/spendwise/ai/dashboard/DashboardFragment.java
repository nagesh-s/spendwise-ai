package com.spendwise.ai.dashboard;

import android.app.Application;
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
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Account;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.data.repository.AccountRepository;
import com.spendwise.ai.data.repository.ExpenseRepository;
import com.spendwise.ai.data.repository.MerchantRepository;
import com.spendwise.ai.databinding.FragmentDashboardBinding;
import com.spendwise.ai.gamification.GamificationEngine;
import com.spendwise.ai.insights.Insight;
import com.spendwise.ai.insights.InsightsAdapter;
import com.spendwise.ai.insights.Prediction;
import com.spendwise.ai.insights.PredictionAdapter;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalTime;
import org.threeten.bp.YearMonth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.Manifest;
import android.widget.Toast;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.provider.Settings;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.spendwise.ai.voice.VoiceExpenseParser;
import com.spendwise.ai.voice.VoiceInputHelper;
import com.spendwise.ai.voice.VoiceUnderstandingEngine;
import com.spendwise.ai.voice.VoiceFallbackExtractorFactory;
import com.spendwise.ai.common.ExpenseEditorDialogFragment;

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

    private VoiceInputHelper voiceInputHelper;
    private final VoiceExpenseParser voiceParser = new VoiceExpenseParser();
    private VoiceUnderstandingEngine voiceUnderstandingEngine;
    private List<String> availableCategories = new ArrayList<>();
    private final Map<String, Long> categoryNameToId = new HashMap<>();
    private ExpenseRepository expenseRepository;
    private MerchantRepository merchantRepository;
    private AccountRepository accountRepository;
    private GamificationEngine gamificationEngine;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startVoiceInput();
                } else {
                    Toast.makeText(getContext(), "Microphone permission needed for voice input", Toast.LENGTH_SHORT)
                            .show();
                }
            });

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
        Application app = requireActivity().getApplication();
        expenseRepository = new ExpenseRepository(app);
        merchantRepository = new MerchantRepository(app);
        accountRepository = new AccountRepository(app);
        gamificationEngine = new GamificationEngine(app);
        voiceUnderstandingEngine = new VoiceUnderstandingEngine(
                voiceParser,
                VoiceFallbackExtractorFactory.create(requireContext()),
                AppDatabase.databaseWriteExecutor);

        voiceInputHelper = new VoiceInputHelper(requireContext());
        setupVoiceListener();

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
            ExpenseEditorDialogFragment.newInstance(null, DateUtils.getTodayEpochDay())
                    .show(getParentFragmentManager(), "add_expense");
        });

        binding.fabVoiceInput.setOnClickListener(v -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startVoiceInput();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (voiceInputHelper != null) {
            voiceInputHelper.destroy();
        }
        binding = null;
    }

    private void setupVoiceListener() {
        voiceInputHelper.setListener(new VoiceInputHelper.VoiceListener() {
            @Override
            public void onVoiceText(String text) {
                processVoiceInput(text);
            }

            @Override
            public void onVoiceError(String error) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                if (isVoiceLanguageSetupError(error)) {
                    promptVoiceLanguageSetup();
                    return;
                }
                if (isVoiceCaptureMismatchError(error)) {
                    openVoiceReviewDialog(0, null, null, null);
                }
            }
        });
    }

    private void startVoiceInput() {
        Toast.makeText(getContext(), "Listening...", Toast.LENGTH_SHORT).show();
        voiceInputHelper.startListening();
    }

    private void processVoiceInput(String text) {
        String spokenText = text == null ? "" : text.trim();
        if (spokenText.isEmpty()) {
            Toast.makeText(getContext(), "Couldn't transcribe speech. Please fill details manually.", Toast.LENGTH_SHORT)
                    .show();
            openVoiceReviewDialog(0, null, null, null);
            return;
        }

        voiceUnderstandingEngine.understandAsync(spokenText, getCategoriesForVoiceInference(), understandingResult -> {
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> handleVoiceUnderstandingResult(spokenText, understandingResult));
        });
    }

    private void handleVoiceUnderstandingResult(String spokenText,
            VoiceUnderstandingEngine.UnderstandingResult understandingResult) {
        VoiceExpenseParser.ParsedExpense parsed = understandingResult.parsedExpense;
        Long categoryId = resolveCategoryId(parsed.category);

        if (parsed.isReadyForAutoSave() && categoryId != null) {
            saveVoiceExpenseDirectly(parsed, categoryId);
            return;
        }

        long amountPrefill = parsed.amountCents > 0 ? parsed.amountCents : 0L;
        String merchantPrefill = parsed.merchant;
        String categoryPrefill = parsed.category;

        String guidance = buildVoiceReviewMessage(parsed);
        if (guidance != null) {
            Toast.makeText(getContext(), guidance, Toast.LENGTH_LONG).show();
        }

        String descriptionPrefill = parsed.description;
        openVoiceReviewDialog(amountPrefill, merchantPrefill, categoryPrefill, descriptionPrefill);
    }

    private List<String> getCategoriesForVoiceInference() {
        if (!availableCategories.isEmpty()) {
            return new ArrayList<>(availableCategories);
        }
        List<String> defaults = new ArrayList<>();
        defaults.add("Food & Dining");
        defaults.add("Transportation");
        defaults.add("Bills & Utilities");
        defaults.add("Shopping");
        defaults.add("Entertainment");
        defaults.add("Health & Fitness");
        defaults.add("Travel");
        defaults.add("Education");
        defaults.add("Personal Care");
        defaults.add("Other");
        return defaults;
    }

    private void saveVoiceExpenseDirectly(VoiceExpenseParser.ParsedExpense parsed, long categoryId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Long merchantId = null;
                if (parsed.merchant != null && !parsed.merchant.trim().isEmpty()) {
                    merchantId = merchantRepository.insertOrGetId(parsed.merchant.trim());
                    if (merchantId != null && merchantId <= 0) {
                        merchantId = null;
                    }
                }

                Expense expense = new Expense(
                        DateUtils.getTodayEpochDay(),
                        parsed.amountCents,
                        parsed.description,
                        categoryId,
                        merchantId);

                Account defaultAccount = accountRepository.getDefaultAccount();
                if (defaultAccount != null) {
                    expense.setAccountId(defaultAccount.getId());
                }

                expenseRepository.insertSync(expense);

                gamificationEngine.addXp(10);
                List<Expense> allExpenses = expenseRepository.getAllExpensesSync();
                boolean earlyBirdEvent = LocalTime.now().getHour() < 8;
                List<String> unlocked = gamificationEngine.checkAchievements(allExpenses, earlyBirdEvent);

                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    String message = "Expense added from voice. +10 XP";
                    if (!unlocked.isEmpty()) {
                        message += " • " + unlocked.size() + " badge(s) unlocked";
                    }
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    viewModel.refreshInsights();
                });
            } catch (Exception e) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Couldn't auto-save. Please review and save.", Toast.LENGTH_LONG)
                            .show();
                    openVoiceReviewDialog(
                            parsed.amountCents > 0 ? parsed.amountCents : 0L,
                            parsed.merchant,
                            parsed.category,
                            parsed.description);
                });
            }
        });
    }

    @Nullable
    private Long resolveCategoryId(@Nullable String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }
        Long directMatch = categoryNameToId.get(categoryName);
        if (directMatch != null) {
            return directMatch;
        }
        for (Map.Entry<String, Long> entry : categoryNameToId.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(categoryName)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    private String buildVoiceReviewMessage(VoiceExpenseParser.ParsedExpense parsed) {
        List<String> missingOrUncertain = new ArrayList<>();
        if (!parsed.amountSure) {
            missingOrUncertain.add("amount");
        }
        if (!parsed.categorySure) {
            missingOrUncertain.add("category");
        }
        if (parsed.merchant != null && !parsed.merchantSure) {
            missingOrUncertain.add("merchant");
        }
        if (missingOrUncertain.isEmpty()) {
            return null;
        }
        return "Please confirm " + android.text.TextUtils.join(", ", missingOrUncertain) + " before saving.";
    }

    private boolean isVoiceCaptureMismatchError(String error) {
        if (error == null) {
            return false;
        }
        return error.startsWith("Couldn't understand")
                || error.startsWith("No speech detected")
                || error.startsWith("Could not start voice capture")
                || error.startsWith("Voice service is busy");
    }

    private boolean isVoiceLanguageSetupError(@Nullable String error) {
        if (error == null) {
            return false;
        }
        return error.startsWith("Speech language data is unavailable")
                || error.startsWith("Selected speech language is not supported");
    }

    private void promptVoiceLanguageSetup() {
        if (!isAdded()) {
            return;
        }
        try {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Voice Setup Needed")
                    .setMessage("Speech language data is missing on this device. Open system voice settings to install language data?")
                    .setNegativeButton("Not now", null)
                    .setPositiveButton("Open Settings", (dialog, which) -> openVoiceSettings())
                    .show();
        } catch (Exception ignored) {
            openVoiceSettings();
        }
    }

    private void openVoiceSettings() {
        if (!isAdded()) {
            return;
        }
        Intent[] candidates = new Intent[] {
                new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
                new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS),
                new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent intent : candidates) {
            try {
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(intent);
                    return;
                }
            } catch (ActivityNotFoundException ignored) {
                // Try next candidate.
            }
        }
    }

    private void openVoiceReviewDialog(long amountCents, @Nullable String merchant, @Nullable String category,
            @Nullable String description) {
        if (!isAdded()) {
            return;
        }
        if (getParentFragmentManager().findFragmentByTag("add_voice_expense") != null) {
            return;
        }
        ExpenseEditorDialogFragment.newInstanceForVoice(
                DateUtils.getTodayEpochDay(),
                amountCents,
                merchant,
                category,
                description).show(getParentFragmentManager(), "add_voice_expense");
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

        binding.btnProfile.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v)
                    .navigate(com.spendwise.ai.R.id.navigation_profile);
        });
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

        // Observe categories for voice parser
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            availableCategories.clear();
            categoryNameToId.clear();
            if (categories != null) {
                for (com.spendwise.ai.data.entity.Category cat : categories) {
                    availableCategories.add(cat.getName());
                    categoryNameToId.put(cat.getName(), cat.getId());
                }
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

}
