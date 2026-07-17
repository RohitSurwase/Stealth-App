# 02 - Build System and Dependencies

This document provides a detailed overview of the build system, configurations, dependencies, and compilation commands for the **Stealth for Reddit** (`unReddit`) project.

## Build Setup Overview

The project is built using **Gradle Kotlin DSL (`.gradle.kts`)** and implements the `buildSrc` pattern to manage build logic and dependencies in a clean, type-safe, and centralized manner.

### Project Properties and Gradle Versions
- **Kotlin Gradle Plugin Version:** `1.7.20`
- **Android Gradle Plugin Version:** `7.3.1`
- **Gradle Wrapper Version:** Defined in `gradle/wrapper/gradle-wrapper.properties` (typically compatible with AGP 7.3.1).
- **Hilt Gradle Plugin Version:** `2.44.2`
- **Navigation Safe Args Plugin:** `2.5.3`

---

## Centralized Configurations (`buildSrc`)

The custom Kotlin project under `buildSrc` defines the key configurations, dependency versions, and argument providers.

### 1. SDK and Project Constants (`Config.kt`)
Found in `buildSrc/src/main/kotlin/Config.kt`:
- **Namespace:** `com.cosmos.unreddit`
- **Application ID:** `com.cosmos.unreddit`
- **Minimum SDK (minSdk):** `23` (Android 6.0 Marshmallow)
- **Compile SDK (compileSdk):** `33` (Android 13.0)
- **Target SDK (targetSdk):** `33` (Android 13.0)
- **Version Code:** `23`
- **Version Name:** `2.3.1`

### 2. Dependency Management (`Dependencies.kt`)
All dependencies are centralized under `buildSrc/src/main/kotlin/Dependencies.kt`. Some key libraries include:
- **Hilt (DI):** `2.44.2`
- **Jetpack Navigation:** `2.5.3`
- **Room Database:** `2.4.3`
- **Paging 3:** `3.1.1`
- **WorkManager:** `2.7.1`
- **ExoPlayer:** `2.18.1` (used for video/audio rendering)
- **Retrofit & Moshi:** `2.9.0` and `1.14.0` (REST client & serialization)
- **Coil:** `2.2.2` (image and GIF loading)
- **Jsoup:** `1.15.3` (HTML parsing/scraping)

### 3. Room Schema Provider (`RoomSchemaArgProvider.kt`)
Custom implementation to export Room schemas relative to the project directory for version history and testing.

---

## App Build Configuration (`app/build.gradle.kts`)

The app module leverages the following plugins and features:

### Plugins Applied
```kotlin
plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("androidx.navigation.safeargs.kotlin")
}
```

### Build Features Enabled
- **View Binding:** `true` (enables type-safe layout reference bindings)
- **Data Binding:** `true` (enables declarative UI bindings)

### Build Types
1. **Debug**
   - Application ID Suffix: `.dev` (allowing parallel installation with production build)
   - Debuggable: `true`
2. **Release**
   - Minify (Proguard/R8): `true`
   - Shrink Resources: `true`
   - Signing config: Configured dynamically via `keystore.properties` under the root project directory.

---

## Build, Build Verification and Running Commands

All commands below assume execution from the project root. On Windows systems, use `gradlew.bat` instead of `./gradlew`.

### Prerequisites
- **JDK 17** (or compatible version for AGP 7.3)
- **Android SDK** with Command Line Tools and Compile SDK 33 installed.
- Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` environment variables are properly set.

### Standard Build Tasks

| Command | Action |
|---|---|
| `./gradlew clean` | Cleans the build directories of all modules. |
| `./gradlew assembleDebug` | Compiles the debug variant and outputs `app-debug.apk`. |
| `./gradlew assembleRelease` | Compiles the obfuscated production variant. |
| `./gradlew installDebug` | Builds and installs the debug APK on a connected emulator/device. |
| `./gradlew lintDebug` | Runs Android Lint rules for static code analysis. |
| `./gradlew testDebugUnitTest` | Executes local unit tests (`test` directories). |
| `./gradlew connectedDebugAndroidTest` | Runs instrumented UI tests on a connected device (`androidTest` directories). |
| `./gradlew tasks` | Displays all registered Gradle tasks. |

---

## Build & Release Pipeline

### No CI/CD Pipeline in Repository

There is **no automated build pipeline** committed to this repository:

- No GitHub Actions (`.github/workflows/`), GitLab CI (`.gitlab-ci.yml`), CircleCI (`.circleci/`), Travis (`.travis.yml`), or Jenkinsfile exist.
- All builds are produced **locally / manually** via the Gradle wrapper using the commands above.
- The `release` build type requires a signing key. It reads `keystore.properties` (expected at the project root) for `keyAlias`, `keyPassword`, `storeFile`, and `storePassword`. If that file is absent, Gradle falls back to a placeholder `keystore.jks` name, which means a release build will fail without proper signing configuration. Debug builds use the default `androiddebugkey` and the `.dev` application-id suffix.

### `fastlane/` — Store Metadata Only (Not a Build Pipeline)

The repository contains a `fastlane/` directory, but it is **not** a CI/task pipeline. It holds **F-Droid / store publishing metadata** only:

```text
fastlane/
└── metadata/
    └── android/
        ├── en-US/
        │   ├── full_description.txt
        │   ├── short_description.txt
        │   ├── changelogs/        # 1.txt … 23.txt (one per versionCode)
        │   └── images/            # icon + 8 phone screenshots
        └── es-AR/
            ├── full_description.txt
            └── short_description.txt
```

There is **no `Fastfile` and no `Appfile`**, so `fastlane` cannot actually build, sign, or upload from this repo. The metadata is consumed by the **F-Droid** build server (the app is published at `f-droid.org/packages/com.cosmos.unreddit/`), which builds releases independently from its own pipeline.

### Release Workflow (Manual)

1. Bump `versionCode` / `versionName` in `buildSrc/src/main/kotlin/Config.kt`.
2. Add a matching changelog at `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
3. Provide `keystore.properties` at the repo root.
4. Run `./gradlew assembleRelease` → produces an obfuscated, shrink-wrapped APK/AAB.
5. Publish metadata to F-Droid (external pipeline handles the actual build & signing).

### List of Source Files Reviewed for This Section

- `app/build.gradle.kts` (signingConfigs, buildTypes)
- `buildSrc/src/main/kotlin/Config.kt` (versioning)
- Repository root (no CI config found)
- `fastlane/` (metadata only, no `Fastfile`/`Appfile`)
