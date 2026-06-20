package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "review_candidates", foreignKeys = {
        @ForeignKey(entity = IngestionEvent.class, parentColumns = "id", childColumns = "ingestionEventId",
                onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Holding.class, parentColumns = "id", childColumns = "proposedHoldingId",
                onDelete = ForeignKey.SET_NULL),
        @ForeignKey(entity = InvestmentTransaction.class, parentColumns = "id",
                childColumns = "proposedTransactionId", onDelete = ForeignKey.SET_NULL)
}, indices = {
        @Index(value = { "ingestionEventId" }, unique = true),
        @Index("proposedHoldingId"),
        @Index("proposedTransactionId"),
        @Index("status")
})
public class ReviewCandidate {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_KEPT_SEPARATE = "KEPT_SEPARATE";
    public static final String STATUS_IGNORED = "IGNORED";

    @PrimaryKey(autoGenerate = true) private long id;
    private long ingestionEventId;
    @Nullable private Long proposedHoldingId;
    @Nullable private Long proposedTransactionId;
    @NonNull private String proposedInterpretation;
    @NonNull private String reason;
    private double confidence;
    @NonNull private String status;
    private long createdAtMillis;
    @Nullable private Long resolvedAtMillis;

    public ReviewCandidate(long ingestionEventId, @Nullable Long proposedHoldingId,
            @Nullable Long proposedTransactionId, @NonNull String proposedInterpretation,
            @NonNull String reason, double confidence, @NonNull String status,
            long createdAtMillis) {
        this.ingestionEventId = ingestionEventId;
        this.proposedHoldingId = proposedHoldingId;
        this.proposedTransactionId = proposedTransactionId;
        this.proposedInterpretation = proposedInterpretation;
        this.reason = reason;
        this.confidence = confidence;
        this.status = status;
        this.createdAtMillis = createdAtMillis;
    }
    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public long getIngestionEventId() { return ingestionEventId; }
    public void setIngestionEventId(long value) { ingestionEventId = value; }
    @Nullable public Long getProposedHoldingId() { return proposedHoldingId; }
    public void setProposedHoldingId(@Nullable Long value) { proposedHoldingId = value; }
    @Nullable public Long getProposedTransactionId() { return proposedTransactionId; }
    public void setProposedTransactionId(@Nullable Long value) { proposedTransactionId = value; }
    @NonNull public String getProposedInterpretation() { return proposedInterpretation; }
    public void setProposedInterpretation(@NonNull String value) { proposedInterpretation = value; }
    @NonNull public String getReason() { return reason; }
    public void setReason(@NonNull String value) { reason = value; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double value) { confidence = value; }
    @NonNull public String getStatus() { return status; }
    public void setStatus(@NonNull String value) { status = value; }
    public long getCreatedAtMillis() { return createdAtMillis; }
    public void setCreatedAtMillis(long value) { createdAtMillis = value; }
    @Nullable public Long getResolvedAtMillis() { return resolvedAtMillis; }
    public void setResolvedAtMillis(@Nullable Long value) { resolvedAtMillis = value; }
}
