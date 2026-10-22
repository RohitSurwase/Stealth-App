# Stealth for Reddit — Knowledge Base

This directory contains a code-grounded analysis of the **Stealth for Reddit** (`unReddit`) repository.

- **Original generation:** commit `ada4bd9` ("Add AI generated Knowledge section").
- **This revision:** regenerated from HEAD `07577e5`, after the cookie hotfix `45fc13e` and the `3.0.0` updates. Claims were verified against the source; where the original docs were stale, they have been corrected.

---

## Documents

| # | Document | Description |
|---|---|---|
| 01 | [Project Overview](01-Project-Overview.md) | Purpose, features, architecture diagram, and the **exact end-to-end runtime flow** (including the cookie bootstrap) |
| 02 | [Build System](02-Build-System.md) | Gradle setup, `buildSrc`, dependency versions, signing (`keys/`), kapt/JDK flags, build commands |
| 03 | [Source Structure](03-Source-Structure.md) | Current package-by-package file inventory |
| 04 | [Data Layer](04-Data-Layer.md) | Room, DataStore, OkHttp clients, **cookie hotfix `45fc13e`**, sources, paging, downloads |
| 05 | [Dependency Injection](05-Dependency-Injection.md) | Hilt modules, qualifiers, exact client wiring |
| 06 | [UI Layer](06-UI-Layer.md) | Navigation, screens, ViewModels, media player, post shortcuts |
| 07 | [Testing](07-Testing.md) | Test structure, current coverage, how to run tests |
| 08 | [Working Principles, Evolution and Fixes](08-Working-Principles-Evolution-and-Fixes.md) | Core invariants, upstream/fork timeline, issue-by-issue fix register, open issues |

---

## Quick Reference

| Item | Value |
|---|---|
| Namespace | `com.cosmos.unreddit` |
| Release application ID | `com.rohitss.stealth` |
| Debug application ID | `com.rohitss.stealth.dev` |
| Version | `3.0.0` (`versionCode 30`) |
| Min SDK | 23 |
| Compile / target SDK | 33 |
| Architecture | Single-Activity MVVM + Repository + Hilt |
| Data | Room `reddit_db` v4 + DataStore `preferences` |
| Network | Reddit JSON / Teddit / old-Reddit HTML, selected at runtime |
| License | GPLv3 |

### Build Commands

```bash
./gradlew assembleDebug            # debug APK (com.rohitss.stealth.dev)
./gradlew installDebug             # install on a connected device
./gradlew assembleRelease          # requires keys/keystore.properties
./gradlew testDebugUnitTest        # JVM unit tests
./gradlew connectedDebugAndroidTest  # instrumented tests on a device
```

### What Changed Most Recently

| Commit | Subject | Documentation |
|---|---|---|
| `45fc13e` | HotFix for Reddit JSON API restriction | [01 §4](01-Project-Overview.md#4-cookie-bootstrap-hotfix-45fc13e), [04](04-Data-Layer.md#cookie-bootstrap-hotfix-45fc13e), [05](05-Dependency-Injection.md) |
| `bcfe7c2` | v.reddit CMAF audio support | [01 §10](01-Project-Overview.md#10-vreddit-audio), [04](04-Data-Layer.md) |
| `dfa795f` | Missing audio in downloaded v.reddit videos | [01 §10](01-Project-Overview.md#10-vreddit-audio), [04](04-Data-Layer.md), [06](06-UI-Layer.md) |
| `302922c` | Keystore config moved to `keys/` | [02](02-Build-System.md) |
| `1349c1f` | kapt JDK 16+ module access flags | [02](02-Build-System.md) |
| `e216456` | Version 3.0.0 / versionCode 30 | [02](02-Build-System.md) |
| `b8474ec` | User/subreddit shortcuts in post views | [03](03-Source-Structure.md), [06](06-UI-Layer.md) |
