# Contributing to SpendWise AI

Thank you for helping improve SpendWise AI. Please read the privacy policy before testing financial-message features.

## Development workflow

- `master` is the release branch.
- `staging` is the development branch.
- Start feature and bug-fix branches from `staging`.
- Open pull requests against `staging`; keep changes focused and small where practical.
- Do not commit directly to `master`.

## Local setup

Use JDK 21, Android SDK 34, and a recent compatible Android Studio. See [README.md](README.md) for build and flavor instructions. Do not add API keys or proprietary model weights merely to build the `standard` flavor.

## Before opening a pull request

Run:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:lintStandardDebug :app:lintOfflineLlmDebug
```

For behavior changes, add focused JVM tests or Android instrumentation tests. Run `./gradlew :app:connectedDebugAndroidTest` on an emulator/device for affected database, UI, notification, permission, and migration flows. State clearly in the pull request which checks were actually run.

If the Room schema changes:

1. Follow the existing migration and testing conventions.
2. Increment the database version and add an explicit migration.
3. Export and commit the updated JSON schema under `app/schemas/`.
4. Add or update migration tests and verify a realistic upgrade path.
5. Never enable destructive migration for release builds to avoid handling an upgrade properly.

See the [Java style guide](memory-bank/coding-standards-java.md) and [architecture guide](project-knowledge-base/architecture.md). Keep code production-quality, use established patterns, and explain non-obvious trade-offs in the pull request.

## Privacy and financial test data

Use synthetic SMS and notification fixtures. Never submit personal financial exports, account identifiers, real message histories, screenshots with private information, or credentials. When reporting a vulnerability, follow [SECURITY.md](SECURITY.md), not the public issue tracker.

Do not commit signing credentials, proprietary models, generated APKs, test databases, or compiled `.class` files. Review [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) before adding assets or dependencies, and retain compatible attribution and license notices.

## Licensing

By intentionally submitting a contribution, you agree that it is provided under the repository's [Apache License 2.0](LICENSE) terms. You must have the right to contribute the code, documentation, or assets. Do not copy code with an incompatible or unknown license.
