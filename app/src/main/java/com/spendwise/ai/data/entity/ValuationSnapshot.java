package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "valuation_snapshots", foreignKeys = {
        @ForeignKey(entity = Holding.class, parentColumns = "id", childColumns = "holdingId",
                onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("holdingId"),
        @Index(value = { "holdingId", "snapshotEpochDay", "source" }, unique = true)
})
public class ValuationSnapshot {
    @PrimaryKey(autoGenerate = true) private long id;
    private long holdingId;
    private long snapshotEpochDay;
    private long valueCents;
    @NonNull private String source;
    private long recordedAtMillis;

    public ValuationSnapshot(long holdingId, long snapshotEpochDay, long valueCents,
            @NonNull String source, long recordedAtMillis) {
        this.holdingId = holdingId;
        this.snapshotEpochDay = snapshotEpochDay;
        this.valueCents = valueCents;
        this.source = source;
        this.recordedAtMillis = recordedAtMillis;
    }
    public long getId() { return id; }
    public void setId(long value) { id = value; }
    public long getHoldingId() { return holdingId; }
    public void setHoldingId(long value) { holdingId = value; }
    public long getSnapshotEpochDay() { return snapshotEpochDay; }
    public void setSnapshotEpochDay(long value) { snapshotEpochDay = value; }
    public long getValueCents() { return valueCents; }
    public void setValueCents(long value) { valueCents = value; }
    @NonNull public String getSource() { return source; }
    public void setSource(@NonNull String value) { source = value; }
    public long getRecordedAtMillis() { return recordedAtMillis; }
    public void setRecordedAtMillis(long value) { recordedAtMillis = value; }
}
