package com.spendwise.ai.quicktiles;

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
import com.spendwise.ai.data.entity.QuickTile;

import java.util.List;

/**
 * Dialog for creating or editing a QuickTile.
 */
public class QuickTileEditorDialog extends DialogFragment {

    private static final String ARG_TILE_ID = "tile_id";

    private QuickTilesViewModel viewModel;
    private TextInputEditText labelInput;
    private TextInputEditText amountInput;
    private AutoCompleteTextView categorySpinner;

    private List<Category> categories;
    private long selectedCategoryId = -1;
    private QuickTile existingTile;

    public static QuickTileEditorDialog newInstance(long tileId) {
        QuickTileEditorDialog dialog = new QuickTileEditorDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_TILE_ID, tileId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment()).get(QuickTilesViewModel.class);

        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_quick_tile_editor, null);

        labelInput = view.findViewById(R.id.input_label);
        amountInput = view.findViewById(R.id.input_amount);
        categorySpinner = view.findViewById(R.id.spinner_category);

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

                // If editing, load existing data
                loadExistingTile();
            });
        });

        long tileId = getArguments() != null ? getArguments().getLong(ARG_TILE_ID, -1) : -1;
        String title = tileId == -1 ? getString(R.string.add_quick_tile) : "Edit Quick Tile";

        return new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.save_tile, (dialog, which) -> saveTile())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void loadExistingTile() {
        long tileId = getArguments() != null ? getArguments().getLong(ARG_TILE_ID, -1) : -1;
        if (tileId == -1)
            return;

        AppDatabase.databaseWriteExecutor.execute(() -> {
            existingTile = viewModel.getTileById(tileId);
            if (existingTile != null) {
                requireActivity().runOnUiThread(() -> {
                    labelInput.setText(existingTile.getLabel());

                    // Convert cents to rupees for display
                    double amount = existingTile.getAmountCents() / 100.0;
                    amountInput.setText(String.valueOf(amount));

                    selectedCategoryId = existingTile.getCategoryId();

                    // Find and select the category in spinner
                    if (categories != null) {
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

    private void saveTile() {
        String label = labelInput.getText() != null ? labelInput.getText().toString().trim() : "";
        String amountStr = amountInput.getText() != null ? amountInput.getText().toString().trim() : "";

        if (TextUtils.isEmpty(label)) {
            Toast.makeText(requireContext(), "Please enter a label", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedCategoryId == -1) {
            Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            long amountCents = Math.round(amount * 100);

            if (existingTile != null) {
                existingTile.setLabel(label);
                existingTile.setAmountCents(amountCents);
                existingTile.setCategoryId(selectedCategoryId);
                viewModel.updateTile(existingTile);
            } else {
                QuickTile newTile = new QuickTile(selectedCategoryId, null, label, amountCents);
                viewModel.insertTile(newTile);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}
