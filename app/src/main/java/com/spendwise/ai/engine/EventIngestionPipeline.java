package com.spendwise.ai.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.IngestionEvent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Canonical event ingestion with strict idempotency via sourceKey.
 */
public class EventIngestionPipeline {

    public static class IngestResult {
        public final boolean inserted;
        public final boolean duplicate;
        public final long eventId;

        public IngestResult(boolean inserted, boolean duplicate, long eventId) {
            this.inserted = inserted;
            this.duplicate = duplicate;
            this.eventId = eventId;
        }
    }

    @NonNull
    public IngestResult ingest(
            @NonNull AppDatabase db,
            @NonNull String domain,
            @NonNull String sourceType,
            @NonNull String sourceKey,
            @Nullable String correlationKey,
            @NonNull String eventType,
            long amountCents,
            long eventTimestampMillis,
            @Nullable String payload) {
        IngestionEvent event = new IngestionEvent(
                domain,
                sourceType,
                sourceKey,
                correlationKey,
                eventType,
                amountCents,
                eventTimestampMillis,
                payload == null ? null : hashPayload(payload));

        long id = db.ingestionEventDao().insertIgnore(event);
        if (id <= 0) {
            IngestionEvent existing = db.ingestionEventDao().getBySourceKey(sourceKey);
            long existingId = existing != null ? existing.getId() : -1L;
            return new IngestResult(false, true, existingId);
        }
        return new IngestResult(true, false, id);
    }

    public void markProcessed(@NonNull AppDatabase db, long eventId, @NonNull String status) {
        if (eventId <= 0) {
            return;
        }
        db.ingestionEventDao().markProcessed(eventId, status, System.currentTimeMillis());
    }

    @NonNull
    private String hashPayload(@NonNull String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(payload.hashCode());
        }
    }
}
