package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Expected/confirmed lifecycle entry generated from an investment schedule.
 */
@Entity(tableName = "investment_occurrences", foreignKeys = {
        @ForeignKey(entity = InvestmentSchedule.class, parentColumns = "id", childColumns = "scheduleId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Holding.class, parentColumns = "id", childColumns = "linkedInvestmentId", onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = InvestmentTransaction.class, parentColumns = "id", childColumns = "transactionId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index(value = { "scheduleId", "dueEpochDay" }, unique = true),
        @Index("status"),
        @Index("correlationKey"),
        @Index("linkedInvestmentId"),
        @Index("transactionId")
})
public class InvestmentOccurrence {

    public static final String STATUS_EXPECTED = "EXPECTED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_ADJUSTED = "ADJUSTED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long scheduleId;

    private long dueEpochDay;

    private long expectedAmountCents;

    @NonNull
    private String status;

    @Nullable
    private Long confirmedAmountCents;

    @Nullable
    private Long confirmedAtMillis;

    @Nullable
    private Long linkedInvestmentId;

    @Nullable
    private Long transactionId;

    @Nullable
    private String correlationKey;

    @Nullable
    private String sourceType;

    public InvestmentOccurrence(long scheduleId, long dueEpochDay, long expectedAmountCents) {
        this.scheduleId = scheduleId;
        this.dueEpochDay = dueEpochDay;
        this.expectedAmountCents = expectedAmountCents;
        this.status = STATUS_EXPECTED;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public long getDueEpochDay() {
        return dueEpochDay;
    }

    public void setDueEpochDay(long dueEpochDay) {
        this.dueEpochDay = dueEpochDay;
    }

    public long getExpectedAmountCents() {
        return expectedAmountCents;
    }

    public void setExpectedAmountCents(long expectedAmountCents) {
        this.expectedAmountCents = expectedAmountCents;
    }

    @NonNull
    public String getStatus() {
        return status;
    }

    public void setStatus(@NonNull String status) {
        this.status = status;
    }

    @Nullable
    public Long getConfirmedAmountCents() {
        return confirmedAmountCents;
    }

    public void setConfirmedAmountCents(@Nullable Long confirmedAmountCents) {
        this.confirmedAmountCents = confirmedAmountCents;
    }

    @Nullable
    public Long getConfirmedAtMillis() {
        return confirmedAtMillis;
    }

    public void setConfirmedAtMillis(@Nullable Long confirmedAtMillis) {
        this.confirmedAtMillis = confirmedAtMillis;
    }

    @Nullable
    public Long getLinkedInvestmentId() {
        return linkedInvestmentId;
    }

    public void setLinkedInvestmentId(@Nullable Long linkedInvestmentId) {
        this.linkedInvestmentId = linkedInvestmentId;
    }

    @Nullable
    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(@Nullable Long transactionId) {
        this.transactionId = transactionId;
    }

    @Nullable
    public String getCorrelationKey() {
        return correlationKey;
    }

    public void setCorrelationKey(@Nullable String correlationKey) {
        this.correlationKey = correlationKey;
    }

    @Nullable
    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(@Nullable String sourceType) {
        this.sourceType = sourceType;
    }
}
