Build and release baseline (v1.6.0):

- Release metadata:
  - `app/build.gradle` -> `versionCode 160`
  - `app/build.gradle` -> `versionName "1.6.0"`

- Required verification commands:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:assembleDebugAndroidTest`
  - `./gradlew :app:connectedDebugAndroidTest` (with emulator/device)
  - `./gradlew :app:lintDebug`
  - Flavor-specific (if validating each build target explicitly):
    - `./gradlew :app:testStandardDebugUnitTest :app:assembleStandardDebug`
    - `./gradlew :app:testOfflineLlmDebugUnitTest :app:assembleOfflineLlmDebug`

- CI automation:
  - `.github/workflows/android-ci.yml` enforces build + test APK + unit test gates on push/PR.

- Dependency/build stability notes:
  - `material-calendarview` pulls legacy support references; keep `android.enableJetifier=true` in `gradle.properties` so legacy support calls are rewritten to AndroidX at build time.
  - Keep `com.android.support` excluded from `material-calendarview` dependency declaration to avoid duplicate-class conflicts.
  - Java compile target is `VERSION_11` to avoid obsolete Java 8 source/target warnings.
  - Use `org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8` in `gradle.properties`.
  - **ProGuard/R8**: `minifyEnabled true` for release builds. Rules in `proguard-rules.pro`.
  - `offlineLlm` flavor pulls `com.google.mediapipe:tasks-genai` and auto-provisions Gemma model from bundled app assets on first use.
  - Bundle model file in flavor assets at `app/src/offlineLlm/assets/models/spendwise_gemma3_1b.task` for zero-setup user experience.
  - `OFFLINE_LLM_MODEL_PATH` can override target path (relative paths resolve under app files dir).

## Git Branching & Workflow
1. **Never commit directly to `master`**.
2. **Staging Branch**: All active development should happen on the `staging` branch. Periodically, `staging` is merged into `master`.
3. **Feature Branches**: For every feature, bugfix, or release, create a new branch (e.g., `feat/review-queue`) off of `staging`.
4. **Merge and Delete**: Once the feature branch is merged into `staging`, the source branch MUST be deleted to maintain a clean repository.
