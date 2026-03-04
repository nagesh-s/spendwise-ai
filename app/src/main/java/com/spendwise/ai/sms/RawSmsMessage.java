package com.spendwise.ai.sms;

import androidx.annotation.NonNull;

/**
 * Raw SMS message payload used by higher-level parsers.
 */
public class RawSmsMessage {

    private final String sender;
    private final String body;
    private final long timestampMillis;

    public RawSmsMessage(@NonNull String sender, @NonNull String body, long timestampMillis) {
        this.sender = sender;
        this.body = body;
        this.timestampMillis = timestampMillis;
    }

    @NonNull
    public String getSender() {
        return sender;
    }

    @NonNull
    public String getBody() {
        return body;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }
}
