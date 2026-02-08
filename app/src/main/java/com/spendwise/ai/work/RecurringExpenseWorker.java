package com.spendwise.ai.work;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.data.entity.RecurringExpense;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.temporal.TemporalAdjusters;

import java.util.List;

/**
 * WorkManager Worker that processes recurring expenses and generates
 * actual Expense entries when they are due.
 * 
 * This worker runs daily (or on app launch) to check for due recurring
 * expenses.
 */
public class RecurringExpenseWorker extends Worker {

    private static final String TAG = "RecurringExpenseWorker";

    private final AppDatabase db;

    public RecurringExpenseWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        db = AppDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting recurring expense processing");

        try {
            long todayEpochDay = LocalDate.now().toEpochDay();
            List<RecurringExpense> dueExpenses = db.recurringExpenseDao().getDueRecurringExpenses(todayEpochDay);

            Log.d(TAG, "Found " + dueExpenses.size() + " recurring expenses to process");

            for (RecurringExpense recurring : dueExpenses) {
                processRecurringExpense(recurring, todayEpochDay);
            }

            Log.d(TAG, "Recurring expense processing complete");
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error processing recurring expenses", e);
            return Result.retry();
        }
    }

    /**
     * Process a single recurring expense and generate expenses for all due dates.
     */
    private void processRecurringExpense(RecurringExpense recurring, long todayEpochDay) {
        LocalDate lastGenerated = LocalDate.ofEpochDay(recurring.getLastGeneratedEpochDay());
        LocalDate today = LocalDate.ofEpochDay(todayEpochDay);
        LocalDate endDate = recurring.getEndDateEpochDay() != null
                ? LocalDate.ofEpochDay(recurring.getEndDateEpochDay())
                : null;

        LocalDate nextDue = calculateNextDueDate(recurring, lastGenerated);

        while (nextDue != null && !nextDue.isAfter(today)) {
            // Don't generate if past end date
            if (endDate != null && nextDue.isAfter(endDate)) {
                break;
            }

            // Create the expense
            Expense expense = new Expense(
                    nextDue.toEpochDay(),
                    recurring.getAmountCents(),
                    recurring.getDescription(),
                    recurring.getCategoryId(),
                    recurring.getMerchantId());
            db.expenseDao().insert(expense);

            Log.d(TAG, "Generated expense: " + recurring.getDescription() + " for " + nextDue);

            // Update last generated and calculate next
            lastGenerated = nextDue;
            nextDue = calculateNextDueDate(recurring, lastGenerated);
        }

        // Update the last generated date
        db.recurringExpenseDao().updateLastGeneratedDate(recurring.getId(), lastGenerated.toEpochDay());
    }

    /**
     * Calculate the next due date after the given date based on frequency.
     */
    private LocalDate calculateNextDueDate(RecurringExpense recurring, LocalDate afterDate) {
        String frequency = recurring.getFrequency();

        switch (frequency) {
            case RecurringExpense.FREQUENCY_DAILY:
                return afterDate.plusDays(1);

            case RecurringExpense.FREQUENCY_WEEKLY:
                // Get next occurrence of the specified day of week
                DayOfWeek targetDay = DayOfWeek.of(recurring.getDayOfPeriod());
                LocalDate next = afterDate.with(TemporalAdjusters.next(targetDay));
                return next;

            case RecurringExpense.FREQUENCY_MONTHLY:
                // Get next occurrence on the specified day of month
                LocalDate nextMonth = afterDate.plusMonths(1);
                int targetDayOfMonth = Math.min(recurring.getDayOfPeriod(),
                        nextMonth.lengthOfMonth());
                return nextMonth.withDayOfMonth(targetDayOfMonth);

            case RecurringExpense.FREQUENCY_YEARLY:
                // Get next occurrence on the specified month and day
                LocalDate nextYear = afterDate.plusYears(1);
                int targetMonth = recurring.getMonthOfYear();
                int targetDom = Math.min(recurring.getDayOfPeriod(),
                        nextYear.withMonth(targetMonth).lengthOfMonth());
                return nextYear.withMonth(targetMonth).withDayOfMonth(targetDom);

            default:
                Log.w(TAG, "Unknown frequency: " + frequency);
                return null;
        }
    }
}
