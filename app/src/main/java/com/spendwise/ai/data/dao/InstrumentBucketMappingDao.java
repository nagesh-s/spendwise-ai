package com.spendwise.ai.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.InstrumentBucketMapping;

import java.util.List;

@Dao
public interface InstrumentBucketMappingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(InstrumentBucketMapping mapping);

    @Query("SELECT * FROM instrument_bucket_mappings WHERE assetType = :assetType ORDER BY confidenceBoost DESC, updatedAtMillis DESC")
    List<InstrumentBucketMapping> getByAssetTypeSync(String assetType);

    @Query("SELECT * FROM instrument_bucket_mappings ORDER BY updatedAtMillis DESC")
    List<InstrumentBucketMapping> getAllSync();
}
