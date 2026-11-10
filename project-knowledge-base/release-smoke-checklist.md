# Release Smoke Checklist

Use this checklist before publishing any build candidate.

## Build and test gates
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleStandardDebug`
- [ ] `./gradlew :app:assembleOfflineLlmDebug`
- [ ] `./gradlew :app:testStandardDebugUnitTest`
- [ ] `./gradlew :app:testOfflineLlmDebugUnitTest`
- [ ] `./gradlew :app:assembleDebugAndroidTest`
- [ ] `./gradlew :app:connectedDebugAndroidTest` (with running emulator/device)

## Core user flows
- [ ] Add expense (amount/category/date/merchant) succeeds
- [ ] Edit existing expense persists updates
- [ ] Delete expense removes row and UI refreshes
- [ ] Quick Tile tap creates expense immediately
- [ ] Home-screen Quick Tile tap adds in background and keeps launcher/home in foreground
- [ ] Quick Tile widget success toast appears after background add
- [ ] Widget `+` opens Quick Tile add modal directly
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
- [ ] Voice `No match` / timeout still routes partial transcript to editor when available
- [ ] Voice high-confidence parse auto-saves expense without showing modal
- [ ] Voice low-confidence parse opens editor with transcript and only trusted field prefill
- [ ] Voice `ERROR_CLIENT`/`ERROR_NO_MATCH` retry path works without crash
- [ ] On-device semantic fallback path enriches low-confidence transcripts without network
- [ ] With a separately supplied compatible Gemma model in flavor assets, `offlineLlm` provisions it into app files (default `files/models/spendwise_gemma3_1b.task`) and enriches low-confidence voice parses
- [ ] `offlineLlm` flavor gracefully falls back to semantic extraction when local model file is missing
- [ ] Calendar tab opens without crash and month/date selection works
- [ ] Profile screen opens from Dashboard and shows level/XP/achievements
- [ ] XP is awarded only on new expense save (not edit)
- [ ] CSV export produces valid rows with escaped values
- [ ] PDF export writes a readable report file

## Buckets + investment sync checks
- [ ] Buckets tab shows compact grid (2-column on phone) without major text truncation
- [ ] Eye icon toggles net worth visibility (`hidden` -> `visible`) correctly
- [ ] Re-tapping Buckets tab from Bucket Detail returns to Buckets root
- [ ] Bucket detail add/edit forms do not overlap fields on small devices
- [ ] `Auto-import from SMS now` ingests investment messages without duplicates
- [ ] `Sync from notifications (apps/email)` prompts notification access when disabled
- [ ] With notification access enabled, broker/email notification signals are ingested
- [ ] Debit/allotment/correction notification/SMS sequence updates a single investment row where correlation matches

## Permission and resilience checks
- [ ] SMS denied flow shows rationale
- [ ] SMS permanently denied flow offers app settings action
- [ ] Failed SMS import can be retried
- [ ] Manual SMS sync only reads messages newer than stored last sync timestamp
- [ ] Background SMS auto-import keeps advancing last sync timestamp and avoids re-import of previously scanned range
- [ ] Microphone permission denied flow shows user feedback without crash

## Release metadata
- [ ] `versionCode` and `versionName` are set for target release
- [ ] `CHANGELOG.md` includes final release notes
- [ ] Progress log and module docs are updated
## Upgrade and data-retention gates
- [ ] An explicit 9→10 migration/upgrade policy exists before shipping to existing users; current source registers migrations only through 8→9
- [ ] Upgrade instrumentation preserves representative existing-user data through version 10 (the current migration test covers only 3→4)
- [ ] Export/import coverage is understood: JSON format version 3 is a partial, merging export, not a complete backup or database migration
- [ ] Verify on a clean test device whether all preferences, ledger/source/review records, and attachments that users expect to survive are actually retained
