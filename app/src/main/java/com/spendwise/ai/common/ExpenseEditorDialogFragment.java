package com.spendwise.ai.common;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.databinding.FragmentExpenseEditorDialogBinding;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog fragment for creating and editing expenses.
 * Supports passing expenseId for editing, or dateEpochDay for new expense on
 * specific date.
 */
public class ExpenseEditorDialogFragment extends DialogFragment {

    private static final String ARG_EXPENSE_ID = "expense_id";
    private static final String ARG_DATE_EPOCH_DAY = "date_epoch_day";

    private FragmentExpenseEditorDialogBinding binding;
    private ExpenseEditorViewModel viewModel;
    private List<Category> categories = new ArrayList<>();
    private Map<String, Long> categoryNameToId = new HashMap<>();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy");

    /**
     * Create new instance for adding expense on a specific date.
     */
    public static ExpenseEditorDialogFragment newInstance(@Nullable Long expenseId, long dateEpochDay) {
        ExpenseEditorDialogFragment fragment = new ExpenseEditorDialogFragment();
        Bundle args = new Bundle();
        if (expenseId != null) {
            args.putLong(ARG_EXPENSE_ID, expenseId);
        }
        args.putLong(ARG_DATE_EPOCH_DAY, dateEpochDay);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentExpenseEditorDialogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(ExpenseEditorViewModel.class);

        // Initialize from arguments
        Bundle args = getArguments();
        if (args != null) {
            if (args.containsKey(ARG_EXPENSE_ID)) {
                long expenseId = args.getLong(ARG_EXPENSE_ID);
                viewModel.initForEdit(expenseId);
                binding.textDialogTitle.setText("Edit Expense");
                binding.btnDelete.setVisibility(View.VISIBLE);
            } else {
                long dateEpochDay = args.getLong(ARG_DATE_EPOCH_DAY, DateUtils.getTodayEpochDay());
                viewModel.initForNewExpense(dateEpochDay);
                binding.textDialogTitle.setText("Add Expense");
                binding.btnDelete.setVisibility(View.GONE);
            }
        }

        setupCategoryDropdown();
        setupMerchantAutocomplete();
        setupDescriptionSuggestions();
        setupDatePicker();
        setupButtons();
        observeViewModel();
    }

    private void setupMerchantAutocomplete() {
        // Load all merchants for autocomplete
        viewModel.getAllMerchants().observe(getViewLifecycleOwner(), merchants -> {
            if (merchants == null || merchants.isEmpty())
                return;

            List<String> merchantNames = new ArrayList<>();
            for (com.spendwise.ai.data.entity.Merchant merchant : merchants) {
                merchantNames.add(merchant.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    merchantNames);
            binding.editMerchant.setAdapter(adapter);
        });
    }

    private void setupDescriptionSuggestions() {
        // When description changes, suggest a category based on keywords
        binding.editDescription.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String description = s.toString().toLowerCase();
                suggestCategoryFromDescription(description);
            }
        });
    }

    private void suggestCategoryFromDescription(String description) {
        if (description.isEmpty())
            return;

        // Only suggest if no category is selected yet
        Long currentCategory = viewModel.getSelectedCategoryId().getValue();
        if (currentCategory != null && currentCategory > 0)
            return;

        // Simple keyword matching for category suggestions
        String suggestedCategory = null;

        if (containsAny(description, "food", "lunch", "dinner", "breakfast", "restaurant", "pizza", "burger",
                "coffee")) {
            suggestedCategory = "Food & Dining";
        } else if (containsAny(description, "grocery", "groceries", "supermarket", "vegetables", "fruits")) {
            suggestedCategory = "Groceries";
        } else if (containsAny(description, "uber", "ola", "taxi", "bus", "train", "metro", "petrol", "fuel", "gas")) {
            suggestedCategory = "Transportation";
        } else if (containsAny(description, "electric", "water", "internet", "wifi", "phone", "mobile", "recharge")) {
            suggestedCategory = "Utilities";
        } else if (containsAny(description, "movie", "netflix", "spotify", "concert", "game", "entertainment")) {
            suggestedCategory = "Entertainment";
        } else if (containsAny(description, "clothes", "shoes", "fashion", "shopping", "amazon", "flipkart")) {
            suggestedCategory = "Shopping";
        } else if (containsAny(description, "doctor", "medicine", "hospital", "pharmacy", "health")) {
            suggestedCategory = "Healthcare";
        } else if (containsAny(description, "rent", "maintenance", "repair", "home")) {
            suggestedCategory = "Housing";
        }

        if (suggestedCategory != null) {
            Long categoryId = categoryNameToId.get(suggestedCategory);
            if (categoryId != null) {
                binding.dropdownCategory.setText(suggestedCategory, false);
                viewModel.setSelectedCategoryId(categoryId);
            }
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword))
                return true;
        }
        return false;
    }

    private void setupCategoryDropdown() {
        viewModel.getAllCategories().observe(getViewLifecycleOwner(), categoryList -> {
            this.categories = categoryList;
            categoryNameToId.clear();

            List<String> categoryNames = new ArrayList<>();
            for (Category category : categoryList) {
                categoryNames.add(category.getName());
                categoryNameToId.put(category.getName(), category.getId());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categoryNames);
            binding.dropdownCategory.setAdapter(adapter);
        });

        binding.dropdownCategory.setOnItemClickListener((parent, view, position, id) -> {
            String categoryName = (String) parent.getItemAtPosition(position);
            Long categoryId = categoryNameToId.get(categoryName);
            if (categoryId != null) {
                viewModel.setSelectedCategoryId(categoryId);
            }
        });
    }

    private void setupDatePicker() {
        binding.editDate.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        LocalDate currentDate = viewModel.getSelectedDate().getValue();
        if (currentDate == null) {
            currentDate = LocalDate.now();
        }

        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    LocalDate selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                    viewModel.setSelectedDate(selectedDate);
                },
                currentDate.getYear(),
                currentDate.getMonthValue() - 1,
                currentDate.getDayOfMonth());
        dialog.show();
    }

    private void setupButtons() {
        binding.btnCancel.setOnClickListener(v -> dismiss());

        binding.btnDelete.setOnClickListener(v -> {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete Expense")
                    .setMessage("Are you sure you want to delete this expense?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        viewModel.deleteExpense();
                        dismiss();
                    })
                    .show();
        });

        binding.btnSave.setOnClickListener(v -> {
            // Get values from form
            String amountStr = binding.editAmount.getText().toString();
            String description = binding.editDescription.getText().toString();
            String merchant = binding.editMerchant.getText().toString();

            viewModel.setAmountFromString(amountStr);
            viewModel.setDescription(description);
            viewModel.setMerchantName(merchant);

            // Attempt save
            viewModel.saveExpense();
        });
    }

    private void observeViewModel() {
        // Observe selected date
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            binding.editDate.setText(date.format(DATE_FORMAT));
        });

        // Observe amount error
        viewModel.getAmountError().observe(getViewLifecycleOwner(), error -> {
            binding.layoutAmount.setError(error);
        });

        // Observe category error
        viewModel.getCategoryError().observe(getViewLifecycleOwner(), error -> {
            binding.layoutCategory.setError(error);
        });

        // Observe save completion
        viewModel.getSaveComplete().observe(getViewLifecycleOwner(), complete -> {
            if (complete != null && complete) {
                viewModel.resetSaveComplete();
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make dialog wider
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
