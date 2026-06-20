package com.spendwise.ai.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.spendwise.ai.data.entity.InvestmentEventLink;

import java.util.List;

@Dao
public interface InvestmentEventLinkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(InvestmentEventLink link);

    @Query("SELECT * FROM investment_event_links WHERE ingestionEventId = :eventId LIMIT 1")
    InvestmentEventLink getByEventId(long eventId);

    @Query("SELECT * FROM investment_event_links WHERE transactionId = :transactionId ORDER BY createdAtMillis")
    List<InvestmentEventLink> getForTransaction(long transactionId);

    @Query("SELECT COUNT(*) FROM investment_event_links l LEFT JOIN ingestion_events e "
            + "ON e.id = l.ingestionEventId WHERE e.id IS NULL")
    int countOrphanEventLinks();
}
