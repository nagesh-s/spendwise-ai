package com.spendwise.ai.sms;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a parsed transaction extracted from an SMS message.
 */
public class ParsedTransaction {

    public enum TransactionType {
        DEBIT, CREDIT
    }

    private final TransactionType type;
    private final long amountCents;
    private final String merchant;
    private final String accountInfo;
    private final String originalMessage;
    private final long timestampMillis;
    private boolean imported;

    public ParsedTransaction(TransactionType type, long amountCents, String merchant,
            String accountInfo, String originalMessage, long timestampMillis) {
        this.type = type;
        this.amountCents = amountCents;
        this.merchant = merchant;
        this.accountInfo = accountInfo;
        this.originalMessage = originalMessage;
        this.timestampMillis = timestampMillis;
        this.imported = false;
    }

    public TransactionType getType() {
        return type;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getMerchant() {
        return merchant;
    }

    public String getAccountInfo() {
        return accountInfo;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    /**
     * Get formatted amount string.
     */
    public String getFormattedAmount() {
        return String.format("₹%,.2f", amountCents / 100.0);
    }
}
