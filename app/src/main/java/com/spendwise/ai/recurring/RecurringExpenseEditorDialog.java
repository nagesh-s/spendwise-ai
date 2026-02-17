package com.spendwise.ai.recurring;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.RecurringExpense;

import org.threeten.bp.LocalDate;

import java.util.List;

/**
 * Dialog for creating or editing a RecurringExpense.
 */
public class RecurringExpenseEditorDialog extends DialogFragment {

    private static final String ARG_EXPENSE_ID = "expense_id";

    private RecurringExpensesViewModel viewModel;
    private TextInputEditText descriptionInput;
    private TextInputEditText amountInput;
    private TextInputEditText dayInput;
    private AutoCompleteTextView categorySpinner;
    private AutoCompleteTextView frequencySpinner;

    private List<Category> categories;
    private long selectedCategoryId = -1;
    private String selectedFrequency = RecurringExpense.FREQUENCY_MONTHLY;
    private RecurringExpense existingExpense;

    private static final String[] FREQUENCIES = {
            "Daily",
            "Every 2 days",
            "Every 3 days",
            "Weekly",
            "Monthly",
            "Yearly"
    };

    public static RecurringExpenseEditorDialog newInstance(long expenseId) {
        RecurringExpenseEditorDialog dialog = new RecurringExpenseEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_EXPENSE_ID, expenseId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(RecurringExpensesViewModel.class);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_recurring_expense_editor, null);

        descriptionInput = view.findViewById(R.id.input_description);
        amountInput = view.findViewById(R.id.input_amount);
        dayInput = view.findViewById(R.id.input_day);
        categorySpinner = view.findViewById(R.id.spinner_category);
        frequencySpinner = view.findViewById(R.id.spinner_frequency);

        // Setup frequency spinner
        ArrayAdapter<String> freqAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, FREQUENCIES);
        frequencySpinner.setAdapter(freqAdapter);
        frequencySpinner.setText("Monthly", false);
        frequencySpinner.setOnItemClickListener((parent, v, position, id) -> {
            switch (position) {
                case 0:
                    selectedFrequency = RecurringExpense.FREQUENCY_DAILY;
                    break;
                case 1:
                    selectedFrequency = RecurringExpense.FREQUENCY_EVERY_2_DAYS;
                    break;
                case 2:
                    selectedFrequency = RecurringExpense.FREQUENCY_EVERY_3_DAYS;
                    break;
                case 3:
                    selectedFrequency = RecurringExpense.FREQUENCY_WEEKLY;
                    break;
                case 4:
                    selectedFrequency = RecurringExpense.FREQUENCY_MONTHLY;
                    break;
                case 5:
                    selectedFrequency = RecurringExpense.FREQUENCY_YEARLY;
                    break;
            }
            updateDayInputHint();
        });

        // Load categories
        AppDatabase.databaseWriteExecutor.execute(() -> {
            categories = AppDatabase.getInstance(requireContext())
                    .categoryDao().getAllCategoriesSync();

            requireActivity().runOnUiThread(() -> {
                String[] categoryNames = new String[categories.size()];
                for (int i = 0; i < categories.size(); i++) {
                    categoryNames[i] = categories.get(i).getName();
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        categoryNames);
                categorySpinner.setAdapter(adapter);

                categorySpinner.setOnItemClickListener((parent, v, position, id) -> {
                    selectedCategoryId = categories.get(position).getId();
                });

                loadExistingExpense();
            });
        });

        long expenseId = getArguments() != null ? getArguments().getLong(ARG_EXPENSE_ID, -1) : -1;
        String title = expenseId == -1 ? getString(R.string.add_recurring_expense) : "Edit Recurring Expense";

        return new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.save, (dialog, which) -> save())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void updateDayInputHint() {
        switch (selectedFrequency) {
            case RecurringExpense.FREQUENCY_DAILY:
            case RecurringExpense.FREQUENCY_EVERY_2_DAYS:
            case RecurringExpense.FREQUENCY_EVERY_3_DAYS:
                dayInput.setEnabled(false);
                dayInput.setText("");
                break;
            case RecurringExpense.FREQUENCY_WEEKLY:
                dayInput.setEnabled(true);
                dayInput.setHint("Day of week (1=Sun, 7=Sat)");
                break;
            case RecurringExpense.FREQUENCY_MONTHLY:
            case RecurringExpense.FREQUENCY_YEARLY:
                dayInput.setEnabled(true);
                dayInput.setHint("Day of month (1-31)");
                break;
        }
    }

    private void loadExistingExpense() {
        long expenseId = getArguments() != null ? getArguments().getLong(ARG_EXPENSE_ID, -1) : -1;
        if (expenseId == -1)
            return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            existingExpense = viewModel.getById(expenseId);
            if (existingExpense != null) {
                requireActivity().runOnUiThread(() -> {
                    descriptionInput.setText(existingExpense.getDescription());
                    amountInput.setText(String.valueOf(existingExpense.getAmountCents() / 100.0));
                    dayInput.setText(String.valueOf(existingExpense.getDayOfPeriod()));

                    selectedCategoryId = existingExpense.getCategoryId() != null ? existingExpense.getCategoryId() : -1;
                    selectedFrequency = existingExpense.getFrequency();

                    // Set frequency spinner
                    switch (selectedFrequency) {
                        case RecurringExpense.FREQUENCY_DAILY:
                            frequencySpinner.setText("Daily", false);
                            break;
                        case RecurringExpense.FREQUENCY_EVERY_2_DAYS:
                            frequencySpinner.setText("Every 2 days", false);
                            break;
                        case RecurringExpense.FREQUENCY_EVERY_3_DAYS:
                            frequencySpinner.setText("Every 3 days", false);
                            break;
                        case RecurringExpense.FREQUENCY_WEEKLY:
                            frequencySpinner.setText("Weekly", false);
                            break;
                        case RecurringExpense.FREQUENCY_MONTHLY:
                            frequencySpinner.setText("Monthly", false);
                            break;
                        case RecurringExpense.FREQUENCY_YEARLY:
                            frequencySpinner.setText("Yearly", false);
                            break;
                    }
                    updateDayInputHint();

                    // Set category spinner
                    if (categories != null && selectedCategoryId != -1) {
                        for (int i = 0; i < categories.size(); i++) {
                            if (categories.get(i).getId() == selectedCategoryId) {
                                categorySpinner.setText(categories.get(i).getName(), false);
                                break;
                            }
                        }
                    }
                });
            }
        });
    }

    private void save() {
        String description = getText(descriptionInput);
        String amountStr = getText(amountInput);
        String dayStr = getText(dayInput);

        if (TextUtils.isEmpty(description)) {
            Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            long amountCents = Math.round(amount * 100);
            int dayOfPeriod;
            if (RecurringExpense.FREQUENCY_WEEKLY.equals(selectedFrequency)) {
                if (TextUtils.isEmpty(dayStr)) {
                    Toast.makeText(requireContext(), "Enter day of week (1-7)", Toast.LENGTH_SHORT).show();
                    return;
                }
                dayOfPeriod = Integer.parseInt(dayStr);
                if (dayOfPeriod < 1 || dayOfPeriod > 7) {
                    Toast.makeText(requireContext(), "Day of week must be between 1 and 7", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if (RecurringExpense.FREQUENCY_MONTHLY.equals(selectedFrequency)
                    || RecurringExpense.FREQUENCY_YEARLY.equals(selectedFrequency)) {
                if (TextUtils.isEmpty(dayStr)) {
                    Toast.makeText(requireContext(), "Enter day of month (1-31)", Toast.LENGTH_SHORT).show();
                    return;
                }
                dayOfPeriod = Integer.parseInt(dayStr);
                if (dayOfPeriod < 1 || dayOfPeriod > 31) {
                    Toast.makeText(requireContext(), "Day of month must be between 1 and 31", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                dayOfPeriod = 1;
            }

            if (existingExpense != null) {
                existingExpense.setDescription(description);
                existingExpense.setAmountCents(amountCents);
                existingExpense.setCategoryId(selectedCategoryId > 0 ? selectedCategoryId : null);
                existingExpense.setFrequency(selectedFrequency);
                existingExpense.setDayOfPeriod(dayOfPeriod);
                viewModel.update(existingExpense);
            } else {
                RecurringExpense expense = new RecurringExpense(
                        amountCents,
                        description,
                        selectedCategoryId > 0 ? selectedCategoryId : null,
                        null,
                        selectedFrequency,
                        dayOfPeriod,
                        LocalDate.now().toEpochDay());
                viewModel.insert(expense);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }

    private String getText(TextInputEditText input) {
        return input.getText() != null ? input.getText().toString().trim() : "";
    }
}
