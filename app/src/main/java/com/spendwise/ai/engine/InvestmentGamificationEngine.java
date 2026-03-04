package com.spendwise.ai.engine;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.spendwise.ai.gamification.GamificationEngine;

/**
 * Reward-only gamification hooks for investment behavior.
 */
public class InvestmentGamificationEngine {

    private static final String PREFS_NAME = "investment_gamification_prefs";
    private static final String KEY_INVESTMENT_XP = "investment_xp";

    private final GamificationEngine gamificationEngine;
    private final SharedPreferences prefs;

    public InvestmentGamificationEngine(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        this.gamificationEngine = new GamificationEngine(appContext);
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void awardSalaryAllocationXp() {
        addInvestmentXp(20);
    }

    public void awardSipReconcileXp() {
        addInvestmentXp(15);
    }

    public void awardGoalProgressXp(int progressPercent) {
        if (progressPercent >= 100) {
            addInvestmentXp(40);
        } else if (progressPercent >= 80) {
            addInvestmentXp(20);
        } else if (progressPercent >= 50) {
            addInvestmentXp(10);
        }
    }

    public long getInvestmentXp() {
        return prefs.getLong(KEY_INVESTMENT_XP, 0L);
    }

    private void addInvestmentXp(long amount) {
        if (amount <= 0) {
            return;
        }
        long current = getInvestmentXp();
        prefs.edit().putLong(KEY_INVESTMENT_XP, current + amount).apply();
        gamificationEngine.addXp(amount);
    }
}
