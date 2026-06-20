package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.Provider;

import java.util.List;

@Dao
public interface ProviderDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(Provider provider);

    @Query("SELECT * FROM providers WHERE id = :id")
    Provider getById(long id);

    @Query("SELECT * FROM providers WHERE normalizedName = :normalizedName LIMIT 1")
    Provider getByNormalizedName(String normalizedName);

    @Query("SELECT * FROM providers ORDER BY name")
    LiveData<List<Provider>> getAll();
}
