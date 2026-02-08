package com.spendwise.ai.categories;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.pojo.BudgetWithSpend;
import com.spendwise.ai.util.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying categories with budget progress.
 */
public class CategoryBudgetAdapter extends RecyclerView.Adapter<CategoryBudgetAdapter.ViewHolder> {

    private List<Category> categories = new ArrayList<>();
    private Map<Long, BudgetWithSpend> budgetMap = new HashMap<>();
    private OnBudgetClickListener listener;

    public interface OnBudgetClickListener {
        void onBudgetClick(Category category, BudgetWithSpend budget);
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        notifyDataSetChanged();
    }

    public void setBudgets(List<BudgetWithSpend> budgets) {
        budgetMap.clear();
        if (budgets != null) {
            for (BudgetWithSpend budget : budgets) {
                budgetMap.put(budget.getCategoryId(), budget);
            }
        }
        notifyDataSetChanged();
    }

    public void setOnBudgetClickListener(OnBudgetClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_budget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        BudgetWithSpend budget = budgetMap.get(category.getId());
        holder.bind(category, budget);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final View colorView;
        private final TextView nameText;
        private final TextView defaultBadge;
        private final View budgetLayout;
        private final TextView spentText;
        private final TextView percentText;
        private final LinearProgressIndicator progressBar;
        private final MaterialButton setBudgetBtn;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorView = itemView.findViewById(R.id.view_category_color);
            nameText = itemView.findViewById(R.id.text_category_name);
            defaultBadge = itemView.findViewById(R.id.text_default_badge);
            budgetLayout = itemView.findViewById(R.id.layout_budget);
            spentText = itemView.findViewById(R.id.text_budget_spent);
            percentText = itemView.findViewById(R.id.text_budget_percent);
            progressBar = itemView.findViewById(R.id.progress_budget);
            setBudgetBtn = itemView.findViewById(R.id.btn_set_budget);
        }

        void bind(Category category, BudgetWithSpend budget) {
            nameText.setText(category.getName());

            // Color indicator
            GradientDrawable background = (GradientDrawable) colorView.getBackground();
            if (background != null) {
                background.setColor(category.getColorArgb());
            }

            // Default badge
            defaultBadge.setVisibility(category.isDefault() ? View.VISIBLE : View.GONE);

            if (budget != null && budget.getLimitCents() > 0) {
                // Show budget progress
                budgetLayout.setVisibility(View.VISIBLE);
                setBudgetBtn.setText("Edit Budget");

                String spent = DateUtils.formatAmountCents(budget.getSpentCents());
                String limit = DateUtils.formatAmountCents(budget.getLimitCents());
                spentText.setText(spent + " of " + limit);

                int percent = budget.getUsagePercent();
                percentText.setText(percent + "%");
                progressBar.setProgress(Math.min(percent, 100));

                // Color progress bar based on usage
                if (percent >= 100) {
                    progressBar.setIndicatorColor(0xFFF44336); // Red
                } else if (percent >= 80) {
                    progressBar.setIndicatorColor(0xFFFF9800); // Orange
                } else {
                    progressBar.setIndicatorColor(0xFF4CAF50); // Green
                }
            } else {
                // No budget set
                budgetLayout.setVisibility(View.GONE);
                setBudgetBtn.setText("Set Budget");
            }

            setBudgetBtn.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBudgetClick(category, budget);
                }
            });
        }
    }
}
