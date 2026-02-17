package com.spendwise.ai.quicktiles;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.util.DateUtils;
import com.spendwise.ai.widget.QuickTilesWidgetProvider;

import org.threeten.bp.LocalDate;

/**
 * Fragment for managing QuickTiles.
 * 
 * Displays a grid of existing tiles and allows users to add, edit, or delete
 * them.
 */
public class QuickTilesFragment extends Fragment implements QuickTileAdapter.OnTileClickListener {

    public static final String RESULT_KEY_OPEN_EDITOR = "quick_tile_open_editor";

    private QuickTilesViewModel viewModel;
    private QuickTileAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_quick_tiles, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(QuickTilesViewModel.class);
        getParentFragmentManager().setFragmentResultListener(
                RESULT_KEY_OPEN_EDITOR,
                getViewLifecycleOwner(),
                (requestKey, bundle) -> showTileEditor(null));

        // Setup RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_quick_tiles);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        adapter = new QuickTileAdapter(this);
        recyclerView.setAdapter(adapter);

        // Setup FAB
        FloatingActionButton fab = view.findViewById(R.id.fab_add_tile);
        fab.setOnClickListener(v -> showTileEditor(null));

        // Observe tiles
        viewModel.getAllQuickTiles().observe(getViewLifecycleOwner(), tiles -> {
            adapter.submitList(tiles);

            // Show/hide empty state
            View emptyView = view.findViewById(R.id.text_empty);
            if (tiles == null || tiles.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onTileClick(QuickTile tile) {
        addExpenseFromTile(tile);
    }

    @Override
    public void onTileLongClick(QuickTile tile) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(tile.getLabel())
                .setItems(new String[] { getString(R.string.edit_tile), getString(R.string.delete_tile) },
                        (dialog, which) -> {
                            if (which == 0) {
                                showTileEditor(tile);
                            } else {
                                confirmDelete(tile);
                            }
                        })
                .show();
    }

    private void confirmDelete(QuickTile tile) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_tile)
                .setMessage(getString(R.string.delete_tile_confirm, tile.getLabel()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> viewModel.deleteTile(tile))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void addExpenseFromTile(QuickTile tile) {
        android.content.Context appContext = requireContext().getApplicationContext();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Expense expense = new Expense(
                    LocalDate.now().toEpochDay(),
                    tile.getAmountCents(),
                    tile.getLabel(),
                    tile.getCategoryId(),
                    tile.getMerchantId());
            AppDatabase.getInstance(appContext).expenseDao().insert(expense);
            QuickTilesWidgetProvider.requestUpdate(appContext);

            if (isAdded()) {
                requireActivity().runOnUiThread(() -> android.widget.Toast.makeText(
                        requireContext(),
                        getString(
                                R.string.expense_added_from_tile,
                                tile.getLabel(),
                                DateUtils.formatAmountCents(tile.getAmountCents())),
                        android.widget.Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showTileEditor(@Nullable QuickTile existingTile) {
        QuickTileEditorDialog dialog = QuickTileEditorDialog.newInstance(
                existingTile != null ? existingTile.getId() : -1);
        dialog.show(getChildFragmentManager(), "tile_editor");
    }
}
