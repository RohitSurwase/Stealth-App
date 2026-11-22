# Instrumented tests
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The `app/src/androidTest` tree holds the instrumented tests (run on a device/emulator). It has two files: `ExampleInstrumentedTest.kt`, a placeholder that asserts the app `Context` is non-null, and `util/DateUtilTest.kt`, which tests `DateUtil.getTimeDifference()` elapsed-time formatting (now, minutes, hours, days, years).

## Key files
- `app/src/androidTest/java/com/cosmos/unreddit/util/DateUtilTest.kt`: asserts `DateUtil.getTimeDifference()` output across the now/minutes/hours/days/years buckets.
- `app/src/androidTest/java/com/cosmos/unreddit/ExampleInstrumentedTest.kt`: placeholder verifying the app context is not null.

## Entry points and flows
Instrumented tests run under `androidx.test.runner.AndroidJUnitRunner` (set as `testInstrumentationRunner` in `app/build.gradle.kts`). `DateUtilTest` calls the pure `DateUtil` function directly; `ExampleInstrumentedTest` uses the injected `Application` context.

## Gotchas
- Coverage is essentially placeholder; no UI or Room/Network instrumented tests exist.
- No CI runs these; they must be launched manually on an emulator/device.

## Covers
- app/src/androidTest/java/com/cosmos/unreddit
Listing: af166909971f

## Files
- app/src/androidTest/java/com/cosmos/unreddit/ExampleInstrumentedTest.kt 2a1bf36b8440
- app/src/androidTest/java/com/cosmos/unreddit/util/DateUtilTest.kt 13cdf05911b4
