package com.spendwise.ai.domain;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.dao.InvestmentDao;
import com.spendwise.ai.data.dao.InvestmentTransactionDao;
import com.spendwise.ai.data.dao.ProviderDao;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.InvestmentTransaction;
import com.spendwise.ai.data.entity.Provider;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class InvestmentLedgerServiceTest {

    @Mock
    private AppDatabase mockDb;

    @Mock
    private InvestmentDao mockInvestmentDao;

    @Mock
    private InvestmentTransactionDao mockTransactionDao;

    @Mock
    private ProviderDao mockProviderDao;

    private InvestmentLedgerService service;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(mockDb.investmentDao()).thenReturn(mockInvestmentDao);
        when(mockDb.investmentTransactionDao()).thenReturn(mockTransactionDao);
        when(mockDb.providerDao()).thenReturn(mockProviderDao);
        
        service = new InvestmentLedgerService(mockDb);
    }

    @Test
    public void recomputeHoldingAggregates_computesCorrectly() {
        Holding holding = new Holding(1L, "Test", "test", Holding.ASSET_OTHER, 0L);
        holding.setId(1L);
        holding.setCurrentValueCents(12000L); // 120.00
        
        when(mockInvestmentDao.getById(1L)).thenReturn(holding);

        InvestmentTransaction t1 = new InvestmentTransaction(1L, InvestmentTransaction.TYPE_CONTRIBUTION, InvestmentTransaction.STATUS_CONFIRMED, 5000L, "tx1", 0L, 0L);
        InvestmentTransaction t2 = new InvestmentTransaction(1L, InvestmentTransaction.TYPE_CONTRIBUTION, InvestmentTransaction.STATUS_CONFIRMED, 5000L, "tx2", 0L, 0L);
        InvestmentTransaction t3 = new InvestmentTransaction(1L, InvestmentTransaction.TYPE_WITHDRAWAL, InvestmentTransaction.STATUS_CONFIRMED, 2000L, "tx3", 0L, 0L);
        InvestmentTransaction t4 = new InvestmentTransaction(1L, InvestmentTransaction.TYPE_FEE, InvestmentTransaction.STATUS_CONFIRMED, 100L, "tx4", 0L, 0L);
        InvestmentTransaction t5 = new InvestmentTransaction(1L, InvestmentTransaction.TYPE_CONTRIBUTION, InvestmentTransaction.STATUS_PENDING, 10000L, "tx5", 0L, 0L);

        when(mockTransactionDao.getForHoldingSync(1L)).thenReturn(Arrays.asList(t1, t2, t3, t4, t5));

        service.recomputeHoldingAggregates(1L);

        // Contributions = 5000 + 5000 = 10000
        // Withdrawals = 2000
        // Fees = 100
        // Net Cash Invested = contributions + fees + taxes - withdrawals = 10000 + 100 + 0 - 2000 = 8100
        // Absolute PNL = current_value + withdrawals + income - contributions - fees - taxes 
        // = 12000 + 2000 + 0 - 10000 - 100 - 0 = 3900

        verify(mockInvestmentDao, times(1)).update(holding);
        assertEquals(10000L, holding.getTotalContributionsCents());
        assertEquals(2000L, holding.getTotalWithdrawalsCents());
        assertEquals(100L, holding.getTotalFeesCents());
        assertEquals(0L, holding.getTotalTaxesCents());
        assertEquals(0L, holding.getTotalIncomeCents());
        assertEquals(8100L, holding.getNetCashInvestedCents());
        assertEquals(3900L, holding.getAbsolutePnlCents());
    }
}
