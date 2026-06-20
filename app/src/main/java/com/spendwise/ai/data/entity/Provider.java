package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "providers", indices = {
        @Index(value = { "normalizedName" }, unique = true)
})
public class Provider {

    public static final String TYPE_BROKER = "BROKER";
    public static final String TYPE_BANK = "BANK";
    public static final String TYPE_AMC = "AMC";
    public static final String TYPE_OTHER = "OTHER";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String normalizedName;

    @NonNull
    private String name;

    @NonNull
    private String type;

    @Nullable
    private String packageName;

    private long createdAtMillis;

    public Provider(@NonNull String normalizedName, @NonNull String name, @NonNull String type,
            @Nullable String packageName, long createdAtMillis) {
        this.normalizedName = normalizedName;
        this.name = name;
        this.type = type;
        this.packageName = packageName;
        this.createdAtMillis = createdAtMillis;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    @NonNull public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(@NonNull String value) { normalizedName = value; }
    @NonNull public String getName() { return name; }
    public void setName(@NonNull String value) { name = value; }
    @NonNull public String getType() { return type; }
    public void setType(@NonNull String value) { type = value; }
    @Nullable public String getPackageName() { return packageName; }
    public void setPackageName(@Nullable String value) { packageName = value; }
    public long getCreatedAtMillis() { return createdAtMillis; }
    public void setCreatedAtMillis(long value) { createdAtMillis = value; }
}
