package com.spendwise.ai.work;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.spendwise.ai.engine.SalaryAllocationOrchestrator;

/**
 * Daily fallback worker: if salary signal is missing, auto-allocate on expected payday.
 */
public class SalaryAutomationWorker extends Worker {

    public SalaryAutomationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SalaryAllocationOrchestrator orchestrator = new SalaryAllocationOrchestrator(getApplicationContext());
            orchestrator.runFallbackIfDue(System.currentTimeMillis());
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
