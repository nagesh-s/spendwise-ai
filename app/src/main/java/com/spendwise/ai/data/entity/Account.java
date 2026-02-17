package com.spendwise.ai.data.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a financial account (Cash, Bank, Credit Card, Wallet, etc.)
 * that can be linked to expenses for multi-account tracking.
 */
@Entity(tableName = "accounts")
public class Account {

    public static final String TYPE_CASH = "CASH";
    public static final String TYPE_BANK = "BANK";
    public static final String TYPE_UPI = "UPI";
    public static final String TYPE_DEBIT_CARD = "DEBIT_CARD";
    public static final String TYPE_CREDIT_CARD = "CREDIT_CARD";
    public static final String TYPE_WALLET = "WALLET";
    public static final String TYPE_OTHER = "OTHER";

    @PrimaryKey(autoGenerate = true)
    private long id;

    @NonNull
    private String name;

    @NonNull
    private String type; // CASH, BANK, CREDIT_CARD, WALLET, OTHER

    private long balanceCents;

    private int colorArgb;

    @Nullable
    private String iconKey;

    private boolean isDefault;

    public Account(@NonNull String name, @NonNull String type, long balanceCents,
            int colorArgb, @Nullable String iconKey, boolean isDefault) {
        this.name = name;
        this.type = type;
        this.balanceCents = balanceCents;
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

    @NonNull
    public String getType() {
        return type;
    }

    public void setType(@NonNull String type) {
        this.type = type;
    }

    public long getBalanceCents() {
        return balanceCents;
    }

    public void setBalanceCents(long balanceCents) {
        this.balanceCents = balanceCents;
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

    /**
     * Get a display-friendly account type name.
     */
    public String getTypeDisplayName() {
        switch (type) {
            case TYPE_CASH:
                return "Cash";
            case TYPE_BANK:
                return "Bank Account";
            case TYPE_UPI:
                return "UPI";
            case TYPE_DEBIT_CARD:
                return "Debit Card";
            case TYPE_CREDIT_CARD:
                return "Credit Card";
            case TYPE_WALLET:
                return "Digital Wallet";
            default:
                return "Other";
        }
    }
}
