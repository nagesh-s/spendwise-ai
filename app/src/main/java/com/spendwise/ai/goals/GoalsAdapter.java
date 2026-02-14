package com.spendwise.ai.goals;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.FinancialGoal;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

/**
 * RecyclerView adapter for displaying financial goals with progress.
 */
public class GoalsAdapter extends ListAdapter<FinancialGoal, GoalsAdapter.GoalViewHolder> {

    public interface OnGoalClickListener {
        void onGoalClick(FinancialGoal goal);
    }

    private final OnGoalClickListener listener;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public GoalsAdapter(OnGoalClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<FinancialGoal> DIFF_CALLBACK = new DiffUtil.ItemCallback<FinancialGoal>() {
        @Override
        public boolean areItemsTheSame(@NonNull FinancialGoal oldItem, @NonNull FinancialGoal newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull FinancialGoal oldItem, @NonNull FinancialGoal newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getCurrentAmountCents() == newItem.getCurrentAmountCents()
                    && oldItem.getTargetAmountCents() == newItem.getTargetAmountCents()
                    && oldItem.getStatus().equals(newItem.getStatus());
        }
    };

    @NonNull
    @Override
    public GoalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_goal, parent, false);
        return new GoalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalViewHolder holder, int position) {
        FinancialGoal goal = getItem(position);
        holder.bind(goal);
    }

    class GoalViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameText;
        private final TextView typeText;
        private final LinearProgressIndicator progressBar;
        private final TextView currentText;
        private final TextView targetText;
        private final TextView deadlineText;

        GoalViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.text_goal_name);
            typeText = itemView.findViewById(R.id.text_goal_type);
            progressBar = itemView.findViewById(R.id.progress_goal);
            currentText = itemView.findViewById(R.id.text_goal_current);
            targetText = itemView.findViewById(R.id.text_goal_target);
            deadlineText = itemView.findViewById(R.id.text_goal_deadline);
        }

        void bind(FinancialGoal goal) {
            nameText.setText(goal.getName());

            // Type display
            switch (goal.getType()) {
                case FinancialGoal.TYPE_SAVINGS:
                    typeText.setText("Savings");
                    break;
                case FinancialGoal.TYPE_SPENDING_LIMIT:
                    typeText.setText("Spending Limit");
                    break;
                case FinancialGoal.TYPE_DEBT_PAYOFF:
                    typeText.setText("Debt Payoff");
                    break;
                default:
                    typeText.setText(goal.getType());
            }

            // Progress
            int progress = goal.getProgressPercent();
            progressBar.setProgress(progress);
            if (goal.getColorArgb() != 0) {
                progressBar.setIndicatorColor(goal.getColorArgb());
            }

            // Amounts
            currentText.setText(String.format(Locale.getDefault(), "₹%,.2f",
                    goal.getCurrentAmountCents() / 100.0));
            targetText.setText(String.format(Locale.getDefault(), "of ₹%,.2f",
                    goal.getTargetAmountCents() / 100.0));

            // Deadline
            if (goal.getEndDateEpochDay() != null) {
                LocalDate endDate = LocalDate.ofEpochDay(goal.getEndDateEpochDay());
                deadlineText.setText("Deadline: " + endDate.format(dateFormatter));
                deadlineText.setVisibility(View.VISIBLE);
            } else {
                deadlineText.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onGoalClick(goal);
                }
            });
        }
    }
}
