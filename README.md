# SpendWise AI

SpendWise AI is an offline-first personal expense and investment tracker for Android, written in Java. It combines manual budgeting with optional on-device extraction of financial information from SMS, notifications, and voice input.

## Features

- **Expenses and budgets:** dashboard, category budgets, merchant analytics, calendar, recurring expenses, and financial goals.
- **Investments:** buckets, holdings, contributions, withdrawals, valuations, recurring investment schedules, and an ambiguous-item review queue.
- **Automation:** opt-in SMS import, optional broker notification ingestion, quick-add widgets, and reminders.
- **On-device intelligence:** expense and investment parsing, semantic bucket suggestions, spending insights, and an optional offline LLM flavor.
- **Privacy controls:** biometric/device-credential lock and user-initiated JSON, CSV, and PDF export through Android's Storage Access Framework.
- **Personalization:** dark mode, achievements, profile progress, and first-run onboarding.

See the [changelog](CHANGELOG.md) for release history. This is a personal finance tracking tool, not financial advice or a substitute for account statements.

## Screenshots

Screenshots will be added before the public showcase. Do not publish screenshots containing real names, balances, account details, SMS, or notifications.

## Requirements

- JDK 21 (CI uses Eclipse Temurin 21). Java 11 source/target compatibility is retained; JDK 21 is required to compile the Java 21-compatible MediaPipe GenAI dependency used by `offlineLlm`.
- Android SDK Platform 34 and Build Tools 34.0.0
- A recent Android Studio version compatible with Android Gradle Plugin 9.0.1 and Gradle 9.1
- Android 7.0 (API 24) or newer to run the app
- A device or emulator for instrumented tests

No account, backend server, or API key is required.

## Getting started

1. Clone the repository and open its root directory in Android Studio.
2. Install Android SDK 34 when prompted. Android Studio manages its own `local.properties`; this machine-specific file is intentionally not committed.
3. Let Gradle sync, then run the `app` configuration on a device or emulator.

For a command-line build, set `ANDROID_HOME` or create the ignored `local.properties` file with your SDK location:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Then run:

```bash
./gradlew :app:assembleStandardDebug
```

The APK is written under `app/build/outputs/apk/`. On Windows, use `gradlew.bat` instead of `./gradlew`.

## Build variants

| Flavor | Purpose | Build task |
| --- | --- | --- |
| `standard` | Standard parsing with the bundled text embedding model for semantic suggestions | `assembleStandardDebug` |
| `offlineLlm` | Adds optional local Gemma inference through MediaPipe | `assembleOfflineLlmDebug` |

The Gemma model is deliberately **not bundled**. To experiment with `offlineLlm`, separately obtain a compatible model and review its license. Place it at `app/src/offlineLlm/assets/models/spendwise_gemma3_1b.task`. Do not commit a model whose license does not permit redistribution. If the model is absent, semantic parsing is used as a fallback.

The bundled Universal Sentence Encoder model is approximately 28.7 MB and works without setup. Its provenance is recorded in [third-party notices](THIRD_PARTY_NOTICES.md). The optional model path and identifier can be overridden using the `OFFLINE_LLM_MODEL_PATH` and `OFFLINE_LLM_MODEL_ID` Gradle properties.

## Permissions and privacy

The app requests sensitive permissions only for their corresponding features:

- `READ_SMS`: optional local transaction and investment extraction
- Notification Listener access: optional local parsing of supported financial notifications
- `RECORD_AUDIO`: optional voice entry through Android's speech recognizer
- `POST_NOTIFICATIONS`: reminders and background-work notifications
- Biometric permissions: optional app lock

Room and SharedPreferences hold app data locally. Raw SMS and supported notification text may be stored as ingestion evidence. The app declares no Internet permission. However, **Android's speech recognizer may use the installed speech-recognition service and is not guaranteed to be fully offline**. Exported files and Android backups can leave the app sandbox.

Read the complete [privacy policy](PRIVACY.md) before distributing the app or screenshots.

## Tests

```bash
# JVM unit tests for both flavors
./gradlew :app:testDebugUnitTest

# Debug APKs and instrumented-test APK compilation
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest

# Android lint (both debug flavors)
./gradlew :app:lintStandardDebug :app:lintOfflineLlmDebug

# Requires a connected device or running emulator
./gradlew :app:connectedDebugAndroidTest
```

GitHub Actions runs the build, unit tests, instrumented-test APK compilation, and both debug-flavor lint checks on pushes to `master` and `staging`, and on pull requests. Release builds require your own signing configuration; never commit signing keys or passwords.

## Architecture

The app uses Android Views/XML, View Binding, Fragments, Navigation, ViewModel, LiveData, Room, WorkManager, TensorFlow Lite, and MediaPipe. UI and repository code is organized by feature under `app/src/main/java/com/spendwise/ai/`. Room schema JSON is checked into `app/schemas/`.

- [System architecture and file map](project-knowledge-base/architecture.md)
- [Project knowledge base](project-knowledge-base/README.md)
- [Build and release guide](memory-bank/build-and-release.md)
- [Testing guide](memory-bank/testing-and-quality.md)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Report security issues privately as described in [SECURITY.md](SECURITY.md); do not include actual financial messages in bug reports.

## License and third-party software

Original SpendWise AI source code and documentation are available under the [Apache License 2.0](LICENSE), Copyright 2026 Nagesh S. You may use, modify, and redistribute them under that license, including the required notices.

Bundled and downloaded third-party components retain their own licenses. See [NOTICE](NOTICE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The application license does not replace a model publisher's terms or grant rights to unrelated names and trademarks.
