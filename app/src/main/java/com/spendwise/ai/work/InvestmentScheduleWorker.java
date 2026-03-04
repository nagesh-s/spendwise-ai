package com.spendwise.ai.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.engine.InvestmentScheduleEngine;

import org.threeten.bp.LocalDate;

/**
 * Generates expected SIP occurrences from active investment schedules.
 */
public class InvestmentScheduleWorker extends Worker {

    public InvestmentScheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase db = AppDatabase.getInstance(getApplicationContext());
            InvestmentScheduleEngine engine = new InvestmentScheduleEngine();
            engine.generateDueOccurrences(db, LocalDate.now().toEpochDay());
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
