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
public class InvestmentAdapter extends ListAdapter<BucketDetailViewModel.ProviderGroup, InvestmentAdapter.GroupViewHolder> {

    public interface OnInvestmentClickListener {
        void onInvestmentClick(Holding investment);
        void onInfoClick(long transactionId);
    }

    private final OnInvestmentClickListener listener;

    public InvestmentAdapter(OnInvestmentClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<BucketDetailViewModel.ProviderGroup> DIFF_CALLBACK = new DiffUtil.ItemCallback<BucketDetailViewModel.ProviderGroup>() {
        @Override
        public boolean areItemsTheSame(@NonNull BucketDetailViewModel.ProviderGroup o, @NonNull BucketDetailViewModel.ProviderGroup n) {
            return o.headerTitle.equals(n.headerTitle);
        }

        @Override
        public boolean areContentsTheSame(@NonNull BucketDetailViewModel.ProviderGroup oldItem, @NonNull BucketDetailViewModel.ProviderGroup newItem) {
            if (oldItem.provider != null && newItem.provider != null) {
                if (oldItem.provider.getId() != newItem.provider.getId()) return false;
            } else if (oldItem.provider != newItem.provider) {
                return false;
            }
            if (oldItem.holdings.size() != newItem.holdings.size()) return false;
            for (int i = 0; i < oldItem.holdings.size(); i++) {
                if (oldItem.holdings.get(i).holding.getId() != newItem.holdings.get(i).holding.getId()) {
                    return false;
                }
            }
            return true;
        }
    };

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider_group_card, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        private final TextView textProviderName;
        private final TextView textProviderTotal;
        private final android.widget.LinearLayout containerHoldings;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            textProviderName = itemView.findViewById(R.id.text_provider_name);
            textProviderTotal = itemView.findViewById(R.id.text_provider_total);
            containerHoldings = itemView.findViewById(R.id.container_holdings);
        }

        void bind(BucketDetailViewModel.ProviderGroup group, OnInvestmentClickListener listener) {
            textProviderName.setText(group.headerTitle);
            textProviderTotal.setText(String.format(Locale.getDefault(), "₹%,.0f", group.totalValueCents / 100.0));

            containerHoldings.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
            
            for (int i = 0; i < group.holdings.size(); i++) {
                com.spendwise.ai.data.pojo.HoldingWithTransactionsAndProvider hp = group.holdings.get(i);
                View holdView = inflater.inflate(R.layout.item_investment, containerHoldings, false);
                
                // Add bottom margin to inner items except the last one (if we wanted to, but LinearLayout handles padding inside the item)
                
                InvestmentViewHolder holdHolder = new InvestmentViewHolder(holdView);
                holdHolder.bind(hp.holding, hp.transactions, listener);
                
                containerHoldings.addView(holdView);
                
                // Optional divider
                if (i < group.holdings.size() - 1) {
                    View divider = new View(itemView.getContext());
                    android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 1);
                    lp.setMargins(0, 0, 0, 0); // Remove vertical margins if item_investment handles its own padding
                    divider.setLayoutParams(lp);
                    // Use a slightly softer divider color than the main outline
                    divider.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.md_theme_surfaceVariant));
                    containerHoldings.addView(divider);
                }
            }
        }
    }

    static class InvestmentViewHolder {
        private final View itemView;
        private final TextView textName;
        private final TextView textJobTitle;
        private final TextView textPlatform;
        private final TextView textValue;
        private final TextView textInvested;
        private final TextView textPnl;
        private final TextView textAssetType;
        private final TextView textUpdated;
        private final android.widget.LinearLayout containerTransactions;
        private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        InvestmentViewHolder(@NonNull View itemView) {
            this.itemView = itemView;
            textName = itemView.findViewById(R.id.text_investment_name);
            textJobTitle = itemView.findViewById(R.id.text_investment_job_title);
            textPlatform = itemView.findViewById(R.id.text_investment_platform);
            textValue = itemView.findViewById(R.id.text_investment_value);
            textInvested = itemView.findViewById(R.id.text_investment_invested);
            textPnl = itemView.findViewById(R.id.text_investment_pnl);
            textAssetType = itemView.findViewById(R.id.text_investment_asset_type);
            textUpdated = itemView.findViewById(R.id.text_investment_updated);
            containerTransactions = itemView.findViewById(R.id.container_transactions);
        }

        void bind(Holding inv, java.util.List<com.spendwise.ai.data.entity.InvestmentTransaction> transactions, OnInvestmentClickListener listener) {
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

            containerTransactions.removeAllViews();
            if (transactions != null && !transactions.isEmpty()) {
                containerTransactions.setVisibility(View.VISIBLE);
                android.view.LayoutInflater inflater = android.view.LayoutInflater.from(context);
                for (com.spendwise.ai.data.entity.InvestmentTransaction t : transactions) {
                    View tView = inflater.inflate(R.layout.item_investment_transaction, containerTransactions, false);
                    TextView tAmount = tView.findViewById(R.id.text_transaction_amount);
                    TextView tDate = tView.findViewById(R.id.text_transaction_date);
                    TextView tStatus = tView.findViewById(R.id.text_transaction_status);
                    android.widget.ImageView tInfo = tView.findViewById(R.id.btn_transaction_info);

                    tAmount.setText(formatCurrency(t.getAmountCents()));
                    tDate.setText(org.threeten.bp.Instant.ofEpochMilli(t.getEventTimestampMillis())
                            .atZone(org.threeten.bp.ZoneId.systemDefault())
                            .toLocalDate()
                            .format(dateFormatter));
                    
                    tStatus.setText(t.getStatus());
                    
                    // Simple styling based on status
                    if ("CONFIRMED".equals(t.getStatus())) {
                        tStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.sw_income));
                    } else if ("CANCELLED".equals(t.getStatus())) {
                        tStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.sw_error));
                    } else {
                        tStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.md_theme_primary));
                    }

                    tInfo.setOnClickListener(v -> {
                        if (listener != null) listener.onInfoClick(t.getId());
                    });

                    containerTransactions.addView(tView);
                }
            } else {
                containerTransactions.setVisibility(View.GONE);
            }
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
