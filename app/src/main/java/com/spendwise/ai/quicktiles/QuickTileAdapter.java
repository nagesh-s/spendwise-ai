package com.spendwise.ai.quicktiles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.QuickTile;
import com.spendwise.ai.util.DateUtils;

/**
 * Adapter for displaying QuickTile items in a grid.
 */
public class QuickTileAdapter extends ListAdapter<QuickTile, QuickTileAdapter.TileViewHolder> {

    private final OnTileClickListener listener;

    public interface OnTileClickListener {
        void onTileClick(QuickTile tile);

        void onTileLongClick(QuickTile tile);
    }

    public QuickTileAdapter(OnTileClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<QuickTile> DIFF_CALLBACK = new DiffUtil.ItemCallback<QuickTile>() {
        @Override
        public boolean areItemsTheSame(@NonNull QuickTile oldItem, @NonNull QuickTile newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull QuickTile oldItem, @NonNull QuickTile newItem) {
            return oldItem.getLabel().equals(newItem.getLabel()) &&
                    oldItem.getAmountCents() == newItem.getAmountCents() &&
                    oldItem.getCategoryId() == newItem.getCategoryId();
        }
    };

    @NonNull
    @Override
    public TileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quick_tile, parent, false);
        return new TileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TileViewHolder holder, int position) {
        QuickTile tile = getItem(position);
        holder.bind(tile, listener);
    }

    static class TileViewHolder extends RecyclerView.ViewHolder {
        private final TextView labelText;
        private final TextView amountText;

        TileViewHolder(@NonNull View itemView) {
            super(itemView);
            labelText = itemView.findViewById(R.id.text_tile_label);
            amountText = itemView.findViewById(R.id.text_tile_amount);
        }

        void bind(QuickTile tile, OnTileClickListener listener) {
            labelText.setText(tile.getLabel());
            amountText.setText(DateUtils.formatAmountCents(tile.getAmountCents()));

            itemView.setOnClickListener(v -> listener.onTileClick(tile));
            itemView.setOnLongClickListener(v -> {
                listener.onTileLongClick(tile);
                return true;
            });
        }
    }
}
