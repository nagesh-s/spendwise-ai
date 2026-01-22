package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents an expense category (e.g., Food, Transport, Shopping).
 * 
 * Categories have a visual identity (color and icon) for display in UI.
 * Default categories (isDefault=true) cannot be deleted by the user.
 */
@Entity(
    tableName = "categories",
    indices = {
        @Index(value = "name", unique = true)
    }
)
public class Category {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    /**
     * Color as ARGB integer (e.g., 0xFF4CAF50 for green).
     * Use Color.parseColor() or color resources for conversion.
     */
    private int colorArgb;

    /**
     * Icon identifier key (e.g., "fa_utensils", "fa_car").
     * Used to look up Font Awesome or drawable icons.
     */
    @Nullable
    private String iconKey;

    /**
     * If true, this is a system default category that cannot be deleted.
     * Users can still edit the name, color, and icon of default categories.
     */
    private boolean isDefault;

    // Constructor
    public Category(@NonNull String name, int colorArgb, @Nullable String iconKey, boolean isDefault) {
        this.name = name;
        this.colorArgb = colorArgb;
        this.iconKey = iconKey;
        this.isDefault = isDefault;
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

    @Nullable
    public String getIconKey() {
        return iconKey;
    }

    public void setIconKey(@Nullable String iconKey) {
        this.iconKey = iconKey;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
