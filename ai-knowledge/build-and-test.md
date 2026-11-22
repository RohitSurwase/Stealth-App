# Build and test
AI-generated, verified by running the commands below on 2026-11-22. Check before relying on it.

## Requirements
- **JDK 17** — required by AGP 7.3.1. The Kotlin compiler in `buildSrc` rejects anything newer (see Gotchas).
- **Android SDK** with **Compile SDK 33** (`platforms/android-33`) and matching Build Tools.
- `ANDROID_HOME` set, or a `local.properties` with `sdk.dir=` (present in this checkout).
- `keys/keystore.properties` + release keystore **only** for `assembleRelease` (debug needs none).

## Commands
Run from the repository root (use `gradlew.bat` on Windows). All stay inside this repo.

- `./gradlew assembleDebug`: builds `app-debug.apk` (applicationId `com.rohitss.stealth.dev`, `.dev` suffix). **Could not run** — see Gotchas.
- `./gradlew testDebugUnitTest`: runs JVM unit tests in `app/src/test` (currently `ExampleUnitTest.kt` + `RedlibScraperTest.kt`). **Could not run** — see Gotchas.
- `./gradlew lintDebug`: runs Android Lint on the debug variant. **Could not run** — see Gotchas.
- `./gradlew assembleRelease`: builds the minified/shrunk release APK/AAB; requires `keys/keystore.properties`. **Could not run** — see Gotchas.
- `./gradlew connectedDebugAndroidTest`: runs instrumented tests in `app/src/androidTest` (`DateUtilTest.kt`, `ExampleInstrumentedTest.kt`) on a connected device/emulator. **Could not run** — needs a device AND JDK 17.
- `./gradlew clean` / `./gradlew tasks` / `./gradlew installDebug` / `:app:kaptDebugKotlin`: clean build dir / list tasks / install debug APK / compile kapt + Room schemas without packaging. **Could not run** — see Gotchas.

**Result of the one attempt made:** `./gradlew help` downloaded Gradle 7.5.1 fine, then **BUILD FAILED in ~7s** at `:buildSrc:compileKotlin` with `exception: java.lang.IllegalArgumentException: 25.0.4` — the bundled Kotlin compiler refuses the installed JDK 25. No JDK 17 is available on this machine (system Java is 25; Android Studio's bundled JBR is also 25).

## Gotchas
- **JDK version is the hard blocker here.** This repo targets AGP 7.3.1 / Gradle 7.5.1 / Kotlin 1.7.20, which do not run on JDK 25. Install a JDK 17 and point Gradle at it (`org.gradle.java.home` in `gradle.properties`, or `JAVA_HOME`), then re-run. Do **not** upgrade Gradle/AGP to "fix" it — that changes the whole toolchain.
- **kapt needs `--add-exports/--add-opens`.** AGP 7.3.1 / Kotlin 1.7.20 kapt fails on JDK 16+ without access to internal `javac` packages (KT-45545). Already configured in two places: `kapt { javacOptions { ... } }` in `app/build.gradle.kts` and the same flags + `-Xmx2048m -Xss4m` in `gradle.properties`. Keep them when using JDK 17.
- **Release signing is local-only.** `app/build.gradle.kts` reads `../keys/keystore.properties` (relative to the `app/` module → repo-root `keys/`). `.gitignore` excludes `keystore.properties`, `*.jks`, `*.keystore`, so a fresh checkout has no `keys/` and `assembleRelease` cannot sign. (This checkout already has `keys/keystore.properties` + `MyKeystore.keystore` locally.)
- **First build is slow.** Gradle 7.5.1 distribution + all dependencies download on first run; keep a generous timeout.
- **Instrumented tests need a device/emulator** (`connectedDebugAndroidTest`); unit tests (`testDebugUnitTest`) do not.

## Files
- app/build.gradle.kts bb4133aa9b17
- buildSrc/src/main/kotlin/Config.kt 4cf1a474a1f6
- buildSrc/src/main/kotlin/Dependencies.kt 0e8efd0e0ce0
- buildSrc/src/main/kotlin/RoomSchemaArgProvider.kt 235803acfdb0
- gradle.properties 91827aa447c1
- gradle/wrapper/gradle-wrapper.properties 1912447f3c54
- settings.gradle.kts e2b1f43980b5

## Folded-in Knowledge (02-Build-System, 07-Testing) — verified against current source at HEAD `c0aad3f` (v3.0.5)

### Cross-check: Knowledge was generated at v3.0.0 (`07577e5`); current is v3.0.5 (`c0aad3f`).
- **STALE — versions bumped:** `Config.kt` now declares `versionCode = 35`, `versionName = "3.0.5"` (doc said `30` / `3.0.0`). Everything else in the toolchain table is unchanged.
- **MATCHES (verified in current files):** Gradle wrapper `7.5.1`; AGP `7.3.1`; Kotlin `1.7.20`; Hilt Gradle plugin `2.44.2`; Navigation Safe Args `2.5.3` (plugin id `androidx.navigation.safeargs.kotlin`); namespace `com.cosmos.unreddit`; release applicationId `com.rohitss.stealth`; debug `.dev` suffix; minSdk `23`, compileSdk/targetSdk `33`; JVM 1.8 target; ViewBinding + DataBinding `true`; opt-in `kotlinx.coroutines.ExperimentalCoroutinesApi`.
- **MATCHES — library versions** in `Dependencies.kt`: AndroidX core-ktx `1.9.0`, appcompat `1.5.1`, constraintlayout `2.1.4`, recyclerview `1.2.1`, fragment `1.5.4`, lifecycle `2.5.1`, coordinatorlayout `1.2.0`, viewpager2 `1.0.0`, preference `1.2.0`; Navigation `2.5.3`; Room `2.4.3`, DataStore `1.0.0`; Paging 3 `3.1.1`, WorkManager `2.7.1`; Material `1.7.0`, browser `1.4.0`, core-splashscreen `1.0.0`, TouchImageView `3.0.3`, drawer `1.0.3`; ExoPlayer `2.18.1`, Coil `2.2.2` (+ `coil-gif`); Retrofit `2.9.0`, Moshi `1.14.0`, Okio `3.2.0`, Jsoup `1.15.3`; Hilt `1.0.0`; coroutines `1.4.1` (transitive only).
- **MATCHES — kapt JDK 16+ flags** (`--add-exports/--add-opens` for `com.sun.tools.javac.main/.tree/.util/.code`), in both `app/build.gradle.kts` and `gradle.properties`. (Doc wrote the module prefix as bare `com.sun.tools.javac.main`; current uses the `jdk.compiler/` prefix — functionally identical.)
- **MATCHES — Room schema** output to `app/schemas/` via `RoomSchemaArgProvider` (`app/build.gradle.kts:56-60`); schema dir `app/schemas/com.cosmos.unreddit.data.local.RedditDatabase/` present.
- **MATCHES — build types:** debug `isDebuggable`, release `isMinifyEnabled` + `isShrinkResources` + `proguard-android-optimize.txt` + `proguard-rules.pro`.
- **MATCHES — test deps:** JUnit `4.13.2`, AndroidX Test `1.1.4`, runner `1.5.1`, Espresso `3.5.0`.
- **NEW since v3.0.0 (not in the Knowledge docs):** `app/src/test/java/com/cosmos/unreddit/RedlibScraperTest.kt` — 11 unit tests covering the Redlib scraper parsers (`RedlibPost/Comment/Subreddit/SubredditSearch/UserScraper`) and anti-bot challenge detection (`RedlibChallenge`, `RedlibBlockedException`). This is real coverage, not a placeholder.
- **Local-only change:** `keys/` (keystore + keystore.properties) now exists in this checkout but is gitignored; the doc correctly notes it must be created locally.
- **No CI/CD** in the tree; `fastlane/` is F-Droid metadata only (no `Fastfile`/`Appfile`).

### Test suite layout (current)
- `app/src/test/java/com/cosmos/unreddit/` — `ExampleUnitTest.kt` (placeholder `2+2=4`), `RedlibScraperTest.kt` (real).
- `app/src/androidTest/java/com/cosmos/unreddit/` — `ExampleInstrumentedTest.kt`, `util/DateUtilTest.kt` (tests `DateUtil.getTimeDifference()`; instrumented despite testing pure logic).
- Coverage is still thin: repositories/ViewModels/scrapers (other than the Redlib parser) are untested.
