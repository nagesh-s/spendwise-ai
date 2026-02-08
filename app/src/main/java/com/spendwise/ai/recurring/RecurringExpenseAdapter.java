package com.spendwise.ai.recurring;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.RecurringExpense;
import com.spendwise.ai.util.DateUtils;

/**
 * Adapter for displaying recurring expense items.
 */
public class RecurringExpenseAdapter extends ListAdapter<RecurringExpense, RecurringExpenseAdapter.ViewHolder> {

    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(RecurringExpense expense);

        void onToggleActive(RecurringExpense expense);

        void onDelete(RecurringExpense expense);
    }

    public RecurringExpenseAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<RecurringExpense> DIFF_CALLBACK = new DiffUtil.ItemCallback<RecurringExpense>() {
        @Override
        public boolean areItemsTheSame(@NonNull RecurringExpense oldItem, @NonNull RecurringExpense newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull RecurringExpense oldItem, @NonNull RecurringExpense newItem) {
            return oldItem.getDescription().equals(newItem.getDescription()) &&
                    oldItem.getAmountCents() == newItem.getAmountCents() &&
                    oldItem.getFrequency().equals(newItem.getFrequency()) &&
                    oldItem.isActive() == newItem.isActive();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recurring_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecurringExpense expense = getItem(position);
        holder.bind(expense, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView descriptionText;
        private final TextView amountText;
        private final TextView frequencyText;
        private final SwitchMaterial activeSwitch;
        private final ImageButton deleteButton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            descriptionText = itemView.findViewById(R.id.text_description);
            amountText = itemView.findViewById(R.id.text_amount);
            frequencyText = itemView.findViewById(R.id.text_frequency);
            activeSwitch = itemView.findViewById(R.id.switch_active);
            deleteButton = itemView.findViewById(R.id.btn_delete);
        }

        void bind(RecurringExpense expense, OnItemClickListener listener) {
            descriptionText.setText(expense.getDescription());
            amountText.setText(DateUtils.formatAmountCents(expense.getAmountCents()));
            frequencyText.setText(formatFrequency(expense));
            activeSwitch.setChecked(expense.isActive());

            itemView.setOnClickListener(v -> listener.onItemClick(expense));
            activeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != expense.isActive()) {
                    listener.onToggleActive(expense);
                }
            });
            deleteButton.setOnClickListener(v -> listener.onDelete(expense));
        }

        private String formatFrequency(RecurringExpense expense) {
            switch (expense.getFrequency()) {
                case RecurringExpense.FREQUENCY_DAILY:
                    return "Daily";
                case RecurringExpense.FREQUENCY_WEEKLY:
                    return "Weekly";
                case RecurringExpense.FREQUENCY_MONTHLY:
                    return "Monthly (Day " + expense.getDayOfPeriod() + ")";
                case RecurringExpense.FREQUENCY_YEARLY:
                    return "Yearly";
                default:
                    return expense.getFrequency();
            }
        }
    }
}
