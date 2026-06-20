package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Canonical ingestion registry event used for strict cross-source idempotency.
 */
@Entity(tableName = "ingestion_events", indices = {
        @Index(value = { "sourceKey" }, unique = true),
        @Index("normalizedPayloadFingerprint"),
        @Index("correlationKey"),
        @Index("eventTimestampMillis"),
        @Index("status")
})
public class IngestionEvent {

    public static final String DOMAIN_SALARY = "SALARY";
    public static final String DOMAIN_INVESTMENT = "INVESTMENT";

    public static final String SOURCE_SMS = "SMS";
    public static final String SOURCE_NOTIFICATION = "NOTIFICATION";
    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_SCHEDULE = "SCHEDULE";
    public static final String SOURCE_EXPENSE_RECOVERY = "EXPENSE_RECOVERY";

    public static final String STATUS_RECEIVED = "RECEIVED";
    public static final String STATUS_PROCESSED = "PROCESSED";
    public static final String STATUS_SKIPPED = "SKIPPED";
    public static final String STATUS_FAILED = "FAILED";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String domain;

    @NonNull
    private String sourceType;

    @NonNull
    private String sourceKey;

    @Nullable
    private String correlationKey;

    @NonNull
    private String eventType;

    private long amountCents;

    private long eventTimestampMillis;

    @Nullable
    private String sourceSender;

    @Nullable
    private String sourceBody;

    @Nullable
    private String payloadHash;

    @Nullable
    private String normalizedPayloadFingerprint;

    @Nullable
    private Long processedAtMillis;

    @NonNull
    private String status;

    public IngestionEvent(
            @NonNull String domain,
            @NonNull String sourceType,
            @NonNull String sourceKey,
            @Nullable String correlationKey,
            @NonNull String eventType,
            long amountCents,
            long eventTimestampMillis,
            @Nullable String sourceSender,
            @Nullable String sourceBody,
            @Nullable String payloadHash) {
        this.domain = domain;
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
        this.correlationKey = correlationKey;
        this.eventType = eventType;
        this.amountCents = amountCents;
        this.eventTimestampMillis = eventTimestampMillis;
        this.sourceSender = sourceSender;
        this.sourceBody = sourceBody;
        this.payloadHash = payloadHash;
        this.status = STATUS_RECEIVED;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getDomain() {
        return domain;
    }

    public void setDomain(@NonNull String domain) {
        this.domain = domain;
    }

    @NonNull
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(@NonNull String sourceType) {
        this.sourceType = sourceType;
    }

    @NonNull
    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(@NonNull String sourceKey) {
        this.sourceKey = sourceKey;
    }

    @Nullable
    public String getCorrelationKey() {
        return correlationKey;
    }

    public void setCorrelationKey(@Nullable String correlationKey) {
        this.correlationKey = correlationKey;
    }

    @NonNull
    public String getEventType() {
        return eventType;
    }

    public void setEventType(@NonNull String eventType) {
        this.eventType = eventType;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }

    public long getEventTimestampMillis() {
        return eventTimestampMillis;
    }

    public void setEventTimestampMillis(long eventTimestampMillis) {
        this.eventTimestampMillis = eventTimestampMillis;
    }

    @Nullable
    public String getSourceSender() {
        return sourceSender;
    }

    public void setSourceSender(@Nullable String sourceSender) {
        this.sourceSender = sourceSender;
    }

    @Nullable
    public String getSourceBody() {
        return sourceBody;
    }

    public void setSourceBody(@Nullable String sourceBody) {
        this.sourceBody = sourceBody;
    }

    @Nullable
    public String getPayloadHash() {
        return payloadHash;
    }

    public void setPayloadHash(@Nullable String payloadHash) {
        this.payloadHash = payloadHash;
    }

    @Nullable
    public String getNormalizedPayloadFingerprint() {
        return normalizedPayloadFingerprint;
    }

    public void setNormalizedPayloadFingerprint(@Nullable String normalizedPayloadFingerprint) {
        this.normalizedPayloadFingerprint = normalizedPayloadFingerprint;
    }

    @Nullable
    public Long getProcessedAtMillis() {
        return processedAtMillis;
    }

    public void setProcessedAtMillis(@Nullable Long processedAtMillis) {
        this.processedAtMillis = processedAtMillis;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }
}
