package com.spendwise.ai.common;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Category;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.util.DateUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying expenses in a RecyclerView.
 * Used in Calendar to show expenses for selected date.
 */
public class ExpenseAdapter extends ListAdapter<Expense, ExpenseAdapter.ViewHolder> {

    private Map<Long, Category> categoryMap = new HashMap<>();
    private Map<Long, String> merchantMap = new HashMap<>();
    private OnExpenseClickListener clickListener;

    public interface OnExpenseClickListener {
        void onExpenseClick(Expense expense);
    }

    public ExpenseAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Expense> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Expense>() {
                @Override
                public boolean areItemsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Expense oldItem, @NonNull Expense newItem) {
                    return oldItem.getAmountCents() == newItem.getAmountCents() &&
                           oldItem.getCategoryId() == newItem.getCategoryId() &&
                           (oldItem.getDescription() == null ? newItem.getDescription() == null 
                               : oldItem.getDescription().equals(newItem.getDescription()));
                }
            };

    public void setCategoryMap(Map<Long, Category> map) {
        this.categoryMap = map;
        notifyDataSetChanged();
    }

    public void setCategories(List<Category> categories) {
        categoryMap.clear();
        for (Category category : categories) {
            categoryMap.put(category.getId(), category);
        }
        notifyDataSetChanged();
    }

    public void setMerchantMap(Map<Long, String> map) {
        this.merchantMap = map;
        notifyDataSetChanged();
    }

    public void setOnExpenseClickListener(OnExpenseClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final View colorIndicator;
        private final TextView description;
        private final TextView category;
        private final TextView merchant;
        private final TextView amount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.view_category_color);
            description = itemView.findViewById(R.id.text_expense_description);
            category = itemView.findViewById(R.id.text_expense_category);
            merchant = itemView.findViewById(R.id.text_expense_merchant);
            amount = itemView.findViewById(R.id.text_expense_amount);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        clickListener.onExpenseClick(getItem(pos));
                    }
                }
            });
        }

        void bind(Expense expense) {
            // Description
            String desc = expense.getDescription();
            description.setText(desc != null && !desc.isEmpty() ? desc : "No description");

            // Amount
            amount.setText(DateUtils.formatAmountCents(expense.getAmountCents()));

            // Category
            Category cat = categoryMap.get(expense.getCategoryId());
            if (cat != null) {
                category.setText(cat.getName());
                
                // Color indicator
                GradientDrawable background = (GradientDrawable) colorIndicator.getBackground();
                if (background != null) {
                    background.setColor(cat.getColorArgb());
                }
            } else {
                category.setText("Unknown");
            }

            // Merchant
            Long merchantId = expense.getMerchantId();
            if (merchantId != null && merchantMap.containsKey(merchantId)) {
                merchant.setText("• " + merchantMap.get(merchantId));
                merchant.setVisibility(View.VISIBLE);
            } else {
                merchant.setVisibility(View.GONE);
            }
        }
    }
}
