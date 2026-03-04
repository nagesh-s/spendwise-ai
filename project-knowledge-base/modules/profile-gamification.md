# Profile & Gamification Module

> Purpose: explain XP, leveling, achievements, and investment progression signals in Profile.

## Status
Implemented. Expense gamification + investment reward-only gamification are both active.

## What It Does
- Expense actions:
  - award XP for new expense saves
  - unlock achievements (first expense, streaks, early bird, level milestone)
- Investment actions (reward-only):
  - XP for salary allocation runs
  - XP for SIP reconciliation events
  - optional XP boosts for higher goal progress bands
- Profile UI:
  - level, lifetime XP, streak
  - achievements grid
  - dedicated `Investment XP` card

## Key Flow
1. Expense save triggers `GamificationEngine`.
2. Salary orchestration / reconciliation triggers `InvestmentGamificationEngine`.
3. Investment gamification increments:
   - investment-local XP store (`investment_gamification_prefs`)
   - global profile XP through `GamificationEngine`.
4. `ProfileFragment` reads and renders both overall XP and investment XP.

## Files
- `app/src/main/java/com/spendwise/ai/gamification/GamificationEngine.java`
- `app/src/main/java/com/spendwise/ai/engine/InvestmentGamificationEngine.java`
- `app/src/main/java/com/spendwise/ai/gamification/ProfileFragment.java`
- `app/src/main/res/layout/fragment_profile.xml`

## Constraints
- Gamification state is app-local (SharedPreferences), not cloud-synced.
- Investment policy is reward-only: no XP deduction paths.

## Last Updated
2026-03-04
