# Stack and versions

`app/build.gradle`, `build.gradle`, `settings.gradle`, and `gradle/wrapper/gradle-wrapper.properties` are authoritative for exact dependency and toolchain pins.

## Toolchain

- JDK 21 for builds/tests; Java 11 source/target compatibility. `tasks-genai` Java bytecode requires JDK 21 or newer.
- Android Gradle Plugin 9.0.1; Gradle 9.1.
- `compileSdk`/`targetSdk` 34; `minSdk` 24.
- Android SDK Build Tools 34.0.0 in GitHub Actions; local Gradle may install other compatible versions.

## Application

- Java + Android Views/XML + Material Components + View Binding.
- Room 2.6.1, LiveData/ViewModel, Jetpack Navigation 2.7.7 with XML arguments, WorkManager 2.9.0.
- MPAndroidChart `v3.1.0`; MaterialCalendarView `2.0.1`; ThreeTenABP `1.4.4`.
- Android vector drawable resources in `res/drawable/` for application icons; no Font Awesome font is bundled.
- LiteRT `1.4.2` retains the existing CPU `org.tensorflow.lite.Interpreter` API and supports 16 KB page sizes from min SDK 21. MediaPipe Tasks Text `0.10.28` provides aligned native libraries for semantic features on all four existing ABIs.
- MediaPipe Tasks GenAI `0.10.27` only in `offlineLlm`; the Gemma model file is supplied separately.
- JUnit 4, Mockito, AndroidX Test/Espresso.

Do not change dependency pins as part of documentation maintenance or speculative updates; test toolchain changes independently.
