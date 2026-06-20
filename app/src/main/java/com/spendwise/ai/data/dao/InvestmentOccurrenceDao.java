package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.InvestmentOccurrence;
import com.spendwise.ai.data.pojo.InvestmentOccurrenceWithSchedule;

import java.util.List;

@Dao
public interface InvestmentOccurrenceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(InvestmentOccurrence occurrence);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    List<Long> insertAllIgnore(List<InvestmentOccurrence> occurrences);

    @Update
    void update(InvestmentOccurrence occurrence);

    @Query("SELECT * FROM investment_occurrences WHERE id = :id")
    InvestmentOccurrence getById(long id);

    @Query("SELECT * FROM investment_occurrences WHERE scheduleId = :scheduleId ORDER BY dueEpochDay DESC")
    List<InvestmentOccurrence> getByScheduleSync(long scheduleId);

    @Query("SELECT MAX(dueEpochDay) FROM investment_occurrences WHERE scheduleId = :scheduleId")
    Long getMaxDueEpochDayForSchedule(long scheduleId);

    @Query("SELECT * FROM investment_occurrences WHERE status = :status AND dueEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dueEpochDay ASC")
    List<InvestmentOccurrence> getByStatusInWindowSync(String status, long startEpochDay, long endEpochDay);

    @Query("SELECT * FROM investment_occurrences WHERE status = :status ORDER BY dueEpochDay ASC")
    List<InvestmentOccurrence> getByStatusSync(String status);

    @Query("SELECT * FROM investment_occurrences WHERE correlationKey = :correlationKey ORDER BY id DESC")
    List<InvestmentOccurrence> getByCorrelationKeySync(String correlationKey);

    @Query("SELECT io.*, s.name AS scheduleName, s.assetType AS scheduleAssetType, s.platform AS schedulePlatform, s.bucketId AS scheduleBucketId " +
            "FROM investment_occurrences io " +
            "JOIN investment_schedules s ON s.id = io.scheduleId " +
            "WHERE s.bucketId = :bucketId " +
            "ORDER BY io.dueEpochDay DESC")
    LiveData<List<InvestmentOccurrenceWithSchedule>> getOccurrencesForBucket(long bucketId);

    @Query("UPDATE investment_occurrences SET status = :status, confirmedAmountCents = :amountCents, confirmedAtMillis = :confirmedAtMillis, " +
            "linkedInvestmentId = :linkedInvestmentId, transactionId = :transactionId, "
            + "correlationKey = :correlationKey, sourceType = :sourceType WHERE id = :id")
    void markResolved(long id, String status, long amountCents, long confirmedAtMillis,
            Long linkedInvestmentId, Long transactionId, String correlationKey, String sourceType);

    @Query("SELECT COUNT(*) FROM investment_occurrences WHERE status = :status")
    int countByStatus(String status);

    @Query("SELECT COUNT(*) FROM investment_occurrences WHERE status = :status AND dueEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    int countByStatusInWindow(String status, long startEpochDay, long endEpochDay);

    @Query("SELECT COALESCE(SUM(expectedAmountCents), 0) FROM investment_occurrences WHERE dueEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    long sumExpectedInWindow(long startEpochDay, long endEpochDay);

    @Query("SELECT COALESCE(SUM(confirmedAmountCents), 0) FROM investment_occurrences WHERE status IN (:statuses) AND dueEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    long sumConfirmedInWindow(List<String> statuses, long startEpochDay, long endEpochDay);

    @Query("SELECT COALESCE(SUM(expectedAmountCents), 0) FROM investment_occurrences WHERE status = :status AND dueEpochDay BETWEEN :startEpochDay AND :endEpochDay")
    long sumExpectedByStatusInWindow(String status, long startEpochDay, long endEpochDay);
}
