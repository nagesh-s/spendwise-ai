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
- `./gradlew :app:connectedDebugAndroidTest`

CI gate:
- `.github/workflows/android-ci.yml` runs:
  - `:app:assembleDebug`
  - `:app:testDebugUnitTest`
  - `:app:assembleDebugAndroidTest`

Quality gates before release:
- Unit tests pass.
- App debug build passes.
- Android test APK compiles.
- Connected instrumentation tests pass on at least one emulator/device run before tagging.
