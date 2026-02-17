package com.spendwise.ai.common;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialog fragment for creating and editing expenses.
 * Supports passing expenseId for editing, or dateEpochDay for new expense on
 * specific date.
 */
public class ExpenseEditorDialogFragment extends DialogFragment {

    private static final String ARG_EXPENSE_ID = "expense_id";
    private static final String ARG_DATE_EPOCH_DAY = "date_epoch_day";
    private static final String ARG_VOICE_AMOUNT = "voice_amount";
    private static final String ARG_VOICE_MERCHANT = "voice_merchant";
    private static final String ARG_VOICE_CATEGORY = "voice_category";
    private static final String ARG_VOICE_DESC = "voice_desc";

    private FragmentExpenseEditorDialogBinding binding;
    private ExpenseEditorViewModel viewModel;
    private List<Category> categories = new ArrayList<>();
    private Map<String, Long> categoryNameToId = new HashMap<>();
    private Map<Long, String> categoryIdToName = new HashMap<>();
    private Map<Long, String> merchantIdToName = new HashMap<>();
    private Long pendingCategoryId;
    private Long pendingMerchantId;
    private boolean editFieldsInitialized = false;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

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

    public static ExpenseEditorDialogFragment newInstanceForVoice(long dateEpochDay, long amount, String merchant,
            String category, String desc) {
        ExpenseEditorDialogFragment fragment = new ExpenseEditorDialogFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DATE_EPOCH_DAY, dateEpochDay);
        if (amount > 0)
            args.putLong(ARG_VOICE_AMOUNT, amount);
        if (merchant != null)
            args.putString(ARG_VOICE_MERCHANT, merchant);
        if (category != null)
            args.putString(ARG_VOICE_CATEGORY, category);
        if (desc != null)
            args.putString(ARG_VOICE_DESC, desc);
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
                viewModel.loadTagsForExpense(expenseId);
                binding.textDialogTitle.setText("Edit Expense");
                binding.btnDelete.setVisibility(View.GONE);
            } else {
                long dateEpochDay = args.getLong(ARG_DATE_EPOCH_DAY, DateUtils.getTodayEpochDay());

                if (hasVoicePrefillArgs(args)) {
                    // Voice init
                    long amount = args.getLong(ARG_VOICE_AMOUNT, 0);
                    String merchant = args.getString(ARG_VOICE_MERCHANT);
                    String category = args.getString(ARG_VOICE_CATEGORY);
                    String desc = args.getString(ARG_VOICE_DESC);
                    viewModel.initForVoiceExpense(dateEpochDay, amount, merchant, category, desc);
                } else {
                    // Normal new expense
                    viewModel.initForNewExpense(dateEpochDay);
                }
                binding.textDialogTitle.setText("Add Expense");
                binding.btnDelete.setVisibility(View.GONE);
            }
        }

        setupCategoryDropdown();
        setupAccountDropdown();
        setupMerchantAutocomplete();
        setupDescriptionSuggestions();
        setupDatePicker();
        setupPresetChips();
        setupTagChips();
        setupButtons();
        observeViewModel();
    }

    private void setupAccountDropdown() {
        final Map<String, Long> accountNameToId = new LinkedHashMap<>();
        viewModel.getAllAccounts().observe(getViewLifecycleOwner(), accounts -> {
            List<String> names = new ArrayList<>();
            accountNameToId.clear();
            Long defaultAccountId = null;
            if (accounts != null) {
                Map<String, String> uniqueLabelByKey = new LinkedHashMap<>();
                for (com.spendwise.ai.data.entity.Account account : accounts) {
                    String displayName = account.getName() + " (" + account.getTypeDisplayName() + ")";
                    String uniqueKey = (account.getName() + "|" + account.getType()).toLowerCase(Locale.US);
                    if (!uniqueLabelByKey.containsKey(uniqueKey)) {
                        uniqueLabelByKey.put(uniqueKey, displayName);
                        names.add(displayName);
                        accountNameToId.put(displayName, account.getId());
                    }
                    if (defaultAccountId == null && account.isDefault()) {
                        defaultAccountId = account.getId();
                    }
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_dropdown_item_1line, names);
            binding.dropdownAccount.setAdapter(adapter);

            // Auto-select a default account exactly once if user has not chosen any account yet.
            if (viewModel.getSelectedAccountId().getValue() == null) {
                Long selectedId = defaultAccountId;
                if (selectedId == null && !accountNameToId.isEmpty()) {
                    selectedId = accountNameToId.values().iterator().next();
                }
                if (selectedId != null) {
                    viewModel.setSelectedAccountId(selectedId);
                    String selectedLabel = findDisplayNameByAccountId(accountNameToId, selectedId);
                    if (selectedLabel != null) {
                        binding.dropdownAccount.setText(selectedLabel, false);
                    }
                }
            }
        });

        viewModel.getSelectedAccountId().observe(getViewLifecycleOwner(), selectedAccountId -> {
            if (selectedAccountId == null || accountNameToId.isEmpty()) {
                return;
            }
            String selectedLabel = findDisplayNameByAccountId(accountNameToId, selectedAccountId);
            if (selectedLabel == null) {
                return;
            }
            String current = binding.dropdownAccount.getText() != null
                    ? binding.dropdownAccount.getText().toString()
                    : "";
            if (!selectedLabel.equals(current)) {
                binding.dropdownAccount.setText(selectedLabel, false);
            }
        });

        binding.dropdownAccount.setOnItemClickListener((parent, view2, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            Long accountId = accountNameToId.get(selected);
            if (accountId != null) {
                viewModel.setSelectedAccountId(accountId);
            }
        });
    }

    @Nullable
    private String findDisplayNameByAccountId(@NonNull Map<String, Long> accountNameToId, long accountId) {
        for (Map.Entry<String, Long> entry : accountNameToId.entrySet()) {
            if (entry.getValue() != null && entry.getValue() == accountId) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void setupTagChips() {
        viewModel.getAllTags().observe(getViewLifecycleOwner(), tags -> {
            binding.chipGroupTags.removeAllViews();
            if (tags != null) {
                for (com.spendwise.ai.data.entity.Tag tag : tags) {
                    com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(
                            requireContext());
                    chip.setText(tag.getName());
                    chip.setCheckable(true);
                    chip.setChecked(viewModel.getSelectedTagIds().contains(tag.getId()));
                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> viewModel.toggleTag(tag.getId()));
                    if (tag.getColorArgb() != 0) {
                        chip.setChipBackgroundColorResource(android.R.color.transparent);
                        chip.setChipStrokeWidth(2f);
                        chip.setChipStrokeColor(android.content.res.ColorStateList.valueOf(tag.getColorArgb()));
                    }
                    binding.chipGroupTags.addView(chip);
                }
            }
            // Add "+ New Tag" chip
            com.google.android.material.chip.Chip addChip = new com.google.android.material.chip.Chip(requireContext());
            addChip.setText("+ New Tag");
            addChip.setCheckable(false);
            addChip.setOnClickListener(v -> showNewTagDialog());
            binding.chipGroupTags.addView(addChip);
        });
    }

    private void showNewTagDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("Create New Tag");

        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Tag name");
        input.setPadding(48, 32, 48, 32);
        builder.setView(input);

        builder.setPositiveButton("Create", (dialog, which) -> {
            String tagName = input.getText().toString().trim();
            if (!tagName.isEmpty()) {
                // Generate a random color from Material palette
                int[] colors = { 0xFFE91E63, 0xFF9C27B0, 0xFF3F51B5, 0xFF03A9F4,
                        0xFF009688, 0xFF4CAF50, 0xFFFF9800, 0xFFFF5722 };
                int color = colors[(int) (Math.random() * colors.length)];
                com.spendwise.ai.data.entity.Tag newTag = new com.spendwise.ai.data.entity.Tag(tagName, color);
                com.spendwise.ai.data.repository.TagRepository tagRepo = new com.spendwise.ai.data.repository.TagRepository(
                        requireActivity().getApplication());
                tagRepo.insert(newTag);
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void setupPresetChips() {
        // Setup preset amount chip click listeners
        binding.chip50.setOnClickListener(v -> binding.editAmount.setText("50"));
        binding.chip100.setOnClickListener(v -> binding.editAmount.setText("100"));
        binding.chip200.setOnClickListener(v -> binding.editAmount.setText("200"));
        binding.chip500.setOnClickListener(v -> binding.editAmount.setText("500"));
        binding.chip1000.setOnClickListener(v -> binding.editAmount.setText("1000"));
    }

    private void setupMerchantAutocomplete() {
        // Load all merchants for autocomplete
        viewModel.getAllMerchants().observe(getViewLifecycleOwner(), merchants -> {
            List<String> merchantNames = new ArrayList<>();
            merchantIdToName.clear();
            if (merchants != null) {
                for (com.spendwise.ai.data.entity.Merchant merchant : merchants) {
                    merchantNames.add(merchant.getName());
                    merchantIdToName.put(merchant.getId(), merchant.getName());
                }
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    merchantNames);
            binding.editMerchant.setAdapter(adapter);
            applyPendingMerchantSelection();
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
            suggestedCategory = "Food & Dining";
        } else if (containsAny(description, "uber", "ola", "taxi", "bus", "train", "metro", "petrol", "fuel", "gas")) {
            suggestedCategory = "Transportation";
        } else if (containsAny(description, "electric", "water", "internet", "wifi", "phone", "mobile", "recharge")) {
            suggestedCategory = "Bills & Utilities";
        } else if (containsAny(description, "movie", "netflix", "spotify", "concert", "game", "entertainment")) {
            suggestedCategory = "Entertainment";
        } else if (containsAny(description, "clothes", "shoes", "fashion", "shopping", "amazon", "flipkart")) {
            suggestedCategory = "Shopping";
        } else if (containsAny(description, "doctor", "medicine", "hospital", "pharmacy", "health")) {
            suggestedCategory = "Health & Fitness";
        } else if (containsAny(description, "rent", "maintenance", "repair", "home")) {
            suggestedCategory = "Bills & Utilities";
        }

        if (suggestedCategory != null) {
            Long categoryId = categoryNameToId.get(suggestedCategory);
            if (categoryId != null) {
                binding.dropdownCategory.setText(suggestedCategory, false);
                viewModel.setSelectedCategoryId(categoryId);
            }
        }
    }

    private boolean hasVoicePrefillArgs(Bundle args) {
        return args.containsKey(ARG_VOICE_AMOUNT)
                || args.containsKey(ARG_VOICE_MERCHANT)
                || args.containsKey(ARG_VOICE_CATEGORY)
                || args.containsKey(ARG_VOICE_DESC);
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
            categoryIdToName.clear();

            List<String> categoryNames = new ArrayList<>();
            if (categoryList != null) {
                for (Category category : categoryList) {
                    categoryNames.add(category.getName());
                    categoryNameToId.put(category.getName(), category.getId());
                    categoryIdToName.put(category.getId(), category.getName());
                }
                // Notify VM to resolve pending voice category if any
                viewModel.onCategoriesLoaded(categoryList);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categoryNames);
            binding.dropdownCategory.setAdapter(adapter);
            applyPendingCategorySelection();
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
            new MaterialAlertDialogBuilder(requireContext())
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
        viewModel.getEditingExpense().observe(getViewLifecycleOwner(), expense -> {
            if (expense == null || editFieldsInitialized) {
                return;
            }

            binding.editAmount.setText(String.format(Locale.getDefault(), "%.2f", expense.getAmountCents() / 100.0));
            binding.editDescription.setText(expense.getDescription() != null ? expense.getDescription() : "");

            pendingCategoryId = expense.getCategoryId();
            pendingMerchantId = expense.getMerchantId();
            applyPendingCategorySelection();
            applyPendingMerchantSelection();
            editFieldsInitialized = true;
        });

        // Observe selected date
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            binding.editDate.setText(date.format(DATE_FORMAT));
        });

        viewModel.getAmountCents().observe(getViewLifecycleOwner(), cents -> {
            if (cents == null || cents <= 0) {
                return;
            }
            String current = binding.editAmount.getText() != null ? binding.editAmount.getText().toString().trim() : "";
            String formatted = cents % 100 == 0
                    ? String.valueOf(cents / 100)
                    : String.format(Locale.getDefault(), "%.2f", cents / 100.0);
            if (!formatted.equals(current)) {
                binding.editAmount.setText(formatted);
            }
        });

        viewModel.getDescription().observe(getViewLifecycleOwner(), desc -> {
            if (desc == null || desc.trim().isEmpty()) {
                return;
            }
            String current = binding.editDescription.getText() != null
                    ? binding.editDescription.getText().toString()
                    : "";
            if (current.trim().isEmpty()) {
                binding.editDescription.setText(desc);
            }
        });

        viewModel.getMerchantName().observe(getViewLifecycleOwner(), merchantName -> {
            if (merchantName == null || merchantName.trim().isEmpty()) {
                return;
            }
            String current = binding.editMerchant.getText() != null
                    ? binding.editMerchant.getText().toString()
                    : "";
            if (current.trim().isEmpty()) {
                binding.editMerchant.setText(merchantName, false);
            }
        });

        viewModel.getSelectedCategoryId().observe(getViewLifecycleOwner(), categoryId -> {
            if (categoryId == null || categoryId <= 0) {
                return;
            }
            String targetName = categoryIdToName.get(categoryId);
            if (targetName == null) {
                return;
            }
            String currentName = binding.dropdownCategory.getText() != null
                    ? binding.dropdownCategory.getText().toString()
                    : "";
            if (targetName.equals(currentName)) {
                return;
            }
            pendingCategoryId = categoryId;
            applyPendingCategorySelection();
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

        // Observe save message (XP/Badges)
        viewModel.getSaveMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void applyPendingCategorySelection() {
        if (pendingCategoryId == null) {
            return;
        }
        Long categoryId = pendingCategoryId;
        pendingCategoryId = null;

        String categoryName = categoryIdToName.get(categoryId);
        if (categoryName == null) {
            return;
        }

        String current = binding.dropdownCategory.getText() != null
                ? binding.dropdownCategory.getText().toString()
                : "";
        if (!categoryName.equals(current)) {
            binding.dropdownCategory.setText(categoryName, false);
        }
    }

    private void applyPendingMerchantSelection() {
        if (pendingMerchantId == null) {
            return;
        }
        String merchantName = merchantIdToName.get(pendingMerchantId);
        if (merchantName == null) {
            return;
        }

        binding.editMerchant.setText(merchantName, false);
        pendingMerchantId = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Use most of the screen height so long forms scroll while actions stay pinned.
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            int dialogHeight = (int) (screenHeight * 0.92f);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeight);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
