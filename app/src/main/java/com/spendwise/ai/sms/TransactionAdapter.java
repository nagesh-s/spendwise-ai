package com.spendwise.ai.sms;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spendwise.ai.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying parsed SMS transactions.
 */
public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {

    public interface OnImportClickListener {
        void onImportClick(ParsedTransaction transaction, int position);
    }

    private final List<ParsedTransaction> transactions = new ArrayList<>();
    private final OnImportClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public TransactionAdapter(OnImportClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<ParsedTransaction> newTransactions) {
        transactions.clear();
        transactions.addAll(newTransactions);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        holder.bind(transactions.get(position), position);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final View typeIndicator;
        private final TextView typeText;
        private final TextView amountText;
        private final TextView merchantText;
        private final TextView accountText;
        private final TextView dateText;
        private final MaterialButton importBtn;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            typeIndicator = itemView.findViewById(R.id.indicator_type);
            typeText = itemView.findViewById(R.id.text_type);
            amountText = itemView.findViewById(R.id.text_amount);
            merchantText = itemView.findViewById(R.id.text_merchant);
            accountText = itemView.findViewById(R.id.text_account);
            dateText = itemView.findViewById(R.id.text_date);
            importBtn = itemView.findViewById(R.id.btn_import);
        }

        void bind(ParsedTransaction transaction, int position) {
            // Type
            boolean isDebit = transaction.getType() == ParsedTransaction.TransactionType.DEBIT;
            typeIndicator.setBackgroundColor(isDebit ? Color.parseColor("#FF6F61") : Color.parseColor("#4CAF50"));
            typeText.setText(isDebit ? "DEBIT" : "CREDIT");

            // Amount
            amountText.setText(transaction.getFormattedAmount());
            amountText.setTextColor(isDebit ? Color.parseColor("#FF6F61") : Color.parseColor("#4CAF50"));

            // Merchant
            String merchant = transaction.getMerchant();
            merchantText.setText(merchant != null ? merchant : "Unknown Merchant");

            // Account
            String account = transaction.getAccountInfo();
            if (account != null) {
                accountText.setText("Account: " + account);
                accountText.setVisibility(View.VISIBLE);
            } else {
                accountText.setVisibility(View.GONE);
            }

            // Date
            dateText.setText(dateFormat.format(new Date(transaction.getTimestampMillis())));

            // Import button
            if (transaction.isImported()) {
                importBtn.setText("Imported ✓");
                importBtn.setEnabled(false);
            } else {
                importBtn.setText("Import as Expense");
                importBtn.setEnabled(true);
                importBtn.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onImportClick(transaction, position);
                    }
                });
            }
        }
    }
}
