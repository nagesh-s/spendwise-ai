package com.spendwise.ai.buckets;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.pojo.BucketWithTotal;

import java.util.Locale;

/**
 * RecyclerView adapter for bucket cards on the main Buckets dashboard.
 * Each card shows: name, timeline, current value, P&L, advice tag, progress
 * bar.
 */
public class BucketsAdapter extends ListAdapter<BucketWithTotal, BucketsAdapter.BucketViewHolder> {

    public enum LedgerMode {
        PLANNED,
        ACTUAL
    }

    public interface OnBucketClickListener {
        void onBucketClick(BucketWithTotal bucketWithTotal);
    }

    private final OnBucketClickListener listener;
    @NonNull
    private LedgerMode ledgerMode = LedgerMode.ACTUAL;

    public BucketsAdapter(OnBucketClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<BucketWithTotal> DIFF_CALLBACK = new DiffUtil.ItemCallback<BucketWithTotal>() {
        @Override
        public boolean areItemsTheSame(@NonNull BucketWithTotal o, @NonNull BucketWithTotal n) {
            return o.bucket.getId() == n.bucket.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull BucketWithTotal o, @NonNull BucketWithTotal n) {
            return o.bucket.getName().equals(n.bucket.getName())
                    && o.totalValueCents == n.totalValueCents
                    && o.totalInvestedCents == n.totalInvestedCents
                    && o.bucket.getCurrentBalanceCents() == n.bucket.getCurrentBalanceCents()
                    && o.bucket.getTargetPercent() == n.bucket.getTargetPercent();
        }
    };

    @NonNull
    @Override
    public BucketViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bucket_card, parent, false);
        return new BucketViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BucketViewHolder holder, int position) {
        BucketWithTotal item = getItem(position);
        holder.bind(item, listener, ledgerMode);
    }

    public void setLedgerMode(@NonNull LedgerMode mode) {
        if (ledgerMode == mode) {
            return;
        }
        ledgerMode = mode;
        notifyDataSetChanged();
    }

    static class BucketViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardRoot;
        private final TextView textName;
        private final TextView textTargetPercent;
        private final TextView textTimeline;
        private final TextView textPlannedValue;
        private final TextView textActualValue;
        private final TextView textInvested;
        private final TextView textPnl;
        private final TextView textTargetHint;
        private final LinearProgressIndicator progressBar;
        private final View colorStrip;
        private final TextView textGuiltFree;
        private final MaterialCardView guiltFreeCard;

        BucketViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_bucket_root);
            textName = itemView.findViewById(R.id.text_bucket_name);
            textTargetPercent = itemView.findViewById(R.id.text_bucket_target_percent);
            textTimeline = itemView.findViewById(R.id.text_bucket_timeline);
            textPlannedValue = itemView.findViewById(R.id.text_bucket_planned_value);
            textActualValue = itemView.findViewById(R.id.text_bucket_actual_value);
            textInvested = itemView.findViewById(R.id.text_bucket_invested);
            textPnl = itemView.findViewById(R.id.text_bucket_pnl);
            textTargetHint = itemView.findViewById(R.id.text_bucket_target_hint);
            progressBar = itemView.findViewById(R.id.progress_bucket);
            colorStrip = itemView.findViewById(R.id.view_color_strip);
            textGuiltFree = itemView.findViewById(R.id.text_guilt_free);
            guiltFreeCard = itemView.findViewById(R.id.card_guilt_free);
        }

        void bind(BucketWithTotal item, OnBucketClickListener listener, LedgerMode mode) {
            Context context = itemView.getContext();
            Bucket bucket = item.bucket;
            textName.setText(bucket.getName());
            textTargetPercent.setText(String.format(Locale.getDefault(), "%d%%", bucket.getTargetPercent()));

            String timeline = bucket.getTimelineLabel();
            textTimeline.setText(timeline != null ? timeline : "");
            textTimeline.setVisibility(timeline != null ? View.VISIBLE : View.GONE);

            textPlannedValue.setText(formatCurrency(bucket.getCurrentBalanceCents()));
            textActualValue.setText(formatCurrency(item.totalValueCents));
            applyLedgerEmphasis(context, mode);
            textInvested.setText(context.getString(
                    R.string.label_invested_format,
                    formatCurrency(item.totalInvestedCents)));

            long pnl = item.getProfitLossCents();
            double returnPct = item.getReturnPercent();
            if (item.totalInvestedCents > 0) {
                String pnlSign = pnl >= 0 ? "+" : "";
                textPnl.setText(String.format(Locale.getDefault(), "%s%s (%.1f%%)",
                        pnlSign, formatCurrency(pnl), returnPct));
                textPnl.setTextColor(pnl >= 0
                        ? ContextCompat.getColor(context, R.color.sw_income)
                        : ContextCompat.getColor(context, R.color.sw_error));
                textPnl.setVisibility(View.VISIBLE);
            } else {
                textPnl.setVisibility(View.GONE);
            }

            progressBar.setMax(100);
            progressBar.setProgress(bucket.getTargetPercent());
            progressBar.setIndicatorColor(bucket.getColorArgb());
            textTargetHint.setText(context.getString(
                    R.string.bucket_salary_target_format,
                    bucket.getTargetPercent()));

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bucket.getColorArgb());
            bg.setCornerRadius(8f);
            colorStrip.setBackground(bg);
            cardRoot.setStrokeColor(ColorUtils.setAlphaComponent(bucket.getColorArgb(), 110));

            if (Bucket.TYPE_SHORT_TERM.equals(bucket.getType()) && item.totalValueCents > 0) {
                guiltFreeCard.setVisibility(View.VISIBLE);
                textGuiltFree.setVisibility(View.VISIBLE);
                textGuiltFree.setText(context.getString(
                        R.string.bucket_guilt_free_banner_format,
                        formatCurrency(bucket.getCurrentBalanceCents())));
            } else {
                guiltFreeCard.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onBucketClick(item);
            });
        }

        private void applyLedgerEmphasis(@NonNull Context context, @NonNull LedgerMode mode) {
            int strongColor = ContextCompat.getColor(context, R.color.md_theme_onSurface);
            int softColor = ContextCompat.getColor(context, R.color.md_theme_onSurfaceVariant);
            if (mode == LedgerMode.PLANNED) {
                textPlannedValue.setTextColor(strongColor);
                textPlannedValue.setAlpha(1f);
                textActualValue.setTextColor(softColor);
                textActualValue.setAlpha(0.75f);
            } else {
                textActualValue.setTextColor(strongColor);
                textActualValue.setAlpha(1f);
                textPlannedValue.setTextColor(softColor);
                textPlannedValue.setAlpha(0.75f);
            }
        }

        private String formatCurrency(long amountCents) {
            return String.format(Locale.getDefault(), "₹%,.0f", amountCents / 100.0);
        }
    }
}
