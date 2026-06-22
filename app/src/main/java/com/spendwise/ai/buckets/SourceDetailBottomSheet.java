package com.spendwise.ai.buckets;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.color.MaterialColors;
import com.spendwise.ai.R;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.Holding;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;
import java.util.Locale;

import com.google.android.material.button.MaterialButton;

public class SourceDetailBottomSheet extends BottomSheetDialogFragment {

    public static final String ARG_INVESTMENT_ID = "investmentId";
    public static final String ARG_TRANSACTION_ID = "transactionId";
    public static final String ARG_EXPENSE_ID = "expenseId";

    private FrameLayout containerLoading;
    private LinearLayout containerContent;
    private LinearLayout containerEmpty;

    private TextView textOrigin;
    private TextView textSender;
    private TextView textMessage;
    private TextView textTimestamp;
    private MaterialButton btnRefreshSources;
    private MaterialButton btnRefreshSourcesEmpty;

    /** Create for an investment. */
    public static SourceDetailBottomSheet newInstance(long investmentId) {
        SourceDetailBottomSheet fragment = new SourceDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_INVESTMENT_ID, investmentId);
        fragment.setArguments(args);
        return fragment;
    }

    public static SourceDetailBottomSheet newInstanceForTransaction(long transactionId) {
        SourceDetailBottomSheet fragment = new SourceDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_TRANSACTION_ID, transactionId);
        fragment.setArguments(args);
        return fragment;
    }

    /** Create for an expense. */
    public static SourceDetailBottomSheet newInstanceForExpense(long expenseId) {
        SourceDetailBottomSheet fragment = new SourceDetailBottomSheet();
        Bundle args = new Bundle();
        args.putLong(ARG_INVESTMENT_ID, -1);
        args.putLong(ARG_EXPENSE_ID, expenseId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_source_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerLoading = view.findViewById(R.id.container_source_loading);
        containerContent = view.findViewById(R.id.container_source_content);
        containerEmpty = view.findViewById(R.id.container_source_empty);

        textOrigin = view.findViewById(R.id.text_source_domain);
        textSender = view.findViewById(R.id.text_source_sender);
        textMessage = view.findViewById(R.id.text_source_message);
        textTimestamp = view.findViewById(R.id.text_source_timestamp);
        btnRefreshSources = view.findViewById(R.id.btn_refresh_sources);
        btnRefreshSourcesEmpty = view.findViewById(R.id.btn_refresh_sources_empty);

        View.OnClickListener refreshClick = v -> {
            com.spendwise.ai.work.WorkScheduler.runSourceRefreshNow(requireContext());
            android.widget.Toast.makeText(requireContext(),
                    "Refreshing sources… this may take a moment.",
                    android.widget.Toast.LENGTH_LONG).show();

            // Observe completion so we can show a toast when done
            androidx.work.WorkManager.getInstance(requireContext())
                    .getWorkInfosForUniqueWorkLiveData(
                            com.spendwise.ai.work.WorkScheduler.SOURCE_REFRESH_WORK)
                    .observe(getViewLifecycleOwner(), workInfos -> {
                        if (workInfos == null || workInfos.isEmpty())
                            return;
                        androidx.work.WorkInfo info = workInfos.get(0);
                        if (info.getState() == androidx.work.WorkInfo.State.SUCCEEDED) {
                            if (getContext() != null) {
                                android.widget.Toast.makeText(getContext(),
                                        "Sources refreshed successfully!",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                            dismiss();
                        } else if (info.getState() == androidx.work.WorkInfo.State.FAILED) {
                            if (getContext() != null) {
                                android.widget.Toast.makeText(getContext(),
                                        "Source refresh failed. Please try again.",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            }
                            dismiss();
                        }
                    });

            // Disable buttons to prevent double-tap
            btnRefreshSources.setEnabled(false);
            btnRefreshSources.setText("Refreshing…");
            btnRefreshSourcesEmpty.setEnabled(false);
            btnRefreshSourcesEmpty.setText("Refreshing…");
        };
        btnRefreshSources.setOnClickListener(refreshClick);
        btnRefreshSourcesEmpty.setOnClickListener(refreshClick);

        long investmentId = getArguments() != null ? getArguments().getLong(ARG_INVESTMENT_ID, -1) : -1;
        long transactionId = getArguments() != null ? getArguments().getLong(ARG_TRANSACTION_ID, -1) : -1;
        long expenseId = getArguments() != null ? getArguments().getLong(ARG_EXPENSE_ID, -1) : -1;

        if (transactionId > 0) {
            loadTransactionSource(transactionId);
        } else if (investmentId > 0) {
            loadInvestmentSource(investmentId);
        } else if (expenseId > 0) {
            loadExpenseSource(expenseId);
        } else {
            showEmptyState();
        }
    }

    // ── Holding lookup ──────────────────────────────────────────────
    private void loadInvestmentSource(long investmentId) {
        showLoadingState();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            Holding investment = db.investmentDao().getById(investmentId);
            if (investment == null) {
                post(this::showEmptyState);
                return;
            }

            // Manual entry: no correlation key and not auto-fetched
            boolean hasCorrelationKey = investment.getPrimaryCorrelationKey() != null && !investment.getPrimaryCorrelationKey().isEmpty();
            
            if (!investment.isAutoFetched() && !hasCorrelationKey) {
                post(() -> showManualEntry(investment.getLastUpdatedEpochDay()));
                return;
            }

            IngestionEvent event = findEventForInvestment(db, investment);
            if (event != null) {
                post(() -> populateDetails(event));
            } else if (!investment.isAutoFetched() && !hasCorrelationKey) {
                post(() -> showManualEntry(investment.getLastUpdatedEpochDay()));
            } else {
                post(this::showEmptyState);
            }
        });
    }

    @Nullable
    private IngestionEvent findEventForInvestment(@NonNull AppDatabase db,
            @NonNull Holding investment) {
        // Primary: use the link table which directly maps holdingId → ingestionEventId
        IngestionEvent linkedEvent = db.investmentEventLinkDao()
                .getLatestEventForHolding(investment.getId());
        if (linkedEvent != null) {
            return linkedEvent;
        }
        // Fallback: try by correlation key
        if (investment.getPrimaryCorrelationKey() != null) {
            List<IngestionEvent> events = db.ingestionEventDao()
                    .getByCorrelationKey(investment.getPrimaryCorrelationKey());
            if (events != null && !events.isEmpty())
                return events.get(0);
        }
        return null;
    }

    private void loadTransactionSource(long transactionId) {
        showLoadingState();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            com.spendwise.ai.data.entity.InvestmentTransaction transaction = db.investmentTransactionDao().getById(transactionId);
            if (transaction == null) {
                post(this::showEmptyState);
                return;
            }

            IngestionEvent linkedEvent = db.investmentEventLinkDao().getLatestEventForTransaction(transaction.getId());
            if (linkedEvent != null) {
                post(() -> populateDetails(linkedEvent));
            } else if (transaction.getCorrelationKey() != null) {
                List<IngestionEvent> events = db.ingestionEventDao().getByCorrelationKey(transaction.getCorrelationKey());
                if (events != null && !events.isEmpty()) {
                    post(() -> populateDetails(events.get(0)));
                } else {
                    post(this::showEmptyState);
                }
            } else {
                post(this::showEmptyState);
            }
        });
    }

    // ── Expense lookup ─────────────────────────────────────────────────
    private void loadExpenseSource(long expenseId) {
        showLoadingState();
        AppDatabase.databaseWriteExecutor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            Expense expense = db.expenseDao().getById(expenseId);
            if (expense == null) {
                post(this::showEmptyState);
                return;
            }

            // Try canonical source key first (new data)
            String sourceKey = expense.getCanonicalSourceKey();
            if (sourceKey != null && !sourceKey.isEmpty()) {
                IngestionEvent event = db.ingestionEventDao().getBySourceKey(sourceKey);
                if (event != null) {
                    post(() -> populateDetails(event));
                    return;
                }
            }

            // Legacy fallback: check if description contains "SMS Import"
            String desc = expense.getDescription();
            if (desc != null && desc.contains("SMS Import")) {
                post(() -> showLegacySmsExpense(expense));
            } else {
                // Must be manual entry
                post(() -> showManualEntry(expense.getDateEpochDay()));
            }
        });
    }

    private void showLegacySmsExpense(@NonNull Expense expense) {
        hideLoadingState();
        containerContent.setVisibility(View.VISIBLE);

        textOrigin.setText("SMS Auto-Import");
        textSender.setText("—");
        textSender.setTextColor(
                MaterialColors.getColor(requireView(),
                        com.google.android.material.R.attr.colorOnSurfaceVariant));

        String desc = expense.getDescription() != null ? expense.getDescription() : "";
        textMessage.setText(desc.isEmpty()
                ? "SMS body not available for legacy imports."
                : desc);

        if (expense.getDateEpochDay() > 0) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String formattedDate = LocalDate.ofEpochDay(expense.getDateEpochDay()).format(formatter);
            textTimestamp.setText("Date: " + formattedDate);
        } else {
            textTimestamp.setVisibility(View.GONE);
        }

        // Show refresh button for legacy imports with missing details
        btnRefreshSources.setVisibility(View.VISIBLE);
    }

    // ── UI population ──────────────────────────────────────────────────
    private void populateDetails(IngestionEvent event) {
        hideLoadingState();
        containerContent.setVisibility(View.VISIBLE);

        // Origin
        String sourceType = event.getSourceType();
        String originText;
        if (IngestionEvent.SOURCE_SMS.equals(sourceType)) {
            originText = "SMS Auto-Import";
        } else if (IngestionEvent.SOURCE_NOTIFICATION.equals(sourceType)) {
            originText = "Push Notification";
        } else if (IngestionEvent.SOURCE_EXPENSE_RECOVERY.equals(sourceType)) {
            originText = "Expense Recovery";
        } else {
            originText = sourceType != null ? sourceType : "Unknown";
        }
        textOrigin.setText(originText);

        // Sender
        String sender = event.getSourceSender();
        boolean senderMissing = (sender == null || sender.isEmpty());
        if (!senderMissing) {
            textSender.setText(sender.toUpperCase(Locale.US));
        } else {
            textSender.setText("—");
            textSender.setTextColor(
                    MaterialColors.getColor(requireView(),
                            com.google.android.material.R.attr.colorOnSurfaceVariant));
        }

        // Message Body
        String body = event.getSourceBody();
        boolean bodyMissing = (body == null || body.isEmpty());
        if (!bodyMissing) {
            textMessage.setText(body);
        } else {
            textMessage.setText("No message body stored for this transaction.");
            textMessage.setTextColor(
                    MaterialColors.getColor(requireView(),
                            com.google.android.material.R.attr.colorOnSurfaceVariant));
            textMessage.setTypeface(null, android.graphics.Typeface.ITALIC);
        }

        // Show refresh button if sender or body is missing
        if (senderMissing || bodyMissing) {
            btnRefreshSources.setVisibility(View.VISIBLE);
        }

        // Timestamp
        if (event.getEventTimestampMillis() > 0) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            String formattedDate = Instant.ofEpochMilli(event.getEventTimestampMillis())
                    .atZone(ZoneId.systemDefault())
                    .format(formatter);
            textTimestamp.setText("Received: " + formattedDate);
        } else {
            textTimestamp.setVisibility(View.GONE);
        }
    }

    private void showManualEntry(long epochDay) {
        hideLoadingState();
        containerContent.setVisibility(View.VISIBLE);

        textOrigin.setText("Manually Added");

        textSender.setText("You");

        textMessage.setText("This entry was added manually by you.");
        textMessage.setTextColor(
                MaterialColors.getColor(requireView(),
                        com.google.android.material.R.attr.colorOnSurfaceVariant));
        textMessage.setTypeface(null, android.graphics.Typeface.ITALIC);

        if (epochDay > 0) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            String formattedDate = LocalDate.ofEpochDay(epochDay).format(formatter);
            textTimestamp.setText("Added on: " + formattedDate);
        } else {
            textTimestamp.setVisibility(View.GONE);
        }
    }

    // ── State helpers ──────────────────────────────────────────────────
    private void showLoadingState() {
        containerLoading.setVisibility(View.VISIBLE);
        containerContent.setVisibility(View.GONE);
        containerEmpty.setVisibility(View.GONE);
    }

    private void hideLoadingState() {
        containerLoading.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        hideLoadingState();
        containerContent.setVisibility(View.GONE);
        containerEmpty.setVisibility(View.VISIBLE);
    }

    private void post(@NonNull Runnable action) {
        new Handler(Looper.getMainLooper()).post(action);
    }
}
