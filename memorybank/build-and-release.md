Build and release baseline (v1.2.2):

- Release metadata:
  - `app/build.gradle` -> `versionCode 122`
  - `app/build.gradle` -> `versionName "1.2.2"`

- Required verification commands:
  - `./gradlew :app:testDebugUnitTest`
  - `./gradlew :app:assembleDebug`
  - `./gradlew :app:assembleDebugAndroidTest`
  - `./gradlew :app:connectedDebugAndroidTest` (with emulator/device)

- CI automation:
  - `.github/workflows/android-ci.yml` enforces build + test APK + unit test gates on push/PR.

- Dependency/build stability notes:
  - `material-calendarview` pulls legacy support libraries transitively; exclude `com.android.support` in Gradle dependency declaration and rely on AndroidX.
  - Java compile target is `VERSION_11` to avoid obsolete Java 8 source/target warnings.
  - Use `org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=768m -Dfile.encoding=UTF-8` in `gradle.properties`.
