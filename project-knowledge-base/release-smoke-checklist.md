# Release Smoke Checklist (v1.6.0)

Use this checklist before publishing any build candidate.

## Build and test gates
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleDebugAndroidTest`
- [ ] `./gradlew :app:connectedDebugAndroidTest` (with running emulator/device)

## Core user flows
- [ ] Add expense (amount/category/date/merchant) succeeds
- [ ] Edit existing expense persists updates
- [ ] Delete expense removes row and UI refreshes
- [ ] Quick Tile tap creates expense immediately
- [ ] Biometric lock toggle and unlock flow works on supported device
- [ ] First-run onboarding appears once, then routes to main app

## Phase 2 feature checks
- [ ] Tags can be created and attached in expense editor
- [ ] Account selection persists on saved expense
- [ ] Goals screen add/update flows are functional
- [ ] Analytics screen period switch renders all charts
- [ ] Auto Transactions scan works with permission granted
- [ ] SMS import handles duplicate detection safely

## Phase 3-6 feature checks
- [ ] Dashboard AI insights section renders with data and hides cleanly when empty
- [ ] Dashboard prediction section renders month-end/category forecast cards
- [ ] Voice input flow opens prefilled expense editor from parsed speech
- [ ] Calendar tab opens without crash and month/date selection works
- [ ] Profile screen opens from Dashboard and shows level/XP/achievements
- [ ] XP is awarded only on new expense save (not edit)
- [ ] CSV export produces valid rows with escaped values
- [ ] PDF export writes a readable report file

## Permission and resilience checks
- [ ] SMS denied flow shows rationale
- [ ] SMS permanently denied flow offers app settings action
- [ ] Failed SMS import can be retried
- [ ] Microphone permission denied flow shows user feedback without crash

## Release metadata
- [ ] `versionCode` and `versionName` are set for target release
- [ ] `CHANGELOG.md` includes final release notes
- [ ] Progress log and module docs are updated
- [ ] `lintDebug` passes with no blocking errors
