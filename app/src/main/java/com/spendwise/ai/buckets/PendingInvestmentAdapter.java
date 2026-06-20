package com.spendwise.ai.buckets;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Holding;

import java.util.Locale;

/**
 * Adapter for investments pending user review (auto-map-pending).
 * Shows investment details with Accept / Reject actions.
 */
public class PendingInvestmentAdapter
        extends ListAdapter<Holding, PendingInvestmentAdapter.PendingViewHolder> {

    public interface PendingActionListener {
        void onAccept(Holding investment);

        void onReject(Holding investment);

        void onInfoClick(Holding investment);
    }

    private final PendingActionListener listener;

    public PendingInvestmentAdapter(@NonNull PendingActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Holding> DIFF_CALLBACK = new DiffUtil.ItemCallback<Holding>() {
        @Override
        public boolean areItemsTheSame(@NonNull Holding o, @NonNull Holding n) {
            return o.getId() == n.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Holding o, @NonNull Holding n) {
            return o.getName().equals(n.getName())
                    && o.getCurrentValueCents() == n.getCurrentValueCents()
                    && o.getInvestedAmountCents() == n.getInvestedAmountCents();
        }
    };

    @NonNull
    @Override
    public PendingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_investment_pending, parent, false);
        return new PendingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PendingViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class PendingViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textAssetType;
        private final TextView textPlatform;
        private final TextView textValue;
        private final TextView textInvested;
        private final MaterialButton btnAccept;
        private final MaterialButton btnReject;
        private final android.widget.ImageView btnInfo;

        PendingViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_pending_name);
            textAssetType = itemView.findViewById(R.id.text_pending_asset_type);
            textPlatform = itemView.findViewById(R.id.text_pending_platform);
            textValue = itemView.findViewById(R.id.text_pending_value);
            textInvested = itemView.findViewById(R.id.text_pending_invested);
            btnAccept = itemView.findViewById(R.id.btn_pending_accept);
            btnReject = itemView.findViewById(R.id.btn_pending_reject);
            btnInfo = itemView.findViewById(R.id.btn_pending_info);
        }

        void bind(Holding inv, PendingActionListener listener) {
            Context context = itemView.getContext();
            textName.setText(inv.getName());
            textAssetType.setText(formatAssetType(inv.getAssetType()));
            textValue.setText(formatCurrency(inv.getCurrentValueCents()));
            textInvested.setText(context.getString(
                    R.string.label_invested_format,
                    formatCurrency(inv.getInvestedAmountCents())));

            String platform = inv.getPlatform();
            if (platform != null && !platform.isEmpty()) {
                textPlatform.setText(platform);
                textPlatform.setVisibility(View.VISIBLE);
            } else {
                textPlatform.setVisibility(View.GONE);
            }

            btnAccept.setOnClickListener(v -> {
                if (listener != null)
                    listener.onAccept(inv);
            });
            btnReject.setOnClickListener(v -> {
                if (listener != null)
                    listener.onReject(inv);
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
                case Holding.ASSET_STOCK:
                    return "Stock";
                case Holding.ASSET_MF:
                    return "Mutual Fund";
                case Holding.ASSET_ETF:
                    return "ETF";
                case Holding.ASSET_FD:
                    return "Fixed Deposit";
                case Holding.ASSET_RD:
                    return "Recurring Deposit";
                case Holding.ASSET_PPF:
                    return "PPF";
                case Holding.ASSET_NPS:
                    return "NPS";
                case Holding.ASSET_GOLD:
                    return "Gold";
                case Holding.ASSET_SAVINGS:
                    return "Savings";
                default:
                    return "Other";
            }
        }
    }
}
