package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.data.entity.InvestmentSchedule;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;

import java.util.List;
import java.util.Map;

/**
 * Reconciles confirmed investment signals against expected SIP occurrences.
 */
public class OccurrenceReconciliationEngine {

    private static final int DAY_WINDOW = 10;

    public static class ReconcileResult {
        public int confirmed;
        public int adjusted;
        public int skipped;
    }

    @NonNull
    public ReconcileResult reconcileSignals(
            @NonNull AppDatabase db,
            @NonNull List<ParsedInvestmentSignal> signals,
            @Nullable Map<String, Long> linkedInvestmentBySourceKey,
            @NonNull String sourceType) {
        ReconcileResult result = new ReconcileResult();
        for (ParsedInvestmentSignal signal : signals) {
            long investmentId = -1L;
            if (linkedInvestmentBySourceKey != null && linkedInvestmentBySourceKey.containsKey(signal.getSourceKey())) {
                Long linkedId = linkedInvestmentBySourceKey.get(signal.getSourceKey());
                investmentId = linkedId == null ? -1L : linkedId;
            }
            if (reconcileSingle(db, signal, investmentId, sourceType)) {
                if (signal.isCorrection()) {
                    result.adjusted++;
                } else {
                    result.confirmed++;
                }
            } else {
                result.skipped++;
            }
        }
        return result;
    }

    public boolean reconcileSingle(
            @NonNull AppDatabase db,
            @NonNull ParsedInvestmentSignal signal,
            long linkedInvestmentId,
            @NonNull String sourceType) {
        InvestmentOccurrence byCorrelation = findByCorrelation(db, signal.getCorrelationKey());
        if (byCorrelation != null) {
            applyResolution(db, byCorrelation, signal, linkedInvestmentId, sourceType);
            return true;
        }

        long day = Instant.ofEpochMilli(signal.getTimestampMillis())
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .toEpochDay();

        List<InvestmentOccurrence> candidates = db.investmentOccurrenceDao()
                .getByStatusInWindowSync(InvestmentOccurrence.STATUS_EXPECTED, day - DAY_WINDOW, day + DAY_WINDOW);
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }

        InvestmentOccurrence best = null;
        double bestScore = 0d;
        for (InvestmentOccurrence occurrence : candidates) {
            InvestmentSchedule schedule = db.investmentScheduleDao().getById(occurrence.getScheduleId());
            if (schedule == null || !schedule.isActive()) {
                continue;
            }
            double score = score(schedule, occurrence, signal, day);
            if (score > bestScore) {
                bestScore = score;
                best = occurrence;
            }
        }

        if (best == null || bestScore < 0.35d) {
            return false;
        }

        applyResolution(db, best, signal, linkedInvestmentId, sourceType);
        return true;
    }

    @Nullable
    private InvestmentOccurrence findByCorrelation(@NonNull AppDatabase db, @Nullable String correlationKey) {
        if (correlationKey == null || correlationKey.trim().isEmpty()) {
            return null;
        }
        List<InvestmentOccurrence> rows = db.investmentOccurrenceDao().getByCorrelationKeySync(correlationKey);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return rows.get(0);
    }

    private void applyResolution(
            @NonNull AppDatabase db,
            @NonNull InvestmentOccurrence occurrence,
            @NonNull ParsedInvestmentSignal signal,
            long linkedInvestmentId,
            @NonNull String sourceType) {
        String status = signal.isCorrection() ? InvestmentOccurrence.STATUS_ADJUSTED : InvestmentOccurrence.STATUS_CONFIRMED;
        long resolvedAmount = signal.getAmountCents() > 0 ? signal.getAmountCents() : occurrence.getExpectedAmountCents();
        db.investmentOccurrenceDao().markResolved(
                occurrence.getId(),
                status,
                resolvedAmount,
                signal.getTimestampMillis(),
                linkedInvestmentId > 0 ? linkedInvestmentId : null,
                null,
                signal.getCorrelationKey(),
                sourceType);
    }

    private double score(
            @NonNull InvestmentSchedule schedule,
            @NonNull InvestmentOccurrence occurrence,
            @NonNull ParsedInvestmentSignal signal,
            long signalDay) {
        double score = 0d;

        if (schedule.getAssetType().equals(signal.getAssetType())) {
            score += 0.4d;
        }

        long expected = Math.max(1L, occurrence.getExpectedAmountCents());
        long actual = Math.max(1L, signal.getAmountCents());
        double amountDelta = Math.abs(expected - actual) / (double) expected;
        if (amountDelta <= 0.05d) {
            score += 0.35d;
        } else if (amountDelta <= 0.15d) {
            score += 0.2d;
        }

        long dayDelta = Math.abs(occurrence.getDueEpochDay() - signalDay);
        if (dayDelta <= 2) {
            score += 0.2d;
        } else if (dayDelta <= 7) {
            score += 0.1d;
        }

        String schedulePlatform = normalize(schedule.getPlatform());
        String signalPlatform = normalize(signal.getPlatform());
        if (!schedulePlatform.isEmpty() && !signalPlatform.isEmpty() && signalPlatform.contains(schedulePlatform)) {
            score += 0.15d;
        }

        return score;
    }

    @NonNull
    private String normalize(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase().replaceAll("\\s+", " ").trim();
    }
}
