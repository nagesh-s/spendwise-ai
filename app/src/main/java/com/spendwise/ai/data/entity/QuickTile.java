package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a quick-add tile for frequently logged expenses.
 * 
 * QuickTiles allow one-tap expense creation for common items like
 * "Coffee - ₹150" or "Lunch - ₹200". They store a preset category,
 * optional merchant, label, and amount.
 */
@Entity(
    tableName = "quick_tiles",
    foreignKeys = {
        @ForeignKey(
            entity = Category.class,
            parentColumns = "id",
            childColumns = "categoryId",
            onDelete = ForeignKey.CASCADE
        ),
        @ForeignKey(
            entity = Merchant.class,
            parentColumns = "id",
            childColumns = "merchantId",
            onDelete = ForeignKey.SET_NULL
        )
    },
    indices = {
        @Index(value = "categoryId"),
        @Index(value = "merchantId")
    }
)
public class QuickTile {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long categoryId;

    @Nullable
    private Long merchantId;

    /**
     * Display label for the tile (e.g., "Coffee", "Lunch").
     */
    @NonNull
    private String label;

    /**
     * Preset amount in cents.
     */
    private long amountCents;

    // Constructor
    public QuickTile(long categoryId, @Nullable Long merchantId, @NonNull String label, long amountCents) {
        this.categoryId = categoryId;
        this.merchantId = merchantId;
        this.label = label;
        this.amountCents = amountCents;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    @Nullable
    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(@Nullable Long merchantId) {
        this.merchantId = merchantId;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public void setLabel(@NonNull String label) {
        this.label = label;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(long amountCents) {
        this.amountCents = amountCents;
    }
}
