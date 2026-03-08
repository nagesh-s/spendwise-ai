package com.spendwise.ai.buckets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Investment;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

/**
 * RecyclerView adapter for investments inside a bucket detail screen.
 * Shows: name, job title, platform, P&L.
 */
public class InvestmentAdapter extends ListAdapter<Investment, InvestmentAdapter.InvestmentViewHolder> {

    public interface OnInvestmentClickListener {
        void onInvestmentClick(Investment investment);

        void onInfoClick(Investment investment);
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
        private final TextView textInvested;
        private final TextView textPnl;
        private final TextView textAssetType;
        private final TextView textUpdated;
        private final android.widget.ImageView btnInfo;
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        InvestmentViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_investment_name);
            textJobTitle = itemView.findViewById(R.id.text_investment_job_title);
            textPlatform = itemView.findViewById(R.id.text_investment_platform);
            textValue = itemView.findViewById(R.id.text_investment_value);
            textInvested = itemView.findViewById(R.id.text_investment_invested);
            textPnl = itemView.findViewById(R.id.text_investment_pnl);
            textAssetType = itemView.findViewById(R.id.text_investment_asset_type);
            textUpdated = itemView.findViewById(R.id.text_investment_updated);
            btnInfo = itemView.findViewById(R.id.btn_investment_info);
        }

        void bind(Investment inv, OnInvestmentClickListener listener) {
            Context context = itemView.getContext();
            textName.setText(inv.getName());

            String jobTitle = inv.getJobTitle();
            if (jobTitle != null && !jobTitle.isEmpty()) {
                textJobTitle.setText(jobTitle);
                textJobTitle.setVisibility(View.VISIBLE);
            } else {
                textJobTitle.setVisibility(View.GONE);
            }

            String platform = inv.getPlatform();
            if (platform != null && !platform.isEmpty()) {
                textPlatform.setText(platform);
                textPlatform.setVisibility(View.VISIBLE);
            } else {
                textPlatform.setVisibility(View.GONE);
            }

            textValue.setText(formatCurrency(inv.getCurrentValueCents()));
            textInvested.setText(context.getString(
                    R.string.label_invested_format,
                    formatCurrency(inv.getInvestedAmountCents())));

            long pnl = inv.getProfitLossCents();
            double returnPct = inv.calculateReturnPercent();
            if (inv.getInvestedAmountCents() > 0) {
                String sign = pnl >= 0 ? "+" : "";
                textPnl.setText(String.format(Locale.getDefault(), "%s%s (%.1f%%)",
                        sign, formatCurrency(pnl), returnPct));
                textPnl.setTextColor(pnl >= 0
                        ? ContextCompat.getColor(context, R.color.sw_income)
                        : ContextCompat.getColor(context, R.color.sw_error));
                textPnl.setVisibility(View.VISIBLE);
            } else {
                textPnl.setVisibility(View.GONE);
            }

            textAssetType.setText(formatAssetType(inv.getAssetType()));
            LocalDate updatedDate = LocalDate.ofEpochDay(inv.getLastUpdatedEpochDay());
            textUpdated.setText(context.getString(
                    R.string.bucket_detail_updated_on,
                    updatedDate.format(dateFormatter)));

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onInvestmentClick(inv);
            });
            btnInfo.setOnClickListener(v -> {
                if (listener != null)
                    listener.onInfoClick(inv);
            });
        }

        private String formatCurrency(long amountCents) {
            return String.format(Locale.getDefault(), "₹%,.0f", amountCents / 100.0);
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
