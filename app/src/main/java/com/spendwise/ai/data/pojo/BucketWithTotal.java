package com.spendwise.ai.data.pojo;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

import com.spendwise.ai.data.entity.Bucket;

/**
 * POJO for queries that join a Bucket with its aggregate investment totals.
 * Used by BucketDao.getAllBucketsWithTotals().
 */
public class BucketWithTotal {

    @Embedded
    public Bucket bucket;

    @ColumnInfo(name = "totalValueCents")
    public long totalValueCents;

    @ColumnInfo(name = "totalInvestedCents")
    public long totalInvestedCents;

    /**
     * Calculate P&L amount in cents for this bucket.
     */
    public long getProfitLossCents() {
        return totalValueCents - totalInvestedCents;
    }

    /**
     * Calculate P&L percentage for this bucket.
     */
    public double getReturnPercent() {
        if (totalInvestedCents <= 0)
            return 0.0;
        return ((double) (totalValueCents - totalInvestedCents) / totalInvestedCents) * 100.0;
    }
}
