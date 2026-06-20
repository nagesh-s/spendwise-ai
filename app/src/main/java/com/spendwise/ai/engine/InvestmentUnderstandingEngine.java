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

        // 1. Correction memory (TODO: check user corrections table once implemented in Phase 4)

        // 2. Deterministic parser
        ParsedInvestmentSignal deterministicSignal = InvestmentSmsParser.parse(sender, body, timestampMillis);
        if (deterministicSignal != null && deterministicSignal.getConfidence() > 0.85d) {
            return deterministicSignal;
        }

        // 3. Lightweight Semantic Model (TFLite/Semantic)
        ParsedInvestmentSignal semanticSignal = semanticExtractor.extract(sender, body, timestampMillis);
        if (semanticSignal != null && semanticSignal.getConfidence() > 0.60d) {
            // Prefer semantic over a weak deterministic parse, or merge them.
            // For now, if deterministic was null, use semantic.
            if (deterministicSignal == null || semanticSignal.getConfidence() > deterministicSignal.getConfidence()) {
                return semanticSignal;
            }
        }

        // Return deterministic if it exists even with lower confidence, else null (goes to review queue)
        if (deterministicSignal != null) {
            return deterministicSignal;
        }

        return null; // Sent to review queue by caller
    }
}
