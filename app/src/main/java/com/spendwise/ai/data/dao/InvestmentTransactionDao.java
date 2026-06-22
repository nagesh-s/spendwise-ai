package com.spendwise.ai.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.spendwise.ai.data.entity.InvestmentTransaction;

import java.util.List;

@Dao
public interface InvestmentTransactionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertIgnore(InvestmentTransaction transaction);

    @Update
    void update(InvestmentTransaction transaction);

    @Query("SELECT * FROM investment_transactions WHERE id = :id")
    InvestmentTransaction getById(long id);

    @Query("SELECT * FROM investment_transactions WHERE transactionKey = :key LIMIT 1")
    InvestmentTransaction getByTransactionKey(String key);

    @Query("SELECT * FROM investment_transactions WHERE referenceId = :referenceId "
            + "AND referenceId IS NOT NULL ORDER BY eventTimestampMillis DESC LIMIT 1")
    InvestmentTransaction getByReferenceId(String referenceId);

    @Query("SELECT * FROM investment_transactions WHERE correlationKey = :correlationKey "
            + "AND correlationKey IS NOT NULL ORDER BY eventTimestampMillis DESC LIMIT 1")
    InvestmentTransaction getLatestByCorrelationKey(String correlationKey);

    @Query("SELECT * FROM investment_transactions WHERE holdingId = :holdingId "
            + "ORDER BY eventTimestampMillis DESC, id DESC")
    LiveData<List<InvestmentTransaction>> getForHolding(long holdingId);

    @Query("SELECT * FROM investment_transactions WHERE holdingId = :holdingId "
            + "ORDER BY eventTimestampMillis DESC, id DESC")
    List<InvestmentTransaction> getForHoldingSync(long holdingId);

    @Query("SELECT * FROM investment_transactions WHERE holdingId = :holdingId "
            + "AND transactionType = :type AND amountCents = :amountCents "
            + "AND eventTimestampMillis BETWEEN :startMillis AND :endMillis "
            + "ORDER BY eventTimestampMillis DESC")
    List<InvestmentTransaction> findCandidates(long holdingId, String type, long amountCents,
            long startMillis, long endMillis);

    @Query("SELECT * FROM investment_transactions WHERE amountCents = :amountCents "
            + "AND eventTimestampMillis BETWEEN :startMillis AND :endMillis "
            + "ORDER BY eventTimestampMillis DESC")
    List<InvestmentTransaction> findGlobalCandidates(long amountCents, long startMillis, long endMillis);

    @Query("UPDATE investment_transactions SET status = :status WHERE id = :id")
    int updateStatus(long id, String status);

    @Query("SELECT COALESCE(SUM(CASE WHEN transactionType = 'CONTRIBUTION' THEN amountCents ELSE 0 END), 0) AS contributions, "
            + "COALESCE(SUM(CASE WHEN transactionType = 'WITHDRAWAL' THEN amountCents ELSE 0 END), 0) AS withdrawals, "
            + "COALESCE(SUM(CASE WHEN transactionType = 'INCOME' THEN amountCents ELSE 0 END), 0) AS income, "
            + "COALESCE(SUM(CASE WHEN transactionType = 'FEE' THEN amountCents ELSE 0 END), 0) AS fees, "
            + "COALESCE(SUM(CASE WHEN transactionType = 'TAX' THEN amountCents ELSE 0 END), 0) AS taxes "
            + "FROM investment_transactions WHERE holdingId = :holdingId AND status = 'CONFIRMED'")
    LedgerTotals getConfirmedTotals(long holdingId);

    class LedgerTotals {
        public long contributions;
        public long withdrawals;
        public long income;
        public long fees;
        public long taxes;
    }
}
