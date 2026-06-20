package com.spendwise.ai.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.engine.ExpenseInvestmentRecoveryEngine;
import com.spendwise.ai.engine.EventIngestionPipeline;
import com.spendwise.ai.engine.InvestmentAutoIngestionEngine;
import com.spendwise.ai.engine.InvestmentGamificationEngine;
import com.spendwise.ai.engine.OccurrenceReconciliationEngine;
import com.spendwise.ai.engine.SalaryAllocationOrchestrator;
import com.spendwise.ai.engine.SalaryDetector;
import com.spendwise.ai.sms.InvestmentSmsParser;
import com.spendwise.ai.sms.InvestmentSignalKeyFactory;
import com.spendwise.ai.sms.ParsedTransaction;
import com.spendwise.ai.sms.ParsedInvestmentSignal;
import com.spendwise.ai.sms.RawSmsMessage;
import com.spendwise.ai.sms.SmsExpenseClassifier;
import com.spendwise.ai.sms.SmsPermissionHelper;
import com.spendwise.ai.sms.SmsSyncState;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Periodic background worker that scans SMS and auto-imports debit
 * transactions.
 */
public class SmsAutoImportWorker extends Worker {

    public static final String KEY_IMPORT_EXPENSES = "key_import_expenses";
    public static final String KEY_IMPORT_INVESTMENTS = "key_import_investments";
    public static final String KEY_INVESTMENT_REPAIR_LOOKBACK_DAYS = "key_investment_repair_lookback_days";

    private static final int LOOKBACK_DAYS = 30;
    private static final int FALLBACK_SCAN_DAYS = 120;
    private static final int DEFAULT_REPAIR_LOOKBACK_DAYS = 180;

    public SmsAutoImportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    public static Data buildInputData(boolean importExpenses, boolean importInvestments, int repairLookbackDays) {
        return new Data.Builder()
                .putBoolean(KEY_IMPORT_EXPENSES, importExpenses)
                .putBoolean(KEY_IMPORT_INVESTMENTS, importInvestments)
                .putInt(KEY_INVESTMENT_REPAIR_LOOKBACK_DAYS, Math.max(0, repairLookbackDays))
                .build();
    }

    public static int defaultRepairLookbackDays() {
        return DEFAULT_REPAIR_LOOKBACK_DAYS;
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!SmsPermissionHelper.hasReadSmsPermission(context)) {
            return Result.success();
        }

        boolean importExpenses = getInputData().getBoolean(KEY_IMPORT_EXPENSES, true);
        boolean importInvestments = getInputData().getBoolean(KEY_IMPORT_INVESTMENTS, true);
        int repairLookbackDays = Math.max(0,
                getInputData().getInt(KEY_INVESTMENT_REPAIR_LOOKBACK_DAYS, 0));

        long lastTransactionImportedTimestamp = repairLookbackDays > 0
                ? 0L
                : SmsSyncState.getLastTransactionSyncTimestamp(context);
        long lastInvestmentImportedTimestamp = repairLookbackDays > 0
                ? 0L
                : SmsSyncState.getLastInvestmentSyncTimestamp(context);
        long latestTransactionSeenTimestamp = repairLookbackDays > 0
                ? SmsSyncState.getLastTransactionSyncTimestamp(context)
                : lastTransactionImportedTimestamp;
        long latestInvestmentSeenTimestamp = repairLookbackDays > 0
                ? SmsSyncState.getLastInvestmentSyncTimestamp(context)
                : lastInvestmentImportedTimestamp;

        AppDatabase db = AppDatabase.getInstance(context);
        List<ParsedTransaction> transactions = new ArrayList<>();
        if (importExpenses) {
            if (repairLookbackDays > 0) {
                transactions = SmsPermissionHelper.readRecentTransactions(context, repairLookbackDays);
            } else {
                transactions = SmsPermissionHelper.readTransactionsSince(
                        context,
                        lastTransactionImportedTimestamp,
                        LOOKBACK_DAYS);
                if (transactions.isEmpty()) {
                    transactions = SmsPermissionHelper.readRecentTransactions(context, FALLBACK_SCAN_DAYS);
                }
            }
        }

        List<RawSmsMessage> rawMessages = new ArrayList<>();
        if (importInvestments) {
            if (repairLookbackDays > 0) {
                rawMessages = SmsPermissionHelper.readRecentMessages(context, repairLookbackDays);
            } else {
                rawMessages = SmsPermissionHelper.readMessagesSince(
                        context,
                        lastInvestmentImportedTimestamp,
                        LOOKBACK_DAYS);
                if (rawMessages.isEmpty()) {
                    rawMessages = SmsPermissionHelper.readRecentMessages(context, FALLBACK_SCAN_DAYS);
                }
            }
        }

        List<com.spendwise.ai.data.entity.Category> categories = db.categoryDao().getAllCategoriesSync();
        List<com.spendwise.ai.data.entity.Account> accounts = db.accountDao().getAllAccountsSync();
        SalaryDetector salaryDetector = new SalaryDetector();
        SalaryAllocationOrchestrator salaryOrchestrator = new SalaryAllocationOrchestrator(context);

        for (int i = transactions.size() - 1; i >= 0; i--) {
            ParsedTransaction transaction = transactions.get(i);

            long timestamp = transaction.getTimestampMillis();
            if (timestamp <= lastTransactionImportedTimestamp) {
                continue;
            }
            if (timestamp > latestTransactionSeenTimestamp) {
                latestTransactionSeenTimestamp = timestamp;
            }

            if (transaction.getType() == ParsedTransaction.TransactionType.CREDIT) {
                SalaryDetector.SalaryDetectionResult salary = salaryDetector.detect(transaction);
                if (salary != null) {
                    salaryOrchestrator.onSalaryDetected(
                            salary.getAmountCents(),
                            salary.getTimestampMillis(),
                            IngestionEvent.SOURCE_SMS,
                            buildSalarySourceKey(transaction),
                            transaction.getOriginalMessage());
                }
                continue;
            }

            if (transaction.getType() != ParsedTransaction.TransactionType.DEBIT) {
                continue;
            }

            LocalDate date = Instant.ofEpochMilli(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            long dateEpochDay = date.toEpochDay();

            long categoryId = SmsExpenseClassifier.resolveCategoryId(categories, transaction);
            String description = SmsExpenseClassifier.resolveDescription(transaction, "SMS Import");

            int duplicateCount = db.expenseDao().countByDateAmountDescription(
                    dateEpochDay,
                    transaction.getAmountCents(),
                    description);
            if (duplicateCount > 0) {
                // Expense already exists — but still create/update ingestion event
                // for source traceability (backfills sender/body for legacy data)
                String expenseSourceKey = buildExpenseSourceKey(transaction);
                new com.spendwise.ai.engine.EventIngestionPipeline().ingest(
                        db,
                        "EXPENSE",
                        com.spendwise.ai.data.entity.IngestionEvent.SOURCE_SMS,
                        expenseSourceKey,
                        null,
                        "DEBIT",
                        transaction.getAmountCents(),
                        timestamp,
                        transaction.getSender(),
                        transaction.getOriginalMessage(),
                        transaction.getOriginalMessage());

                // Backfill canonicalSourceKey on the existing expense if missing
                Expense existing = db.expenseDao().getByDateAmountDescription(
                        dateEpochDay, transaction.getAmountCents(), description);
                if (existing != null
                        && (existing.getCanonicalSourceKey() == null
                                || existing.getCanonicalSourceKey().isEmpty())) {
                    existing.setCanonicalSourceKey(expenseSourceKey);
                    db.expenseDao().update(existing);
                }
                continue;
            }

            Long merchantId = resolveMerchantId(db, transaction.getMerchant());
            Long accountId = SmsExpenseClassifier.resolveAccountId(
                    accounts,
                    transaction);
            Expense expense = new Expense(
                    dateEpochDay,
                    transaction.getAmountCents(),
                    description,
                    categoryId,
                    merchantId);
            if (accountId != null && accountId > 0) {
                expense.setAccountId(accountId);
            }

            // Build source key for traceability
            String expenseSourceKey = buildExpenseSourceKey(transaction);
            expense.setCanonicalSourceKey(expenseSourceKey);
            long insertedId = db.expenseDao().insert(expense);

            // Create ingestion event so sender + body are preserved
            new com.spendwise.ai.engine.EventIngestionPipeline().ingest(
                    db,
                    "EXPENSE",
                    com.spendwise.ai.data.entity.IngestionEvent.SOURCE_SMS,
                    expenseSourceKey,
                    null,
                    "DEBIT",
                    transaction.getAmountCents(),
                    timestamp,
                    transaction.getSender(),
                    transaction.getOriginalMessage(),
                    transaction.getOriginalMessage());
        }

        // Parse and ingest investment signals from raw SMS stream.
        List<ParsedInvestmentSignal> investmentSignals = new ArrayList<>();
        for (int i = rawMessages.size() - 1; i >= 0; i--) {
            RawSmsMessage message = rawMessages.get(i);
            long timestamp = message.getTimestampMillis();
            if (repairLookbackDays <= 0 && timestamp <= lastInvestmentImportedTimestamp) {
                continue;
            }
            if (timestamp > latestInvestmentSeenTimestamp) {
                latestInvestmentSeenTimestamp = timestamp;
            }
            ParsedInvestmentSignal signal = InvestmentSmsParser.parse(
                    message.getSender(),
                    message.getBody(),
                    timestamp);
            if (signal != null) {
                investmentSignals.add(signal);
            }
        }

        if (!investmentSignals.isEmpty()) {
            EventIngestionPipeline pipeline = new EventIngestionPipeline();
            List<ParsedInvestmentSignal> acceptedSignals = new ArrayList<>();
            List<Long> acceptedEventIds = new ArrayList<>();
            for (ParsedInvestmentSignal signal : investmentSignals) {
                String sourceKey = InvestmentSignalKeyFactory.buildIngestionSourceKey(signal);
                EventIngestionPipeline.IngestResult event = pipeline.ingest(
                        db,
                        "EXPENSES",
                        "SMS",
                        sourceKey,
                        null,
                        signal.getSignalType().name(),
                        signal.getAmountCents(),
                        signal.getTimestampMillis(),
                        signal.getSender(),
                        signal.getRawMessage(),
                        signal.getRawMessage());
                if (event.duplicate) {
                    continue;
                }
                acceptedSignals.add(signal);
                acceptedEventIds.add(event.eventId);
            }

            if (!acceptedSignals.isEmpty()) {
                InvestmentAutoIngestionEngine engine = new InvestmentAutoIngestionEngine();
                InvestmentAutoIngestionEngine.IngestResult ingestionResult = engine.ingest(db, acceptedSignals);
                OccurrenceReconciliationEngine reconcileEngine = new OccurrenceReconciliationEngine();
                OccurrenceReconciliationEngine.ReconcileResult reconcileResult = reconcileEngine.reconcileSignals(
                        db,
                        acceptedSignals,
                        ingestionResult.investmentIdBySourceKey,
                        ingestionResult.transactionIdBySourceKey,
                        IngestionEvent.SOURCE_SMS);
                if (reconcileResult.confirmed + reconcileResult.adjusted > 0) {
                    new InvestmentGamificationEngine(context).awardSipReconcileXp();
                }
                for (Long eventId : acceptedEventIds) {
                    pipeline.markProcessed(db, eventId, IngestionEvent.STATUS_PROCESSED);
                }
            }
        }

        if (importInvestments && repairLookbackDays > 0) {
            long endEpochDay = LocalDate.now().toEpochDay();
            long startEpochDay = LocalDate.now().minusDays(repairLookbackDays).toEpochDay();
            new ExpenseInvestmentRecoveryEngine().recover(db, startEpochDay, endEpochDay);
        }

        if (importExpenses) {
            SmsSyncState.updateLastTransactionSyncTimestamp(context, latestTransactionSeenTimestamp);
        }
        if (importInvestments) {
            SmsSyncState.updateLastInvestmentSyncTimestamp(context, latestInvestmentSeenTimestamp);
        }
        return Result.success();
    }

    @NonNull
    private String buildSalarySourceKey(@NonNull ParsedTransaction transaction) {
        long day = transaction.getTimestampMillis() / (24L * 60L * 60L * 1000L);
        String account = transaction.getAccountInfo() == null ? "na" : transaction.getAccountInfo();
        return String.format(
                Locale.US,
                "salary_sms_%s_%d_%d",
                sanitize(account),
                transaction.getAmountCents(),
                day);
    }

    @NonNull
    private String buildExpenseSourceKey(@NonNull ParsedTransaction transaction) {
        long day = transaction.getTimestampMillis() / (24L * 60L * 60L * 1000L);
        String merchant = transaction.getMerchant() == null ? "na" : transaction.getMerchant();
        return String.format(
                Locale.US,
                "expense_sms_%s_%d_%d",
                sanitize(merchant),
                transaction.getAmountCents(),
                day);
    }

    @NonNull
    private String sanitize(@NonNull String value) {
        String cleaned = value.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return cleaned.isEmpty() ? "na" : cleaned;
    }

    private Long resolveMerchantId(AppDatabase db, String merchantName) {
        if (merchantName == null || merchantName.trim().isEmpty()) {
            return null;
        }

        String normalized = merchantName.trim();
        Merchant existing = db.merchantDao().findByName(normalized);
        if (existing != null) {
            return existing.getId();
        }

        long insertedId = db.merchantDao().insertIfNotExists(new Merchant(normalized));
        if (insertedId > 0) {
            return insertedId;
        }

        Merchant reloaded = db.merchantDao().findByName(normalized);
        return reloaded != null ? reloaded.getId() : null;
    }
}
