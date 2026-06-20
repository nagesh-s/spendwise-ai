package com.spendwise.ai.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.buckets.BucketSalaryPrefs;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.notifications.NotificationHelper;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.YearMonth;

import java.util.Arrays;
import java.util.List;

/**
 * Sends one daily action-based investment digest notification.
 */
public class InvestmentDigestWorker extends Worker {

    public InvestmentDigestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            AppDatabase db = AppDatabase.getInstance(context);
            String monthKey = DateUtils.getMonthKey(YearMonth.now());
            YearMonth month = YearMonth.now();
            long monthStart = month.atDay(1).toEpochDay();
            long monthEnd = month.atEndOfMonth().toEpochDay();

            String allocationStatus = BucketSalaryPrefs.getMonthStatus(context, monthKey);
            int pendingCount = db.investmentOccurrenceDao().countByStatusInWindow(
                    InvestmentOccurrence.STATUS_EXPECTED,
                    monthStart,
                    monthEnd);
            int confirmedCount = db.investmentOccurrenceDao().countByStatusInWindow(
                    InvestmentOccurrence.STATUS_CONFIRMED,
                    monthStart,
                    monthEnd);
            long expectedMonth = db.investmentOccurrenceDao().sumExpectedInWindow(monthStart, monthEnd);
            List<String> resolvedStatuses = Arrays.asList(
                    InvestmentOccurrence.STATUS_CONFIRMED,
                    InvestmentOccurrence.STATUS_ADJUSTED);
            long resolvedMonth = db.investmentOccurrenceDao().sumConfirmedInWindow(
                    resolvedStatuses,
                    monthStart,
                    monthEnd);
            long pendingExpected = db.investmentOccurrenceDao().sumExpectedByStatusInWindow(
                    InvestmentOccurrence.STATUS_EXPECTED,
                    monthStart,
                    monthEnd);
            double weightedProgress = expectedMonth > 0L
                    ? ((resolvedMonth + (pendingExpected * 0.35d)) * 100d) / expectedMonth
                    : 0d;
            double executionScore = expectedMonth > 0L
                    ? (resolvedMonth * 100d) / expectedMonth
                    : 0d;
            int progressRounded = (int) Math.min(100d, Math.round(weightedProgress));
            int executionRounded = (int) Math.min(100d, Math.round(executionScore));

            String title = "Holding automation digest";
            String body = "Status: " + allocationStatus + " • Confirmed SIPs: " + confirmedCount
                    + " • Pending: " + pendingCount
                    + " • Goal: " + progressRounded + "%"
                    + " • Execution: " + executionRounded + "%";
            NotificationHelper.showInvestmentDigestNotification(context, title, body);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
