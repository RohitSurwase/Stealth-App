# 02 - Build System and Dependencies

This document reflects the current Gradle configuration at HEAD (`07577e5`), including the `3.0.0` bump and the post-`ada4bd9` build changes (`302922c`, `1349c1f`, `e216456`).

---

## Build Setup Overview

- **Gradle wrapper:** `7.5.1` (`gradle/wrapper/gradle-wrapper.properties`).
- **Gradle Kotlin DSL** with the `buildSrc` pattern for type-safe, centralized configuration.
- **Root project name:** `unReddit` (`settings.gradle.kts`).
- **Modules:** one module, `:app`.
- **JDK:** JDK 17 is required for AGP 7.3.1; kapt needs the module-access flags described below.

### Toolchain versions

| Component | Version | Source |
|---|---|---|
| Android Gradle Plugin | `7.3.1` | `buildSrc/src/main/kotlin/Dependencies.kt` |
| Kotlin | `1.7.20` | same |
| Hilt Gradle Plugin | `2.44.2` | same |
| Navigation Safe Args | `2.5.3` | same |
| Gradle wrapper | `7.5.1` | `gradle/wrapper/gradle-wrapper.properties` |

---

## Centralized Configuration (`buildSrc`)

### `Config.kt`

`buildSrc/src/main/kotlin/Config.kt`:

```kotlin
object Config {
    const val namespace = "com.cosmos.unreddit"
    const val applicationId = "com.rohitss.stealth"

    const val minSdk = 23
    const val compileSdk = 33
    const val targetSdk = 33

    const val versionCode = 30
    const val versionName = "3.0.0"
}
```

| Setting | Value |
|---|---|
| Namespace | `com.cosmos.unreddit` |
| Release application ID | `com.rohitss.stealth` |
| Debug application ID | `com.rohitss.stealth.dev` (`.dev` suffix) |
| minSdk | 23 |
| compileSdk / targetSdk | 33 |
| versionCode / versionName | 30 / `3.0.0` |

### `Dependencies.kt`

All library versions are centralized in `buildSrc/src/main/kotlin/Dependencies.kt`:

| Area | Libraries / versions |
|---|---|
| DI | Hilt `2.44.2`, AndroidX Hilt `1.0.0` |
| AndroidX | core-ktx `1.9.0`, appcompat `1.5.1`, constraintlayout `2.1.4`, recyclerview `1.2.1`, fragment `1.5.4`, lifecycle `2.5.1`, coordinatorlayout `1.2.0`, viewpager2 `1.0.0`, preference `1.2.0` |
| Navigation | `2.5.3` |
| Persistence | Room `2.4.3`, DataStore `1.0.0` |
| Paging / Work | Paging 3 `3.1.1`, WorkManager `2.7.1` |
| UI | Material `1.7.0`, browser `1.4.0`, core-splashscreen `1.0.0`, TouchImageView `3.0.3`, drawer `1.0.3` |
| Media | ExoPlayer `2.18.1`, Coil `2.2.2` (+ `coil-gif`) |
| Network | Retrofit `2.9.0`, Moshi `1.14.0`, Okio `3.2.0`, Jsoup `1.15.3` |
| Tests | JUnit `4.13.2`, AndroidX Test `1.1.4`, runner `1.5.1`, Espresso `3.5.0` |

`kotlinx-coroutines` is declared as `1.4.1` but only used transitively through the AndroidX/Kotlin dependencies.

### Room schema output

`RoomSchemaArgProvider.kt` exports Room schemas to `app/schemas` via the kapt argument provider (`app/build.gradle.kts:56-60`). The generated schema for the current database is under `app/schemas/com.cosmos.unreddit.data.local.RedditDatabase/`.

---

## App Build Configuration (`app/build.gradle.kts`)

### Plugins

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

- **View Binding:** `true`.
- **Data Binding:** `true`.
- **Java/Kotlin target:** JVM 1.8 (`compileOptions`, `kotlinOptions`).
- **Opt-in:** `kotlinx.coroutines.ExperimentalCoroutinesApi`.

### kapt on JDK 16+ (`1349c1f`)

AGP 7.3.1 / Kotlin 1.7.20 kapt fails on JDK 16+ without access to internal `javac` packages (KT-45545). The project applies two layers of flags:

1. `kapt { javacOptions { ... } }` in `app/build.gradle.kts:22-36`:
   - `-Xss4m`
   - `-J--add-exports` / `-J--add-opens` for `com.sun.tools.javac.main`, `.tree`, `.util`, `.code`
2. The same flags plus `-Xmx2048m -Xss4m` in `gradle.properties:10` for the Gradle daemon.

If kapt is ever replaced with KSP, these flags can be removed.

### Build types

| Build type | Configuration |
|---|---|
| `debug` | `applicationIdSuffix = ".dev"`, `isDebuggable = true` |
| `release` | `isMinifyEnabled = true`, `isShrinkResources = true`, ProGuard `proguard-android-optimize.txt` + `proguard-rules.pro`, release signing config |

### Signing (`302922c`)

The release signing config reads `../keys/keystore.properties` **relative to the `app/` module**, i.e. **`keys/keystore.properties` at the repo root** (`app/build.gradle.kts:13`):

```kotlin
val keystorePropertiesFile = file("../keys/keystore.properties")
```

- If the file is absent, Gradle sets `storeFile = "keystore.jks"` (`:19`); the property values stay empty, so a release build cannot be signed.
- The configured `storeFile` path is resolved with `file(...)` from the `app/` module.
- `.gitignore` ignores `keystore.properties`, `*.jks` and `*.keystore`; the `keys/` directory does not exist in a fresh checkout and must be created locally.
- Debug builds use the default Android debug keystore and do not need this file.

---

## Build and Run Commands

Run from the repository root. On Windows use `gradlew.bat`.

### Prerequisites

- **JDK 17** (or a JDK compatible with AGP 7.3.1). The kapt flags assume JDK 16+.
- **Android SDK** with **Compile SDK 33** and Build Tools installed.
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` set, or a valid `local.properties`.
- For **F-Droid-style reproducible builds**, no `keys/keystore.properties` is needed for `assembleDebug`.

### Tasks

| Command | Action |
|---|---|
| `./gradlew clean` | Delete all module build directories |
| `./gradlew assembleDebug` | Build `app-debug.apk` with application ID `com.rohitss.stealth.dev` |
| `./gradlew assembleRelease` | Build the minified/shrunk release APK/AAB; requires `keys/keystore.properties` |
| `./gradlew installDebug` | Install the debug APK on a connected device/emulator |
| `./gradlew lintDebug` | Run Android Lint on the debug variant |
| `./gradlew testDebugUnitTest` | Run JVM unit tests (`app/src/test`) |
| `./gradlew connectedDebugAndroidTest` | Run instrumented tests on a device (`app/src/androidTest`) |
| `./gradlew tasks` | List all available tasks |

### Useful variants

```bash
# Build and install only the debug variant
./gradlew :app:assembleDebug

# Compile kapt/Room schemas without packaging
./gradlew :app:kaptDebugKotlin

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.cosmos.unreddit.ExampleUnitTest"

# Run a single instrumented test class
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.cosmos.unreddit.util.DateUtilTest
```

---

## Build and Release Pipeline

### No CI/CD in the repository

There is **no** GitHub Actions, GitLab CI, CircleCI, Travis or Jenkins configuration in the tree. All builds are local/manual through the Gradle wrapper.

### `fastlane/` is metadata only

The repository ships F-Droid/store metadata, not a Fastlane build pipeline:

```text
fastlane/metadata/android/
├── en-US/
│   ├── full_description.txt
│   ├── short_description.txt
│   ├── changelogs/1.txt … 23.txt
│   └── images/
│       ├── icon.png
│       └── phoneScreenshots/01_home.png … 08_preferences.png
└── es-AR/
    ├── full_description.txt
    └── short_description.txt
```

There is **no `Fastfile` and no `Appfile`**, so Fastlane cannot build, sign or upload from this repository. F-Droid consumes the metadata and builds from source independently.

### Manual release checklist

1. Bump `versionCode` and `versionName` in `buildSrc/src/main/kotlin/Config.kt`.
2. Add a matching changelog at `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
3. Create `keys/keystore.properties`:
   ```properties
   keyAlias=...
   keyPassword=...
   storeFile=/absolute/path/to/release.jks
   storePassword=...
   ```
4. Ensure the keystore file exists and `storeFile` is reachable from the `app/` module.
5. Run `./gradlew assembleRelease`.
6. Publish the APK/AAB and update the Fastlane metadata consumed by F-Droid.

### Version history note

The current tree is `3.0.0` (`versionCode 30`). The newest changelog file in `fastlane/` is still `23.txt`; the `e216456` version bump did not add changelogs `24.txt`–`30.txt`.
