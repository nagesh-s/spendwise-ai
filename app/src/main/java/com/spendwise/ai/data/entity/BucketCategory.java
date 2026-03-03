package com.spendwise.ai.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Join entity linking expense categories to buckets (many-to-one).
 * E.g., "Food & Dining" → Bucket 1 (Enjoy the Present),
 * "Travel" → Bucket 1 (Enjoy the Present).
 *
 * Enables per-bucket spending analysis by mapping existing spending
 * categories to their corresponding life-stage bucket.
 */
@Entity(tableName = "bucket_categories", foreignKeys = {
        @ForeignKey(entity = Bucket.class, parentColumns = "id", childColumns = "bucketId", onDelete = ForeignKey.CASCADE),
        @ForeignKey(entity = Category.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.CASCADE)
}, indices = {
        @Index("bucketId"),
        @Index("categoryId")
})
public class BucketCategory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long bucketId;

    private long categoryId;

    public BucketCategory(long bucketId, long categoryId) {
        this.bucketId = bucketId;
        this.categoryId = categoryId;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBucketId() {
        return bucketId;
    }

    public void setBucketId(long bucketId) {
        this.bucketId = bucketId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }
}
