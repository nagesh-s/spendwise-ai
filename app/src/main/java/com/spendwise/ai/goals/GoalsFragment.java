package com.spendwise.ai.goals;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.FinancialGoal;

import org.threeten.bp.LocalDate;

/**
 * Fragment for displaying and managing financial goals.
 */
public class GoalsFragment extends Fragment implements GoalsAdapter.OnGoalClickListener {

    private GoalsViewModel viewModel;
    private GoalsAdapter adapter;
    private RecyclerView recyclerView;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_goals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(GoalsViewModel.class);

        recyclerView = view.findViewById(R.id.recycler_goals);
        emptyText = view.findViewById(R.id.text_empty_goals);

        adapter = new GoalsAdapter(this);
        recyclerView.setAdapter(adapter);

        // FAB
        view.findViewById(R.id.fab_add_goal).setOnClickListener(v -> showAddGoalDialog());

        // Observe goals
        viewModel.getAllGoals().observe(getViewLifecycleOwner(), goals -> {
            adapter.submitList(goals);
            if (goals == null || goals.isEmpty()) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onGoalClick(FinancialGoal goal) {
        // Show update progress dialog
        showUpdateProgressDialog(goal);
    }

    private void showAddGoalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Create Financial Goal");

        View dialogView = LayoutInflater.from(requireContext()).inflate(
                android.R.layout.simple_list_item_2, null);
        // Use a simple input layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 0);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Goal name (e.g., Emergency Fund)");
        layout.addView(nameInput);

        EditText amountInput = new EditText(requireContext());
        amountInput.setHint("Target amount (₹)");
        amountInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(amountInput);

        Spinner typeSpinner = new Spinner(requireContext());
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item,
                new String[] { "Savings", "Spending Limit", "Debt Payoff" });
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        layout.addView(typeSpinner);

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            if (!name.isEmpty() && !amountStr.isEmpty()) {
                try {
                    long amountCents = (long) (Double.parseDouble(amountStr) * 100);
                    String type;
                    switch (typeSpinner.getSelectedItemPosition()) {
                        case 1:
                            type = FinancialGoal.TYPE_SPENDING_LIMIT;
                            break;
                        case 2:
                            type = FinancialGoal.TYPE_DEBT_PAYOFF;
                            break;
                        default:
                            type = FinancialGoal.TYPE_SAVINGS;
                    }
                    // Random color from palette
                    int[] colors = { 0xFF00897B, 0xFFFF6F61, 0xFF1A237E, 0xFF4CAF50,
                            0xFFFF9800, 0xFF9C27B0, 0xFF03A9F4 };
                    int color = colors[(int) (Math.random() * colors.length)];

                    FinancialGoal goal = new FinancialGoal(name, type, amountCents,
                            LocalDate.now().toEpochDay(), null, color);
                    viewModel.addGoal(goal);
                } catch (NumberFormatException e) {
                    // ignore invalid amount
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showUpdateProgressDialog(FinancialGoal goal) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(goal.getName());

        android.widget.LinearLayout layout = new android.widget.LinearLayout(requireContext());
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(48, 32, 48, 0);

        TextView statusText = new TextView(requireContext());
        statusText.setText(String.format("Progress: ₹%.2f / ₹%.2f (%d%%)",
                goal.getCurrentAmountCents() / 100.0,
                goal.getTargetAmountCents() / 100.0,
                goal.getProgressPercent()));
        layout.addView(statusText);

        EditText progressInput = new EditText(requireContext());
        progressInput.setHint("Update amount (₹)");
        progressInput.setInputType(
                android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(progressInput);

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            String amountStr = progressInput.getText().toString().trim();
            if (!amountStr.isEmpty()) {
                try {
                    long newAmount = (long) (Double.parseDouble(amountStr) * 100);
                    viewModel.updateProgress(goal.getId(), newAmount);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        });

        if (goal.getStatus().equals(FinancialGoal.STATUS_ACTIVE)) {
            builder.setNeutralButton("Mark Complete", (dialog, which) -> viewModel.markComplete(goal.getId()));
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
}
