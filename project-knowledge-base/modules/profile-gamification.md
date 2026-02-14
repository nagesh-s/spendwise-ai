# Profile & Gamification Module

> Purpose: explain XP, leveling, achievement unlocks, and profile UI flow.

---

## Status: Implemented (v1.5.0+) and Audited (v1.6.0)

## What It Does

- Awards XP when users log new expenses.
- Tracks level progression from XP.
- Unlocks achievements (first expense, streak milestones, early bird, level milestone).
- Shows user profile with:
  - current level/title
  - XP progress to next level
  - achievement list and unlock status

## Key Flow

1. New expense save in `ExpenseEditorViewModel` awards XP and checks achievement conditions.
2. `GamificationEngine` persists XP + unlocked IDs in SharedPreferences.
3. Dashboard profile button navigates to `ProfileFragment`.
4. `ProfileFragment` loads computed stats and badge list for display.

## Files

| File | Purpose |
|---|---|
| `gamification/GamificationEngine.java` | XP math + achievement checks |
| `gamification/ProfileFragment.java` | Profile UI binding |
| `gamification/Achievement.java` | Achievement model |
| `gamification/AchievementAdapter.java` | Badge list renderer |
| `common/ExpenseEditorViewModel.java` | XP award + trigger point |
| `res/layout/fragment_profile.xml` | Profile screen layout |
| `res/layout/item_achievement.xml` | Achievement item layout |

## Known Constraints

- Achievements are app-local (SharedPreferences), not cloud-synced.
- Early Bird unlock is event-driven at save time (before 8 AM local time).

## Last Updated
2026-02-14
