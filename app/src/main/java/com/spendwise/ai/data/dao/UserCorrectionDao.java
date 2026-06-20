package com.spendwise.ai.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.UserCorrection;

@Dao
public interface UserCorrectionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(UserCorrection correction);

    @Update
    void update(UserCorrection correction);

    @Query("SELECT * FROM user_corrections WHERE correctionType = :type "
            + "AND normalizedKey = :key LIMIT 1")
    UserCorrection get(String type, String key);
}
