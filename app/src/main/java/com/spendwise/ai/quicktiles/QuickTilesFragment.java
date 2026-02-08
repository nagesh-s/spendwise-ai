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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.QuickTile;

/**
 * Fragment for managing QuickTiles.
 * 
 * Displays a grid of existing tiles and allows users to add, edit, or delete
 * them.
 */
public class QuickTilesFragment extends Fragment implements QuickTileAdapter.OnTileClickListener {

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
        showTileEditor(tile);
    }

    @Override
    public void onTileLongClick(QuickTile tile) {
        // Show delete confirmation
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_tile)
                .setMessage("Delete \"" + tile.getLabel() + "\"?")
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    viewModel.deleteTile(tile);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showTileEditor(@Nullable QuickTile existingTile) {
        QuickTileEditorDialog dialog = QuickTileEditorDialog.newInstance(
                existingTile != null ? existingTile.getId() : -1);
        dialog.show(getChildFragmentManager(), "tile_editor");
    }
}
