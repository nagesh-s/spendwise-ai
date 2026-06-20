package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "investment_event_links", foreignKeys = {
        @ForeignKey(entity = IngestionEvent.class, parentColumns = "id", childColumns = "ingestionEventId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Holding.class, parentColumns = "id", childColumns = "holdingId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = InvestmentTransaction.class, parentColumns = "id", childColumns = "transactionId",
                onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index(value = { "ingestionEventId" }, unique = true),
        @Index("holdingId"),
        @Index("transactionId")
})
public class InvestmentEventLink {
    @PrimaryKey(autoGenerate = true) private long id;
    private long ingestionEventId;
    private long holdingId;
    @Nullable private Long transactionId;
    @NonNull private String parsedSignalType;
    @NonNull private String matchingStrategy;
    private double confidence;
    private long createdAtMillis;

    public InvestmentEventLink(long ingestionEventId, long holdingId, @Nullable Long transactionId,
            @NonNull String parsedSignalType, @NonNull String matchingStrategy,
            double confidence, long createdAtMillis) {
        this.ingestionEventId = ingestionEventId;
        this.holdingId = holdingId;
        this.transactionId = transactionId;
        this.parsedSignalType = parsedSignalType;
        this.matchingStrategy = matchingStrategy;
        this.confidence = confidence;
        this.createdAtMillis = createdAtMillis;
    }

    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public long getIngestionEventId() { return ingestionEventId; }
    public void setIngestionEventId(long value) { ingestionEventId = value; }
    public long getHoldingId() { return holdingId; }
    public void setHoldingId(long value) { holdingId = value; }
    @Nullable public Long getTransactionId() { return transactionId; }
    public void setTransactionId(@Nullable Long value) { transactionId = value; }
    @NonNull public String getParsedSignalType() { return parsedSignalType; }
    public void setParsedSignalType(@NonNull String value) { parsedSignalType = value; }
    @NonNull public String getMatchingStrategy() { return matchingStrategy; }
    public void setMatchingStrategy(@NonNull String value) { matchingStrategy = value; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double value) { confidence = value; }
    public long getCreatedAtMillis() { return createdAtMillis; }
    public void setCreatedAtMillis(long value) { createdAtMillis = value; }
}
