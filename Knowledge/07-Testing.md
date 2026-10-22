# 07 - Testing

This document outlines the current test coverage and how to execute tests.

---

## Test Structure

Tests follow the standard Android project layout:

```text
app/src/
├── test/               # Local unit tests (JVM, no Android dependencies)
│   └── java/com/cosmos/unreddit/
│       └── ExampleUnitTest.kt
└── androidTest/        # Instrumented tests (run on device/emulator)
    └── java/com/cosmos/unreddit/
        ├── ExampleInstrumentedTest.kt
        └── util/
            └── DateUtilTest.kt
```

---

## Current Coverage

### Unit Tests (`test/`)
- **`ExampleUnitTest.kt`** — Placeholder test (`2 + 2 = 4`) with no real coverage.

### Instrumented Tests (`androidTest/`)
- **`ExampleInstrumentedTest.kt`** — Placeholder that verifies the app context is not null.
- **`DateUtilTest.kt`** — Tests the `DateUtil.getTimeDifference()` function for elapsed time formatting (now, minutes, hours, days, years).

---

## Test Dependencies

| Dependency | Scope | Version |
|---|---|---|
| JUnit 4 | `testImplementation` | 4.13.2 |
| AndroidX Test Runner | `androidTestImplementation` | 1.5.1 |
| AndroidX Test JUnit Ext | `androidTestImplementation` | 1.1.4 |
| Espresso Core | `androidTestImplementation` | 3.5.0 |

---

## Running Tests

From the project root:

```bash
# Run local unit tests
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Run a specific test class
./gradlew testDebugUnitTest --tests "com.cosmos.unreddit.ExampleUnitTest"
```

On Windows: use `gradlew.bat` instead of `./gradlew`.

---

## Notes

- Test coverage is currently minimal. Most business logic (repositories, ViewModels, scrapers) lacks dedicated tests.
- The `DateUtilTest.kt` in `androidTest` requires an Android context, making it an instrumented test despite testing pure utility logic (consider migrating to local unit test with mock context).
- Room, DataStore, and network layers are not tested. Consider adding unit tests with `MockWebServer` for APIs and an in-memory Room database for DAOs.

## Coverage Gaps for Recent Changes

None of the post-`ada4bd9` changes have tests:

| Change | Untested surface |
|---|---|
| `45fc13e` cookie hotfix | `RedditCookieInitializationInterceptor` (challenge parsing, `executeOperation`, failure fallback) and `RedditCookieJar` (`CookieManager` round-trip, `over18=1` injection) |
| `bcfe7c2` CMAF audio | `LinkUtil.getRedditSoundTrack()` with `CMAF_<n>` URLs; `isRedditSoundTrack()` |
| `dfa795f` download audio | `MediaDownloadWorker.muxAudioVideo()` and the API < 29 failure path |
| `b8474ec` shortcuts | Navigation from post cards/details to user/subreddit screens |

The cookie interceptor is a good first target: `executeOperation()` is a pure function, so it can be unit-tested without Android. The two-request flow can be tested with `MockWebServer` plus a fake cookie store, and `RedditCookieJar` can be tested with a mocked `CookieManager` or an instrumented test.
