package com.spendwise.ai.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.IngestionEvent;

import java.util.List;

@Dao
public interface IngestionEventDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(IngestionEvent event);

    @Query("SELECT * FROM ingestion_events WHERE sourceKey = :sourceKey LIMIT 1")
    IngestionEvent getBySourceKey(String sourceKey);

    @Query("SELECT * FROM ingestion_events WHERE normalizedPayloadFingerprint = :fingerprint "
            + "AND eventTimestampMillis BETWEEN :startMillis AND :endMillis "
            + "ORDER BY eventTimestampMillis DESC LIMIT 1")
    IngestionEvent getByFingerprintInWindow(String fingerprint, long startMillis, long endMillis);

    @Query("SELECT * FROM ingestion_events WHERE correlationKey = :correlationKey ORDER BY eventTimestampMillis DESC")
    List<IngestionEvent> getByCorrelationKey(String correlationKey);

    @Query("UPDATE ingestion_events SET status = :status, processedAtMillis = :processedAtMillis WHERE id = :id")
    void markProcessed(long id, String status, long processedAtMillis);

    @Query("SELECT * FROM ingestion_events WHERE domain = :domain AND status = :status ORDER BY eventTimestampMillis ASC")
    List<IngestionEvent> getByDomainAndStatus(String domain, String status);

    @Query("UPDATE ingestion_events SET sourceSender = :sender, sourceBody = :body WHERE sourceKey = :sourceKey AND (sourceSender IS NULL OR sourceSender = '')")
    int backfillSourceInfo(String sourceKey, String sender, String body);
}
