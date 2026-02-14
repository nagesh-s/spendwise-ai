package com.spendwise.ai.insights;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.spendwise.ai.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for insight cards displayed on the Dashboard.
 */
public class InsightsAdapter extends RecyclerView.Adapter<InsightsAdapter.InsightViewHolder> {

    private final List<Insight> insights = new ArrayList<>();
    private OnInsightActionListener actionListener;

    public interface OnInsightActionListener {
        void onActionClicked(Insight insight);

        void onDismissed(Insight insight, int position);
    }

    public void setActionListener(OnInsightActionListener listener) {
        this.actionListener = listener;
    }

    public void setInsights(List<Insight> newInsights) {
        insights.clear();
        insights.addAll(newInsights);
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < insights.size()) {
            insights.remove(position);
            notifyItemRemoved(position);
        }
    }

    @NonNull
    @Override
    public InsightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_insight, parent, false);
        return new InsightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InsightViewHolder holder, int position) {
        Insight insight = insights.get(position);
        holder.bind(insight, position);
    }

    @Override
    public int getItemCount() {
        return insights.size();
    }

    class InsightViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final ImageView iconView;
        private final TextView titleView;
        private final TextView descriptionView;
        private final MaterialButton actionButton;
        private final ImageView dismissButton;

        InsightViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_insight);
            iconView = itemView.findViewById(R.id.insight_icon);
            titleView = itemView.findViewById(R.id.insight_title);
            descriptionView = itemView.findViewById(R.id.insight_description);
            actionButton = itemView.findViewById(R.id.insight_action);
            dismissButton = itemView.findViewById(R.id.insight_dismiss);
        }

        void bind(Insight insight, int position) {
            iconView.setImageResource(insight.getIconRes());
            titleView.setText(insight.getTitle());
            descriptionView.setText(insight.getDescription());

            // Set card stroke color based on severity
            int strokeColor;
            switch (insight.getSeverity()) {
                case WARNING:
                    strokeColor = card.getContext().getColor(R.color.insight_warning);
                    break;
                case POSITIVE:
                    strokeColor = card.getContext().getColor(R.color.insight_positive);
                    break;
                default:
                    strokeColor = card.getContext().getColor(R.color.insight_info);
                    break;
            }
            card.setStrokeColor(strokeColor);

            // Action button
            if (insight.getActionLabel() != null) {
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(insight.getActionLabel());
                actionButton.setOnClickListener(v -> {
                    if (actionListener != null)
                        actionListener.onActionClicked(insight);
                });
            } else {
                actionButton.setVisibility(View.GONE);
            }

            // Dismiss button
            dismissButton.setOnClickListener(v -> {
                if (actionListener != null)
                    actionListener.onDismissed(insight, position);
                removeAt(position);
            });
        }
    }
}
