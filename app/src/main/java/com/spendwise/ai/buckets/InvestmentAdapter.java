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
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.pojo.HoldingWithProvider;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

/**
 * RecyclerView adapter for investments inside a bucket detail screen.
 * Shows Provider headers and Holding items.
 */
public class InvestmentAdapter extends ListAdapter<Object, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnInvestmentClickListener {
        void onInvestmentClick(Holding investment);
        void onInfoClick(Holding investment);
    }

    private final OnInvestmentClickListener listener;

    public InvestmentAdapter(OnInvestmentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Object> DIFF_CALLBACK = new DiffUtil.ItemCallback<Object>() {
        @Override
        public boolean areItemsTheSame(@NonNull Object o, @NonNull Object n) {
            if (o instanceof BucketDetailViewModel.ProviderHeader && n instanceof BucketDetailViewModel.ProviderHeader) {
                BucketDetailViewModel.ProviderHeader oHead = (BucketDetailViewModel.ProviderHeader) o;
                BucketDetailViewModel.ProviderHeader nHead = (BucketDetailViewModel.ProviderHeader) n;
                return oHead.headerTitle.equals(nHead.headerTitle);
            }
            if (o instanceof HoldingWithProvider && n instanceof HoldingWithProvider) {
                HoldingWithProvider oHold = (HoldingWithProvider) o;
                HoldingWithProvider nHold = (HoldingWithProvider) n;
                return oHold.holding.getId() == nHold.holding.getId();
            }
            return false;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Object o, @NonNull Object n) {
            if (o instanceof BucketDetailViewModel.ProviderHeader && n instanceof BucketDetailViewModel.ProviderHeader) {
                BucketDetailViewModel.ProviderHeader oHead = (BucketDetailViewModel.ProviderHeader) o;
                BucketDetailViewModel.ProviderHeader nHead = (BucketDetailViewModel.ProviderHeader) n;
                return oHead.totalValueCents == nHead.totalValueCents;
            }
            if (o instanceof HoldingWithProvider && n instanceof HoldingWithProvider) {
                Holding oHold = ((HoldingWithProvider) o).holding;
                Holding nHold = ((HoldingWithProvider) n).holding;
                return oHold.getName().equals(nHold.getName())
                        && oHold.getCurrentValueCents() == nHold.getCurrentValueCents()
                        && oHold.getInvestedAmountCents() == nHold.getInvestedAmountCents();
            }
            return false;
        }
    };

    @Override
    public int getItemViewType(int position) {
        Object item = getItem(position);
        if (item instanceof BucketDetailViewModel.ProviderHeader) {
            return VIEW_TYPE_HEADER;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_provider_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_investment, parent, false);
            return new InvestmentViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = getItem(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind((BucketDetailViewModel.ProviderHeader) item);
        } else if (holder instanceof InvestmentViewHolder) {
            ((InvestmentViewHolder) holder).bind(((HoldingWithProvider) item).holding, listener);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView textProviderName;
        private final TextView textProviderTotal;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textProviderName = itemView.findViewById(R.id.text_provider_name);
            textProviderTotal = itemView.findViewById(R.id.text_provider_total);
        }

        void bind(BucketDetailViewModel.ProviderHeader header) {
            textProviderName.setText(header.headerTitle);
            textProviderTotal.setText(String.format(Locale.getDefault(), "₹%,.0f", header.totalValueCents / 100.0));
        }
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

        void bind(Holding inv, OnInvestmentClickListener listener) {
            Context context = itemView.getContext();
            textName.setText(inv.getName());

            String jobTitle = inv.getJobTitle();
            if (jobTitle != null && !jobTitle.isEmpty()) {
                textJobTitle.setText(jobTitle);
                textJobTitle.setVisibility(View.VISIBLE);
            } else {
                textJobTitle.setVisibility(View.GONE);
            }

            // We can hide the individual platform text since it's grouped now, 
            // but we'll leave it in case a user uses a generic provider like "OTHER" 
            // and wants to see the specific platform text.
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
