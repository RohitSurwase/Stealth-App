# Gradle wrapper
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The Gradle wrapper pins the Gradle version used to build the project. `gradle/wrapper/gradle-wrapper.properties` requests Gradle 7.5.1 (`gradle-7.5.1-bin.zip`), dated Wed Mar 24 2021; `gradle/wrapper/gradle-wrapper.jar` is the wrapper bootstrap. This matches AGP 7.3.1 / Kotlin 1.7.20 used elsewhere in the build.

## Key files
- `gradle/wrapper/gradle-wrapper.properties`: `distributionUrl` = `gradle-7.5.1-bin.zip`; `distributionBase/Path` and `zipStoreBase/Path` are the standard `GRADLE_USER_HOME/wrapper/dists`.
- `gradle/wrapper/gradle-wrapper.jar`: wrapper launcher (downloaded by Gradle, committed here).

## Entry points and flows
`./gradlew` reads the properties, downloads the pinned distribution to `GRADLE_USER_HOME/wrapper/dists`, and runs it. Changing the version here changes every build's Gradle.

## Gotchas
- The wrapper version is old (7.5.1); a newer Gradle/AGP may require a bump plus the kapt `--add-exports/--add-opens` flags (see build.md).

## Covers
- gradle/wrapper
Listing: e2f530a8b43b

## Files
- gradle/wrapper/gradle-wrapper.properties 1912447f3c54
- gradle/wrapper/gradle-wrapper.jar 60415cbb9861
