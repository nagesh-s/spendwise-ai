package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Links every raw investment source event to the canonical holding it updated.
 */
@Entity(tableName = "investment_sources", foreignKeys = {
        @ForeignKey(entity = Investment.class, parentColumns = "id", childColumns = "investmentId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("investmentId"),
        @Index(value = { "sourceKey" }, unique = true),
        @Index("correlationKey"),
        @Index("canonicalInstrumentKey")
})
public class InvestmentSource {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long investmentId;

    @NonNull
    private String sourceType;

    @NonNull
    private String sourceKey;

    @Nullable
    private String correlationKey;

    @Nullable
    private String canonicalInstrumentKey;

    @NonNull
    private String eventType;

    private long amountCents;

    private long eventTimestampMillis;

    public InvestmentSource(
            long investmentId,
            @NonNull String sourceType,
            @NonNull String sourceKey,
            @Nullable String correlationKey,
            @Nullable String canonicalInstrumentKey,
            @NonNull String eventType,
            long amountCents,
            long eventTimestampMillis) {
        this.investmentId = investmentId;
        this.sourceType = sourceType;
        this.sourceKey = sourceKey;
        this.correlationKey = correlationKey;
        this.canonicalInstrumentKey = canonicalInstrumentKey;
        this.eventType = eventType;
        this.amountCents = amountCents;
        this.eventTimestampMillis = eventTimestampMillis;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(long investmentId) {
        this.investmentId = investmentId;
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

    @Nullable
    public String getCanonicalInstrumentKey() {
        return canonicalInstrumentKey;
    }

    public void setCanonicalInstrumentKey(@Nullable String canonicalInstrumentKey) {
        this.canonicalInstrumentKey = canonicalInstrumentKey;
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
}
