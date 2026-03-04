package com.spendwise.ai.recurring;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.InvestmentSchedule;
import com.spendwise.ai.data.entity.RecurringExpense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fragment for managing recurring expenses and recurring investments (SIPs).
 */
public class RecurringExpensesFragment extends Fragment
        implements RecurringExpenseAdapter.OnItemClickListener, InvestmentScheduleAdapter.OnItemClickListener {

    private enum Mode {
        EXPENSES,
        INVESTMENTS
    }

    private RecurringExpensesViewModel viewModel;
    private RecurringExpenseAdapter expenseAdapter;
    private InvestmentScheduleAdapter investmentAdapter;
    private RecyclerView recyclerView;
    private TextView emptyView;
    private TextView titleView;
    private TextView subtitleView;

    private Mode currentMode = Mode.EXPENSES;
    private List<RecurringExpense> latestExpenses = new ArrayList<>();
    private List<InvestmentSchedule> latestSchedules = new ArrayList<>();
    private final Map<Long, String> bucketNames = new HashMap<>();

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
        recyclerView = view.findViewById(R.id.recycler_recurring);
        emptyView = view.findViewById(R.id.text_empty);
        titleView = view.findViewById(R.id.text_recurring_title);
        subtitleView = view.findViewById(R.id.text_recurring_subtitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        expenseAdapter = new RecurringExpenseAdapter(this);
        investmentAdapter = new InvestmentScheduleAdapter(this);
        recyclerView.setAdapter(expenseAdapter);

        FloatingActionButton fab = view.findViewById(R.id.fab_add_recurring);
        fab.setOnClickListener(v -> showEditorForCurrentMode(null, null));

        MaterialButtonToggleGroup modeToggle = view.findViewById(R.id.toggle_recurring_mode);
        modeToggle.check(R.id.btn_mode_expenses);
        modeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            if (checkedId == R.id.btn_mode_investments) {
                currentMode = Mode.INVESTMENTS;
            } else {
                currentMode = Mode.EXPENSES;
            }
            renderCurrentMode();
        });

        viewModel.getAllRecurringExpenses().observe(getViewLifecycleOwner(), expenses -> {
            latestExpenses = expenses == null ? new ArrayList<>() : expenses;
            if (currentMode == Mode.EXPENSES) {
                renderCurrentMode();
            }
        });

        viewModel.getAllInvestmentSchedules().observe(getViewLifecycleOwner(), schedules -> {
            latestSchedules = schedules == null ? new ArrayList<>() : schedules;
            if (currentMode == Mode.INVESTMENTS) {
                renderCurrentMode();
            }
        });

        loadBucketNames();
    }

    private void loadBucketNames() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Bucket> buckets = viewModel.getAllBucketsSync();
            Map<Long, String> names = new HashMap<>();
            if (buckets != null) {
                for (Bucket bucket : buckets) {
                    names.put(bucket.getId(), bucket.getName());
                }
            }
            androidx.fragment.app.FragmentActivity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(() -> {
                bucketNames.clear();
                bucketNames.putAll(names);
                investmentAdapter.setBucketNames(bucketNames);
                if (currentMode == Mode.INVESTMENTS) {
                    renderCurrentMode();
                }
            });
        });
    }

    private void renderCurrentMode() {
        if (!isAdded()) {
            return;
        }
        if (currentMode == Mode.EXPENSES) {
            titleView.setText(R.string.recurring_expenses);
            subtitleView.setText(R.string.recurring_expenses_subtitle);
            recyclerView.setAdapter(expenseAdapter);
            expenseAdapter.submitList(new ArrayList<>(latestExpenses));
            boolean empty = latestExpenses == null || latestExpenses.isEmpty();
            emptyView.setText(R.string.no_recurring_expenses);
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        } else {
            titleView.setText(R.string.recurring_investments_title);
            subtitleView.setText(R.string.recurring_investments_subtitle);
            recyclerView.setAdapter(investmentAdapter);
            investmentAdapter.setBucketNames(bucketNames);
            investmentAdapter.submitList(new ArrayList<>(latestSchedules));
            boolean empty = latestSchedules == null || latestSchedules.isEmpty();
            emptyView.setText(R.string.no_recurring_investments);
            emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    private void showEditorForCurrentMode(
            @Nullable RecurringExpense recurringExpense,
            @Nullable InvestmentSchedule investmentSchedule) {
        if (currentMode == Mode.EXPENSES) {
            RecurringExpenseEditorDialog dialog = RecurringExpenseEditorDialog.newInstance(
                    recurringExpense != null ? recurringExpense.getId() : -1);
            dialog.show(getChildFragmentManager(), "recurring_editor");
        } else {
            InvestmentScheduleEditorDialog dialog = InvestmentScheduleEditorDialog.newInstance(
                    investmentSchedule != null ? investmentSchedule.getId() : -1);
            dialog.show(getChildFragmentManager(), "investment_schedule_editor");
        }
    }

    @Override
    public void onItemClick(RecurringExpense expense) {
        showEditorForCurrentMode(expense, null);
    }

    @Override
    public void onToggleActive(RecurringExpense expense) {
        viewModel.toggleActive(expense);
    }

    @Override
    public void onDelete(RecurringExpense expense) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.recurring_delete_expense_title)
                .setMessage(getString(R.string.recurring_delete_expense_message, expense.getDescription()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.delete(expense))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onItemClick(InvestmentSchedule schedule) {
        showEditorForCurrentMode(null, schedule);
    }

    @Override
    public void onToggleActive(InvestmentSchedule schedule) {
        viewModel.toggleInvestmentScheduleActive(schedule);
    }

    @Override
    public void onDelete(InvestmentSchedule schedule) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.recurring_delete_investment_title)
                .setMessage(getString(R.string.recurring_delete_investment_message, schedule.getName()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.deleteInvestmentSchedule(schedule))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
