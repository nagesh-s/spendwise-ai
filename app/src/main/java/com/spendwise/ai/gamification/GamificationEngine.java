package com.spendwise.ai.gamification;

import android.content.Context;
import android.content.SharedPreferences;

import com.spendwise.ai.R;
import com.spendwise.ai.data.entity.Expense;
import com.spendwise.ai.util.DateUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Engine for gamification: XP, Levels, and Achievements.
 */
public class GamificationEngine {

    private static final String PREFS_NAME = "gamification_prefs";
    private static final String KEY_XP = "user_xp";
    private static final String KEY_UNLOCKED = "unlocked_badges";

    private final SharedPreferences prefs;

    // Static achievement definitions
    public static final String ID_FIRST_EXPENSE = "first_expense";
    public static final String ID_STREAK_3 = "streak_3";
    public static final String ID_STREAK_7 = "streak_7";
    public static final String ID_SAVER_LEVEL_5 = "saver_5";
    public static final String ID_EARLY_BIRD = "early_bird";

    public GamificationEngine(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // ==================== XP & Leveling ====================

    public long getXp() {
        return prefs.getLong(KEY_XP, 0);
    }

    public void addXp(long amount) {
        long current = getXp();
        prefs.edit().putLong(KEY_XP, current + amount).apply();
    }

    public int getLevel() {
        // Simple formula: Level = sqrt(XP / 50) + 1
        // XP: 0 -> L1, 50 -> L2, 200 -> L3, 450 -> L4
        long xp = getXp();
        return (int) Math.sqrt(xp / 50.0) + 1;
    }

    public long getXpForNextLevel() {
        int nextLevel = getLevel() + 1;
        // Inverse: XP = (L-1)^2 * 50
        return (long) (Math.pow(nextLevel - 1, 2) * 50);
    }

    public long getXpProgress() {
        int currentLevel = getLevel();
        long currentLevelXp = (long) (Math.pow(currentLevel - 1, 2) * 50);
        return getXp() - currentLevelXp;
    }

    public long getXpToNextLevel() {
        int currentLevel = getLevel();
        long currentLevelXp = (long) (Math.pow(currentLevel - 1, 2) * 50);
        long nextLevelXp = (long) (Math.pow(currentLevel, 2) * 50);
        return nextLevelXp - currentLevelXp;
    }

    public String getLevelTitle() {
        int level = getLevel();
        if (level >= 50)
            return "Wealth Wizard";
        if (level >= 30)
            return "Financial Guru";
        if (level >= 20)
            return "Budget Master";
        if (level >= 10)
            return "Smart Saver";
        if (level >= 5)
            return "Tracker Pro";
        return "Novice Saver";
    }

    // ==================== Achievements ====================

    public List<Achievement> getAllAchievements() {
        Set<String> unlocked = getUnlockedIds();
        List<Achievement> list = new ArrayList<>();

        list.add(new Achievement(ID_FIRST_EXPENSE, "First Step", "Log your first expense", R.drawable.ic_check_circle,
                unlocked.contains(ID_FIRST_EXPENSE)));
        list.add(new Achievement(ID_STREAK_3, "Consistency", "Track expenses for 3 days in a row", R.drawable.ic_bolt,
                unlocked.contains(ID_STREAK_3)));
        list.add(new Achievement(ID_STREAK_7, "Streak Master", "Track expenses for 7 days in a row", R.drawable.ic_fire,
                unlocked.contains(ID_STREAK_7)));
        list.add(new Achievement(ID_EARLY_BIRD, "Early Bird", "Log an expense before 8 AM", R.drawable.ic_wb_sunny,
                unlocked.contains(ID_EARLY_BIRD)));
        list.add(new Achievement(ID_SAVER_LEVEL_5, "Level Up", "Reach Level 5", R.drawable.ic_trophy,
                unlocked.contains(ID_SAVER_LEVEL_5)));

        return list;
    }

    private Set<String> getUnlockedIds() {
        return prefs.getStringSet(KEY_UNLOCKED, new HashSet<>());
    }

    public boolean unlockAchievement(String id) {
        Set<String> unlocked = new HashSet<>(getUnlockedIds());
        if (!unlocked.contains(id)) {
            unlocked.add(id);
            prefs.edit().putStringSet(KEY_UNLOCKED, unlocked).apply();
            addXp(100); // Bonus XP for badge
            return true;
        }
        return false;
    }

    public List<String> checkAchievements(List<Expense> recentExpenses) {
        return checkAchievements(recentExpenses, false);
    }

    public List<String> checkAchievements(List<Expense> recentExpenses, boolean earlyBirdEvent) {
        List<String> newUnlocks = new ArrayList<>();

        // Check First Expense
        if (!recentExpenses.isEmpty()) {
            if (unlockAchievement(ID_FIRST_EXPENSE))
                newUnlocks.add(ID_FIRST_EXPENSE);
        }

        // Check streak achievements
        int streak = calculateCurrentStreak(recentExpenses);
        if (streak >= 3 && unlockAchievement(ID_STREAK_3)) {
            newUnlocks.add(ID_STREAK_3);
        }
        if (streak >= 7 && unlockAchievement(ID_STREAK_7)) {
            newUnlocks.add(ID_STREAK_7);
        }

        // Check Early Bird (event flag or current local time fallback)
        if (!recentExpenses.isEmpty() && earlyBirdEvent) {
            if (unlockAchievement(ID_EARLY_BIRD)) {
                newUnlocks.add(ID_EARLY_BIRD);
            }
        }

        // Check Level 5
        if (getLevel() >= 5) {
            if (unlockAchievement(ID_SAVER_LEVEL_5))
                newUnlocks.add(ID_SAVER_LEVEL_5);
        }

        return newUnlocks;
    }

    // Helper for Streak Calculation
    public int calculateCurrentStreak(List<Expense> allExpenses) {
        if (allExpenses == null || allExpenses.isEmpty())
            return 0;

        // 1. Extract unique dates
        Set<Long> uniqueDays = new HashSet<>();
        for (Expense e : allExpenses) {
            uniqueDays.add(e.getDateEpochDay());
        }

        // 2. Check overlap with Today/Yesterday
        long today = DateUtils.getTodayEpochDay();
        long yesterday = today - 1;

        int streak = 0;
        long checkDay = today;

        // If no expense today, check if streak continued from yesterday
        if (!uniqueDays.contains(today)) {
            if (uniqueDays.contains(yesterday)) {
                checkDay = yesterday;
            } else {
                return 0; // Streak broken/not started
            }
        }

        // 3. Count backwards
        while (uniqueDays.contains(checkDay)) {
            streak++;
            checkDay--;
        }

        return streak;
    }
}
