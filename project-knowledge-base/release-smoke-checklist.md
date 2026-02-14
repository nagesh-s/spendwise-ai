# Release Smoke Checklist (v1.2.2)

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

## Phase 2 feature checks
- [ ] Tags can be created and attached in expense editor
- [ ] Account selection persists on saved expense
- [ ] Goals screen add/update flows are functional
- [ ] Analytics screen period switch renders all charts
- [ ] Auto Transactions scan works with permission granted
- [ ] SMS import handles duplicate detection safely

## Permission and resilience checks
- [ ] SMS denied flow shows rationale
- [ ] SMS permanently denied flow offers app settings action
- [ ] Failed SMS import can be retried

## Release metadata
- [ ] `versionCode` and `versionName` are set for target release
- [ ] `CHANGELOG.md` includes final release notes
- [ ] Progress log and module docs are updated
