package com.spendwise.ai.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.Merchant;
import com.spendwise.ai.engine.InvestmentAutoIngestionEngine;
import com.spendwise.ai.sms.InvestmentSmsParser;
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

/**
 * Periodic background worker that scans SMS and auto-imports debit transactions.
 */
public class SmsAutoImportWorker extends Worker {

    private static final int LOOKBACK_DAYS = 30;
    private static final int FALLBACK_SCAN_DAYS = 120;

    public SmsAutoImportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!SmsPermissionHelper.hasReadSmsPermission(context)) {
            return Result.success();
        }

        long lastImportedTimestamp = SmsSyncState.getLastSyncTimestamp(context);
        long latestSeenTimestamp = lastImportedTimestamp;

        AppDatabase db = AppDatabase.getInstance(context);
        List<ParsedTransaction> transactions = SmsPermissionHelper.readTransactionsSince(
                context,
                lastImportedTimestamp,
                LOOKBACK_DAYS);
        if (transactions.isEmpty()) {
            transactions = SmsPermissionHelper.readRecentTransactions(context, FALLBACK_SCAN_DAYS);
        }
        List<RawSmsMessage> rawMessages = SmsPermissionHelper.readMessagesSince(
                context,
                lastImportedTimestamp,
                LOOKBACK_DAYS);
        if (rawMessages.isEmpty()) {
            rawMessages = SmsPermissionHelper.readRecentMessages(context, FALLBACK_SCAN_DAYS);
        }

        List<com.spendwise.ai.data.entity.Category> categories = db.categoryDao().getAllCategoriesSync();
        List<com.spendwise.ai.data.entity.Account> accounts = db.accountDao().getAllAccountsSync();

        for (int i = transactions.size() - 1; i >= 0; i--) {
            ParsedTransaction transaction = transactions.get(i);
            if (transaction.getType() != ParsedTransaction.TransactionType.DEBIT) {
                continue;
            }

            long timestamp = transaction.getTimestampMillis();
            if (timestamp <= lastImportedTimestamp) {
                continue;
            }
            if (timestamp > latestSeenTimestamp) {
                latestSeenTimestamp = timestamp;
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
            db.expenseDao().insert(expense);
        }

        // Parse and ingest investment signals from raw SMS stream.
        List<ParsedInvestmentSignal> investmentSignals = new ArrayList<>();
        for (int i = rawMessages.size() - 1; i >= 0; i--) {
            RawSmsMessage message = rawMessages.get(i);
            long timestamp = message.getTimestampMillis();
            if (timestamp <= lastImportedTimestamp) {
                continue;
            }
            if (timestamp > latestSeenTimestamp) {
                latestSeenTimestamp = timestamp;
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
            InvestmentAutoIngestionEngine engine = new InvestmentAutoIngestionEngine();
            engine.ingest(db, investmentSignals);
        }

        SmsSyncState.updateLastSyncTimestamp(context, latestSeenTimestamp);
        return Result.success();
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
