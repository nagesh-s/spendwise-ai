package com.spendwise.ai.merchants;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.pojo.MerchantAgg;
import com.spendwise.ai.util.DateUtils;

/**
 * Adapter for displaying top merchants with their stats.
 */
public class MerchantAdapter extends ListAdapter<MerchantAgg, MerchantAdapter.ViewHolder> {

    // Rank colors for top 3
    private static final int[] RANK_COLORS = {
            Color.parseColor("#FFD700"), // Gold
            Color.parseColor("#C0C0C0"), // Silver
            Color.parseColor("#CD7F32"), // Bronze
            Color.parseColor("#607D8B")  // Default
    };

    public MerchantAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<MerchantAgg> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<MerchantAgg>() {
                @Override
                public boolean areItemsTheSame(@NonNull MerchantAgg oldItem, @NonNull MerchantAgg newItem) {
                    return oldItem.getName().equals(newItem.getName());
                }

                @Override
                public boolean areContentsTheSame(@NonNull MerchantAgg oldItem, @NonNull MerchantAgg newItem) {
                    return oldItem.getTotalCents() == newItem.getTotalCents() &&
                           oldItem.getTransactionCount() == newItem.getTransactionCount();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_merchant, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), position + 1);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView rankBadge;
        private final TextView merchantName;
        private final TextView merchantStats;
        private final TextView merchantTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankBadge = itemView.findViewById(R.id.text_rank);
            merchantName = itemView.findViewById(R.id.text_merchant_name);
            merchantStats = itemView.findViewById(R.id.text_merchant_stats);
            merchantTotal = itemView.findViewById(R.id.text_merchant_total);
        }

        void bind(MerchantAgg merchant, int rank) {
            rankBadge.setText(String.valueOf(rank));
            
            // Set rank badge color
            GradientDrawable background = (GradientDrawable) rankBadge.getBackground();
            if (background != null) {
                int colorIndex = Math.min(rank - 1, RANK_COLORS.length - 1);
                background.setColor(RANK_COLORS[colorIndex]);
            }

            merchantName.setText(merchant.getName());

            // Stats: "12 transactions • Avg ₹450"
            String stats = merchant.getTransactionCount() + " transactions • Avg " + 
                           merchant.getFormattedAverage();
            merchantStats.setText(stats);

            merchantTotal.setText(merchant.getFormattedTotal());
        }
    }
}
