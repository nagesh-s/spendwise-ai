package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "investment_transactions", foreignKeys = {
        @ForeignKey(entity = Holding.class, parentColumns = "id", childColumns = "holdingId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = InvestmentTransaction.class, parentColumns = "id",
                childColumns = "supersedesTransactionId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index("holdingId"),
        @Index(value = { "transactionKey" }, unique = true),
        @Index("referenceId"),
        @Index("correlationKey"),
        @Index("supersedesTransactionId"),
        @Index("effectiveEpochDay")
})
public class InvestmentTransaction {
    public static final String TYPE_CONTRIBUTION = "CONTRIBUTION";
    public static final String TYPE_WITHDRAWAL = "WITHDRAWAL";
    public static final String TYPE_INCOME = "INCOME";
    public static final String TYPE_FEE = "FEE";
    public static final String TYPE_TAX = "TAX";
    public static final String TYPE_ADJUSTMENT = "ADJUSTMENT";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    @PrimaryKey(autoGenerate = true) private long id;
    private long holdingId;
    @NonNull private String transactionType;
    @NonNull private String status;
    private long amountCents;
    @NonNull private String transactionKey;
    private long eventTimestampMillis;
    private long effectiveEpochDay;
    @Nullable private String referenceId;
    @Nullable private String correlationKey;
    @Nullable private Long supersedesTransactionId;
    @Nullable private String note;

    public InvestmentTransaction(long holdingId, @NonNull String transactionType,
            @NonNull String status, long amountCents, @NonNull String transactionKey,
            long eventTimestampMillis, long effectiveEpochDay) {
        this.holdingId = holdingId;
        this.transactionType = transactionType;
        this.status = status;
        this.amountCents = amountCents;
        this.transactionKey = transactionKey;
        this.eventTimestampMillis = eventTimestampMillis;
        this.effectiveEpochDay = effectiveEpochDay;
    }

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public long getHoldingId() { return holdingId; }
    public void setHoldingId(long value) { holdingId = value; }
    @NonNull public String getTransactionType() { return transactionType; }
    public void setTransactionType(@NonNull String value) { transactionType = value; }
    @NonNull public String getStatus() { return status; }
    public void setStatus(@NonNull String value) { status = value; }
    public long getAmountCents() { return amountCents; }
    public void setAmountCents(long value) { amountCents = value; }
    @NonNull public String getTransactionKey() { return transactionKey; }
    public void setTransactionKey(@NonNull String value) { transactionKey = value; }
    public long getEventTimestampMillis() { return eventTimestampMillis; }
    public void setEventTimestampMillis(long value) { eventTimestampMillis = value; }
    public long getEffectiveEpochDay() { return effectiveEpochDay; }
    public void setEffectiveEpochDay(long value) { effectiveEpochDay = value; }
    @Nullable public String getReferenceId() { return referenceId; }
    public void setReferenceId(@Nullable String value) { referenceId = value; }
    @Nullable public String getCorrelationKey() { return correlationKey; }
    public void setCorrelationKey(@Nullable String value) { correlationKey = value; }
    @Nullable public Long getSupersedesTransactionId() { return supersedesTransactionId; }
    public void setSupersedesTransactionId(@Nullable Long value) { supersedesTransactionId = value; }
    @Nullable public String getNote() { return note; }
    public void setNote(@Nullable String value) { note = value; }
}
