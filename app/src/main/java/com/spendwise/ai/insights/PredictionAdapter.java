package com.spendwise.ai.insights;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.spendwise.ai.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for prediction cards on the Dashboard.
 */
public class PredictionAdapter extends RecyclerView.Adapter<PredictionAdapter.PredictionViewHolder> {

    private final List<Prediction> predictions = new ArrayList<>();

    public void setPredictions(List<Prediction> newPredictions) {
        predictions.clear();
        predictions.addAll(newPredictions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PredictionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prediction, parent, false);
        return new PredictionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PredictionViewHolder holder, int position) {
        holder.bind(predictions.get(position));
    }

    @Override
    public int getItemCount() {
        return predictions.size();
    }

    static class PredictionViewHolder extends RecyclerView.ViewHolder {

        private final TextView labelView;
        private final TextView amountView;
        private final TextView descriptionView;
        private final ProgressBar confidenceBar;
        private final TextView confidenceText;
        private final MaterialCardView card;

        PredictionViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_prediction);
            labelView = itemView.findViewById(R.id.prediction_label);
            amountView = itemView.findViewById(R.id.prediction_amount);
            descriptionView = itemView.findViewById(R.id.prediction_description);
            confidenceBar = itemView.findViewById(R.id.prediction_confidence_bar);
            confidenceText = itemView.findViewById(R.id.prediction_confidence_text);
        }

        void bind(Prediction prediction) {
            labelView.setText(prediction.getLabel());
            amountView.setText(prediction.getFormattedAmount());
            descriptionView.setText(prediction.getDescription());
            confidenceBar.setProgress(prediction.getConfidencePercent());
            confidenceText.setText(prediction.getConfidencePercent() + "% confidence");

            // Set card border color based on prediction type
            int strokeColor;
            switch (prediction.getType()) {
                case BUDGET_RISK:
                    strokeColor = card.getContext().getColor(R.color.insight_warning);
                    break;
                default:
                    strokeColor = card.getContext().getColor(R.color.insight_info);
                    break;
            }
            card.setStrokeColor(strokeColor);
        }
    }
}
