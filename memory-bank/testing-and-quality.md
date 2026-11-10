Use unit tests for pure business logic (parsers, formatters, helpers) and androidTest for Room migration, DAO integrity, and ViewModel↔Repository persistence flows.

Current baseline coverage:
- `app/src/test/java/com/spendwise/ai/sms/TransactionParserTest.java`
- `app/src/test/java/com/spendwise/ai/sms/SmsExpenseClassifierTest.java`
- `app/src/test/java/com/spendwise/ai/voice/VoiceExpenseParserTest.java`
- `app/src/test/java/com/spendwise/ai/voice/VoiceUnderstandingEngineTest.java`
- `app/src/test/java/com/spendwise/ai/voice/OnDeviceSemanticExpenseExtractorTest.java`
- `app/src/test/java/com/spendwise/ai/voice/VoiceFallbackExtractorFactoryTest.java`
- `app/src/test/java/com/spendwise/ai/gamification/GamificationEngineTest.java`
- `app/src/test/java/com/spendwise/ai/data/export/ReportGeneratorTest.java`
- `app/src/test/java/com/spendwise/ai/engine/BucketAllocationEngineTest.java`
- `app/src/test/java/com/spendwise/ai/engine/SalaryDetectorTest.java`
- `app/src/test/java/com/spendwise/ai/engine/CashflowGuardrailTest.java`
- `app/src/test/java/com/spendwise/ai/sms/InvestmentSmsParserTest.java`
- `app/src/test/java/com/spendwise/ai/sms/GoldenCorpusEvaluatorTest.java`
- `app/src/test/java/com/spendwise/ai/sms/TransactionParserPromotionalTest.java`
- `app/src/test/java/com/spendwise/ai/domain/InvestmentLedgerServiceTest.java`
- `app/src/androidTest/java/com/spendwise/ai/data/AppDatabaseMigrationTest.java`
- `app/src/androidTest/java/com/spendwise/ai/common/ExpenseEditorViewModelSaveTest.java`
- `app/src/androidTest/java/com/spendwise/ai/categories/CategoryBudgetIntegrityTest.java`
- `app/src/androidTest/java/com/spendwise/ai/calendar/CalendarInflationSmokeTest.java`

Coverage notes:
- Parser tests include sender filtering, amount/type extraction, merchant extraction, and invalid sender rejection.
- SMS classifier tests validate category mapping (bills/transport/fallback) and description fallback behavior.
- Voice parser tests include currency-token amounts (`Rs`, `INR`, `₹`), comma decimals, merchant phrase variants (`at`, `from`, `paid ... to`), keyword category inference, invalid-input fallbacks, and confidence/auto-save eligibility flags.
- Voice understanding engine tests validate parser-first behavior, optional on-device fallback merge gating, and low-confidence override protection.
- On-device semantic extractor tests validate number-word amount extraction, keyword category extraction, and merchant extraction in offline mode.
- Voice fallback factory tests validate flavor-driven extractor creation and working fallback extraction.
- Report export tests validate CSV formatting, escaping, and fallback field behavior.
- Gamification tests cover leveling math, streak calculation, and achievement unlock paths.
- BucketAllocationEngine tests (11) cover: default allocation, reverse budgeting order, guilt-free calculation, guardrail overrides, normalization, zero salary, single bucket, formatted output.
- SalaryDetector tests (15) cover: keyword detection, NEFT/IMPS signals, refund/cashback/interest rejection, tolerance checks, custom thresholds, result formatting.
- CashflowGuardrail tests (11) cover: healthy state, emergency threshold/critical, bills buffer, overspend detection, edge cases.
- Investment SMS parser tests cover: MF/ETF/stock/FD/RD signal detection, source/correlation key generation, and correction-message parsing.
- Expense editor instrumentation covers save, edit, and delete paths with merchant/account/tag persistence checks.
- Category/budget instrumentation validates default-category delete protection and categoryId-based budget spend joins.
- Calendar smoke instrumentation validates navigation + successful `MaterialCalendarView` inflation.

Verification commands:
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
- `./gradlew :app:testStandardDebugUnitTest`
- `./gradlew :app:testOfflineLlmDebugUnitTest`
- `./gradlew :app:assembleStandardDebug`
- `./gradlew :app:assembleOfflineLlmDebug`
- `./gradlew :app:assembleDebugAndroidTest`
- `./gradlew :app:lintStandardDebug :app:lintOfflineLlmDebug`
- `./gradlew :app:connectedDebugAndroidTest`

CI gate:
- `.github/workflows/android-ci.yml` runs:
  - `:app:assembleDebug`
  - `:app:testDebugUnitTest`
  - `:app:assembleDebugAndroidTest`
  - `:app:lintStandardDebug :app:lintOfflineLlmDebug`

Quality gates before release:
- Unit tests pass.
- App debug build passes.
- Android test APK compiles.
- Connected instrumentation tests pass on at least one emulator/device run before tagging.

Migration coverage and current-run status:
- `AppDatabaseMigrationTest` verifies **3→4 only**. It does not validate upgrades through schema 10; no 9→10 migration exists. Add explicit migration/backfill coverage before making existing-user upgrade claims.
- `GoldenCorpusEvaluatorTest` exercises corpus-based parser evaluation; `TransactionParserPromotionalTest` asserts promotional rental-plan text is rejected; `InvestmentLedgerServiceTest` checks transaction/holding ledger behavior. File presence is not proof of a passing run.
- Latest native JDK 21 gate: **passed** with `./gradlew --no-daemon --continue :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest :app:lintStandardDebug :app:lintOfflineLlmDebug`. Both debug app APKs and both Android-test APKs assembled; each flavor passed **84 JVM tests with zero failures or errors**. Both lint reports have **zero errors and 376 pre-existing warnings**. The command used Temurin 21 only through `JAVA_HOME`/`PATH` for that invocation; no global Java setting changed.
- Verification exposed and fixed a genuine accounting defect: `recomputeHoldingAggregates(...)` previously counted a pending contribution. It now includes only confirmed transactions; the unchanged existing ledger regression passes.
- Lint exposed and fixed two theme-resource errors per flavor: `windowLightNavigationBar` now lives in API-27-qualified resource variants. The 376 existing warnings remain follow-up work rather than a passing-lint error count.
- Historical native Java 25 `offlineLlmDebugUnitTest` result: **84 tests, 8 failures**, all Mockito/Byte Buddy initialization failures because Java 25 exceeded the installed Byte Buddy support. Use JDK 21 rather than enabling Byte Buddy's experimental Java 25 override.
- JDK 17 cannot compile `tasks-genai:0.10.27` because it contains Java 21 bytecode (`class version 65`). Toolchain/CI use JDK 21. An earlier isolated Linux-container attempt did not complete resource processing because the installed Android SDK resource compiler was a macOS executable; the native JDK 21 result above supersedes that environment attempt.
- Gitleaks completed full Git-history scanning: 54 commits, no leaks found. The final `gitleaks dir` filesystem scan processed 167.83 MB and found no leaks. The one earlier planning-file alert was resolved by replacing unnecessary identifier wording rather than adding a broad exclusion. Final `git diff --check`, workflow YAML parsing, and relative Markdown-link validation passed.
- No connected emulator/device tests have been run in this preparation task.
