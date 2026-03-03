package com.spendwise.ai.buckets;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.pojo.BucketWithTotal;

/**
 * RecyclerView adapter for bucket cards on the main Buckets dashboard.
 * Each card shows: name, timeline, current value, P&L, advice tag, progress
 * bar.
 */
public class BucketsAdapter extends ListAdapter<BucketWithTotal, BucketsAdapter.BucketViewHolder> {

    public interface OnBucketClickListener {
        void onBucketClick(BucketWithTotal bucketWithTotal);
    }

    private final OnBucketClickListener listener;

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
        holder.bind(item, listener);
    }

    static class BucketViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textTimeline;
        private final TextView textValue;
        private final TextView textPnl;
        private final TextView textAdvice;
        private final ProgressBar progressBar;
        private final View colorStrip;
        private final TextView textGuiltFree;

        BucketViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_bucket_name);
            textTimeline = itemView.findViewById(R.id.text_bucket_timeline);
            textValue = itemView.findViewById(R.id.text_bucket_value);
            textPnl = itemView.findViewById(R.id.text_bucket_pnl);
            textAdvice = itemView.findViewById(R.id.text_bucket_advice);
            progressBar = itemView.findViewById(R.id.progress_bucket);
            colorStrip = itemView.findViewById(R.id.view_color_strip);
            textGuiltFree = itemView.findViewById(R.id.text_guilt_free);
        }

        void bind(BucketWithTotal item, OnBucketClickListener listener) {
            Bucket bucket = item.bucket;
            textName.setText(bucket.getName());

            // Timeline label
            String timeline = bucket.getTimelineLabel();
            textTimeline.setText(timeline != null ? timeline : "");
            textTimeline.setVisibility(timeline != null ? View.VISIBLE : View.GONE);

            // Current value (per-bucket, NOT total portfolio)
            textValue.setText(String.format("₹%,.0f", item.totalValueCents / 100.0));

            // P&L
            long pnl = item.getProfitLossCents();
            double returnPct = item.getReturnPercent();
            if (item.totalInvestedCents > 0) {
                String pnlSign = pnl >= 0 ? "+" : "";
                textPnl.setText(String.format("%s₹%,.0f (%.1f%%)", pnlSign, pnl / 100.0, returnPct));
                textPnl.setTextColor(pnl >= 0 ? 0xFF43A047 : 0xFFE53935);
                textPnl.setVisibility(View.VISIBLE);
            } else {
                textPnl.setVisibility(View.GONE);
            }

            // Investment advice tag
            String advice = bucket.getInvestmentAdvice();
            textAdvice.setText(advice != null ? advice : "");
            textAdvice.setVisibility(advice != null ? View.VISIBLE : View.GONE);

            // Progress bar (allocation percentage)
            progressBar.setMax(100);
            progressBar.setProgress(bucket.getTargetPercent());

            // Color strip
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(bucket.getColorArgb());
            bg.setCornerRadius(8f);
            colorStrip.setBackground(bg);

            // Guilt-free banner (only for short-term bucket)
            if (Bucket.TYPE_SHORT_TERM.equals(bucket.getType())) {
                textGuiltFree.setVisibility(View.VISIBLE);
                textGuiltFree.setText(String.format(
                        "Spend ₹%,.0f guilt-free — your future is funded ✓",
                        item.totalValueCents / 100.0));
            } else {
                textGuiltFree.setVisibility(View.GONE);
            }

            // Click listener → navigate to detail
            itemView.setOnClickListener(v -> {
                if (listener != null)
                    listener.onBucketClick(item);
            });
        }
    }
}
