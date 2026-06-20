package com.spendwise.ai.engine;

import androidx.annotation.Nullable;

import com.spendwise.ai.data.AppDatabase;
import com.spendwise.ai.data.entity.Holding;
import com.spendwise.ai.sms.InvestmentSmsParser;
import com.spendwise.ai.sms.ParsedInvestmentSignal;
import com.spendwise.ai.sms.OnDeviceSemanticInvestmentExtractor;

import java.util.concurrent.Executor;

/**
 * Pipeline orchestrator for investment signal extraction.
 *
 * Enforcement sequence:
 * 1) Correction memory (User overrides)
 * 2) Deterministic parser (InvestmentSmsParser)
 * 3) Semantic lightweight model (OnDeviceSemanticInvestmentExtractor)
 * 4) Review candidate queue (if all fail)
 */
public class InvestmentUnderstandingEngine {

    private final AppDatabase db;
    private final OnDeviceSemanticInvestmentExtractor semanticExtractor;
    private final Executor backgroundExecutor;

    public InvestmentUnderstandingEngine(AppDatabase db, Executor backgroundExecutor) {
        this.db = db;
        this.semanticExtractor = new OnDeviceSemanticInvestmentExtractor();
        this.backgroundExecutor = backgroundExecutor;
    }

    /**
     * Extracts investment signals from raw text.
     * Evaluates synchronously. Returns null if all parsers fail or yield low confidence
     * (in which case it should be sent to the review queue).
     */
    @Nullable
    public ParsedInvestmentSignal understandSync(String sender, String body, long timestampMillis) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        // 1. Correction memory
        if (db != null) {
            String formatKey = buildCorrectionKey(sender, body);
            com.spendwise.ai.data.entity.UserCorrection correction = db.userCorrectionDao().get("INVESTMENT_FORMAT", formatKey);
            if (correction != null) {
                // We found a correction. The correctedValue contains the holding ID.
                // We still need the amount. Let's try to parse amount deterministically.
                ParsedInvestmentSignal partial = InvestmentSmsParser.parse(sender, body, timestampMillis);
                long amountCents = partial != null ? partial.getAmountCents() : 0L;
                
                String[] parts = correction.getCorrectedValue().split("\\|");
                String assetType = parts.length > 0 ? parts[0] : Holding.ASSET_MF;
                String platform = parts.length > 1 ? parts[1] : "unknown";
                String name = parts.length > 2 ? parts[2] : "unknown";
                
                String sourceKey = "src_corr_" + formatKey + "_" + timestampMillis;
                String corrKey = "corr_" + formatKey;
                
                return new ParsedInvestmentSignal(
                        ParsedInvestmentSignal.SignalType.UNIT_ALLOTTED,
                        sender,
                        platform,
                        name,
                        assetType,
                        amountCents,
                        timestampMillis,
                        sourceKey,
                        corrKey,
                        body,
                        true,
                        null,
                        null,
                        1.0d,
                        "user_correction_memory"
                );
            }
        }
        // 2. Deterministic parser
        ParsedInvestmentSignal deterministicSignal = InvestmentSmsParser.parse(sender, body, timestampMillis);
        if (deterministicSignal != null && deterministicSignal.getConfidence() > 0.85d) {
            return deterministicSignal;
        }

        // 3. Lightweight Semantic Model (TFLite/Semantic)
        ParsedInvestmentSignal semanticSignal = semanticExtractor.extract(sender, body, timestampMillis);
        
        if (semanticSignal != null && deterministicSignal != null) {
            return semanticSignal.getConfidence() > deterministicSignal.getConfidence() ? semanticSignal : deterministicSignal;
        } else if (semanticSignal != null) {
            return semanticSignal;
        } else {
            return deterministicSignal;
        }
    }

    @androidx.annotation.NonNull
    public static String buildCorrectionKey(String sender, String body) {
        String s = sender == null ? "" : sender.trim().toLowerCase(java.util.Locale.US);
        String b = body == null ? "" : body.trim().toLowerCase(java.util.Locale.US).replaceAll("\\d+", "#");
        return s + "|" + b;
    }
}
