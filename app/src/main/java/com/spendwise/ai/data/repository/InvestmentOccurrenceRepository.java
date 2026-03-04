package com.spendwise.ai.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.InvestmentOccurrenceDao;
import com.spendwise.ai.data.pojo.InvestmentOccurrenceWithSchedule;

import java.util.List;

public class InvestmentOccurrenceRepository {

    private final InvestmentOccurrenceDao dao;

    public InvestmentOccurrenceRepository(Application application) {
        dao = AppDatabase.getInstance(application).investmentOccurrenceDao();
    }

    public LiveData<List<InvestmentOccurrenceWithSchedule>> getOccurrencesForBucket(long bucketId) {
        return dao.getOccurrencesForBucket(bucketId);
    }

    public void resolveOccurrence(
            long id,
            String status,
            long amountCents,
            long confirmedAtMillis,
            Long linkedInvestmentId,
            String correlationKey,
            String sourceType) {
        AppDatabase.databaseWriteExecutor.execute(() -> dao.markResolved(
                id,
                status,
                amountCents,
                confirmedAtMillis,
                linkedInvestmentId,
                correlationKey,
                sourceType));
    }
}
