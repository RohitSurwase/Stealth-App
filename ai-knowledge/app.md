# App entry points
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The three files directly in the `com.cosmos.unreddit` package root are the app's entry points: `UnredditApplication` (Hilt init, theme, Coil, WorkManager), `MainActivity` (the single activity), and `UiViewModel` (activity-scoped UI state). They tie the DI layer to the UI layer.

## Key files
- `app/src/main/java/com/cosmos/unreddit/UnredditApplication.kt`: `@HiltAndroidApp`; reads `night_mode` from DataStore and applies the theme; supplies the Coil `ImageLoader` (uses the `@TedditOkHttp` browser-like client so redlib-proxied media gets the right headers/cookies) and the `HiltWorkerFactory`.
- `app/src/main/java/com/cosmos/unreddit/MainActivity.kt`: `@AndroidEntryPoint`; `setTheme(appTheme)` before `super.onCreate`; hosts the `NavHostFragment`; builds the floating bottom nav; flips gravity/corners for left-handed mode; hides nav outside the four tab destinations; shows the policy-disclaimer Snackbar.
- `app/src/main/java/com/cosmos/unreddit/UiViewModel.kt`: `@HiltViewModel`; exposes `navigationVisibility`, `leftHandedMode`, `policyDisclaimerShown` as flows from preferences plus the mutable navigation-visibility flag.

## Entry points and flows
`UnredditApplication.onCreate` applies the theme from DataStore and installs the uncaught-exception handler. `MainActivity.onCreate` re-applies the theme, sets up the nav host + bottom nav, and collects the `UiViewModel` flows to show/hide the nav, rebuild it on left-handed change, and show the policy disclaimer after a 5 s delay.

## Gotchas
- `setTheme(...)` must run before `super.onCreate` or the theme attribute is ignored; edge-to-edge is on and fragments apply their own insets.
- The application injects the `@TedditOkHttp` client for Coil, so image requests carry the browser UA + `show_nsfw`/clearance cookies that the API client uses.
- `POLICY_DISCLAIMER_DATE` is computed as `Calendar.getInstance().set(1900 + 123, 5, 10)` — month is 0-based, so this is `2023-06-10`.
- Navigation visibility state lives in `UiViewModel` (activity-scoped), so it survives configuration changes but not process death.

## Covers
- app/src/main/java/com/cosmos/unreddit/*
Listing: 08edf129dcac

## Files
- app/src/main/java/com/cosmos/unreddit/MainActivity.kt 5abbf4a689d6
- app/src/main/java/com/cosmos/unreddit/UiViewModel.kt 9956c64d9631
- app/src/main/java/com/cosmos/unreddit/UnredditApplication.kt 7afb07446ff4
