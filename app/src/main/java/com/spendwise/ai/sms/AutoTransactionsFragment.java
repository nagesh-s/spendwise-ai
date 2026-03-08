package com.spendwise.ai.sms;

import android.content.pm.PackageManager;
import android.content.Context;
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
import com.google.android.material.materialswitch.MaterialSwitch;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.work.WorkScheduler;

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

    private static final String PREF_SMS_PERMISSION = "sms_permission_prefs";
    private static final String KEY_SMS_PERMISSION_REQUESTED = "sms_permission_requested";
    private static final long SMS_RESYNC_GRACE_WINDOW_MILLIS = 24L * 60 * 60 * 1000;
    private static final long SMS_UI_RESYNC_WINDOW_MILLIS = 14L * 24 * 60 * 60 * 1000;
    private static final int SMS_FALLBACK_SCAN_DAYS = 120;

    private RecyclerView recyclerView;
    private TextView emptyText;
    private TextView permissionMessageText;
    private View permissionLayout;
    private MaterialButton scanBtn;
    private MaterialButton grantPermissionBtn;
    private MaterialButton openSettingsBtn;
    private MaterialSwitch autoImportSwitch;
    private TransactionAdapter adapter;
    private boolean isLoadingTransactions;
    private boolean suppressAutoImportToggleCallback;

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
        permissionMessageText = view.findViewById(R.id.text_permission_message);
        permissionLayout = view.findViewById(R.id.layout_permission);
        scanBtn = view.findViewById(R.id.btn_scan);
        grantPermissionBtn = view.findViewById(R.id.btn_grant_permission);
        openSettingsBtn = view.findViewById(R.id.btn_open_sms_settings);
        autoImportSwitch = view.findViewById(R.id.switch_sms_auto_import);

        adapter = new TransactionAdapter(this);
        recyclerView.setAdapter(adapter);

        autoImportSwitch.setChecked(SmsAutoImportPrefs.isEnabled(requireContext()));
        autoImportSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (suppressAutoImportToggleCallback) {
                return;
            }
            SmsAutoImportPrefs.setEnabled(requireContext(), isChecked);
            WorkScheduler.setSmsAutoImportEnabled(requireContext(), isChecked);
            Toast.makeText(requireContext(),
                    isChecked ? getString(R.string.sms_auto_import_enabled) : getString(R.string.sms_auto_import_disabled),
                    Toast.LENGTH_SHORT).show();
        });

        // Permission button
        grantPermissionBtn.setOnClickListener(v -> {
            markSmsPermissionRequested();
            SmsPermissionHelper.requestSmsPermission(requireActivity());
        });
        openSettingsBtn.setOnClickListener(v -> SmsPermissionHelper.openAppSettings(requireContext()));

        // Scan button
        scanBtn.setOnClickListener(v -> scanSms());

        updateUiState();
    }

    @Override
    public void onResume() {
        super.onResume();
        boolean enabled = SmsAutoImportPrefs.isEnabled(requireContext());
        if (autoImportSwitch != null && autoImportSwitch.isChecked() != enabled) {
            suppressAutoImportToggleCallback = true;
            autoImportSwitch.setChecked(enabled);
            suppressAutoImportToggleCallback = false;
        }
        updateUiState();
    }

    private void updateUiState() {
        boolean hasPermission = SmsPermissionHelper.hasReadSmsPermission(requireContext());

        if (!hasPermission) {
            permissionLayout.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.GONE);
            scanBtn.setVisibility(View.GONE);
            renderPermissionGuidance();
        } else {
            permissionLayout.setVisibility(View.GONE);
            scanBtn.setVisibility(View.VISIBLE);
            if (adapter.getItemCount() == 0 && !isLoadingTransactions) {
                loadTransactionsForDisplay(false);
            }
        }
    }

    private void renderPermissionGuidance() {
        boolean requestedBefore = hasRequestedSmsPermission();
        boolean canShowRationale = SmsPermissionHelper.shouldShowRationale(requireActivity());

        openSettingsBtn.setVisibility(View.GONE);
        grantPermissionBtn.setVisibility(View.VISIBLE);

        if (requestedBefore && !canShowRationale) {
            permissionMessageText.setText(getString(R.string.sms_permission_settings_required));
            grantPermissionBtn.setText(getString(R.string.grant_sms_permission_again));
            openSettingsBtn.setVisibility(View.VISIBLE);
        } else if (canShowRationale) {
            permissionMessageText.setText(getString(R.string.sms_permission_rationale));
            grantPermissionBtn.setText(getString(R.string.grant_sms_permission));
        } else {
            permissionMessageText.setText(getString(R.string.sms_permission_initial));
            grantPermissionBtn.setText(getString(R.string.grant_sms_permission));
        }
    }

    private void markSmsPermissionRequested() {
        requireContext().getSharedPreferences(PREF_SMS_PERMISSION, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_SMS_PERMISSION_REQUESTED, true)
                .apply();
    }

    private boolean hasRequestedSmsPermission() {
        return requireContext().getSharedPreferences(PREF_SMS_PERMISSION, Context.MODE_PRIVATE)
                .getBoolean(KEY_SMS_PERMISSION_REQUESTED, false);
    }

    private void scanSms() {
        if (!SmsPermissionHelper.hasReadSmsPermission(requireContext())) {
            markSmsPermissionRequested();
            SmsPermissionHelper.requestSmsPermission(requireActivity());
            return;
        }
        loadTransactionsForDisplay(true);
    }

    private void loadTransactionsForDisplay(boolean userInitiatedScan) {
        if (!isAdded() || isLoadingTransactions) {
            return;
        }
        isLoadingTransactions = true;
        if (userInitiatedScan) {
            scanBtn.setEnabled(false);
            scanBtn.setText(getString(R.string.sms_scanning));
        }

        Context appContext = requireContext().getApplicationContext();
        long lastSyncTimestamp = SmsSyncState.getLastTransactionSyncTimestamp(appContext);

        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<ParsedTransaction> transactions;
            if (userInitiatedScan) {
                transactions = readTransactionsForSyncScan(appContext, lastSyncTimestamp);
            } else {
                transactions = readTransactionsForDisplay(appContext, lastSyncTimestamp);
            }
            transactions.removeIf(t -> t.getType() != ParsedTransaction.TransactionType.DEBIT);
            markImportedTransactions(appContext, transactions);
            final List<ParsedTransaction> finalTransactions = transactions;

            if (!isAdded()) {
                isLoadingTransactions = false;
                return;
            }
            requireActivity().runOnUiThread(() -> {
                isLoadingTransactions = false;
                if (userInitiatedScan) {
                    scanBtn.setEnabled(true);
                    scanBtn.setText(getString(R.string.sms_scan_inbox));
                }
                adapter.setTransactions(finalTransactions);

                if (finalTransactions.isEmpty()) {
                    emptyText.setText(userInitiatedScan
                            ? getString(R.string.sms_scan_empty)
                            : getString(R.string.sms_empty_initial));
                    emptyText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (userInitiatedScan) {
                        Toast.makeText(requireContext(),
                                getString(R.string.sms_found_transactions, finalTransactions.size()),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    @NonNull
    private List<ParsedTransaction> readTransactionsForSyncScan(@NonNull Context appContext, long lastSyncTimestamp) {
        List<ParsedTransaction> transactions = SmsPermissionHelper.readTransactionsSince(
                appContext,
                lastSyncTimestamp,
                30);
        boolean usedFallbackWindow = false;
        if (transactions.isEmpty() && lastSyncTimestamp > 0L) {
            long recoveryStart = Math.max(0L, lastSyncTimestamp - SMS_RESYNC_GRACE_WINDOW_MILLIS);
            transactions = SmsPermissionHelper.readTransactionsSince(appContext, recoveryStart, 30);
        }
        if (transactions.isEmpty()) {
            transactions = SmsPermissionHelper.readRecentTransactions(appContext, SMS_FALLBACK_SCAN_DAYS);
            usedFallbackWindow = true;
        }

        long latestSeenTimestamp = lastSyncTimestamp;
        for (ParsedTransaction transaction : transactions) {
            latestSeenTimestamp = Math.max(latestSeenTimestamp, transaction.getTimestampMillis());
        }
        if (usedFallbackWindow && latestSeenTimestamp == 0L) {
            SmsSyncState.clearTransactionSync(appContext);
        }
        SmsSyncState.updateLastTransactionSyncTimestamp(appContext, latestSeenTimestamp);
        return transactions;
    }

    @NonNull
    private List<ParsedTransaction> readTransactionsForDisplay(@NonNull Context appContext, long lastSyncTimestamp) {
        long displayStart = lastSyncTimestamp > 0L
                ? Math.max(0L, lastSyncTimestamp - SMS_UI_RESYNC_WINDOW_MILLIS)
                : 0L;
        List<ParsedTransaction> transactions = SmsPermissionHelper.readTransactionsSince(
                appContext,
                displayStart,
                SMS_FALLBACK_SCAN_DAYS);
        if (transactions.isEmpty()) {
            transactions = SmsPermissionHelper.readRecentTransactions(appContext, SMS_FALLBACK_SCAN_DAYS);
        }
        return transactions;
    }

    private void markImportedTransactions(@NonNull Context appContext, @NonNull List<ParsedTransaction> transactions) {
        AppDatabase db = AppDatabase.getInstance(appContext);
        String fallbackDescription = appContext.getString(R.string.sms_import_description_fallback);
        for (ParsedTransaction transaction : transactions) {
            LocalDate date = Instant.ofEpochMilli(transaction.getTimestampMillis())
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long dateEpochDay = date.toEpochDay();
            String description = SmsExpenseClassifier.resolveDescription(transaction, fallbackDescription);
            int duplicateCount = db.expenseDao().countByDateAmountDescription(
                    dateEpochDay,
                    transaction.getAmountCents(),
                    description);
            transaction.setImported(duplicateCount > 0);
        }
    }

    @Override
    public void onImportClick(ParsedTransaction transaction, int position) {
        // Convert parsed transaction to Expense
        LocalDate date = Instant.ofEpochMilli(transaction.getTimestampMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        long dateEpochDay = date.toEpochDay();
        Context appContext = requireContext().getApplicationContext();

        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(appContext);
            long importCategoryId = SmsExpenseClassifier.resolveCategoryId(
                    db.categoryDao().getAllCategoriesSync(),
                    transaction);
            String description = SmsExpenseClassifier.resolveDescription(transaction,
                    getString(R.string.sms_import_description_fallback));

            Long merchantId = null;
            if (transaction.getMerchant() != null && !transaction.getMerchant().trim().isEmpty()) {
                Merchant existingMerchant = db.merchantDao().findByName(transaction.getMerchant().trim());
                if (existingMerchant != null) {
                    merchantId = existingMerchant.getId();
                } else {
                    long insertedMerchantId = db.merchantDao().insertIfNotExists(new Merchant(transaction.getMerchant().trim()));
                    if (insertedMerchantId > 0) {
                        merchantId = insertedMerchantId;
                    } else {
                        Merchant reloaded = db.merchantDao().findByName(transaction.getMerchant().trim());
                        if (reloaded != null) {
                            merchantId = reloaded.getId();
                        }
                    }
                }
            }

            Long accountId = SmsExpenseClassifier.resolveAccountId(
                    db.accountDao().getAllAccountsSync(),
                    transaction);

            Expense expense = new Expense(
                    dateEpochDay,
                    transaction.getAmountCents(),
                    description,
                    importCategoryId,
                    merchantId);
            if (accountId != null && accountId > 0) {
                expense.setAccountId(accountId);
            }

            int duplicateCount = db.expenseDao().countByDateAmountDescription(
                    dateEpochDay,
                    transaction.getAmountCents(),
                    description);

            if (duplicateCount > 0) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    transaction.setImported(true);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(),
                            getString(R.string.sms_duplicate_transaction_skipped),
                            Toast.LENGTH_SHORT).show();
                });
                return;
            }

            try {
                db.expenseDao().insert(expense);

                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    transaction.setImported(true);
                    adapter.notifyItemChanged(position);
                    Toast.makeText(requireContext(),
                            getString(R.string.sms_import_success, transaction.getFormattedAmount()),
                            Toast.LENGTH_SHORT).show();
                });
            } catch (Exception ex) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> Toast.makeText(
                        requireContext(),
                        getString(R.string.sms_import_failed_retry),
                        Toast.LENGTH_LONG).show());
            }
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
                if (!SmsPermissionHelper.shouldShowRationale(requireActivity()) && hasRequestedSmsPermission()) {
                    Toast.makeText(requireContext(),
                            getString(R.string.sms_permission_open_settings_to_enable),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(),
                            getString(R.string.sms_permission_required_to_detect),
                            Toast.LENGTH_LONG).show();
                }
                updateUiState();
            }
        }
    }
}
