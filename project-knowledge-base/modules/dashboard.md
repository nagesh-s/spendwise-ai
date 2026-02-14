# Dashboard Module

> Purpose: explain home analytics, AI cards, voice input, and navigation actions.

---

## Status: Implemented + Expanded (v1.6.0)

## What This Screen Does

- Core analytics:
  - monthly total
  - budget health
  - spending pace (avg/day + projected total)
  - category pie chart
  - budget usage bar chart
  - top categories list
- AI layer:
  - insights cards (spikes, budget alerts, streaks, month comparison)
  - prediction cards (month-end and category forecasts, budget risk)
- Quick actions:
  - add expense FAB
  - voice input FAB
  - profile shortcut (gamification screen)

## Data Flow

1. `DashboardViewModel` owns selected month state.
2. DAO aggregates update totals/charts via LiveData observers.
3. `InsightsEngine` and `PredictionEngine` run in background and publish card models.
4. Voice input text is parsed and routed to prefilled `ExpenseEditorDialogFragment`.

## Files

| File | Purpose |
|---|---|
| `dashboard/DashboardFragment.java` | UI wiring, charts, card sections, voice + profile actions |
| `dashboard/DashboardViewModel.java` | Month state + data orchestration |
| `insights/InsightsEngine.java` | Insight generation rules |
| `insights/PredictionEngine.java` | Prediction generation rules |
| `voice/VoiceInputHelper.java` | Speech recognizer wrapper |
| `voice/VoiceExpenseParser.java` | Parses spoken text to expense fields |
| `res/layout/fragment_dashboard.xml` | Dashboard layout |

## Common Issues

| Issue | Likely Cause | Check |
|---|---|---|
| Voice button does nothing | mic permission denied | `DashboardFragment` permission launcher |
| Insights/predictions hidden | no qualifying data | engine conditions + date range |
| Profile icon not navigating | missing nav destination ID | `navigation_profile` in nav graph |
| Projected total seems off | early month low sample size | run-rate logic in `updateSpendingPace()` |

## Last Updated
2026-02-14
