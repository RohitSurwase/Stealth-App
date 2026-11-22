# UI
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The UI layer is a single `MainActivity` hosting a `NavHostFragment`; every screen is a fragment or bottom sheet, following MVVM with View Binding + Data Binding. Fragments observe `ViewModel` flows; `BaseViewModel` supplies the shared profile-scoped flows (current profile, history, subscriptions, saved ids) so switching profiles invalidates every derived flow. Lists use Paging 3 `PagingDataAdapter`, navigation arguments come from the XML graphs via Safe Args, and the floating bottom nav shows only on the four tab destinations. Since v3.0.0 the layer gained Redlib support: a home app-bar instance label, an inline Cloudflare/Anubis bot-challenge overlay, HLS playback behind a challenge, and friendly search messages for instances without search.

## Key files
- `app/src/main/java/com/cosmos/unreddit/MainActivity.kt`: single activity; `setTheme(appTheme)` before `super.onCreate`; hosts NavHostFragment; builds floating bottom nav; left-handed gravity/corners; hides nav outside the four tabs; policy-disclaimer Snackbar.
- `app/src/main/java/com/cosmos/unreddit/ui/base/BaseFragment.kt`: `@AndroidEntryPoint`; `navigate(NavDirections)`; `openUser`/`openSubreddit` shortcut helpers.
- `app/src/main/java/com/cosmos/unreddit/ui/base/BaseViewModel.kt`: shared profile scope — `currentProfile`, `historyIds`, `subscriptions`, `savedPostIds` via `flatMapLatest`.
- `app/src/main/java/com/cosmos/unreddit/ui/postlist/PostListFragment.kt`: feed; shows the instance build label `#version : instance` in the app bar; hosts the inline challenge overlay.
- `app/src/main/java/com/cosmos/unreddit/ui/postlist/PostViewHolder.kt`: binds the user/subreddit shortcut buttons in feed cards.
- `app/src/main/java/com/cosmos/unreddit/ui/mediaviewer/MediaViewerFragment.kt`: fullscreen media; resolves IMAGE→Coil, VIDEO→ExoPlayer; launches the challenge activity for redlib HLS.
- `app/src/main/java/com/cosmos/unreddit/ui/mediaviewer/MediaViewerAdapter.kt`: ExoPlayer HLS + redlib bot-challenge detection/retry; Redgifs query params; v.reddit audio merge.
- `app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedditSourceDialogFragment.kt`: source picker; Teddit/Redlib instance spinner, verify + refresh instance buttons.
- `app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedlibVerificationFragment.kt`: inline WebView overlay solving the Cloudflare/Anubis challenge, sharing cookies with OkHttp.
- `app/src/main/java/com/cosmos/unreddit/ui/search/SearchPostFragment.kt`: search; shows a friendly message when the instance has no search support.

## Entry points and flows
Fragments are reached through the navigation graphs in `res/navigation/` (home, subscriptions, profile, settings, post, subreddit, user, viewer, search, backup). A fragment gets its `ViewModel` via `hiltNavGraphViewModels`, reads a `ViewModel` flow, and renders it; user actions call repository methods that read Room/DataStore and delegate remote work. Feeds are Paging 3 streams cached in memory. The home fragment updates its app-bar instance label from `RedlibInstanceInfo.versions` (per-host build cached from the scraped page footer). Redlib content blocked by a bot challenge opens either the inline `RedlibVerificationFragment` overlay (feed) or `RedlibVerificationActivity` (media); on completion it flushes the clearance cookie and retries.

## Gotchas
- The bottom nav is rebuilt on left-handed preference change; navigation state is held by `NavController`, not the views, so it survives the rebuild but not process death.
- `MainActivity.setTheme(...)` must run before `super.onCreate` or the theme attribute is ignored; edge-to-edge is on and fragments apply their own insets.
- The inline challenge overlay is added to the parent fragment manager (not the nav graph) and posts a `setFragmentResult` on "Done" so the parent flushes cookies and retries.
- The subreddit shortcut strips the `r/` prefix because `post.subreddit` stores it but the navigation argument expects the bare name.
- Coil caching in the media viewer is read-only; the full-resolution image is re-fetched if not cached.
- Policy-disclaimer date is hard-coded to `2023-06-10`.

## Covers
- app/src/main/java/com/cosmos/unreddit/ui
Listing: 16651f6b5949

## Files
- app/src/main/java/com/cosmos/unreddit/MainActivity.kt 5abbf4a689d6
- app/src/main/java/com/cosmos/unreddit/ui/base/BaseFragment.kt ffc07f0fba4f
- app/src/main/java/com/cosmos/unreddit/ui/base/BaseViewModel.kt 82c8fbd374f7
- app/src/main/java/com/cosmos/unreddit/ui/postlist/PostListFragment.kt 6e9790087fc7
- app/src/main/java/com/cosmos/unreddit/ui/postlist/PostViewHolder.kt 27a4359f043e
- app/src/main/java/com/cosmos/unreddit/ui/mediaviewer/MediaViewerFragment.kt 2316933242c1
- app/src/main/java/com/cosmos/unreddit/ui/mediaviewer/MediaViewerAdapter.kt ea6e42db3006
- app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedditSourceDialogFragment.kt 1c63c5f14afd
- app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedlibVerificationFragment.kt c0719390f35b
- app/src/main/java/com/cosmos/unreddit/ui/search/SearchPostFragment.kt f0e505c67734
