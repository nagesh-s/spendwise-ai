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
import com.spendwise.ai.data.entity.InvestmentSchedule;
import com.spendwise.ai.util.DateUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter for recurring investment schedules (SIPs).
 */
public class InvestmentScheduleAdapter
        extends ListAdapter<InvestmentSchedule, InvestmentScheduleAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(InvestmentSchedule schedule);

        void onToggleActive(InvestmentSchedule schedule);

        void onDelete(InvestmentSchedule schedule);
    }

    private final OnItemClickListener listener;
    private Map<Long, String> bucketNames = new HashMap<>();

    public InvestmentScheduleAdapter(@NonNull OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setBucketNames(@NonNull Map<Long, String> names) {
        bucketNames = new HashMap<>(names);
        notifyDataSetChanged();
    }

    private static final DiffUtil.ItemCallback<InvestmentSchedule> DIFF_CALLBACK = new DiffUtil.ItemCallback<InvestmentSchedule>() {
        @Override
        public boolean areItemsTheSame(@NonNull InvestmentSchedule oldItem, @NonNull InvestmentSchedule newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull InvestmentSchedule oldItem, @NonNull InvestmentSchedule newItem) {
            return oldItem.getName().equals(newItem.getName())
                    && oldItem.getAmountCents() == newItem.getAmountCents()
                    && oldItem.getBucketId() == newItem.getBucketId()
                    && oldItem.getFrequency().equals(newItem.getFrequency())
                    && oldItem.getDayOfPeriod() == newItem.getDayOfPeriod()
                    && oldItem.isActive() == newItem.isActive();
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_investment_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), bucketNames, listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textMeta;
        private final TextView textAmount;
        private final TextView textFrequency;
        private final SwitchMaterial switchActive;
        private final ImageButton buttonDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_name);
            textMeta = itemView.findViewById(R.id.text_meta);
            textAmount = itemView.findViewById(R.id.text_amount);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            switchActive = itemView.findViewById(R.id.switch_active);
            buttonDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(
                @NonNull InvestmentSchedule schedule,
                @NonNull Map<Long, String> bucketNames,
                @NonNull OnItemClickListener listener) {
            textName.setText(schedule.getName());
            String bucketName = bucketNames.containsKey(schedule.getBucketId())
                    ? bucketNames.get(schedule.getBucketId())
                    : itemView.getContext().getString(R.string.recurring_investment_unknown_bucket);
            String meta = String.format(
                    Locale.getDefault(),
                    "%s • %s",
                    schedule.getAssetType(),
                    bucketName);
            textMeta.setText(meta);
            textAmount.setText(DateUtils.formatAmountCents(schedule.getAmountCents()));
            textFrequency.setText(formatFrequency(schedule));

            switchActive.setOnCheckedChangeListener(null);
            switchActive.setChecked(schedule.isActive());
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked != schedule.isActive()) {
                    listener.onToggleActive(schedule);
                }
            });

            itemView.setOnClickListener(v -> listener.onItemClick(schedule));
            buttonDelete.setOnClickListener(v -> listener.onDelete(schedule));
        }

        @NonNull
        private String formatFrequency(@NonNull InvestmentSchedule schedule) {
            switch (schedule.getFrequency()) {
                case InvestmentSchedule.FREQUENCY_DAILY:
                    return "Daily";
                case InvestmentSchedule.FREQUENCY_EVERY_2_DAYS:
                    return "Every 2 days";
                case InvestmentSchedule.FREQUENCY_EVERY_3_DAYS:
                    return "Every 3 days";
                case InvestmentSchedule.FREQUENCY_WEEKLY:
                    return "Weekly";
                case InvestmentSchedule.FREQUENCY_MONTHLY:
                    return String.format(Locale.getDefault(), "Monthly (D%d)", schedule.getDayOfPeriod());
                case InvestmentSchedule.FREQUENCY_YEARLY:
                    return String.format(Locale.getDefault(), "Yearly (D%d)", schedule.getDayOfPeriod());
                default:
                    return schedule.getFrequency();
            }
        }
    }
}
