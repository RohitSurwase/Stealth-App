# 03 - Source Structure

This document maps the current `app/src/main/java/com/cosmos/unreddit` tree (HEAD `07577e5`). Class names and package contents are taken directly from the files, and the interceptor/cookie entries reflect the `45fc13e` hotfix.

---

## High-Level Source Tree

```text
com.cosmos.unreddit/
│
├── UnredditApplication.kt              # Hilt Application — theme, Coil, WorkManager, crash handler
├── MainActivity.kt                     # Single Activity — NavHost, bottom nav, left-handed mode
├── UiViewModel.kt                      # Activity-scoped ViewModel — nav visibility, left-hand, disclaimer
│
├── data/                               # Data layer
│   ├── local/                          #   Room DB, DAOs, mappers, backup
│   ├── model/                          #   Domain, DB, preference and backup models
│   ├── receiver/                       #   BroadcastReceiver for download retry/cancel
│   ├── remote/                         #   Retrofit APIs, scrapers, interceptors, PagingSources
│   ├── repository/                     #   Repository singletons
│   └── worker/                         #   WorkManager workers
│
├── di/                                 # Hilt modules (5)
├── ui/                                 # Feature-based UI packages (22)
└── util/                               # Utilities and extensions (21 files + 18 extensions)
```

---

## 1. Root Files

| File | Responsibility |
|---|---|
| `UnredditApplication.kt` | `@HiltAndroidApp`; reads `night_mode` from DataStore with `runBlocking`, applies Light/Dark/Amoled themes, installs `FileUncaughtExceptionHandler`, supplies the Coil `ImageLoader` and WorkManager `Configuration` with `HiltWorkerFactory`. |
| `MainActivity.kt` | Single activity; `setTheme(appTheme)` before `super.onCreate`; hosts `NavHostFragment`; builds the floating bottom navigation; flips gravity/corners for left-handed mode; hides the nav outside the four tab destinations; shows the policy disclaimer Snackbar. |
| `UiViewModel.kt` | Exposes `navigationVisibility`, `leftHandedMode` and `policyDisclaimerShown` as flows derived from preferences and navigation state. |

---

## 2. `data/` — Data Layer

### `data/local/` — Room and offline storage

| File | Responsibility |
|---|---|
| `RedditDatabase.kt` | Room DB `reddit_db`, **version 4**, `exportSchema = true`. Entities: `Subscription`, `History`, `Profile`, `PostEntity`, `Comment.CommentEntity`, `Redirect`. Migrations `1→2`, `2→3`, `3→4`; first-creation callback inserts profile `id=1`, name `"Stealth"`. |
| `Converters.kt` | Room type converters. |

**`local/dao/`**

| DAO | Operations |
|---|---|
| `BaseDao.kt` | Generic insert/upsert/delete helpers |
| `SubscriptionDao.kt` | CRUD subscriptions by `profile_id` |
| `HistoryDao.kt` | Insert/read/delete post view history by profile |
| `ProfileDao.kt` | Create/delete/get/update profiles |
| `PostDao.kt` | Save/unsave/get saved posts by profile |
| `CommentDao.kt` | Save/unsave/get saved comments by profile |
| `RedirectDao.kt` | Redirect rules (`service` primary key) |

**`local/mapper/`** — entity/domain mapping:

`Mapper.kt`, `PostMapper2.kt`, `CommentMapper2.kt`, `SubredditMapper2.kt`, `UserMapper2.kt`, `SavedMapper2.kt`, `ProfileMapper.kt`, `SubscriptionMapper.kt`, `BackupPostMapper.kt`, `BackupCommentMapper.kt`.

**`local/backup/`**

| File | Purpose |
|---|---|
| `BackupManager.kt` | Abstract backup interface |
| `RedditBackupManager.kt` | Reddit-format backup |
| `StealthBackupManager.kt` | Full Room backup in Stealth's own format |

### `data/model/` — Domain models

Root models (23 files):

`Award.kt`, `BackupTypeItem.kt`, `Block.kt`, `Comment.kt`, `CommentType.kt`, `CreditItem.kt`, `Data.kt`, `Flair.kt`, `GalleryMedia.kt`, `HtmlBlock.kt`, `MediaType.kt`, `PosterType.kt`, `PostType.kt`, `ProfileItem.kt`, `RedditText.kt`, `Resource.kt`, `SavedItem.kt`, `ServiceExternal.kt`, `ServiceRedirect.kt`, `Sort.kt`, `Sorting.kt`, `TimeSorting.kt`, `User.kt`.

**`model/db/`**

| File | Role |
|---|---|
| `Subscription.kt` | `subscription` entity: name, time, icon, `profile_id` |
| `History.kt` | `history` entity: `post_id`, time, `profile_id` |
| `Profile.kt` | `profile` entity: id, name |
| `ProfileWithDetails.kt` | Projection with subscription/post counts |
| `PostEntity.kt` | `post` entity: full saved-post snapshot |
| `Comment.CommentEntity` | `comment` entity: full saved-comment snapshot |
| `Redirect.kt` | `redirect` entity: pattern, redirect host, service, `RedirectMode` |
| `SubredditEntity.kt` | **Not a Room entity**; UI model produced by `SubredditMapper2` |

**`model/preferences/`**

| File | DataStore keys |
|---|---|
| `UiPreferences.kt` | `night_mode`, `left_handed_mode` (`NightMode`: LIGHT, DARK, AMOLED `434`, FOLLOW_SYSTEM) |
| `DataPreferences.kt` | `reddit_source`, `reddit_source_instance`, `privacy_enhancer` (`RedditSource`: REDDIT=0, TEDDIT=1, REDDIT_SCRAP=2) |
| `ContentPreferences.kt` | `show_nsfw`, `show_nsfw_preview`, `show_spoiler_preview` |
| `MediaPreferences.kt` | `mute_video` |
| `ProfilePreferences.kt` | `current_profile` |
| `PolicyDisclaimerPreferences.kt` | `policy_disclaimer_shown` |

**`model/backup/`**

`BackupType.kt` (`REDDIT`, `STEALTH`), `Post.kt`, `Comment.kt`, `Profile.kt`, `Subscription.kt`.

### `data/receiver/`

| File | Purpose |
|---|---|
| `DownloadManagerReceiver.kt` | Handles `<applicationId>.ACTION_DOWNLOAD_RETRY` and `.ACTION_DOWNLOAD_CANCEL` for media-download notifications |

### `data/remote/` — Network layer

**`remote/` root**

| File | Purpose |
|---|---|
| `RawJsonInterceptor.kt` | Appends `raw_json=1` to every request |
| `TargetRedditInterceptor.kt` | Appends `target=reddit` for Teddit requests |

**`remote/api/reddit/`**

| File / package | Purpose |
|---|---|
| `RedditApi.kt` | Retrofit interface for `www.reddit.com` (`BASE_URL`) and `old.reddit.com` (`BASE_URL_OLD`) |
| `TedditApi.kt` | Retrofit interface for a Teddit instance (`?api` endpoints, default `https://teddit.net/`) |
| `JsonInterceptor.kt` | Appends `.json` as a path segment |
| `RedditCookieInitializationInterceptor.kt` | **Added by `45fc13e`**; detects a missing `token_v2` cookie and solves Reddit's JS challenge to obtain it |
| `RedditCookieJar.kt` | **Rewritten by `45fc13e`**; reads/writes Android `CookieManager`, adds `over18=1` for reddit hosts |
| `SortingConverterFactory.kt` | Converts `Sort` / `TimeSorting` to their `type` strings |
| `source/` | `BaseRedditSource`, `CurrentSource` (strategy wrapper), `RedditSource`, `TedditSource`, `RedditScrapingSource` |
| `model/` | 29 JSON/domain models: `Listing`, `ListingData`, `PostData`, `CommentData`, `Child`, `Media`, `MediaMetadata`, `MediaPreview`, `RedditVideoPreview`, `GalleryData`, `GalleryDataItem`, `GalleryImage`, `GalleryItem`, `Crosspost`, `Awarding`, `RichText`, `AboutData`, `AboutUserData`, `MoreChildren`, `MoreData`, `TedditUser`, `Variants`, `Tagline`, `Subreddit`, `Embed`, `Image`, `ImageSource`, `Data`, `JsonMore` |
| `adapter/` | `RepliesAdapter`, `EditedAdapter`, `MediaMetadataAdapter`, `NullToEmptyStringAdapter` |
| `scraper/` | `RedditScraper` (base), `PostScraper`, `CommentScraper`, `SubredditScraper`, `SubredditSearchScraper`, `UserScaper`, `Over18Scraper` |

**Other APIs**

| Package | Files |
|---|---|
| `api/imgur/` | `ImgurApi.kt`, `adapter/AlbumData.kt`, `model/Album.kt`, `model/Data.kt`, `model/Image.kt` |
| `api/gfycat/` | `GfycatApi.kt`, `model/ContentUrls.kt`, `Gif.kt`, `GfyItem.kt`, `Item.kt` |
| `api/redgifs/` | `RedgifsApi.kt`, `RedgifsToken.kt`, `model/Gif.kt`, `Token.kt`, `Item.kt`, `Urls.kt` |
| `api/streamable/` | `StreamableApi.kt`, `model/Files.kt`, `VideoFile.kt`, `Video.kt` |
| `remote/scraper/` | `Scraper.kt` — abstract Jsoup base class (timeout, IO dispatcher, selectors) |

**`remote/datasource/`** — Paging 3 sources:

| DataSource | Feeds |
|---|---|
| `SmartPostListDataSource.kt` | Home / multi-subreddit feed; **splits >100 subs into parallel chunks** |
| `PostListDataSource.kt` | Generic post list paging |
| `UserPostsDataSource.kt` | User submitted posts |
| `CommentsDataSource.kt` | User comments |
| `SearchPostDataSource.kt` | Global post search |
| `SearchUserDataSource.kt` | User search |
| `SearchSubredditDataSource.kt` | Subreddit search |
| `SubredditSearchPostDataSource.kt` | In-subreddit search |

### `data/repository/`

| Repository | Role |
|---|---|
| `PostListRepository.kt` | Pager creation, subscriptions, history, profiles, saved posts/comments, search; page size 25 |
| `PreferencesRepository.kt` | Typed access to every DataStore key |
| `BackupRepository.kt` | Export/import backups |
| `ImgurRepository.kt` | Imgur album/image metadata |
| `GfycatRepository.kt` | Gfycat metadata |
| `RedgifsRepository.kt` | Redgifs metadata (token + GIF URL) |
| `StreamableRepository.kt` | Streamable metadata |
| `AssetsRepository.kt` | Reads bundled assets (licenses) |

### `data/worker/`

| File | Purpose |
|---|---|
| `MediaDownloadWorker.kt` | `@HiltWorker`; downloads media, splits v.reddit video/audio, muxes on API 29+, posts progress/retry/cancel notifications |

---

## 3. `di/` — Dependency Injection

All modules install into `SingletonComponent`:

| Module | Provides |
|---|---|
| `NetworkModule.kt` | 3 Moshi instances (`@RedditMoshi`, `@BasicMoshi`, `@ImgurMoshi`), 4 OkHttp clients (`@RedditOkHttp`, `@TedditOkHttp`, `@RedditScrapOkHttp`, `@GenericOkHttp`), `RedditApi` × 2 (`@RedditOfficial`, `@RedditScrap`), `TedditApi`, `ImgurApi`, `StreamableApi`, `GfycatApi`, `RedgifsApi` |
| `DatabaseModule.kt` | `RedditDatabase` singleton with all migrations |
| `PreferencesModule.kt` | `DataStore<Preferences>` named `"preferences"` |
| `DispatchersModule.kt` | `@DefaultDispatcher`, `@IoDispatcher`, `@MainDispatcher`, `@MainImmediateDispatcher` |
| `CoroutinesScopesModule.kt` | `@ApplicationScope` = `CoroutineScope(SupervisorJob() + Dispatchers.Default)` |

`NetworkModule` contains 9 qualifiers total (`RedditMoshi`, `BasicMoshi`, `ImgurMoshi`, `RedditOkHttp`, `TedditOkHttp`, `GenericOkHttp`, `RedditScrapOkHttp`, `RedditOfficial`, `RedditScrap`).

---

## 4. `ui/` — Feature Packages (22)

| Package | Main classes | Purpose |
|---|---|---|
| `about/` | `AboutFragment`, `CreditAdapter` | App info, license, credits |
| `backup/` | `BackupFragment`, `BackupChoiceFragment`, `BackupLoadingFragment`, `BackupLocationFragment`, `BackupOperationFragment`, `BackupViewModel` | Export/import Room backup |
| `base/` | `BaseFragment`, `BaseViewModel` | Shared navigation helpers and profile-scoped flows |
| `commentmenu/` | `CommentMenuFragment` | Comment long-press actions |
| `common/` | `FragmentAdapter`, `ListFragment`, `PagingListFragment`, `FullscreenBottomSheetFragment`, span classes, widgets (`CradleView`, `RedditTextView`, `PullToRefreshLayout`, …) | Shared UI plumbing |
| `linkmenu/` | `LinkMenuFragment` | Link long-press actions |
| `loadstate/` | `NetworkLoadStateAdapter`, `ResourceStateAdapter` | Paging load/error/empty indicators |
| `mediaviewer/` | `MediaViewerFragment`, `MediaViewerViewModel`, `MediaViewerAdapter`, `MediaViewerThumbnailAdapter` | Fullscreen image/video viewer |
| `policydisclaimer/` | `PolicyDisclaimerDialogFragment`, `PolicyDisclaimerViewModel` | First-run privacy disclaimer |
| `postdetails/` | `PostDetailsFragment`, `PostDetailsViewModel`, `PostAdapter`, `CommentAdapter` | Post body + threaded comments |
| `postlist/` | `PostListFragment`, `PostListViewModel`, `PostListAdapter`, `PostViewHolder`, `ProfileAdapter` | Feed / profile list |
| `postmenu/` | `PostMenuFragment` | Post long-press actions |
| `preferences/` | `PreferencesFragment`, `PreferencesViewModel` | Settings screen |
| `privacyenhancer/` | `PrivacyEnhancerFragment`, `PrivacyEnhancerViewModel`, `PrivacyEnhancerInstanceAdapter`, `PrivacyEnhancerServiceDialog` | Redirect rules and per-service mode |
| `profile/` | `ProfileFragment`, `ProfileSavedFragment`, `ProfileViewModel`, `ProfileSavedAdapter` | Profile details + saved items |
| `profilemanager/` | `ProfileManagerDialogFragment`, `ProfileManagerViewModel`, `ProfileManagerAdapter` | Multi-profile CRUD |
| `redditsource/` | `RedditSourceDialogFragment` | Choose Reddit / Teddit / scraped |
| `search/` | `SearchFragment`, `SearchPostFragment`, `SearchSubredditFragment`, `SearchUserFragment`, `SearchViewModel`, `SearchSubredditAdapter`, `SearchUserAdapter` | Search posts/users/subreddits |
| `sort/` | `SortFragment` | Sort picker |
| `subreddit/` | `SubredditFragment`, `SubredditSearchFragment`, `SubredditViewModel`, `SubredditSearchViewModel` | Subreddit browsing + in-sub search |
| `subscriptions/` | `SubscriptionsFragment`, `SubscriptionsViewModel`, `SubscriptionsAdapter` | Subscription management |
| `user/` | `UserFragment`, `UserPostFragment`, `UserCommentFragment`, `UserViewModel`, `UserCommentsAdapter` | User posts/comments |

### Post shortcut buttons (`b8474ec`)

`b8474ec` added user and subreddit shortcut buttons to post views:

- `ui/base/BaseFragment.kt` — `openUser` / `openSubreddit` navigation helpers.
- `ui/postdetails/PostAdapter.kt` — shortcut binding in post details.
- `ui/postlist/PostListAdapter.kt` — click listeners for the buttons.
- `ui/postlist/PostViewHolder.kt` — attaches the shortcut listeners.
- `ui/profile/ProfileSavedAdapter.kt` — same behavior for saved posts.
- Layout `res/layout/include_post_metrics.xml` and drawable `ic_profile_post.xml`; strings added to `res/values/strings.xml`.

---

## 5. `util/` — Utilities

**Root files (21):**

| File | Purpose |
|---|---|
| `LinkHandler.kt` | Classifies links and routes them to navigation, media viewer or browser |
| `LinkValidator.kt` | Adds `https://` if missing and validates with OkHttp |
| `LinkRedirector.kt` | Privacy Enhancer redirect rules (regex host → host) |
| `LinkUtil.kt` | Media URL helpers, `REDDIT_VIDEO_REGEX`, `_AUDIO_128` soundtrack URL |
| `RedditUri.kt` | Parses Reddit deep links |
| `ExoPlayerHelper.kt` | ExoPlayer data source, 100 MB disk cache, request properties |
| `HtmlParser.kt` | Parses Reddit HTML into `RedditText` |
| `RedditTagHandler.kt` | Custom HTML tag handling |
| `CommentUtil.kt` | Comment formatting |
| `PostUtil.kt` | Post mapping/filtering and post-type detection |
| `RedditUtil.kt` | Joins subreddit lists with `+` |
| `SearchUtil.kt` | Search query sanitization |
| `DateUtil.kt` | Relative time formatting, file-date formatting |
| `FileUncaughtExceptionHandler.kt` | Writes crash logs to `externalFilesDir/uncaught` and exits |
| `HideBottomViewBehavior.kt` | `CoordinatorLayout.Behavior` that slides the bottom nav |
| `IntentUtil.kt` | Share/open intents and `PendingIntent` flags |
| `DrawerContent.kt` | Bottom drawer state |
| `ClickableMovementMethod.kt` | Link movement that respects `ClickableSpan` |
| `BlurTransformation.kt` | Coil NSFW blur |
| `PagerHelper.kt` | ViewPager2 helpers |
| `Util.kt` | Misc helpers |

**`util/extension/` (18):**

`ActivityExt.kt`, `ApplicationExt.kt`, `BindingExt.kt`, `BundleExt.kt`, `DataStoreExt.kt`, `DateExt.kt`, `Ext.kt`, `FragmentExt.kt`, `IterableExt.kt`, `NotificationExt.kt`, `NumberExt.kt`, `PermissionExt.kt`, `StringExt.kt`, `UriExt.kt`, `ViewExt.kt`, `ViewModelExt.kt`, `ViewPagerExt.kt`, `WorkManagerExt.kt`.

---

## 6. Tests

```text
app/src/test/java/com/cosmos/unreddit/ExampleUnitTest.kt
app/src/androidTest/java/com/cosmos/unreddit/ExampleInstrumentedTest.kt
app/src/androidTest/java/com/cosmos/unreddit/util/DateUtilTest.kt
```

See [07-Testing.md](07-Testing.md).
