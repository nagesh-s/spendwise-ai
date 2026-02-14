package com.spendwise.ai.gamification;

public class Achievement {
    private final String id;
    private final String title;
    private final String description;
    private final int iconRes;
    private final boolean isUnlocked;

    public Achievement(String id, String title, String description, int iconRes, boolean isUnlocked) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.iconRes = iconRes;
        this.isUnlocked = isUnlocked;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getIconRes() {
        return iconRes;
    }

    public boolean isUnlocked() {
        return isUnlocked;
    }
}
