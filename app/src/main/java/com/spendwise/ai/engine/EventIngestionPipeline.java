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
    private static final long EXACT_PAYLOAD_WINDOW_MILLIS = 5L * 60L * 1000L;

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
            @Nullable String sourceSender,
            @Nullable String sourceBody,
            @Nullable String payloadHashBase) {
        String fingerprint = payloadHashBase == null ? null : normalizedFingerprint(payloadHashBase);
        IngestionEvent existingBySource = db.ingestionEventDao().getBySourceKey(sourceKey);
        if (existingBySource != null) {
            return new IngestResult(false, true, existingBySource.getId());
        }
        if (fingerprint != null) {
            IngestionEvent existingByPayload = db.ingestionEventDao().getByFingerprintInWindow(
                    fingerprint,
                    eventTimestampMillis - EXACT_PAYLOAD_WINDOW_MILLIS,
                    eventTimestampMillis + EXACT_PAYLOAD_WINDOW_MILLIS);
            if (existingByPayload != null) {
                return new IngestResult(false, true, existingByPayload.getId());
            }
        }

        IngestionEvent event = new IngestionEvent(
                domain,
                sourceType,
                sourceKey,
                correlationKey,
                eventType,
                amountCents,
                eventTimestampMillis,
                sourceSender,
                sourceBody,
                payloadHashBase == null ? null : hashPayload(payloadHashBase));
        event.setNormalizedPayloadFingerprint(fingerprint);

        long id = db.ingestionEventDao().insertIgnore(event);
        if (id <= 0) {
            // Duplicate event detected — backfill sender/body if missing
            if (sourceSender != null && !sourceSender.isEmpty()) {
                db.ingestionEventDao().backfillSourceInfo(sourceKey, sourceSender,
                        sourceBody != null ? sourceBody : "");
            }
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

    @NonNull
    private String normalizedFingerprint(@NonNull String payload) {
        String normalized = payload.toLowerCase(java.util.Locale.US)
                .replaceAll("\\s+", " ")
                .trim();
        return hashPayload(normalized);
    }
}
