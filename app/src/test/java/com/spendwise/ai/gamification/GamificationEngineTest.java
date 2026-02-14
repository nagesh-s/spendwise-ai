package com.spendwise.ai.gamification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;

import com.spendwise.ai.data.entity.Expense;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class GamificationEngineTest {

    @Mock
    private Context context;

    @Mock
    private SharedPreferences sharedPreferences;

    @Mock
    private SharedPreferences.Editor editor;

    private GamificationEngine engine;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(context.getSharedPreferences("gamification_prefs", Context.MODE_PRIVATE)).thenReturn(sharedPreferences);
        org.mockito.Mockito.lenient().when(sharedPreferences.edit()).thenReturn(editor);
        org.mockito.Mockito.lenient()
                .when(editor.putLong(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(editor);
        org.mockito.Mockito.lenient()
                .when(editor.putStringSet(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(editor);

        engine = new GamificationEngine(context);
    }

    @Test
    public void testLevelCalculation() {
        // Level 1: 0 XP
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(0L);
        assertEquals(1, engine.getLevel());

        // Level 2: 50 XP (sqrt(50/50) + 1 = 2)
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(50L);
        assertEquals(2, engine.getLevel());

        // Level 3: 200 XP (sqrt(200/50) + 1 = sqrt(4)+1 = 3)
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(200L);
        assertEquals(3, engine.getLevel());
    }

    @Test
    public void testXpProgress() {
        // Level 1, 10 XP
        // Level 1 starts at 0 XP.
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(10L);
        // Progress should be 10 - 0 = 10
        assertEquals(10, engine.getXpProgress());

        // Level 2 starts at 50 XP
        // Current XP 70
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(70L);
        // Level 2
        // Progress = 70 - 50 = 20
        assertEquals(20, engine.getXpProgress());
    }

    @Test
    public void testXpToNextLevel() {
        // Level 1 (0 XP) to Level 2 (50 XP) range is 50
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(0L);
        assertEquals(50, engine.getXpToNextLevel());

        // Level 2 starts at 50 XP. Level 3 starts at 200 XP.
        // Range = 200 - 50 = 150
        when(sharedPreferences.getLong("user_xp", 0)).thenReturn(50L);
        assertEquals(150, engine.getXpToNextLevel());
    }

    @Test
    public void testStreakCalculation() {
        try (MockedStatic<com.spendwise.ai.util.DateUtils> mockedDateUtils = org.mockito.Mockito
                .mockStatic(com.spendwise.ai.util.DateUtils.class)) {
            long today = 1000L;
            mockedDateUtils.when(com.spendwise.ai.util.DateUtils::getTodayEpochDay).thenReturn(today);

            List<Expense> expenses = new ArrayList<>();
            // No expenses -> 0 streak
            assertEquals(0, engine.calculateCurrentStreak(expenses));

            // 1 expense today -> 1 streak
            expenses.add(createExpense(today));
            assertEquals(1, engine.calculateCurrentStreak(expenses));

            // Expense today and yesterday -> 2 streak
            expenses.add(createExpense(today - 1));
            assertEquals(2, engine.calculateCurrentStreak(expenses));

            // Expense today, yesterday, and day before -> 3 streak
            expenses.add(createExpense(today - 2));
            assertEquals(3, engine.calculateCurrentStreak(expenses));

            // Gap: Today, Yesterday, GAP, 4 days ago -> 2 streak
            List<Expense> gapExpenses = new ArrayList<>();
            gapExpenses.add(createExpense(today));
            gapExpenses.add(createExpense(today - 1));
            gapExpenses.add(createExpense(today - 3));
            assertEquals(2, engine.calculateCurrentStreak(gapExpenses));
        }
    }

    private Expense createExpense(long epochDay) {
        return new Expense(epochDay, 100, "Test", 1L, null);
    }
}
