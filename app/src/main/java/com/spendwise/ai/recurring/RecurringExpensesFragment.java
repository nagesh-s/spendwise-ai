package com.spendwise.ai.recurring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.RecurringExpense;

/**
 * Fragment for managing recurring expenses.
 */
public class RecurringExpensesFragment extends Fragment implements RecurringExpenseAdapter.OnItemClickListener {

    private RecurringExpensesViewModel viewModel;
    private RecurringExpenseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recurring_expenses, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecurringExpensesViewModel.class);

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_recurring);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RecurringExpenseAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup FAB
        FloatingActionButton fab = view.findViewById(R.id.fab_add_recurring);
        fab.setOnClickListener(v -> showEditor(null));

        // Observe data
        viewModel.getAllRecurringExpenses().observe(getViewLifecycleOwner(), expenses -> {
            adapter.submitList(expenses);

            View emptyView = view.findViewById(R.id.text_empty);
            if (expenses == null || expenses.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onItemClick(RecurringExpense expense) {
        showEditor(expense);
    }

    @Override
    public void onToggleActive(RecurringExpense expense) {
        viewModel.toggleActive(expense);
    }

    @Override
    public void onDelete(RecurringExpense expense) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Recurring Expense")
                .setMessage("Delete \"" + expense.getDescription() + "\"?")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.delete(expense))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showEditor(@Nullable RecurringExpense existing) {
        RecurringExpenseEditorDialog dialog = RecurringExpenseEditorDialog.newInstance(
                existing != null ? existing.getId() : -1);
        dialog.show(getChildFragmentManager(), "recurring_editor");
    }
}
