package com.spendwise.ai.dashboard;

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
import com.spendwise.ai.data.pojo.CategoryTotal;
import com.spendwise.ai.util.DateUtils;

/**
 * Adapter for displaying category totals in a RecyclerView.
 * Used in Dashboard to show top spending categories.
 */
public class CategoryTotalAdapter extends ListAdapter<CategoryTotal, CategoryTotalAdapter.ViewHolder> {

    public CategoryTotalAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<CategoryTotal> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CategoryTotal>() {
                @Override
                public boolean areItemsTheSame(@NonNull CategoryTotal oldItem, @NonNull CategoryTotal newItem) {
                    return oldItem.getName().equals(newItem.getName());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CategoryTotal oldItem, @NonNull CategoryTotal newItem) {
                    return oldItem.getName().equals(newItem.getName()) &&
                           oldItem.getTotal() == newItem.getTotal();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_total, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final View colorIndicator;
        private final TextView categoryName;
        private final TextView categoryTotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.view_category_color);
            categoryName = itemView.findViewById(R.id.text_category_name);
            categoryTotal = itemView.findViewById(R.id.text_category_total);
        }

        void bind(CategoryTotal item) {
            categoryName.setText(item.getName());
            categoryTotal.setText(DateUtils.formatAmountCents(item.getTotal()));
            
            // Set color indicator
            GradientDrawable background = (GradientDrawable) colorIndicator.getBackground();
            if (background != null) {
                background.setColor(item.getColorArgb());
            }
        }
    }
}
