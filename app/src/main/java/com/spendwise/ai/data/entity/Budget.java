package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a monthly budget limit for a specific category.
 * 
 * Each budget is scoped to a month (stored as YYYYMM string like "202601")
 * and a category. Users can set different budget limits for different months.
 */
@Entity(
    tableName = "budgets",
    foreignKeys = {
        @ForeignKey(
            entity = Category.class,
            parentColumns = "id",
            childColumns = "categoryId",
            onDelete = ForeignKey.CASCADE
        )
    },
    indices = {
        @Index(value = {"monthKeyYYYYMM", "categoryId"}, unique = true),
        @Index(value = "categoryId")
    }
)
public class Budget {

    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * Month key in YYYYMM format (e.g., "202601" for January 2026).
     * Using String for easy comparison and sorting.
     */
    @NonNull
    private String monthKeyYYYYMM;

    private long categoryId;

    /**
     * Budget limit in cents (e.g., ₹5000 is stored as 500000).
     */
    private long limitCents;

    // Constructor
    public Budget(@NonNull String monthKeyYYYYMM, long categoryId, long limitCents) {
        this.monthKeyYYYYMM = monthKeyYYYYMM;
        this.categoryId = categoryId;
        this.limitCents = limitCents;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getMonthKeyYYYYMM() {
        return monthKeyYYYYMM;
    }

    public void setMonthKeyYYYYMM(@NonNull String monthKeyYYYYMM) {
        this.monthKeyYYYYMM = monthKeyYYYYMM;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public long getLimitCents() {
        return limitCents;
    }

    public void setLimitCents(long limitCents) {
        this.limitCents = limitCents;
    }
}
