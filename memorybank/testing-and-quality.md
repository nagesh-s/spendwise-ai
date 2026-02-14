Use unit tests for pure business logic (parsers, formatters, helpers) and androidTest for Room migration, DAO integrity, and ViewModel↔Repository persistence flows.

Current baseline coverage:
- `app/src/test/java/com/spendwise/ai/sms/TransactionParserTest.java`
- `app/src/test/java/com/spendwise/ai/gamification/GamificationEngineTest.java`
- `app/src/androidTest/java/com/spendwise/ai/data/AppDatabaseMigrationTest.java`
- `app/src/androidTest/java/com/spendwise/ai/common/ExpenseEditorViewModelSaveTest.java`
- `app/src/androidTest/java/com/spendwise/ai/categories/CategoryBudgetIntegrityTest.java`

Coverage notes:
- Parser tests include sender filtering, amount/type extraction, merchant extraction, and invalid sender rejection.
- Expense editor instrumentation covers save, edit, and delete paths with merchant/account/tag persistence checks.
- Category/budget instrumentation validates default-category delete protection and categoryId-based budget spend joins.

Verification commands:
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebug`
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
