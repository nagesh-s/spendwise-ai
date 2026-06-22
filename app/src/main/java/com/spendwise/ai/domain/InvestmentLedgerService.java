package com.spendwise.ai.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Transaction;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.InvestmentEventLink;
import com.spendwise.ai.data.entity.InvestmentTransaction;
import com.spendwise.ai.data.entity.Provider;

import java.util.List;

/**
 * Transaction-bound domain service responsible for maintaining the investment ledger.
 * No worker, listener, ViewModel, or parser should directly mutate ledger aggregates.
 * They must all route through this service.
 */
public class InvestmentLedgerService {

    private final AppDatabase db;

    public InvestmentLedgerService(@NonNull AppDatabase db) {
        this.db = db;
    }

    @NonNull
    public Provider resolveOrCreateProvider(@NonNull String providerName, @NonNull String providerType, @Nullable String packageName) {
        String normalized = providerName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.isEmpty()) {
            normalized = "unknown";
        }
        
        Provider existing = db.providerDao().getByNormalizedName(normalized);
        if (existing != null) {
            return existing;
        }
        
        Provider provider = new Provider(normalized, providerName.trim(), providerType, packageName, System.currentTimeMillis());
        long id = db.providerDao().insertIgnore(provider);
        if (id > 0) {
            provider.setId(id);
            return provider;
        } else {
            return db.providerDao().getByNormalizedName(normalized);
        }
    }

    @NonNull
    public Holding resolveOrCreateHolding(
            @Nullable Long providerId,
            @NonNull String instrumentName,
            @NonNull String assetType,
            long bucketId) {
                
        String normalized = instrumentName.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
        if (normalized.isEmpty()) {
            normalized = "unknown_instrument";
        }
        
        // Use a simple canonical key for now
        String canonicalKey = "inst_" + assetType + "_" + (providerId == null ? "0" : providerId) + "_" + normalized;
        
        Holding existing = db.investmentDao().getByCanonicalInstrumentKey(canonicalKey);
        if (existing != null) {
            return existing;
        }
        
        Holding holding = new Holding(bucketId, instrumentName.trim(), normalized, assetType, System.currentTimeMillis() / 86400000L);
        holding.setProviderId(providerId);
        holding.setCanonicalInstrumentKey(canonicalKey);
        
        long id = db.investmentDao().insert(holding);
        holding.setId(id);
        return holding;
    }

    @Transaction
    @NonNull
    public LedgerResult processEvidence(@NonNull IngestionEvent event, @NonNull com.spendwise.ai.sms.ParsedInvestmentSignal signal, long assignedBucketId) {
        
        // 0. Skip zero-amount signals
        if (signal.getAmountCents() <= 0) {
            return new LedgerResult(LedgerResult.Status.REJECTED, null, null);
        }

        // 1. Resolve Provider
        Provider provider = null;
        if (signal.getPlatform() != null) {
            provider = resolveOrCreateProvider(signal.getPlatform(), Provider.TYPE_BROKER, null);
        }
        
        // 2. Resolve Holding
        String instrument = signal.getInvestmentName() != null ? signal.getInvestmentName() : "Unknown Instrument";
        Holding holding = resolveOrCreateHolding(
                provider != null ? provider.getId() : null,
                instrument,
                signal.getAssetType(),
                assignedBucketId
        );
        
        // 3. Find Candidate Transaction or Create New
        InvestmentTransaction transaction = null;
        if (signal.getCorrelationKey() != null) {
            transaction = db.investmentTransactionDao().getLatestByCorrelationKey(signal.getCorrelationKey());
        }
        
        LedgerResult.Status resultStatus = LedgerResult.Status.INSERTED;
        
        if (transaction != null && transaction.getHoldingId() == holding.getId()) {
            // Transition state if needed
            if (InvestmentTransaction.STATUS_PENDING.equals(transaction.getStatus()) &&
                (com.spendwise.ai.sms.ParsedInvestmentSignal.SignalType.UNIT_ALLOTTED.equals(signal.getSignalType()) ||
                 com.spendwise.ai.sms.ParsedInvestmentSignal.SignalType.FD_BOOKED.equals(signal.getSignalType()))) {
                transaction.setStatus(InvestmentTransaction.STATUS_CONFIRMED);
                db.investmentTransactionDao().update(transaction);
            }
            resultStatus = LedgerResult.Status.CONFIRMED;
        } else {
            // Create new transaction
            String type = InvestmentTransaction.TYPE_CONTRIBUTION;
            if (com.spendwise.ai.sms.ParsedInvestmentSignal.SignalType.MATURITY_CREDIT.equals(signal.getSignalType())) {
                type = InvestmentTransaction.TYPE_WITHDRAWAL;
            }
            
            String status = InvestmentTransaction.STATUS_CONFIRMED;
            if (com.spendwise.ai.sms.ParsedInvestmentSignal.SignalType.INVESTMENT_DEBIT.equals(signal.getSignalType())) {
                status = InvestmentTransaction.STATUS_PENDING;
            }
            
            transaction = new InvestmentTransaction(
                    holding.getId(),
                    type,
                    status,
                    signal.getAmountCents(),
                    "tx_" + event.getId() + "_" + System.currentTimeMillis(),
                    signal.getTimestampMillis(),
                    signal.getTimestampMillis() / 86400000L
            );
            transaction.setCorrelationKey(signal.getCorrelationKey());
            long txId = db.investmentTransactionDao().insertIgnore(transaction);
            transaction.setId(txId);
        }
        
        // Update holding to reflect auto-ingestion source
        boolean holdingNeedsUpdate = false;
        if (!holding.isAutoFetched()) {
            holding.setAutoFetched(true);
            holdingNeedsUpdate = true;
        }
        if (holding.getPrimaryCorrelationKey() == null && signal.getCorrelationKey() != null) {
            holding.setPrimaryCorrelationKey(signal.getCorrelationKey());
            holdingNeedsUpdate = true;
        }
        if (holding.getPlatform() == null && signal.getPlatform() != null) {
            holding.setPlatform(signal.getPlatform());
            holdingNeedsUpdate = true;
        }
        if (holdingNeedsUpdate) {
            db.investmentDao().update(holding);
        }

        // 4. Link Evidence
        InvestmentEventLink link = new InvestmentEventLink(
                event.getId(),
                holding.getId(),
                transaction.getId(),
                signal.getSignalType().name(),
                "CORRELATION",
                1.0,
                System.currentTimeMillis()
        );
        db.investmentEventLinkDao().insertIgnore(link);
        
        // 5. Recompute
        recomputeHoldingAggregates(holding.getId());

        return new LedgerResult(resultStatus, transaction.getId(), holding.getId());
    }

    public static class LedgerResult {
        public enum Status {
            INSERTED, CONFIRMED, DUPLICATE, REVIEW_REQUIRED, REJECTED
        }

        
        public final Status status;
        @Nullable public final Long transactionId;
        @Nullable public final Long holdingId;
        
        public LedgerResult(Status status, @Nullable Long transactionId, @Nullable Long holdingId) {
            this.status = status;
            this.transactionId = transactionId;
            this.holdingId = holdingId;
        }
    }

    /**
     * Recomputes the materialized aggregates on a Holding based on its confirmed logical transactions.
     * This ensures the Holding entity is always a true reflection of the ledger.
     */
    @Transaction
    public void recomputeHoldingAggregates(long holdingId) {
        Holding holding = db.investmentDao().getById(holdingId);
        if (holding == null) return;

        List<InvestmentTransaction> transactions = db.investmentTransactionDao().getForHoldingSync(holdingId);
        
        long totalContributions = 0;
        long totalWithdrawals = 0;
        long totalIncome = 0;
        long totalFees = 0;
        long totalTaxes = 0;

        long oldNetCashInvested = holding.getNetCashInvestedCents();

        for (InvestmentTransaction t : transactions) {
            if (InvestmentTransaction.STATUS_CANCELLED.equals(t.getStatus())) continue;
            
            switch (t.getTransactionType()) {
                case InvestmentTransaction.TYPE_CONTRIBUTION:
                    totalContributions += t.getAmountCents();
                    break;
                case InvestmentTransaction.TYPE_WITHDRAWAL:
                    totalWithdrawals += t.getAmountCents();
                    break;
                case InvestmentTransaction.TYPE_INCOME:
                    totalIncome += t.getAmountCents();
                    break;
                case InvestmentTransaction.TYPE_FEE:
                    totalFees += t.getAmountCents();
                    break;
                case InvestmentTransaction.TYPE_TAX:
                    totalTaxes += t.getAmountCents();
                    break;
            }
        }

        long netCashInvested = totalContributions + totalFees + totalTaxes - totalWithdrawals;
        
        // If we haven't synced market prices (current value is 0) or if it perfectly matched the OLD net cash,
        // assume current value perfectly tracks the new net cash invested to prevent artificially negative PnL.
        if (holding.getCurrentValueCents() <= 0 || holding.getCurrentValueCents() == oldNetCashInvested) {
            holding.setCurrentValueCents(netCashInvested);
        }

        long absolutePnl = holding.getCurrentValueCents() + totalWithdrawals + totalIncome - totalContributions - totalFees - totalTaxes;

        holding.setTotalContributionsCents(totalContributions);
        holding.setTotalWithdrawalsCents(totalWithdrawals);
        holding.setTotalIncomeCents(totalIncome);
        holding.setTotalFeesCents(totalFees);
        holding.setTotalTaxesCents(totalTaxes);
        holding.setNetCashInvestedCents(netCashInvested);
        holding.setAbsolutePnlCents(absolutePnl);

        db.investmentDao().update(holding);
    }

    // TODO: implement provider/holding resolution, evidence matching, and transaction creation.
}
