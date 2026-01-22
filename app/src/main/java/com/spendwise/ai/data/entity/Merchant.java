package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a merchant/store where expenses occurred (e.g., Amazon, Starbucks).
 * 
 * Merchants are auto-created when users enter new merchant names during expense entry.
 * They enable merchant-level analytics like top merchants by spend.
 */
@Entity(
    tableName = "merchants",
    indices = {
        @Index(value = "name", unique = true)
    }
)
public class Merchant {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    // Constructor
    public Merchant(@NonNull String name) {
        this.name = name;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }
}
