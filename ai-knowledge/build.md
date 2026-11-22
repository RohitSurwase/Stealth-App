# Build
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
Build configuration lives in `buildSrc` (Gradle Kotlin DSL) and the repo-root Gradle files. `Config.kt` and `Dependencies.kt` centralize application coordinates and library versions; `RoomSchemaArgProvider.kt` feeds the Room schema location to kapt. The root `build.gradle.kts` declares the classpath plugins and repositories; `settings.gradle.kts` defines the single `:app` module; `gradle.properties` sets JVM args (including the kapt JDK 16+ flags) and AndroidX.

## Key files
- `buildSrc/src/main/kotlin/Config.kt`: `object Config` — namespace `com.cosmos.unreddit`, applicationId `com.rohitss.stealth`, minSdk 23, compile/targetSdk 33, versionCode 35, versionName `3.0.5`.
- `buildSrc/src/main/kotlin/Dependencies.kt`: `object Dependencies.Versions` — all library versions (AGP 7.3.1, Kotlin 1.7.20, Hilt gradle plugin 2.44.2, Room 2.4.3, Paging 3.1.1, ExoPlayer 2.18.1, Coil 2.2.2, Retrofit 2.9.0, Moshi 1.14.0, …).
- `buildSrc/src/main/kotlin/RoomSchemaArgProvider.kt`: `CommandLineArgumentProvider` passing `-Aroom.schemaLocation=…` so schemas are written to `app/schemas`.
- `buildSrc/build.gradle.kts`: applies the `kotlin-dsl` plugin and `mavenCentral`.
- `build.gradle.kts`: `buildscript` classpath for AGP/Kotlin/Hilt/safe-args; `allprojects` repos (google, mavenCentral, jitpack); a `clean` task.
- `settings.gradle.kts`: `include(":app")`, `rootProject.name = "unReddit"`.
- `gradle.properties`: daemon `-Xmx2048m` plus `--add-exports/--add-opens` for kapt on JDK 16+ (KT-45545); `android.useAndroidX=true`, `android.enableJetifier=true`.

## Entry points and flows
Gradle loads `buildSrc` first, making `Config`/`Dependencies` available to every `.gradle.kts`. The root `build.gradle.kts` puts the build tools on the classpath; `app/build.gradle.kts` (not here) applies the Android/Kotlin/Hilt/safe-args plugins and reads `Config`/`Dependencies`. Release signing reads `../keys/keystore.properties` resolved from the `app/` module, i.e. `keys/keystore.properties` at the repo root.

## Gotchas
- The doc's `versionCode 30 / 3.0.0` is stale — `Config.kt` is now `35 / 3.0.5`.
- kapt needs the `--add-exports/--add-opens` flags both in `app/build.gradle.kts` (javacOptions) and in `gradle.properties` (daemon); without them AGP 7.3.1/Kotlin 1.7.20 kapt fails on JDK 16+.
- The release keystore is resolved relative to the `app/` module, so `storeFile` must be reachable from `app/`; a fresh checkout has no `keys/` directory and cannot sign a release.
- There is no CI/CD and no `Fastfile`/`Appfile`; `fastlane/` is F-Droid metadata only.

## Covers
- buildSrc
- ./*
- app/*
Listing: 6f20ad449447

## Files
- buildSrc/src/main/kotlin/Config.kt 4cf1a474a1f6
- buildSrc/src/main/kotlin/Dependencies.kt 0e8efd0e0ce0
- buildSrc/src/main/kotlin/RoomSchemaArgProvider.kt 235803acfdb0
- buildSrc/build.gradle.kts 38379f0b2619
- build.gradle.kts 4bfa4fab0db4
- settings.gradle.kts e2b1f43980b5
- gradle.properties 91827aa447c1
