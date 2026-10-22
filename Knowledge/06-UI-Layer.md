# 06 - UI Layer

This document describes the current UI layer at HEAD (`07577e5`), including the `b8474ec` post shortcut buttons and the `dfa795f` media behavior.

---

## Architecture

- **Single Activity:** `MainActivity` hosts a `NavHostFragment`; all screens are fragments or bottom sheets.
- **MVVM:** fragments observe `ViewModel` flows; `BaseViewModel` provides account/profile-scoped flows.
- **View Binding + Data Binding:** both enabled (`app/build.gradle.kts:88-91`).
- **Paging 3:** `PagingDataAdapter` subclasses in list screens.
- **Safe Args:** all navigation arguments are generated from the XML graphs.

---

## Navigation

Graphs live in `app/src/main/res/navigation/`:

| File | Contents |
|---|---|
| `navigation_graph.xml` | Root graph, includes sub-graphs and global actions |
| `home.xml` | `PostListFragment` feed |
| `subscriptions.xml` | `SubscriptionsFragment` |
| `profile.xml` | `ProfileFragment` / `ProfileSavedFragment` |
| `settings.xml` | `PreferencesFragment` |
| `post.xml` | `PostDetailsFragment` |
| `subreddit.xml` | `SubredditFragment` / `SubredditSearchFragment` |
| `user.xml` | `UserFragment` / `UserPostFragment` / `UserCommentFragment` |
| `viewer.xml` | `MediaViewerFragment` |
| `search.xml` | `SearchFragment` and search sub-screens |
| `backup.xml` | Backup/restore screens |

### Bottom navigation destinations

`MainActivity.onDestinationChanged()` (`MainActivity.kt:211-227`) shows the floating bottom navigation **only** for:

- `postListFragment` (home)
- `subscriptionsFragment`
- `profileFragment`
- `preferencesFragment`

All other destinations (post details, subreddit, user, search, media viewer, backup) hide it. Re-selecting `home` scrolls the feed to the top (`:104-111`).

### Left-handed mode

`initBottomNavigationView(leftHandedMode)` (`:115-174`):

- Gravity: `BOTTOM or START` when left-handed, otherwise `BOTTOM or END`.
- Shape: rounded corners on the inner side; cut corners on the screen edge.
- Behavior: `HideBottomViewBehavior(leftHandedMode)` slides the bar in/out with the content scroll.
- Bottom margin includes the system navigation-bar inset via a one-shot window-insets listener.
- The bar is recreated when the preference changes at runtime.

### Policy disclaimer

- Preference key: `policy_disclaimer_shown`.
- If not shown and the fixed date `2023-06-10` has passed (`:239-242`), `MainActivity` waits 5 seconds (`:244`) and shows an indefinite Snackbar.
- The Snackbar action opens `PolicyDisclaimerDialogFragment`.

### Theming

- `UnredditApplication.appTheme` chooses `R.style.AppTheme` or `R.style.AmoledAppTheme`.
- AMOLED is implemented by forcing dark mode and using a pure-black theme.
- `MainActivity.setTheme(unredditApplication.appTheme)` must run before `super.onCreate` (`:55`), otherwise the theme attribute is ignored.
- Edge-to-edge is enabled with `WindowCompat.setDecorFitsSystemWindows(window, false)`; fragments apply their own insets.

---

## Screens (22 packages)

| Package | Classes | Purpose |
|---|---|---|
| `about/` | `AboutFragment`, `CreditAdapter` | App info, license, credits |
| `backup/` | `BackupFragment`, `BackupChoiceFragment`, `BackupLoadingFragment`, `BackupLocationFragment`, `BackupOperationFragment`, `BackupViewModel` | Export/import |
| `base/` | `BaseFragment`, `BaseViewModel` | Navigation helpers and profile-scoped flows |
| `commentmenu/` | `CommentMenuFragment` | Comment actions |
| `common/` | `ListFragment`, `PagingListFragment`, `FullscreenBottomSheetFragment`, widgets, spans | Shared UI |
| `linkmenu/` | `LinkMenuFragment` | Link actions |
| `loadstate/` | `NetworkLoadStateAdapter`, `ResourceStateAdapter` | Loading/error/retry |
| `mediaviewer/` | `MediaViewerFragment`, `MediaViewerViewModel`, `MediaViewerAdapter`, `MediaViewerThumbnailAdapter` | Fullscreen media |
| `policydisclaimer/` | `PolicyDisclaimerDialogFragment`, `PolicyDisclaimerViewModel` | First-run disclaimer |
| `postdetails/` | `PostDetailsFragment`, `PostDetailsViewModel`, `PostAdapter`, `CommentAdapter` | Post + comments |
| `postlist/` | `PostListFragment`, `PostListViewModel`, `PostListAdapter`, `PostViewHolder`, `ProfileAdapter` | Feed |
| `postmenu/` | `PostMenuFragment` | Post actions |
| `preferences/` | `PreferencesFragment`, `PreferencesViewModel` | Settings |
| `privacyenhancer/` | `PrivacyEnhancerFragment`, `PrivacyEnhancerViewModel`, `PrivacyEnhancerInstanceAdapter`, `PrivacyEnhancerServiceDialog` | Redirect rules |
| `profile/` | `ProfileFragment`, `ProfileSavedFragment`, `ProfileViewModel`, `ProfileSavedAdapter` | Profile + saved |
| `profilemanager/` | `ProfileManagerDialogFragment`, `ProfileManagerViewModel`, `ProfileManagerAdapter` | Profile CRUD |
| `redditsource/` | `RedditSourceDialogFragment` | Source picker |
| `search/` | `SearchFragment`, `SearchPostFragment`, `SearchSubredditFragment`, `SearchUserFragment`, `SearchViewModel`, `SearchSubredditAdapter`, `SearchUserAdapter` | Search |
| `sort/` | `SortFragment` | Sort picker |
| `subreddit/` | `SubredditFragment`, `SubredditSearchFragment`, `SubredditViewModel`, `SubredditSearchViewModel` | Subreddit browsing |
| `subscriptions/` | `SubscriptionsFragment`, `SubscriptionsViewModel`, `SubscriptionsAdapter` | Subscription management |
| `user/` | `UserFragment`, `UserPostFragment`, `UserCommentFragment`, `UserViewModel`, `UserCommentsAdapter` | User content |

---

## Post Shortcut Buttons (`b8474ec`)

The commit adds user and subreddit shortcut buttons to post surfaces:

- `ui/base/BaseFragment.kt` — exposes `openUser(...)` / `openSubreddit(...)` navigation helpers.
- `ui/postdetails/PostAdapter.kt` — binds the shortcuts in the post details header.
- `ui/postlist/PostListAdapter.kt` — wires click listeners.
- `ui/postlist/PostViewHolder.kt` — connects the shortcut views.
- `ui/profile/ProfileSavedAdapter.kt` — same affordances for saved posts.
- `res/layout/include_post_metrics.xml` — layout additions.
- `res/drawable/ic_profile_post.xml` — new icon.
- `res/values/strings.xml` — new content descriptions/labels.

Behaviorally:

- Tapping the user shortcut calls `openUser(post.author)` → `UserFragment`.
- Tapping the subreddit shortcut calls `openSubreddit(post.subreddit.removePrefix("r/"))` → `SubredditFragment`. The `r/` prefix is stripped because `post.subreddit` stores the prefixed form but the navigation argument expects the bare name.
- The buttons (`buttonUser`, `buttonSubreddit`) live in `include_post_metrics.xml` and appear in feed cards, post details and saved posts.

---

## Key ViewModels

| ViewModel | Scope | Exposes |
|---|---|---|
| `UiViewModel` | Activity | `navigationVisibility`, `leftHandedMode`, `policyDisclaimerShown` |
| `PostListViewModel` | Fragment | `postDataFlow: Flow<PagingData<PostEntity>>`, `fetchData`, `lastRefresh`, profile switching |
| `PostDetailsViewModel` | Fragment | Post + comment listings, more-children expansion, save toggles |
| `SubredditViewModel` | Fragment | Paging feed, `SubredditEntity` info, subscription toggle |
| `SearchViewModel` | Fragment | Post/user/subreddit search paging |
| `MediaViewerViewModel` | Fragment | `Resource<List<GalleryMedia>>`, selected page, mute preference |
| `PreferencesViewModel` | Fragment | Preference reads/writes and dialogs |
| `BackupViewModel` | Fragment | Backup/restore state machine |
| `ProfileManagerViewModel` | Dialog | Profile CRUD and active-profile switch |
| `PrivacyEnhancerViewModel` | Fragment | Redirect rules and service modes |

`BaseViewModel` (`ui/base/BaseViewModel.kt`) is the shared profile scope:

```text
currentProfile = getCurrentProfile() → getProfile(id)
historyIds     = currentProfile.flatMapLatest { getHistoryIds(it.id) }
subscriptions  = currentProfile.flatMapLatest { getSubscriptions(it.id) }
savedPostIds   = currentProfile.flatMapLatest { getSavedPostIds(it.id) }
```

Switching profiles therefore invalidates every derived flow automatically.

---

## Media Player

### Rendering pipeline

`MediaViewerFragment` receives either a single link + `MediaType` or a gallery list. `MediaViewerViewModel.retrieveMedia()` resolves it into `List<GalleryMedia>`:

- `GalleryMedia.Type.IMAGE` → Coil.
- `GalleryMedia.Type.VIDEO` → ExoPlayer.
- `GalleryMedia.Type.AUDIO` exists in the model but the viewer only creates IMAGE/VIDEO items.

`MediaViewerAdapter`:

- Image: Coil `ImageRequest` with `Scale.FILL`, `Precision.AUTOMATIC`, memory/disk `READ_ONLY`, crossfade, retry on error.
- Video: `SimpleExoPlayer` built from `ExoPlayerHelper.defaultMediaSourceFactory`.
- `ExoPlayerHelper`:
  - `DefaultHttpDataSource` with `LinkUtil.USER_AGENT` (desktop Firefox UA).
  - `setAllowCrossProtocolRedirects(true)`.
  - `SimpleCache` with `LeastRecentlyUsedCacheEvictor(100 MB)`.
  - `MediaViewerAdapter` forwards Redgifs query parameters via `ExoPlayerHelper.setRequestProperties()` (`MediaViewerAdapter.kt:188-193`).

### v.reddit audio (`dfa795f`, `bcfe7c2`)

- `MediaViewerViewModel` sets `sound = LinkUtil.getRedditSoundTrack(url)` for `MediaType.REDDIT_VIDEO` (`:100-103`).
- The adapter merges the video and audio media sources with `MergingMediaSource` (`:205`).
- If the audio track returns HTTP 403/404 and the URL contains `_AUDIO_128`, it retries video-only (`:248-258`).
- The mute control is shown only when an audio track is detected (`:273-291`).

### Gestures and fullscreen

- `FullscreenBottomSheetFragment` handles the fullscreen bottom-sheet presentation and system bars.
- `MediaViewerFragment` toggles controls on tap, supports multi-touch zoom via `TouchImageView`, and disallows `ViewPager2` interception during pinch/pan (`MediaViewerAdapter.ImageViewHolder.onTouch`).
- Thumbnails at the bottom use `MediaViewerThumbnailAdapter`; the page counter is `current/total`.

---

## Favorites, Saving and History

- Tapping save calls `BaseViewModel.toggleSavePost()` / `toggleSaveComment()`; the repository inserts or deletes the full snapshot scoped to the active profile.
- Opening a post calls `insertPostInHistory(postId)`; history is stored per profile.
- Saved/seen flags are applied during paging by `PostUtil.filterPosts()`, so a page refresh reflects changes.

---

## Loading and Error States

- Paging screens use `NetworkLoadStateAdapter` for footer retry/loading; failures come from `LoadResult.Error`.
- `Resource<T>` (`Loading` / `Success` / `Error`) drives the media viewer and subreddit info.
- `ResourceStateAdapter`/`InfoBarView` render inline retry bars (e.g. media load failure, Reddit video audio failure).
- Pull-to-refresh is implemented by `PullToRefreshLayout` / `PullToRefreshView` in feed screens.

---

## UI Caveats

- The bottom navigation is fully rebuilt when the left-handed preference changes; state is remembered by the `NavController`, not the views.
- `MainActivity` uses an activity-scoped `UiViewModel`; navigation visibility state survives configuration changes but not process death.
- Coil disk/memory caching is read-only in the media viewer; the full-resolution image is re-fetched if not already cached.
- The policy disclaimer date is hard-coded to `2023-06-10`.
