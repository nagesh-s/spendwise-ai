package com.spendwise.ai.engine;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.buckets.BucketSalaryPrefs;
import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Bucket;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.util.DateUtils;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.YearMonth;
import org.threeten.bp.ZoneId;

import java.util.List;

/**
 * Runs fully-automatic monthly salary allocation with idempotency.
 */
public class SalaryAllocationOrchestrator {

    private final Context appContext;
    private final AppDatabase db;
    private final EventIngestionPipeline ingestionPipeline;
    private final InvestmentGamificationEngine gamificationEngine;

    public static class AllocationResult {
        public boolean allocated;
        public boolean duplicate;
        public boolean skipped;
        @NonNull
        public String status = BucketSalaryPrefs.STATUS_PENDING;
        public long salaryAmountCents;
    }

    public SalaryAllocationOrchestrator(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.db = AppDatabase.getInstance(appContext);
        this.ingestionPipeline = new EventIngestionPipeline();
        this.gamificationEngine = new InvestmentGamificationEngine(appContext);
    }

    @NonNull
    public AllocationResult onSalaryDetected(
            long salaryAmountCents,
            long timestampMillis,
            @NonNull String sourceType,
            @NonNull String sourceKey,
            @Nullable String payload) {
        AllocationResult result = new AllocationResult();
        result.salaryAmountCents = salaryAmountCents;

        String monthKey = monthKeyForTimestamp(timestampMillis);
        BucketSalaryPrefs.setLastDetectedSalaryMonth(appContext, monthKey);

        if (BucketSalaryPrefs.hasAllocatedThisMonth(appContext, monthKey)) {
            result.skipped = true;
            result.status = BucketSalaryPrefs.getAllocationStatus(appContext);
            return result;
        }

        EventIngestionPipeline.IngestResult ingestResult = ingestionPipeline.ingest(
                db,
                IngestionEvent.DOMAIN_SALARY,
                sourceType,
                sourceKey,
                "salary_" + monthKey,
                "SALARY_CREDIT",
                salaryAmountCents,
                timestampMillis,
                payload);
        if (ingestResult.duplicate) {
            result.duplicate = true;
            result.status = BucketSalaryPrefs.getMonthStatus(appContext, monthKey);
            return result;
        }

        boolean allocated = allocateForMonthInternal(salaryAmountCents, timestampMillis,
                BucketSalaryPrefs.STATUS_AUTO_ALLOCATED, sourceType, monthKey);
        ingestionPipeline.markProcessed(db, ingestResult.eventId,
                allocated ? IngestionEvent.STATUS_PROCESSED : IngestionEvent.STATUS_FAILED);

        result.allocated = allocated;
        result.status = allocated ? BucketSalaryPrefs.STATUS_AUTO_ALLOCATED : BucketSalaryPrefs.STATUS_PENDING;
        if (allocated) {
            BucketSalaryPrefs.setExpectedSalaryCents(appContext, salaryAmountCents);
            gamificationEngine.awardSalaryAllocationXp();
        }
        return result;
    }

    @NonNull
    public AllocationResult runFallbackIfDue(long nowMillis) {
        AllocationResult result = new AllocationResult();
        YearMonth currentYm = YearMonth.now();
        String monthKey = DateUtils.getMonthKey(currentYm);
        if (BucketSalaryPrefs.hasAllocatedThisMonth(appContext, monthKey)) {
            result.skipped = true;
            result.status = BucketSalaryPrefs.getAllocationStatus(appContext);
            return result;
        }

        long expectedSalaryCents = BucketSalaryPrefs.getExpectedSalaryCents(appContext);
        if (expectedSalaryCents <= 0L) {
            result.skipped = true;
            result.status = BucketSalaryPrefs.STATUS_PENDING;
            return result;
        }

        int expectedPayday = BucketSalaryPrefs.getExpectedPayday(appContext);
        LocalDate today = LocalDate.now();
        if (!isFallbackWindow(today, expectedPayday)) {
            result.skipped = true;
            result.status = BucketSalaryPrefs.STATUS_PENDING;
            return result;
        }

        String sourceKey = "salary_fallback_" + monthKey;
        EventIngestionPipeline.IngestResult ingestResult = ingestionPipeline.ingest(
                db,
                IngestionEvent.DOMAIN_SALARY,
                IngestionEvent.SOURCE_SCHEDULE,
                sourceKey,
                "salary_" + monthKey,
                "SALARY_FALLBACK",
                expectedSalaryCents,
                nowMillis,
                "fallback");
        if (ingestResult.duplicate) {
            result.duplicate = true;
            result.status = BucketSalaryPrefs.getMonthStatus(appContext, monthKey);
            return result;
        }

        boolean allocated = allocateForMonthInternal(expectedSalaryCents, nowMillis,
                BucketSalaryPrefs.STATUS_FALLBACK_ALLOCATED, IngestionEvent.SOURCE_SCHEDULE, monthKey);
        ingestionPipeline.markProcessed(db, ingestResult.eventId,
                allocated ? IngestionEvent.STATUS_PROCESSED : IngestionEvent.STATUS_FAILED);
        result.allocated = allocated;
        result.status = allocated ? BucketSalaryPrefs.STATUS_FALLBACK_ALLOCATED : BucketSalaryPrefs.STATUS_PENDING;
        result.salaryAmountCents = expectedSalaryCents;
        if (allocated) {
            gamificationEngine.awardSalaryAllocationXp();
        }
        return result;
    }

    @NonNull
    public AllocationResult runManualAllocation(long salaryAmountCents) {
        return runManualAllocation(salaryAmountCents, false);
    }

    @NonNull
    public AllocationResult runManualAllocation(long salaryAmountCents, boolean forceOverride) {
        AllocationResult result = new AllocationResult();
        String monthKey = DateUtils.getMonthKey(YearMonth.now());
        if (BucketSalaryPrefs.hasAllocatedThisMonth(appContext, monthKey) && !forceOverride) {
            result.skipped = true;
            result.status = BucketSalaryPrefs.getAllocationStatus(appContext);
            result.salaryAmountCents = salaryAmountCents;
            return result;
        }
        boolean allocated = allocateForMonthInternal(salaryAmountCents, System.currentTimeMillis(),
                BucketSalaryPrefs.STATUS_MANUAL_ALLOCATED, IngestionEvent.SOURCE_MANUAL, monthKey);
        result.allocated = allocated;
        result.salaryAmountCents = salaryAmountCents;
        result.status = allocated ? BucketSalaryPrefs.STATUS_MANUAL_ALLOCATED : BucketSalaryPrefs.STATUS_PENDING;
        if (allocated) {
            BucketSalaryPrefs.setExpectedSalaryCents(appContext, salaryAmountCents);
            gamificationEngine.awardSalaryAllocationXp();
        }
        return result;
    }

    private boolean allocateForMonthInternal(
            long salaryAmountCents,
            long timestampMillis,
            @NonNull String status,
            @NonNull String source,
            @NonNull String monthKey) {
        if (salaryAmountCents <= 0L) {
            return false;
        }

        List<Bucket> buckets = db.bucketDao().getAllBucketsSync();
        if (buckets == null || buckets.isEmpty()) {
            return false;
        }

        long today = LocalDate.now().toEpochDay();
        long monthStart = LocalDate.now().withDayOfMonth(1).toEpochDay();
        long thisMonthSpend = db.expenseDao().getTotalForRange(monthStart, today);

        long emergencyBalance = 0L;
        for (Bucket bucket : buckets) {
            if (Bucket.TYPE_EMERGENCY.equals(bucket.getType())) {
                emergencyBalance = bucket.getCurrentBalanceCents();
                break;
            }
        }

        long avgMonthlySpend = thisMonthSpend > 0 ? thisMonthSpend : (long) (salaryAmountCents * 0.3d);
        CashflowGuardrail guardrail = new CashflowGuardrail();
        CashflowGuardrail.GuardrailResult guardrailResult = guardrail.check(
                buckets,
                emergencyBalance,
                avgMonthlySpend,
                0L,
                thisMonthSpend,
                (long) (salaryAmountCents * 0.2d));

        BucketAllocationEngine allocationEngine = new BucketAllocationEngine();
        BucketAllocationEngine.AllocationPlan plan = allocationEngine.allocate(
                salaryAmountCents,
                buckets,
                guardrailResult);

        for (BucketAllocationEngine.AllocationResult allocation : plan.getAllocations()) {
            db.bucketDao().updateBalance(allocation.getBucketId(), allocation.getAllocatedAmountCents());
        }

        BucketSalaryPrefs.setLastAllocatedMonth(appContext, monthKey);
        BucketSalaryPrefs.setAllocationStatus(appContext, status, source, timestampMillis);
        return true;
    }

    private boolean isFallbackWindow(@NonNull LocalDate today, int expectedPayday) {
        int day = today.getDayOfMonth();
        if (day >= expectedPayday) {
            return true;
        }
        // Grace window for delayed salary posting at start of month.
        return day <= 3 && expectedPayday >= 28;
    }

    @NonNull
    private String monthKeyForTimestamp(long timestampMillis) {
        return YearMonth.from(Instant.ofEpochMilli(timestampMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()).format(org.threeten.bp.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }
}
