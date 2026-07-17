# 03 - Source Structure

This document details the code package structure in `app/src/main/java/com/cosmos/unreddit`, mapping every package and class to its responsibility.

---

## High-Level Source Tree

```text
com.cosmos.unreddit/
│
├── UnredditApplication.kt          # Hilt Application — Coil, WorkManager, theme init
├── MainActivity.kt                 # Single Activity — NavHost + bottom nav + left-handed mode
├── UiViewModel.kt                  # Activity-scoped ViewModel — nav visibility, left-hand, disclaimer
│
├── data/                           # Data layer
│   ├── local/                      #   Room DB, DAOs, mappers, backup
│   ├── model/                      #   Domain / DB / preference / backup models
│   ├── receiver/                   #   BroadcastReceiver (download retry/cancel)
│   ├── remote/                     #   Retrofit APIs, scrapers, OkHttp interceptors, PagingSources
│   ├── repository/                 #   Repository singletons
│   └── worker/                     #   WorkManager workers
│
├── di/                             # Hilt modules (5 modules)
│
├── ui/                             # Feature-based UI packages (22 packages)
│
└── util/                           # Utilities & extensions (22 files/dirs)
```

---

## 1. `data/` — Data Layer

### `data/local/` — Room Database & Offline Storage

| File | Responsibility |
|---|---|
| `RedditDatabase.kt` | Room DB (v4, export schema). Entities: Subscription, History, Profile, PostEntity, CommentEntity, Redirect. Contains 3 migrations (1→2, 2→3, 3→4) + default-profile callback. |
| `Converters.kt` | Room type converters (lists, enums, timestamps). |

**`local/dao/`** — Data Access Objects:

| DAO | Key Operations |
|---|---|
| `BaseDao.kt` | Generic upsert / delete / insert helpers |
| `SubscriptionDao.kt` | CRUD subscriptions by profile |
| `HistoryDao.kt` | Insert / get / delete post view history |
| `ProfileDao.kt` | Create / delete / get / update profiles |
| `PostDao.kt` | Save / unsave / get saved posts |
| `CommentDao.kt` | Save / unsave / get saved comments |
| `RedirectDao.kt` | URL redirect rules (service→target) |

**`local/mapper/`** — Entity ↔ Domain mappers:

| Mapper | Maps |
|---|---|
| `Mapper.kt` | Base mapper interface |
| `PostMapper2.kt` | PostChild/PostEntity ↔ domain PostType |
| `CommentMapper2.kt` | CommentChild/CommentEntity ↔ domain Comment |
| `SubredditMapper2.kt` | Subreddit data → domain models |
| `UserMapper2.kt` | User data → domain models |
| `SavedMapper2.kt` | Saved posts/comments → UI models |
| `ProfileMapper.kt` | Profile DB ↔ domain |
| `SubscriptionMapper.kt` | Subscription DB ↔ domain |
| `BackupPostMapper.kt` | Post ↔ backup model |
| `BackupCommentMapper.kt` | Comment ↔ backup model |

**`local/backup/`** — Export/Import:

| File | Purpose |
|---|---|
| `BackupManager.kt` | Abstract backup interface |
| `RedditBackupManager.kt` | SharedPreferences-based backup |
| `StealthBackupManager.kt` | Full Room backup (proprietary format) |

---

### `data/model/` — Data Models

**Root models** (26 files in `model/` root):

| File | Description |
|---|---|
| `PostType.kt` | Domain model for Reddit posts |
| `Comment.kt` | Domain model for comments (includes nested `CommentEntity`) |
| `MediaType.kt` | Enum: IMAGE, VIDEO, GIF, AUDIO, LINK, SELF, GALLERY, etc. |
| `RedditText.kt` | Rich text representation (HTML + awards) |
| `Resource.kt` | Generic sealed class: Success / Error / Loading |
| `Sort.kt` / `Sorting.kt` / `TimeSorting.kt` | Sort enums and wrappers |
| `Award.kt` / `Flair.kt` | Award / image-flair data |
| `User.kt` / `Block.kt` / `SavedItem.kt` | User, block-list, saved-item models |
| `Data.kt` | Generic wrapper for Reddit API data nodes |
| `HtmlBlock.kt` / `CreditItem.kt` | HTML block segments, credit attributions |
| `BackupTypeItem.kt` | Backup-type selection item |
| `PosterType.kt` | Enum: USER, MOD, ADMIN, OP, etc. |
| `GalleryMedia.kt` | Gallery media parcelable |
| `ServiceExternal.kt` / `ServiceRedirect.kt` | External/redirect service models |
| `CommentType.kt` | Comment kind enum |

**`model/db/`** — Room entity models:

| Entity | Fields |
|---|---|
| `Subscription.kt` | name, time, icon, profileId |
| `History.kt` | postId, time, profileId |
| `Profile.kt` | id, name |
| `ProfileWithDetails.kt` | Profile + sub count, post count |
| `PostEntity.kt` | Full post data for saved posts |
| `Comment.CommentEntity.kt` | Full comment data for saved comments |
| `Redirect.kt` | pattern, redirect, service, mode |
| `SubredditEntity.kt` | Cached subreddit metadata |

**`model/preferences/`** — DataStore preference models:

| File | Settings Group |
|---|---|
| `UiPreferences.kt` | Theme (Light/Dark/Amoled), left-handed mode |
| `DataPreferences.kt` | Reddit source, instance URL, backup enable |
| `ContentPreferences.kt` | NSFW toggle, blur NSFW |
| `MediaPreferences.kt` | Autoplay, HD toggle |
| `ProfilePreferences.kt` | Active profile ID |
| `PolicyDisclaimerPreferences.kt` | Disclaimer shown flag |

**`model/backup/`** — Backup data models:

| File | Purpose |
|---|---|
| `BackupType.kt` | Enum: REDDIT / STEALTH |
| `Post.kt` / `Comment.kt` | Backup-serializable post/comment |
| `Profile.kt` / `Subscription.kt` | Backup-serializable profile/subscription |

---

### `data/receiver/` — Broadcast Receivers

| File | Purpose |
|---|---|
| `DownloadManagerReceiver.kt` | Handles `ACTION_DOWNLOAD_RETRY` / `ACTION_DOWNLOAD_CANCEL` intents for failed media downloads |

---

### `data/remote/` — Network Layer

**`remote/` root:**

| File | Purpose |
|---|---|
| `RawJsonInterceptor.kt` | OkHttp interceptor that forces `raw_json=1` query param |
| `TargetRedditInterceptor.kt` | Rewrites request URL target (for Teddit) |

**`remote/api/`** — Retrofit service interfaces:

```
api/
├── reddit/          # Reddit JSON API + Teddit API + scrapers + model JSON adapters
├── gfycat/          # Gfycat video metadata API
├── imgur/           # Imgur album/image API (with AlbumDataAdapter)
├── redgifs/         # Redgifs NSFW video API
└── streamable/      # Streamable video API
```

**`remote/api/reddit/`** — Reddit API (largest API package, ~50 files):

| Sub-package / File | Purpose |
|---|---|
| `RedditApi.kt` / `TedditApi.kt` | Retrofit interfaces for JSON & Teddit endpoints |
| `RedditCookieJar.kt` | In-memory cookie jar for scraped session |
| `JsonInterceptor.kt` | Manipulates JSON API request headers |
| `SortingConverterFactory.kt` | Retrofit converter for Sort/TimeSorting params |
| `source/` | `BaseRedditSource` interface + 3 implementations: `RedditSource`, `TedditSource`, `RedditScrapingSource` + strategy wrapper `CurrentSource` |
| `model/` | ~30 JSON data classes: `Listing`, `ListingData`, `PostData`, `CommentData`, `Child` (polymorphic), `Media`, `MediaMetadata`, `GalleryData`, `Crosspost`, `Awarding`, `RichText`, `AboutData`, `AboutUserData`, `MoreChildren`, `MoreData`, etc. |
| `adapter/` | Custom Moshi adapters: `RepliesAdapter`, `EditedAdapter`, `MediaMetadataAdapter`, `NullToEmptyStringAdapter` |
| `scraper/` | Jsoup scrapers: `RedditScraper` (base), `PostScraper`, `CommentScraper`, `SubredditScraper`, `SubredditSearchScraper`, `UserScaper`, `Over18Scraper` |

**`remote/datasource/`** — Paging 3 data sources:

| DataSource | Backed By | Feeds |
|---|---|---|
| `SmartPostListDataSource.kt` | `CurrentSource` | Home feed, multi-reddit |
| `PostListDataSource.kt` | `CurrentSource` | Generic post list paging |
| `UserPostsDataSource.kt` | `CurrentSource` | User post history |
| `CommentsDataSource.kt` | `CurrentSource` | User comment history |
| `SearchPostDataSource.kt` | `CurrentSource` | Global post search |
| `SearchUserDataSource.kt` | `CurrentSource` | User search |
| `SearchSubredditDataSource.kt` | `CurrentSource` | Subreddit search |
| `SubredditSearchPostDataSource.kt` | `CurrentSource` | In-subreddit search |

**`remote/scraper/`:**

| File | Purpose |
|---|---|
| `Scraper.kt` | Abstract base class for Jsoup-based scrapers. Defines common selectors, timeout, IO dispatcher. |

---

### `data/repository/` — Repositories

| Repository | Role / Key Methods |
|---|---|
| `PostListRepository.kt` | Central repository: posts (Pager), subscriptions CRUD, profiles CRUD, history, save/unsave posts/comments, search |
| `PreferencesRepository.kt` | DataStore access: theme, source, NSFW, left-hand, instance URL, disclaimer, etc. |
| `BackupRepository.kt` | Initiate/restore DB backups |
| `ImgurRepository.kt` | Fetch Imgur album by ID |
| `GfycatRepository.kt` | Fetch Gfycat video metadata |
| `RedgifsRepository.kt` | Fetch Redgifs video metadata |
| `StreamableRepository.kt` | Fetch Streamable video metadata |
| `AssetsRepository.kt` | Read raw asset files (e.g., license text) |

---

### `data/worker/` — WorkManager

| File | Purpose |
|---|---|
| `MediaDownloadWorker.kt` | Background file download with retry support; enqueued with ForegroundInfo for Android 12+ |

---

## 2. `di/` — Dependency Injection (Hilt)

All modules install into `SingletonComponent`:

| Module | Provides |
|---|---|
| `NetworkModule.kt` | 4 OkHttpClients (RedditOfficial, Teddit, RedditScrap, Generic), 2 RedditApis (official + old), TedditApi, ImgurApi, StreamableApi, GfycatApi, RedgifsApi; 3 Moshi instances (RedditMoshi, BasicMoshi, ImgurMoshi). 9 qualifiers total. |
| `DatabaseModule.kt` | Singleton `RedditDatabase` with migrations |
| `PreferencesModule.kt` | Singleton `DataStore<Preferences>` named `"preferences"` |
| `DispatchersModule.kt` | 4 named dispatchers: `@DefaultDispatcher`, `@IoDispatcher`, `@MainDispatcher`, `@MainImmediateDispatcher` |
| `CoroutinesScopesModule.kt` | `@ApplicationScope` — `CoroutineScope(SupervisorJob() + Dispatchers.Default)` |

---

## 3. `ui/` — Feature-Based UI Packages (22 packages)

Each package follows MVVM: 0–1 Fragment, 0–1 ViewModel, 0–N Adapters/Dialogs.

| Package | Fragment(s) / Dialogs | Purpose |
|---|---|---|
| `postlist/` | `PostListFragment` | Paginated feed (home, multi-reddit) |
| `postdetails/` | `PostDetailsFragment` | Post body + threaded comments |
| `mediaviewer/` | `MediaViewerFragment` | Fullscreen ExoPlayer / `TouchImageView` gallery |
| `subreddit/` | `SubredditFragment` | Browse a specific subreddit |
| `user/` | `UserFragment` | Browse a user's posts & comments |
| `subscriptions/` | `SubscriptionsFragment` | Manage subscribed subreddits |
| `profile/` | `ProfileFragment` | View profile details & saved items |
| `profilemanager/` | `ProfileManagerDialogFragment` | Multi-profile CRUD |
| `preferences/` | `PreferencesFragment` | Theme, source, NSFW, left-hand, privacy settings |
| `search/` | `SearchFragment` | Search posts/subreddits/users |
| `sort/` | `SortDialogFragment` | Sort picker (Hot/New/Top/Controversial) |
| `commentmenu/` | `CommentMenuBottomSheet` | Long-press actions on comments |
| `postmenu/` | `PostMenuBottomSheet` | Long-press actions on posts |
| `linkmenu/` | `LinkMenuBottomSheet` | Long-press actions on links |
| `redditsource/` | `RedditSourceDialogFragment` | Choose data source (Reddit/Teddit/Scraped) |
| `policydisclaimer/` | `PolicyDisclaimerDialogFragment` | Privacy policy acceptance dialog |
| `backup/` | Backup/restore dialogs | Export/import DB backup |
| `about/` | About screen | App info, license, credits |
| `privacyenhancer/` | N/A (might be part of preferences) | Privacy enhancement toggles |
| `loadstate/` | Shared load-state UI | Paging loading/error/empty indicators |
| `base/` | Base Fragment/Adapter classes | Shared UI plumbing |
| `common/` | Common widgets | Dividers, spacers, shared layouts |

---

## 4. `util/` — Utilities & Extensions

**Root util files** (21 files):

| File | Purpose |
|---|---|
| `LinkHandler.kt` | Resolves Reddit links, custom schemes, external media URLs |
| `LinkValidator.kt` | URL validation (scheme, host, path checks) |
| `LinkRedirector.kt` | Follows HTTP redirects to resolve final media URL |
| `LinkUtil.kt` | Utility extractors for domain, query params |
| `RedditUri.kt` | Parses Reddit deep links into structured paths |
| `ExoPlayerHelper.kt` | ExoPlayer lifecycle, playback controls, audio focus |
| `HtmlParser.kt` | Parses Reddit's rich-text HTML into `RedditText` |
| `RedditTagHandler.kt` | Custom `Html.TagHandler` for Reddit-specific tags |
| `CommentUtil.kt` | Comment formatting helpers |
| `PostUtil.kt` | Post type detection, domain extraction |
| `RedditUtil.kt` | General Reddit string/number formatting |
| `SearchUtil.kt` | Search query sanitization |
| `DateUtil.kt` | Relative time formatting ("6h ago", "2y ago") |
| `FileUncaughtExceptionHandler.kt` | Writes uncaught exceptions to file |
| `HideBottomViewBehavior.kt` | `CoordinatorLayout.Behavior` for bottom nav slide-out |
| `IntentUtil.kt` | Intent builders (share, open in browser) |
| `DrawerContent.kt` | Bottom drawer content state holders |
| `ClickableMovementMethod.kt` | `LinkMovementMethod` that respects `ClickableSpan` |
| `BlurTransformation.kt` | Coil image transformation for NSFW blur |
| `PagerHelper.kt` | ViewPager2 page indicator / behavior helpers |
| `Util.kt` | Generic helpers (display metrics, dp/px, keyboard) |

**`util/extension/`** — Kotlin extension functions (18 files):

| File | Extensions on |
|---|---|
| `ActivityExt.kt` | Activity lifecycle helpers |
| `ApplicationExt.kt` | Application-scoped access |
| `BindingExt.kt` | View/ViewBinding helpers |
| `BundleExt.kt` | Bundle key/value helpers |
| `DataStoreExt.kt` | DataStore read/write flows |
| `DateExt.kt` | `toSeconds`, `timeAgo` |
| `Ext.kt` | General-purpose extensions |
| `FragmentExt.kt` | Fragment lifecycle, `launchRepeat` |
| `IterableExt.kt` | Collection operations |
| `NotificationExt.kt` | Notification channel/build helpers |
| `NumberExt.kt` | `toPx`, `toDp`, `toTimeStamp` |
| `PermissionExt.kt` | Runtime permission helpers |
| `StringExt.kt` | String manipulation |
| `UriExt.kt` | URI parsing helpers |
| `ViewExt.kt` | `clearWindowInsetsListener`, `isVisible`, insets |
| `ViewModelExt.kt` | ViewModel scope helpers |
| `ViewPagerExt.kt` | ViewPager2 behavior helpers |
| `WorkManagerExt.kt` | WorkManager enqueue helpers |
