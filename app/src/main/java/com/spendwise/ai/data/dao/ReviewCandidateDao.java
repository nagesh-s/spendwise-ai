package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.ReviewCandidate;

import java.util.List;

@Dao
public interface ReviewCandidateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(ReviewCandidate candidate);

    @Query("SELECT * FROM review_candidates WHERE status = 'PENDING' ORDER BY createdAtMillis DESC")
    LiveData<List<ReviewCandidate>> getPending();

    @Query("SELECT COUNT(*) FROM review_candidates WHERE status = 'PENDING'")
    int countPendingSync();

    @Query("UPDATE review_candidates SET status = :status, resolvedAtMillis = :resolvedAtMillis WHERE id = :id")
    int resolve(long id, String status, long resolvedAtMillis);
}
