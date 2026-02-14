package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a tag/label that can be applied to expenses.
 * Tags provide flexible categorization beyond the main category system.
 * Examples: "Business trip", "Tax deductible", "Wedding planning"
 */
@Entity(tableName = "tags")
public class Tag {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    private int colorArgb;

    public Tag(@NonNull String name, int colorArgb) {
        this.name = name;
        this.colorArgb = colorArgb;
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

    public int getColorArgb() {
        return colorArgb;
    }

    public void setColorArgb(int colorArgb) {
        this.colorArgb = colorArgb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Tag tag = (Tag) o;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
