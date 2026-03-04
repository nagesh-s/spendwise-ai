package com.spendwise.ai.buckets;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.data.pojo.InvestmentOccurrenceWithSchedule;

import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.Locale;

/**
 * Adapter for expected SIP occurrence items in bucket detail.
 */
public class ExpectedSipAdapter extends ListAdapter<InvestmentOccurrenceWithSchedule, ExpectedSipAdapter.ViewHolder> {

    public interface OnOccurrenceClickListener {
        void onOccurrenceClick(InvestmentOccurrenceWithSchedule occurrence);
    }

    private final OnOccurrenceClickListener listener;

    public ExpectedSipAdapter(@NonNull OnOccurrenceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<InvestmentOccurrenceWithSchedule> DIFF_CALLBACK = new DiffUtil.ItemCallback<InvestmentOccurrenceWithSchedule>() {
        @Override
        public boolean areItemsTheSame(@NonNull InvestmentOccurrenceWithSchedule oldItem,
                @NonNull InvestmentOccurrenceWithSchedule newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull InvestmentOccurrenceWithSchedule oldItem,
                @NonNull InvestmentOccurrenceWithSchedule newItem) {
            return oldItem.status.equals(newItem.status)
                    && oldItem.expectedAmountCents == newItem.expectedAmountCents
                    && valueOrZero(oldItem.confirmedAmountCents) == valueOrZero(newItem.confirmedAmountCents)
                    && oldItem.dueEpochDay == newItem.dueEpochDay
                    && textOrEmpty(oldItem.scheduleName).equals(textOrEmpty(newItem.scheduleName));
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expected_sip, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textStatus;
        private final TextView textDue;
        private final TextView textAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_name);
            textStatus = itemView.findViewById(R.id.text_status);
            textDue = itemView.findViewById(R.id.text_due);
            textAmount = itemView.findViewById(R.id.text_amount);
        }

        void bind(@NonNull InvestmentOccurrenceWithSchedule row, @NonNull OnOccurrenceClickListener listener) {
            textName.setText(textOrEmpty(row.scheduleName));
            LocalDate dueDate = LocalDate.ofEpochDay(row.dueEpochDay);
            textDue.setText(itemView.getContext().getString(
                    R.string.bucket_detail_expected_due_format,
                    dueDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()))));

            long amountCents = row.getResolvedAmountCents();
            textAmount.setText(String.format(Locale.getDefault(), "₹%,.0f", amountCents / 100.0));

            String statusLabel;
            int statusBg;
            int statusColor;
            if (InvestmentOccurrence.STATUS_CONFIRMED.equals(row.status)) {
                statusLabel = itemView.getContext().getString(R.string.bucket_detail_status_confirmed);
                statusBg = R.color.md_theme_secondaryContainer;
                statusColor = R.color.md_theme_onSecondaryContainer;
            } else if (InvestmentOccurrence.STATUS_ADJUSTED.equals(row.status)) {
                statusLabel = itemView.getContext().getString(R.string.bucket_detail_status_adjusted);
                statusBg = R.color.md_theme_tertiaryContainer;
                statusColor = R.color.md_theme_onTertiaryContainer;
            } else if (InvestmentOccurrence.STATUS_SKIPPED.equals(row.status)) {
                statusLabel = itemView.getContext().getString(R.string.bucket_detail_status_skipped);
                statusBg = R.color.md_theme_surfaceContainerHigh;
                statusColor = R.color.md_theme_onSurfaceVariant;
            } else {
                statusLabel = itemView.getContext().getString(R.string.bucket_detail_status_expected);
                statusBg = R.color.md_theme_primaryContainer;
                statusColor = R.color.md_theme_onPrimaryContainer;
            }
            textStatus.setText(statusLabel);
            textStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), statusBg));
            textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), statusColor));

            itemView.setOnClickListener(v -> listener.onOccurrenceClick(row));
        }
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    @NonNull
    private static String textOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
