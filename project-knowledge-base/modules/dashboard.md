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
4. Voice input text is resolved by parser-first `VoiceUnderstandingEngine` with flavor-aware on-device fallback.
5. If recognizer final result fails (`ERROR_NO_MATCH`/timeout), latest partial transcript is used as fallback.
6. If parsed amount/category/merchant confidence is high, dashboard auto-saves the expense without modal confirmation.
7. If any field confidence is low, dashboard opens Add Expense and pre-fills only trusted fields.

## Files

| File | Purpose |
|---|---|
| `dashboard/DashboardFragment.java` | UI wiring, charts, card sections, voice + profile actions |
| `dashboard/DashboardViewModel.java` | Month state + data orchestration |
| `insights/InsightsEngine.java` | Insight generation rules |
| `insights/PredictionEngine.java` | Prediction generation rules |
| `voice/VoiceInputHelper.java` | Speech recognizer wrapper |
| `voice/VoiceExpenseParser.java` | Parses spoken text to expense fields |
| `voice/VoiceUnderstandingEngine.java` | parser-first + optional on-device fallback merge orchestration |
| `voice/VoiceFallbackExtractorFactory.java` | flavor-aware extractor selection (`standard` vs `offlineLlm`) |
| `voice/OnDeviceSemanticExpenseExtractor.java` | offline semantic fallback extraction for low-confidence transcripts |
| `voice/mediapipe/MediaPipeGemmaExpenseExtractor.java` | (`offlineLlm` flavor) MediaPipe Gemma local extraction |
| `res/layout/fragment_dashboard.xml` | Dashboard layout |

## Common Issues

| Issue | Likely Cause | Check |
|---|---|---|
| Voice button does nothing | mic permission denied | `DashboardFragment` permission launcher |
| Voice shows repeated "No match" | recognizer final result dropped but no partial fallback | `VoiceInputHelper` partial-result handling and error mapping |
| Voice log shows Error 13 | speech language unavailable on recognizer service | `VoiceInputHelper` language-error mapping and fallback retry |
| Voice always opens modal (no auto-save) | parsed category/amount not high-confidence | `VoiceExpenseParser` certainty flags + auto-save gate in `DashboardFragment` |
| `offlineLlm` still behaves like semantic-only fallback | bundled model asset missing or copy failed | ensure `app/src/offlineLlm/assets/models/spendwise_gemma3_1b.task` is packaged and check `MediaPipeGemmaExpenseExtractor` init logs |
| Insights/predictions hidden | no qualifying data | engine conditions + date range |
| Profile icon not navigating | missing nav destination ID | `navigation_profile` in nav graph |
| Projected total seems off | early month low sample size | run-rate logic in `updateSpendingPace()` |

## Last Updated
2026-02-14 (offline LLM flavor integration)
