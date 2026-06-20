package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_corrections", indices = {
        @Index(value = { "correctionType", "normalizedKey" }, unique = true)
})
public class UserCorrection {
    @PrimaryKey(autoGenerate = true) private long id;
    @NonNull private String correctionType;
    @NonNull private String normalizedKey;
    @NonNull private String correctedValue;
    private int confirmationCount;
    private long lastUpdatedMillis;

    public UserCorrection(@NonNull String correctionType, @NonNull String normalizedKey,
            @NonNull String correctedValue, int confirmationCount, long lastUpdatedMillis) {
        this.correctionType = correctionType;
        this.normalizedKey = normalizedKey;
        this.correctedValue = correctedValue;
        this.confirmationCount = confirmationCount;
        this.lastUpdatedMillis = lastUpdatedMillis;
    }
    public long getId() { return id; }
    public void setId(long value) { id = value; }
    @NonNull public String getCorrectionType() { return correctionType; }
    public void setCorrectionType(@NonNull String value) { correctionType = value; }
    @NonNull public String getNormalizedKey() { return normalizedKey; }
    public void setNormalizedKey(@NonNull String value) { normalizedKey = value; }
    @NonNull public String getCorrectedValue() { return correctedValue; }
    public void setCorrectedValue(@NonNull String value) { correctedValue = value; }
    public int getConfirmationCount() { return confirmationCount; }
    public void setConfirmationCount(int value) { confirmationCount = value; }
    public long getLastUpdatedMillis() { return lastUpdatedMillis; }
    public void setLastUpdatedMillis(long value) { lastUpdatedMillis = value; }
}
