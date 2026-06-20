package com.spendwise.ai.data.pojo;

import androidx.room.ColumnInfo;

import com.spendwise.ai.data.entity.InvestmentOccurrence;

/**
 * Flat row projection for bucket detail expected SIP list.
 */
public class InvestmentOccurrenceWithSchedule {

    public long id;
    public long scheduleId;
    public long dueEpochDay;
    public long expectedAmountCents;
    public String status;
    public Long confirmedAmountCents;
    public Long confirmedAtMillis;
    public Long linkedInvestmentId;
    public Long transactionId;
    public String correlationKey;
    public String sourceType;

    @ColumnInfo(name = "scheduleName")
    public String scheduleName;

    @ColumnInfo(name = "scheduleAssetType")
    public String scheduleAssetType;

    @ColumnInfo(name = "schedulePlatform")
    public String schedulePlatform;

    @ColumnInfo(name = "scheduleBucketId")
    public long scheduleBucketId;

    public boolean isPending() {
        return InvestmentOccurrence.STATUS_EXPECTED.equals(status);
    }

    public long getResolvedAmountCents() {
        if (confirmedAmountCents != null && confirmedAmountCents > 0) {
            return confirmedAmountCents;
        }
        return expectedAmountCents;
    }
}
