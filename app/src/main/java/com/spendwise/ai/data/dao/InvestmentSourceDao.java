package com.spendwise.ai.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.InvestmentSource;

import java.util.List;

@Dao
public interface InvestmentSourceDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(InvestmentSource source);

    @Query("SELECT * FROM investment_sources WHERE sourceKey = :sourceKey LIMIT 1")
    InvestmentSource getBySourceKey(String sourceKey);

    @Query("SELECT * FROM investment_sources WHERE investmentId = :investmentId ORDER BY eventTimestampMillis DESC")
    List<InvestmentSource> getForInvestment(long investmentId);

    @Query("SELECT * FROM investment_sources WHERE canonicalInstrumentKey = :canonicalInstrumentKey ORDER BY eventTimestampMillis DESC")
    List<InvestmentSource> getByCanonicalInstrumentKey(String canonicalInstrumentKey);

    @Query("UPDATE investment_sources SET investmentId = :targetInvestmentId WHERE investmentId = :sourceInvestmentId")
    int moveSources(long sourceInvestmentId, long targetInvestmentId);
}
