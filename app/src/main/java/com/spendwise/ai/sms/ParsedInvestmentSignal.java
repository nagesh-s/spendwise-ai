package com.spendwise.ai.sms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Parsed investment signal extracted from SMS.
 *
 * Signals can represent intermediate debit messages or final unit/FD
 * confirmations; both are merged by correlation/source keys during ingestion.
 */
public class ParsedInvestmentSignal {

    public enum SignalType {
        INVESTMENT_DEBIT,
        UNIT_ALLOTTED,
        FD_BOOKED,
        RD_BOOKED,
        MATURITY_CREDIT
    }

    private final SignalType signalType;
    private final String sender;
    private final String platform;
    private final String investmentName;
    private final String assetType;
    private final long amountCents;
    private final long timestampMillis;
    private final String sourceKey;
    private final String correlationKey;
    private final String rawMessage;
    private final boolean correction;

    public ParsedInvestmentSignal(
            @NonNull SignalType signalType,
            @Nullable String sender,
            @Nullable String platform,
            @Nullable String investmentName,
            @NonNull String assetType,
            long amountCents,
            long timestampMillis,
            @NonNull String sourceKey,
            @NonNull String correlationKey,
            @NonNull String rawMessage,
            boolean correction) {
        this.signalType = signalType;
        this.sender = sender;
        this.platform = platform;
        this.investmentName = investmentName;
        this.assetType = assetType;
        this.amountCents = amountCents;
        this.timestampMillis = timestampMillis;
        this.sourceKey = sourceKey;
        this.correlationKey = correlationKey;
        this.rawMessage = rawMessage;
        this.correction = correction;
    }

    @NonNull
    public SignalType getSignalType() {
        return signalType;
    }

    @Nullable
    public String getSender() {
        return sender;
    }

    @Nullable
    public String getPlatform() {
        return platform;
    }

    @Nullable
    public String getInvestmentName() {
        return investmentName;
    }

    @NonNull
    public String getAssetType() {
        return assetType;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    @NonNull
    public String getSourceKey() {
        return sourceKey;
    }

    @NonNull
    public String getCorrelationKey() {
        return correlationKey;
    }

    @NonNull
    public String getRawMessage() {
        return rawMessage;
    }

    public boolean isCorrection() {
        return correction;
    }
}
