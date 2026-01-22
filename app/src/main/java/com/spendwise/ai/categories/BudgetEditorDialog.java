package com.spendwise.ai.categories;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.spendwise.ai.databinding.DialogBudgetEditorBinding;
import com.spendwise.ai.util.DateUtils;

/**
 * Dialog for setting or editing a category budget.
 */
public class BudgetEditorDialog extends DialogFragment {

    private static final String ARG_CATEGORY_ID = "category_id";
    private static final String ARG_CATEGORY_NAME = "category_name";
    private static final String ARG_CURRENT_BUDGET = "current_budget";

    private DialogBudgetEditorBinding binding;
    private OnBudgetSavedListener listener;

    public interface OnBudgetSavedListener {
        void onBudgetSaved(long categoryId, long amountCents);
        void onBudgetRemoved(long categoryId);
    }

    public static BudgetEditorDialog newInstance(long categoryId, String categoryName, long currentBudgetCents) {
        BudgetEditorDialog dialog = new BudgetEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_CATEGORY_ID, categoryId);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        args.putLong(ARG_CURRENT_BUDGET, currentBudgetCents);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnBudgetSavedListener(OnBudgetSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DialogBudgetEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) {
            dismiss();
            return;
        }

        long categoryId = args.getLong(ARG_CATEGORY_ID);
        String categoryName = args.getString(ARG_CATEGORY_NAME, "");
        long currentBudget = args.getLong(ARG_CURRENT_BUDGET, 0);

        // Set category name
        binding.textCategoryName.setText("Category: " + categoryName);

        // If editing existing budget, show current value and remove button
        if (currentBudget > 0) {
            binding.textDialogTitle.setText("Edit Budget");
            double amount = currentBudget / 100.0;
            binding.editBudgetAmount.setText(String.format("%.2f", amount));
            binding.btnRemoveBudget.setVisibility(View.VISIBLE);
        } else {
            binding.textDialogTitle.setText("Set Budget");
            binding.btnRemoveBudget.setVisibility(View.GONE);
        }

        // Cancel button
        binding.btnCancel.setOnClickListener(v -> dismiss());

        // Remove button
        binding.btnRemoveBudget.setOnClickListener(v -> {
            if (listener != null) {
                listener.onBudgetRemoved(categoryId);
            }
            dismiss();
        });

        // Save button
        binding.btnSave.setOnClickListener(v -> {
            String amountStr = binding.editBudgetAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                binding.layoutBudgetAmount.setError("Please enter an amount");
                return;
            }

            long amountCents = DateUtils.parseAmountToCents(amountStr);
            if (amountCents <= 0) {
                binding.layoutBudgetAmount.setError("Please enter a valid amount");
                return;
            }

            if (listener != null) {
                listener.onBudgetSaved(categoryId, amountCents);
            }
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
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
