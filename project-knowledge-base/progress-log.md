# Progress Log

> Purpose: track shipped milestones and current release readiness status.

---

## Current State: Unreleased (Salary-to-Buckets Automation 2.0)

| Metric | Value |
|---|---|
| Version | `Unreleased` |
| Status | Automation 2.0 implemented: dual ledger, SIP schedules, idempotent event pipeline, reconciliation, reward-only investment XP, digest notifications |
| Last Updated | 2026-03-04 (salary orchestration + recurring investments mode + expected SIP lifecycle + no-dupe pipeline wiring) |

---

## Salary-to-Buckets Automation 2.0 Sweep (2026-03-04)

- Salary automation:
  - salary detection wired from SMS credits and notification text
  - monthly idempotent allocation orchestration with auto/fallback/manual status tracking
  - `Run Allocation Now` action implemented with month guardrails
- Dual ledger UX:
  - bucket cards now show both planned and actual values
  - `Planned | Actual` chips drive emphasis without losing context
  - month-status banner added (pending/detected/auto/fallback/manual)
- Recurring investments:
  - recurring screen now supports mode switch `Expenses | Investments`
  - SIP schedule editor + list added under investment mode
  - schedule backfill integrated with 24-month detail cap and older rollup behavior
- Unified no-dupe ingestion + reconciliation:
  - added `IngestionEvent` registry and pipeline (`sourceKey` unique idempotency)
  - SMS and notification investment ingestion now pass through event pipeline
  - expected occurrence reconciliation supports confirmed/adjusted paths
- Bucket detail expected SIP workflow:
  - expected occurrence section with status chips
  - manual resolve/edit/skip actions for unresolved entries
- Mapping intelligence:
  - low-confidence auto-assign path now prompts user once and learns mapping
  - learned mapping reused for future similar instruments
- Worker scheduling:
  - added and scheduled salary fallback, SIP materialization, and daily digest workers
- Gamification and profile:
  - reward-only investment XP engine implemented
  - profile now surfaces dedicated investment XP stat
- Validation executed:
  - `./gradlew :app:compileStandardDebugJavaWithJavac`
  - `./gradlew :app:compileOfflineLlmDebugJavaWithJavac`
  - `./gradlew :app:testStandardDebugUnitTest`
  - `./gradlew :app:assembleStandardDebug`

---

## Buckets UX + Investment Sync Sweep (2026-03-04)

- Buckets UX refactor:
  - bucket cards moved to denser grid view to keep 4 buckets visible quickly
  - top pinned block removed; action row compacted for better bucket visibility
  - net worth toggle switched to eye/eye-off icon interaction
  - bottom-tab reselect now pops back to tab root (Bucket Detail -> Buckets home)
- Bucket detail form stability:
  - investment add/edit and salary dialogs moved to dedicated layouts to avoid overlap on smaller screens
- Investment ingestion expansion:
  - added `InvestmentSmsParser` + `InvestmentAutoIngestionEngine` dedupe pipeline
  - supports debit/allotment/correction phase merging using source/correlation keys
  - voice-assisted investment entry added in bucket detail
- Import route cleanup:
  - removed non-functional direct AA/Broker/Email menu paths
  - added notification-based sync route (broker/email app notifications)
  - added notification listener service with access-check helper and settings handoff
- Validation executed:
  - `./gradlew :app:assembleStandardDebug`
  - `./gradlew :app:testStandardDebugUnitTest`

---

## UX + Navigation + Automation Sweep (2026-02-17)

- Bottom navigation streamlined to high-frequency destinations only:
  - Removed Categories/Budgets from bottom bar
  - Added Budgets & Categories entry under Settings
- Navigation behavior stabilized:
  - Bottom-tab selection now preserves fragment state/scroll instead of forcing refresh
  - Reselection on current tab is no-op
- Auto Transactions UX + control updates:
  - Added enable/disable switch in Settings and Auto Transactions screen
  - Added persisted preference (`SmsAutoImportPrefs`) and WorkManager wiring to schedule/cancel periodic SMS import
  - Auto Transactions screen now reloads recent synced SMS window and marks already-imported rows
- Recurring expenses updated with shorter intervals:
  - Added `EVERY_2_DAYS` and `EVERY_3_DAYS` support in editor, list labels, and worker due-date generation
- Validation executed:
  - `./gradlew :app:assembleDebug :app:testDebugUnitTest`

## Latest Validation Sweep (Post-v1.2.2 Audit)

- Verified full build gates:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:assembleRelease`
  - `./gradlew :app:assembleDebugAndroidTest`
  - `./gradlew :app:connectedDebugAndroidTest`
  - `./gradlew :app:lintDebug`
- Fixed release blockers:
  - Lint `MissingSuperCall` in `LockScreenActivity`
  - Manifest telephony feature declaration for SMS permission compatibility
  - API-safe biometric enrollment settings routing with fallback
  - Runtime Calendar crash due to legacy `android.support.v4` class resolution in `MaterialCalendarView`
- Fixed implementation gap:
  - Expense edit flow no longer shows `+10 XP` save message
  - Streak + Early Bird achievement checks now wired on new-expense save events
- Added unit coverage for post-v1.2.2 features:
  - `ReportGeneratorTest` (CSV escaping/fallback correctness)
  - `GamificationEngineTest` expansion for achievement unlock scenarios
- Added instrumentation crash-regression coverage:
  - `CalendarInflationSmokeTest` (navigates to Calendar and asserts view inflation)
- Synced release metadata to actual latest release:
  - `versionCode 160`
  - `versionName "1.6.0"`

---

## Hotfix Sweep (Voice Reliability)

- Hardened speech handling in `VoiceInputHelper`:
  - partial transcript capture enabled as fallback when recognizer returns no final match
  - tuned recognizer intent extras (max results, offline preference, silence windows)
  - replaced raw `"No match"` surface errors with actionable messaging
  - added one-time retry for `ERROR_NO_MATCH` and `ERROR_CLIENT` paths
  - added explicit handling for language support/unavailable errors (`12/13`) with fallback-language retry
- Upgraded `VoiceExpenseParser`:
  - currency-aware amount parsing (`₹`, `Rs`, `INR`)
  - comma/decimal amount parsing
  - richer merchant extraction (`at`, `from`, `paid ... to`)
  - keyword-scored category inference against existing categories
- Added parser-first hybrid orchestration:
  - `VoiceUnderstandingEngine` routes parser-first results and attempts optional on-device semantic extraction only for low-confidence parses
  - high-confidence voice parses auto-save directly (no modal confirmation)
  - low-confidence parses open editor with transcript and only trusted prefilled fields
- Added optional offline LLM flavor:
  - `offlineLlm` product flavor added in Gradle with build fields `OFFLINE_LLM_ENABLED`, `OFFLINE_LLM_MODEL_PATH`, `OFFLINE_LLM_MODEL_ID`
  - `VoiceFallbackExtractorFactory` now routes extractor creation by flavor
  - `offlineLlm` flavor includes `MediaPipeGemmaExpenseExtractor` (Gemma 3 1B int4 task model) with semantic fallback when model is missing/unavailable
  - model provisioning is zero-setup: extractor auto-copies bundled asset model into app-private files on first use
- Validation executed:
  - `./gradlew :app:testDebugUnitTest :app:assembleDebug`
  - `./gradlew :app:testStandardDebugUnitTest :app:testOfflineLlmDebugUnitTest`
  - `./gradlew :app:assembleStandardDebug :app:assembleOfflineLlmDebug`

---

## Hotfix Sweep (Widget + SMS Continuity)

- QuickTiles widget action updates:
  - tapping a widget tile now inserts expense in background without launching app UI
  - success feedback is shown via toast from widget action
  - widget add (`+`) now routes to Quick Tile add modal directly
  - widget refresh path switched to dataset notifications to reduce home-screen flicker
- SMS import continuity updates:
  - introduced shared `SmsSyncState` (`last_sync_timestamp`) used by both manual scan and periodic worker
  - manual SMS sync now reads only messages newer than last synced timestamp (bounded by fallback lookback)
  - periodic SMS auto-import now uses same pointer, preventing repeated re-reads of old inbox data
- Additional coverage:
  - `SmsExpenseClassifierTest` added for category mapping and fallback behavior
- Validation executed:
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:test`

---

## Feature Delivery Timeline

### v1.6.0 (Release Prep)
- Onboarding flow (`OnboardingActivity`, 3-slide pager)
- Splash/theme polish and launcher icon refinement
- Release minification enabled (R8/ProGuard)

### v1.5.0 (Phase 4 + 5)
- Export reports from Settings (CSV/PDF)
- Gamification system (XP, levels, badges)
- Profile screen with progress and achievements

### v1.4.0 (Phase 3 Part 2)
- Voice input on Dashboard
- Voice text parsing into amount/category/merchant prefill

### v1.3.0 (Phase 3 Part 1)
- AI insights cards (spending spikes, budget alerts, streaks, month comparison)
- Smart predictions (month-end forecast + budget risk)

### v1.2.2 (Stabilization + CI/Test Baseline)
- CI workflow
- SMS hardening
- Unit + instrumentation baseline tests

### v1.2.0 - v1.2.1
- Tags, accounts, goals, analytics, SMS reader
- Category and settings stabilization fixes

### v1.1.x
- Quick tiles, recurring, reminders, biometric, photo attachments
- UX and reliability refinements

---

## Version Table

| Version | Description | Date |
|---|---|---|
| v0.0.1 | Scaffold + navigation shell | 2026-01-22 |
| v0.1.0 | Data layer foundation | 2026-01-22 |
| v0.2.0 | Repository + ViewModel architecture | 2026-01-22 |
| v0.3.0 | Core UI screens and flows | 2026-01-22 |
| v1.1.0 | QuickTiles, recurring, reminders, biometric, photos | 2026-02-08 |
| v1.1.1 | UX + dashboard/category/merchant insights fixes | 2026-02-08 |
| v1.1.2 | UI polish + quick add + biometric/modal/navigation fixes | 2026-02-08 |
| v1.2.0 | Tags, multi-account, goals, analytics, SMS reader | 2026-02-14 |
| v1.2.1 | Stabilization fixes | 2026-02-14 |
| v1.2.2 | CI + tests + SMS hardening + release prep | 2026-02-14 |
| v1.3.0 | AI insights + predictions | 2026-02-14 |
| v1.4.0 | Voice input | 2026-02-14 |
| v1.5.0 | Reports + gamification/profile | 2026-02-14 |
| v1.6.0 | Onboarding + release polish + release audit fixes | 2026-02-14 |
