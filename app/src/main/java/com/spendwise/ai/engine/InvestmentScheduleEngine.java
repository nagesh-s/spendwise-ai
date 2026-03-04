package com.spendwise.ai.engine;

import androidx.annotation.NonNull;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.data.entity.InvestmentSchedule;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.TemporalAdjusters;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates expected SIP occurrences from recurring investment schedules.
 */
public class InvestmentScheduleEngine {

    private static final int DETAILED_BACKFILL_MONTHS = 24;

    public int generateDueOccurrences(@NonNull AppDatabase db, long todayEpochDay) {
        int generated = 0;
        List<InvestmentSchedule> schedules = db.investmentScheduleDao().getActiveSchedulesSync();
        if (schedules == null || schedules.isEmpty()) {
            return generated;
        }
        for (InvestmentSchedule schedule : schedules) {
            generated += generateDueOccurrencesForSchedule(db, schedule, todayEpochDay);
        }
        return generated;
    }

    public int generateDueOccurrencesForSchedule(
            @NonNull AppDatabase db,
            @NonNull InvestmentSchedule schedule,
            long todayEpochDay) {
        if (!schedule.isActive()) {
            return 0;
        }

        LocalDate today = LocalDate.ofEpochDay(todayEpochDay);
        LocalDate end = schedule.getEndEpochDay() == null
                ? today
                : LocalDate.ofEpochDay(Math.min(todayEpochDay, schedule.getEndEpochDay()));

        Long maxDueEpoch = db.investmentOccurrenceDao().getMaxDueEpochDayForSchedule(schedule.getId());
        LocalDate start;
        int inserted = 0;

        if (maxDueEpoch == null) {
            start = LocalDate.ofEpochDay(schedule.getStartEpochDay());
            if (InvestmentSchedule.FREQUENCY_MONTHLY.equals(schedule.getFrequency())) {
                LocalDate capStart = today.minusMonths(DETAILED_BACKFILL_MONTHS);
                if (start.isBefore(capStart)) {
                    long aggregateAmount = buildAggregatedBackfill(db, schedule, start, capStart.minusDays(1));
                    if (aggregateAmount > 0L) {
                        InvestmentOccurrence aggregate = new InvestmentOccurrence(
                                schedule.getId(),
                                capStart.minusDays(1).toEpochDay(),
                                aggregateAmount);
                        aggregate.setSourceType(IngestionEvent.SOURCE_SCHEDULE);
                        aggregate.setCorrelationKey("backfill_agg_" + schedule.getId() + "_" + capStart.toEpochDay());
                        long id = db.investmentOccurrenceDao().insertIgnore(aggregate);
                        if (id > 0) {
                            inserted++;
                        }
                    }
                    start = capStart;
                }
            }
            start = alignFirstDue(schedule, start);
        } else {
            start = nextDueDate(schedule, LocalDate.ofEpochDay(maxDueEpoch));
        }

        if (start == null || start.isAfter(end)) {
            return inserted;
        }

        List<InvestmentOccurrence> batch = new ArrayList<>();
        LocalDate cursor = start;
        int safety = 0;
        while (!cursor.isAfter(end) && safety < 4000) {
            InvestmentOccurrence occurrence = new InvestmentOccurrence(
                    schedule.getId(),
                    cursor.toEpochDay(),
                    schedule.getAmountCents());
            occurrence.setSourceType(IngestionEvent.SOURCE_SCHEDULE);
            occurrence.setCorrelationKey("sched_" + schedule.getId() + "_" + cursor.toEpochDay());
            batch.add(occurrence);
            cursor = nextDueDate(schedule, cursor);
            if (cursor == null) {
                break;
            }
            safety++;
        }

        if (!batch.isEmpty()) {
            List<Long> ids = db.investmentOccurrenceDao().insertAllIgnore(batch);
            for (Long id : ids) {
                if (id != null && id > 0) {
                    inserted++;
                }
            }
        }
        return inserted;
    }

    private long buildAggregatedBackfill(
            @NonNull AppDatabase db,
            @NonNull InvestmentSchedule schedule,
            @NonNull LocalDate start,
            @NonNull LocalDate end) {
        if (end.isBefore(start)) {
            return 0L;
        }
        LocalDate cursor = alignFirstDue(schedule, start);
        long total = 0L;
        int safety = 0;
        while (cursor != null && !cursor.isAfter(end) && safety < 4000) {
            total += schedule.getAmountCents();
            cursor = nextDueDate(schedule, cursor);
            safety++;
        }
        return total;
    }

    private LocalDate alignFirstDue(@NonNull InvestmentSchedule schedule, @NonNull LocalDate start) {
        switch (schedule.getFrequency()) {
            case InvestmentSchedule.FREQUENCY_DAILY:
            case InvestmentSchedule.FREQUENCY_EVERY_2_DAYS:
            case InvestmentSchedule.FREQUENCY_EVERY_3_DAYS:
                return start;
            case InvestmentSchedule.FREQUENCY_WEEKLY: {
                int day = Math.max(1, Math.min(7, schedule.getDayOfPeriod()));
                DayOfWeek dow = DayOfWeek.of(day);
                LocalDate aligned = start.with(TemporalAdjusters.nextOrSame(dow));
                return aligned;
            }
            case InvestmentSchedule.FREQUENCY_MONTHLY: {
                int targetDay = Math.max(1, Math.min(31, schedule.getDayOfPeriod()));
                LocalDate sameMonth = start.withDayOfMonth(Math.min(targetDay, start.lengthOfMonth()));
                if (sameMonth.isBefore(start)) {
                    LocalDate nextMonth = start.plusMonths(1);
                    return nextMonth.withDayOfMonth(Math.min(targetDay, nextMonth.lengthOfMonth()));
                }
                return sameMonth;
            }
            case InvestmentSchedule.FREQUENCY_YEARLY: {
                int targetDay = Math.max(1, Math.min(31, schedule.getDayOfPeriod()));
                int targetMonth = LocalDate.ofEpochDay(schedule.getStartEpochDay()).getMonthValue();
                LocalDate sameYear = start.withMonth(targetMonth)
                        .withDayOfMonth(Math.min(targetDay, start.withMonth(targetMonth).lengthOfMonth()));
                if (sameYear.isBefore(start)) {
                    LocalDate nextYear = start.plusYears(1).withMonth(targetMonth);
                    return nextYear.withDayOfMonth(Math.min(targetDay, nextYear.lengthOfMonth()));
                }
                return sameYear;
            }
            default:
                return start;
        }
    }

    private LocalDate nextDueDate(@NonNull InvestmentSchedule schedule, @NonNull LocalDate after) {
        switch (schedule.getFrequency()) {
            case InvestmentSchedule.FREQUENCY_DAILY:
                return after.plusDays(1);
            case InvestmentSchedule.FREQUENCY_EVERY_2_DAYS:
                return after.plusDays(2);
            case InvestmentSchedule.FREQUENCY_EVERY_3_DAYS:
                return after.plusDays(3);
            case InvestmentSchedule.FREQUENCY_WEEKLY:
                return after.plusWeeks(1);
            case InvestmentSchedule.FREQUENCY_MONTHLY: {
                int targetDay = Math.max(1, Math.min(31, schedule.getDayOfPeriod()));
                LocalDate next = after.plusMonths(1);
                return next.withDayOfMonth(Math.min(targetDay, next.lengthOfMonth()));
            }
            case InvestmentSchedule.FREQUENCY_YEARLY: {
                int targetDay = Math.max(1, Math.min(31, schedule.getDayOfPeriod()));
                int month = LocalDate.ofEpochDay(schedule.getStartEpochDay()).getMonthValue();
                LocalDate next = after.plusYears(1).withMonth(month);
                return next.withDayOfMonth(Math.min(targetDay, next.lengthOfMonth()));
            }
            default:
                return null;
        }
    }
}
