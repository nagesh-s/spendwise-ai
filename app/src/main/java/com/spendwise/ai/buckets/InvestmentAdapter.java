package com.spendwise.ai.buckets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Investment;

/**
 * RecyclerView adapter for investments inside a bucket detail screen.
 * Shows: name, job title, platform, P&L.
 */
public class InvestmentAdapter extends ListAdapter<Investment, InvestmentAdapter.InvestmentViewHolder> {

    public interface OnInvestmentClickListener {
        void onInvestmentClick(Investment investment);
    }

    private final OnInvestmentClickListener listener;

    public InvestmentAdapter(OnInvestmentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Investment> DIFF_CALLBACK = new DiffUtil.ItemCallback<Investment>() {
        @Override
        public boolean areItemsTheSame(@NonNull Investment o, @NonNull Investment n) {
            return o.getId() == n.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Investment o, @NonNull Investment n) {
            return o.getName().equals(n.getName())
                    && o.getCurrentValueCents() == n.getCurrentValueCents()
                    && o.getInvestedAmountCents() == n.getInvestedAmountCents();
        }
    };

    @NonNull
    @Override
    public InvestmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_investment, parent, false);
        return new InvestmentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvestmentViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class InvestmentViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textJobTitle;
        private final TextView textPlatform;
        private final TextView textValue;
        private final TextView textPnl;
        private final TextView textAssetType;

        InvestmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_investment_name);
            textJobTitle = itemView.findViewById(R.id.text_investment_job_title);
            textPlatform = itemView.findViewById(R.id.text_investment_platform);
            textValue = itemView.findViewById(R.id.text_investment_value);
            textPnl = itemView.findViewById(R.id.text_investment_pnl);
            textAssetType = itemView.findViewById(R.id.text_investment_asset_type);
        }

        void bind(Investment inv, OnInvestmentClickListener listener) {
            textName.setText(inv.getName());

            // Job title
            String jobTitle = inv.getJobTitle();
            if (jobTitle != null && !jobTitle.isEmpty()) {
                textJobTitle.setText(jobTitle);
                textJobTitle.setVisibility(View.VISIBLE);
            } else {
                textJobTitle.setVisibility(View.GONE);
            }

            // Platform
            String platform = inv.getPlatform();
            if (platform != null && !platform.isEmpty()) {
                textPlatform.setText(platform);
                textPlatform.setVisibility(View.VISIBLE);
            } else {
                textPlatform.setVisibility(View.GONE);
            }

            // Current value
            textValue.setText(String.format("₹%,.0f", inv.getCurrentValueCents() / 100.0));

            // P&L
            long pnl = inv.getProfitLossCents();
            double returnPct = inv.calculateReturnPercent();
            if (inv.getInvestedAmountCents() > 0) {
                String sign = pnl >= 0 ? "+" : "";
                textPnl.setText(String.format("%s₹%,.0f (%.1f%%)", sign, pnl / 100.0, returnPct));
                textPnl.setTextColor(pnl >= 0 ? 0xFF43A047 : 0xFFE53935);
                textPnl.setVisibility(View.VISIBLE);
            } else {
                textPnl.setVisibility(View.GONE);
            }

            // Asset type label
            textAssetType.setText(formatAssetType(inv.getAssetType()));

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onInvestmentClick(inv);
            });
        }

        private String formatAssetType(String type) {
            switch (type) {
                case Investment.ASSET_STOCK:
                    return "Stock";
                case Investment.ASSET_MF:
                    return "Mutual Fund";
                case Investment.ASSET_ETF:
                    return "ETF";
                case Investment.ASSET_FD:
                    return "Fixed Deposit";
                case Investment.ASSET_RD:
                    return "Recurring Deposit";
                case Investment.ASSET_PPF:
                    return "PPF";
                case Investment.ASSET_NPS:
                    return "NPS";
                case Investment.ASSET_GOLD:
                    return "Gold";
                case Investment.ASSET_SAVINGS:
                    return "Savings";
                default:
                    return "Other";
            }
        }
    }
}
