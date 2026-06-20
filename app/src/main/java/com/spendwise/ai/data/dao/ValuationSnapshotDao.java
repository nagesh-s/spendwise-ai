package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.ValuationSnapshot;

import java.util.List;

@Dao
public interface ValuationSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(ValuationSnapshot snapshot);

    @Query("SELECT * FROM valuation_snapshots WHERE holdingId = :holdingId "
            + "ORDER BY snapshotEpochDay DESC, recordedAtMillis DESC LIMIT 1")
    ValuationSnapshot getLatest(long holdingId);

    @Query("SELECT * FROM valuation_snapshots WHERE holdingId = :holdingId "
            + "ORDER BY snapshotEpochDay DESC, recordedAtMillis DESC")
    LiveData<List<ValuationSnapshot>> getForHolding(long holdingId);
}
