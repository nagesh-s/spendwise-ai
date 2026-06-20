package com.spendwise.ai.review;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.pojo.ReviewCandidateWithEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReviewQueueAdapter extends ListAdapter<ReviewCandidateWithEvent, ReviewQueueAdapter.ViewHolder> {

    public interface OnCandidateActionListener {
        void onConfirmSuggestion(ReviewCandidateWithEvent item);
        void onChangeRequested(ReviewCandidateWithEvent item);
        void onIgnore(ReviewCandidateWithEvent item);
    }

    private final OnCandidateActionListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US);

    public ReviewQueueAdapter(OnCandidateActionListener listener) {
        super(new DiffUtil.ItemCallback<ReviewCandidateWithEvent>() {
            @Override
            public boolean areItemsTheSame(@NonNull ReviewCandidateWithEvent oldItem, @NonNull ReviewCandidateWithEvent newItem) {
                return oldItem.candidate.getId() == newItem.candidate.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull ReviewCandidateWithEvent oldItem, @NonNull ReviewCandidateWithEvent newItem) {
                return oldItem.candidate.getStatus().equals(newItem.candidate.getStatus());
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_candidate, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReviewCandidateWithEvent item = getItem(position);
        
        holder.textSender.setText(item.ingestionEvent.getSourceSender() != null ? item.ingestionEvent.getSourceSender() : "Unknown Sender");
        holder.textDate.setText(dateFormat.format(new Date(item.ingestionEvent.getEventTimestampMillis())));
        holder.textRawMessage.setText(item.ingestionEvent.getSourceBody());

        if (item.candidate.getProposedHoldingId() != null) {
            holder.layoutSuggestion.setVisibility(View.VISIBLE);
            String amountStr = String.format(Locale.US, "₹%.2f", item.ingestionEvent.getAmountCents() / 100.0);
            holder.textSuggestionPrompt.setText(String.format("Did you invest %s based on this message?\n%s", 
                amountStr, item.candidate.getProposedInterpretation()));
            holder.btnConfirm.setVisibility(View.VISIBLE);
            holder.btnChange.setText("No, Change");
        } else {
            holder.layoutSuggestion.setVisibility(View.GONE);
            holder.btnConfirm.setVisibility(View.GONE);
            holder.btnChange.setText("Select Bucket");
        }

        holder.btnConfirm.setOnClickListener(v -> listener.onConfirmSuggestion(item));
        holder.btnChange.setOnClickListener(v -> listener.onChangeRequested(item));
        holder.btnIgnore.setOnClickListener(v -> listener.onIgnore(item));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textSender;
        TextView textDate;
        TextView textRawMessage;
        LinearLayout layoutSuggestion;
        TextView textSuggestionPrompt;
        Button btnIgnore;
        Button btnChange;
        Button btnConfirm;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textSender = itemView.findViewById(R.id.textSender);
            textDate = itemView.findViewById(R.id.textDate);
            textRawMessage = itemView.findViewById(R.id.textRawMessage);
            layoutSuggestion = itemView.findViewById(R.id.layoutSuggestion);
            textSuggestionPrompt = itemView.findViewById(R.id.textSuggestionPrompt);
            btnIgnore = itemView.findViewById(R.id.btnIgnore);
            btnChange = itemView.findViewById(R.id.btnChange);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
        }
    }
}
