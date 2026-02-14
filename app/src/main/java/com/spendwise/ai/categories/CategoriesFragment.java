package com.spendwise.ai.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.color.MaterialColors;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.databinding.DialogAddCategoryBinding;
import com.spendwise.ai.databinding.FragmentCategoriesBinding;
import com.spendwise.ai.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for managing categories and viewing/setting budgets.
 */
public class CategoriesFragment extends Fragment {

    private FragmentCategoriesBinding binding;
    private CategoriesViewModel viewModel;
    private CategoryBudgetAdapter adapter;
    private List<Category> latestCategories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CategoriesViewModel.class);

        setupRecyclerView();
        setupMonthNavigation();
        setupBudgetActions();
        setupFab();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new CategoryBudgetAdapter();
        adapter.setOnBudgetClickListener((category, currentBudget) -> {
            long currentBudgetCents = currentBudget != null ? currentBudget.getLimitCents() : 0;
            BudgetEditorDialog dialog = BudgetEditorDialog.newInstance(
                    category.getId(),
                    category.getName(),
                    currentBudgetCents
            );
            dialog.setOnBudgetSavedListener(new BudgetEditorDialog.OnBudgetSavedListener() {
                @Override
                public void onBudgetSaved(long categoryId, long amountCents) {
                    viewModel.setBudget(categoryId, amountCents);
                }

                @Override
                public void onBudgetRemoved(long categoryId) {
                    viewModel.setBudget(categoryId, 0);
                }
            });
            dialog.show(getChildFragmentManager(), "budget_editor");
        });
        binding.recyclerCategories.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerCategories.setAdapter(adapter);
    }

    private void setupMonthNavigation() {
        binding.btnPrevMonth.setOnClickListener(v -> viewModel.goToPreviousMonth());
        binding.btnNextMonth.setOnClickListener(v -> viewModel.goToNextMonth());
    }

    private void setupFab() {
        binding.fabAddCategory.setOnClickListener(v -> showAddCategoryDialog());
    }

    private void setupBudgetActions() {
        binding.btnCopyPreviousBudgets.setOnClickListener(v -> {
            viewModel.copyBudgetsFromPreviousMonth();
            Toast.makeText(requireContext(), R.string.copy_previous_month_hint, Toast.LENGTH_SHORT).show();
        });
    }

    private void observeData() {
        // Observe current budget month
        viewModel.getBudgetMonth().observe(getViewLifecycleOwner(), month -> {
            binding.textBudgetMonth.setText(DateUtils.getDisplayMonth(month) + " Budgets");
        });

        // Observe categories
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            latestCategories = categories != null ? new ArrayList<>(categories) : new ArrayList<>();
            adapter.setCategories(latestCategories);
        });

        // Observe budgets with spend
        viewModel.getBudgetsWithSpend().observe(getViewLifecycleOwner(), budgets -> {
            adapter.setBudgets(budgets);
            updateBudgetSummary(budgets);
        });
    }

    private void updateBudgetSummary(List<BudgetWithSpend> budgets) {
        if (budgets == null || budgets.isEmpty()) {
            binding.textBudgetSummaryAmount.setText(getString(R.string.no_budgets_set));
            binding.textBudgetSummaryMeta.setText(getString(R.string.no_budgets_summary_meta));
            binding.progressBudgetSummary.setProgress(0);
            return;
        }

        long totalLimit = 0;
        long totalSpent = 0;
        int overBudgetCount = 0;
        for (BudgetWithSpend budget : budgets) {
            totalLimit += budget.getLimitCents();
            totalSpent += budget.getSpentCents();
            if (budget.isOverBudget()) {
                overBudgetCount++;
            }
        }

        int usagePercent = totalLimit > 0 ? (int) ((totalSpent * 100) / totalLimit) : 0;
        binding.progressBudgetSummary.setProgress(Math.min(usagePercent, 100));
        binding.progressBudgetSummary.setIndicatorColor(getProgressColor(usagePercent));

        String summary = getString(R.string.budget_usage_format,
                DateUtils.formatAmountCents(totalSpent),
                DateUtils.formatAmountCents(totalLimit));
        binding.textBudgetSummaryAmount.setText(summary);

        String meta = getResources().getQuantityString(
                R.plurals.over_budget_categories_count,
                overBudgetCount,
                overBudgetCount);
        binding.textBudgetSummaryMeta.setText(meta);
    }

    private int getProgressColor(int usagePercent) {
        if (usagePercent >= 100) {
            return MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorError, 0);
        }
        if (usagePercent >= 80) {
            return MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorSecondary, 0);
        }
        return MaterialColors.getColor(requireContext(), com.google.android.material.R.attr.colorPrimary, 0);
    }

    private void showAddCategoryDialog() {
        DialogAddCategoryBinding dialogBinding = DialogAddCategoryBinding.inflate(getLayoutInflater());

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.add_category)
                .setView(dialogBinding.getRoot())
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String name = "";
                    if (dialogBinding.editCategoryName.getText() != null) {
                        name = dialogBinding.editCategoryName.getText().toString().trim();
                    }

                    if (name.isEmpty()) {
                        dialogBinding.layoutCategoryName.setError(getString(R.string.error_category_name_required));
                        return;
                    }

                    if (isCategoryNameTaken(name)) {
                        dialogBinding.layoutCategoryName.setError(getString(R.string.error_category_exists));
                        return;
                    }

                    dialogBinding.layoutCategoryName.setError(null);
                    viewModel.addCategory(name, pickCategoryColor(name), "fa_tag");
                    Toast.makeText(requireContext(),
                            getString(R.string.category_added, name),
                            Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));

        dialog.show();
    }

    private boolean isCategoryNameTaken(String candidate) {
        for (Category category : latestCategories) {
            if (category.getName().equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private int pickCategoryColor(String categoryName) {
        int[] palette = new int[] {
                ContextCompat.getColor(requireContext(), R.color.category_food),
                ContextCompat.getColor(requireContext(), R.color.category_transport),
                ContextCompat.getColor(requireContext(), R.color.category_shopping),
                ContextCompat.getColor(requireContext(), R.color.category_entertainment),
                ContextCompat.getColor(requireContext(), R.color.category_bills),
                ContextCompat.getColor(requireContext(), R.color.category_health),
                ContextCompat.getColor(requireContext(), R.color.category_travel),
                ContextCompat.getColor(requireContext(), R.color.category_education),
                ContextCompat.getColor(requireContext(), R.color.category_personal),
                ContextCompat.getColor(requireContext(), R.color.category_other)
        };
        int hash = categoryName.toLowerCase(Locale.US).hashCode() & Integer.MAX_VALUE;
        return palette[hash % palette.length];
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
