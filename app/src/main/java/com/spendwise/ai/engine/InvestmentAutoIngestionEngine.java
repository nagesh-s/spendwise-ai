package com.spendwise.ai.engine;

import androidx.annotation.NonNull;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.IngestionEvent;
import com.spendwise.ai.domain.InvestmentLedgerService;
import com.spendwise.ai.sms.InvestmentSignalKeyFactory;
import com.spendwise.ai.sms.ParsedInvestmentSignal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ingests investment SMS signals into the investments ledger.
 * Delegates actual ledger updates to InvestmentLedgerService.
 */
public class InvestmentAutoIngestionEngine {

    public static class IngestResult {
        public int inserted;
        public int updated;
        public int skipped;
        public final Map<String, Long> investmentIdBySourceKey = new HashMap<>();
        public final Map<String, Long> transactionIdBySourceKey = new HashMap<>();
    }

    @NonNull
    public IngestResult ingest(
            @NonNull android.content.Context context,
            @NonNull AppDatabase db,
            @NonNull List<ParsedInvestmentSignal> signals) {
        return ingest(context, db, signals, IngestionEvent.SOURCE_SMS);
    }

    @NonNull
    public IngestResult ingest(
            @NonNull android.content.Context context,
            @NonNull AppDatabase db,
            @NonNull List<ParsedInvestmentSignal> signals,
            @NonNull String sourceType) {
        IngestResult result = new IngestResult();
        if (signals.isEmpty()) {
            return result;
        }

        List<ParsedInvestmentSignal> orderedSignals = new ArrayList<>(signals);
        orderedSignals.sort(Comparator.comparingLong(ParsedInvestmentSignal::getTimestampMillis));

        EventIngestionPipeline pipeline = new EventIngestionPipeline();
        InvestmentLedgerService ledgerService = new InvestmentLedgerService(db);
        BucketAssignmentEngine assignmentEngine = new BucketAssignmentEngine(context);
        List<com.spendwise.ai.data.entity.Bucket> buckets = db.bucketDao().getAllBucketsSync();

        for (ParsedInvestmentSignal signal : orderedSignals) {
            if (signal.getAmountCents() <= 0
                    && signal.getSignalType() != ParsedInvestmentSignal.SignalType.MATURITY_CREDIT) {
                result.skipped++;
                continue;
            }

            String sourceKey = InvestmentSignalKeyFactory.buildIngestionSourceKey(signal);
            EventIngestionPipeline.IngestResult eventResult = pipeline.ingest(
                    db,
                    IngestionEvent.DOMAIN_INVESTMENT,
                    sourceType,
                    sourceKey,
                    signal.getCorrelationKey(),
                    signal.getSignalType().name(),
                    signal.getAmountCents(),
                    signal.getTimestampMillis(),
                    signal.getSender(),
                    signal.getRawMessage(), // payloadHashBase
                    signal.getRawMessage()); // rawMessage

            IngestionEvent event = db.ingestionEventDao().getBySourceKey(sourceKey);
            if (event == null) {
                result.skipped++;
                continue;
            }

            if (IngestionEvent.STATUS_PROCESSED.equals(event.getStatus())) {
                result.skipped++;
                continue;
            }

            BucketAssignmentEngine.AssignmentResult assignment = assignmentEngine.assign(db, signal, buckets);
            
            InvestmentLedgerService.LedgerResult ledgerResult = ledgerService.processEvidence(event, signal, assignment.bucketId);
            
            pipeline.markProcessed(db, event.getId(), IngestionEvent.STATUS_PROCESSED);

            if (ledgerResult.status == InvestmentLedgerService.LedgerResult.Status.INSERTED) {
                result.inserted++;
            } else if (ledgerResult.status == InvestmentLedgerService.LedgerResult.Status.CONFIRMED) {
                result.updated++;
            } else {
                result.skipped++;
            }
            
            if (ledgerResult.holdingId != null) {
                result.investmentIdBySourceKey.put(sourceKey, ledgerResult.holdingId);
                result.investmentIdBySourceKey.put(signal.getSourceKey(), ledgerResult.holdingId);
            }
            if (ledgerResult.transactionId != null) {
                result.transactionIdBySourceKey.put(sourceKey, ledgerResult.transactionId);
                result.transactionIdBySourceKey.put(signal.getSourceKey(), ledgerResult.transactionId);
            }
        }
        return result;
    }
}
