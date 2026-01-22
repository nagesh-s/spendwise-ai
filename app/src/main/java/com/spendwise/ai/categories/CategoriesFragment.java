package com.spendwise.ai.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.spendwise.ai.databinding.FragmentCategoriesBinding;
import com.spendwise.ai.util.DateUtils;

/**
 * Fragment for managing categories and viewing/setting budgets.
 */
public class CategoriesFragment extends Fragment {

    private FragmentCategoriesBinding binding;
    private CategoriesViewModel viewModel;
    private CategoryBudgetAdapter adapter;

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
        binding.fabAddCategory.setOnClickListener(v -> {
            // TODO: Show category editor dialog
        });
    }

    private void observeData() {
        // Observe current budget month
        viewModel.getBudgetMonth().observe(getViewLifecycleOwner(), month -> {
            binding.textBudgetMonth.setText(DateUtils.getDisplayMonth(month) + " Budgets");
        });

        // Observe categories
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            adapter.setCategories(categories);
        });

        // Observe budgets with spend
        viewModel.getBudgetsWithSpend().observe(getViewLifecycleOwner(), budgets -> {
            adapter.setBudgets(budgets);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
