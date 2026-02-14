package com.spendwise.ai.sms;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.util.List;

/**
 * Fragment for Auto Transaction Reader.
 * Scans SMS inbox for bank transaction messages and allows importing as
 * expenses.
 */
public class AutoTransactionsFragment extends Fragment implements TransactionAdapter.OnImportClickListener {

    private RecyclerView recyclerView;
    private TextView emptyText;
    private View permissionLayout;
    private MaterialButton scanBtn;
    private TransactionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auto_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_transactions);
        emptyText = view.findViewById(R.id.text_empty);
        permissionLayout = view.findViewById(R.id.layout_permission);
        scanBtn = view.findViewById(R.id.btn_scan);

        adapter = new TransactionAdapter(this);
        recyclerView.setAdapter(adapter);

        // Permission button
        view.findViewById(R.id.btn_grant_permission)
                .setOnClickListener(v -> SmsPermissionHelper.requestSmsPermission(requireActivity()));

        // Scan button
        scanBtn.setOnClickListener(v -> scanSms());

        updateUiState();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUiState();
    }

    private void updateUiState() {
        boolean hasPermission = SmsPermissionHelper.hasReadSmsPermission(requireContext());

        if (!hasPermission) {
            permissionLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            scanBtn.setVisibility(View.GONE);
        } else {
            permissionLayout.setVisibility(View.GONE);
            scanBtn.setVisibility(View.VISIBLE);
            // Show empty state if no transactions loaded yet
            if (adapter.getItemCount() == 0) {
                emptyText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        }
    }

    private void scanSms() {
        if (!SmsPermissionHelper.hasReadSmsPermission(requireContext())) {
            SmsPermissionHelper.requestSmsPermission(requireActivity());
            return;
        }

        scanBtn.setEnabled(false);
        scanBtn.setText("Scanning...");

        // Run on background thread
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ParsedTransaction> transactions = SmsPermissionHelper.readRecentTransactions(requireContext(), 30);

            // Filter to debit transactions only (those are expenses)
            transactions.removeIf(t -> t.getType() != ParsedTransaction.TransactionType.DEBIT);

            requireActivity().runOnUiThread(() -> {
                adapter.setTransactions(transactions);
                scanBtn.setEnabled(true);
                scanBtn.setText("Scan SMS Inbox");

                if (transactions.isEmpty()) {
                    emptyText.setText(
                            "No debit transactions found in recent SMS.\nTry checking your SMS inbox manually.");
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    Toast.makeText(requireContext(),
                            "Found " + transactions.size() + " transactions",
                            Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onImportClick(ParsedTransaction transaction, int position) {
        // Convert parsed transaction to Expense
        LocalDate date = Instant.ofEpochMilli(transaction.getTimestampMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // Create expense with "Uncategorized" (category 1 is usually the first default)
        Expense expense = new Expense(
                date.toEpochDay(),
                transaction.getAmountCents(),
                transaction.getMerchant() != null ? transaction.getMerchant() : "SMS Import",
                1L, // Default category (first category)
                null // No merchant link
        );

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext().getApplicationContext());
            db.expenseDao().insert(expense);

            requireActivity().runOnUiThread(() -> {
                transaction.setImported(true);
                adapter.notifyItemChanged(position);
                Toast.makeText(requireContext(),
                        "Imported " + transaction.getFormattedAmount() + " as expense",
                        Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        if (requestCode == SmsPermissionHelper.SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateUiState();
                scanSms();
            } else {
                Toast.makeText(requireContext(),
                        "SMS permission is required to detect transactions",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
