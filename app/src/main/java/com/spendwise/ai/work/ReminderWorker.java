package com.spendwise.ai.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.RecurringExpense;
import com.spendwise.ai.notifications.NotificationHelper;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.TemporalAdjusters;

import java.util.List;

/**
 * WorkManager Worker that checks for upcoming recurring expenses
 * and sends reminder notifications.
 */
public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";

    private final AppDatabase db;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting reminder check");

        try {
            List<RecurringExpense> expensesWithReminders = db.recurringExpenseDao().getRecurringExpensesWithReminders();

            LocalDate today = LocalDate.now();

            for (RecurringExpense expense : expensesWithReminders) {
                checkAndSendReminder(expense, today);
            }

            Log.d(TAG, "Reminder check complete");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error checking reminders", e);
            return Result.retry();
        }
    }

    private void checkAndSendReminder(RecurringExpense expense, LocalDate today) {
        LocalDate nextDue = calculateNextDueDate(expense);

        if (nextDue == null)
            return;

        // Check if end date has passed
        if (expense.getEndDateEpochDay() != null &&
                nextDue.toEpochDay() > expense.getEndDateEpochDay()) {
            return;
        }

        long daysUntilDue = nextDue.toEpochDay() - today.toEpochDay();

        // Check if we should send reminder
        if (daysUntilDue >= 0 && daysUntilDue <= expense.getReminderDaysBefore()) {
            NotificationHelper.showBillReminderNotification(
                    getApplicationContext(),
                    expense.getId(),
                    expense.getDescription(),
                    expense.getDescription(),
                    expense.getAmountCents(),
                    (int) daysUntilDue);
            Log.d(TAG, "Sent reminder for: " + expense.getDescription() +
                    ", due in " + daysUntilDue + " days");
        }
    }

    private LocalDate calculateNextDueDate(RecurringExpense expense) {
        LocalDate lastGenerated = LocalDate.ofEpochDay(expense.getLastGeneratedEpochDay());
        String frequency = expense.getFrequency();

        switch (frequency) {
            case RecurringExpense.FREQUENCY_DAILY:
                return lastGenerated.plusDays(1);

            case RecurringExpense.FREQUENCY_WEEKLY:
                DayOfWeek targetDay = DayOfWeek.of(expense.getDayOfPeriod());
                return lastGenerated.with(TemporalAdjusters.next(targetDay));

            case RecurringExpense.FREQUENCY_MONTHLY:
                LocalDate nextMonth = lastGenerated.plusMonths(1);
                int targetDayOfMonth = Math.min(expense.getDayOfPeriod(),
                        nextMonth.lengthOfMonth());
                return nextMonth.withDayOfMonth(targetDayOfMonth);

            case RecurringExpense.FREQUENCY_YEARLY:
                LocalDate nextYear = lastGenerated.plusYears(1);
                int targetMonth = expense.getMonthOfYear();
                int targetDom = Math.min(expense.getDayOfPeriod(),
                        nextYear.withMonth(targetMonth).lengthOfMonth());
                return nextYear.withMonth(targetMonth).withDayOfMonth(targetDom);

            default:
                return null;
        }
    }
}
