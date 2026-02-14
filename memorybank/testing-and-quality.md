Use unit tests for pure business logic (parsers, formatters, helpers) and androidTest for Room migration and ViewModel↔Repository persistence flows.

Current baseline coverage:
- `app/src/test/java/com/spendwise/ai/sms/TransactionParserTest.java`
- `app/src/androidTest/java/com/spendwise/ai/data/AppDatabaseMigrationTest.java`
- `app/src/androidTest/java/com/spendwise/ai/common/ExpenseEditorViewModelSaveTest.java`

Verification commands:
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:assembleDebugAndroidTest`

Quality gates before release:
- Unit tests must pass.
- Android test APK must compile (`assembleDebugAndroidTest`).
- Full app build must pass (`assembleDebug`).

Verify Navigation actions and destination swaps with NavController test utilities and device/emulator matrix as next expansion.
