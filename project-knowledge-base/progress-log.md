# Progress Log

> Purpose: track shipped milestones and current release readiness status.

---

## Current State: v1.6.0 (Release-Candidate Ready)

| Metric | Value |
|---|---|
| Version | `v1.6.0` |
| Status | Phase 1-6 features implemented, validated, and documented |
| Last Updated | 2026-02-14 |

---

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
