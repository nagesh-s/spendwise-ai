package com.spendwise.ai.review;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.data.entity.InvestmentTransaction;
import com.spendwise.ai.data.entity.ReviewCandidate;
import com.spendwise.ai.data.entity.UserCorrection;
import com.spendwise.ai.data.pojo.ReviewCandidateWithEvent;
import com.spendwise.ai.engine.InvestmentUnderstandingEngine;

import java.util.List;

public class ReviewQueueViewModel extends AndroidViewModel {

    private final AppDatabase db;
    private final LiveData<List<ReviewCandidateWithEvent>> pendingCandidates;

    public ReviewQueueViewModel(@NonNull Application application) {
        super(application);
        db = AppDatabase.getInstance(application);
        pendingCandidates = db.reviewCandidateDao().getPending();
    }

    public LiveData<List<ReviewCandidateWithEvent>> getPendingCandidates() {
        return pendingCandidates;
    }

    public void ignoreCandidate(long candidateId) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.reviewCandidateDao().resolve(candidateId, ReviewCandidate.STATUS_IGNORED, System.currentTimeMillis());
        });
    }

    /**
     * Accepts the "Best Guess" suggested by the system.
     */
    public void acceptSuggestion(ReviewCandidateWithEvent item) {
        if (item.candidate.getProposedHoldingId() == null) {
            return;
        }
        resolveCandidate(item, item.candidate.getProposedHoldingId(), item.ingestionEvent.getAmountCents());
    }

    /**
     * Manually resolves the candidate by assigning it to a specific holding and amount.
     */
    public void resolveCandidate(ReviewCandidateWithEvent item, long holdingId, long amountCents) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            Holding holding = db.investmentDao().getById(holdingId);
            if (holding == null) {
                return;
            }

            long timestamp = item.ingestionEvent.getEventTimestampMillis();
            String correlationKey = item.ingestionEvent.getCorrelationKey();
            if (correlationKey == null || correlationKey.isEmpty()) {
                correlationKey = "corr_manual_" + item.candidate.getId();
            }

            // 1. Insert Transaction
            InvestmentTransaction tx = new InvestmentTransaction(
                    holdingId,
                    InvestmentTransaction.TYPE_CONTRIBUTION,
                    InvestmentTransaction.STATUS_CONFIRMED,
                    amountCents,
                    "tx_manual_" + item.candidate.getId() + "_" + timestamp,
                    timestamp,
                    timestamp / (24L * 60 * 60 * 1000)
            );
            tx.setReferenceId("MANUAL_REVIEW");
            tx.setCorrelationKey(correlationKey);
            long txId = db.investmentTransactionDao().insertIgnore(tx);

            // 2. Mark Candidate Accepted
            db.reviewCandidateDao().resolve(item.candidate.getId(), ReviewCandidate.STATUS_ACCEPTED, System.currentTimeMillis());
            
            // 3. Update Ingestion Event
            db.ingestionEventDao().markProcessed(item.ingestionEvent.getId(), IngestionEvent.STATUS_PROCESSED, System.currentTimeMillis());

            // 4. Update Correction Memory
            if (item.ingestionEvent.getSourceSender() != null && item.ingestionEvent.getSourceBody() != null) {
                String formatKey = InvestmentUnderstandingEngine.buildCorrectionKey(
                        item.ingestionEvent.getSourceSender(),
                        item.ingestionEvent.getSourceBody());
                
                // Format: assetType|platform|name
                String correctedValue = holding.getAssetType() + "|" + holding.getPlatform() + "|" + holding.getName();
                
                UserCorrection correction = db.userCorrectionDao().get("INVESTMENT_FORMAT", formatKey);
                if (correction != null) {
                    correction.setCorrectedValue(correctedValue);
                    correction.setConfirmationCount(correction.getConfirmationCount() + 1);
                    correction.setLastUpdatedMillis(System.currentTimeMillis());
                    db.userCorrectionDao().update(correction);
                } else {
                    UserCorrection newCorrection = new UserCorrection(
                            "INVESTMENT_FORMAT",
                            formatKey,
                            correctedValue,
                            1,
                            System.currentTimeMillis()
                    );
                    db.userCorrectionDao().insertIgnore(newCorrection);
                }
            }
        });
    }
}
