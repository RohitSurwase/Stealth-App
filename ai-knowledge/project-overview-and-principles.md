# Project overview & principles
AI-generated, verified against the files listed below on 2026-11-22 (HEAD c0aad3f, v3.0.5). Check before relying on it.

## Summary
Stealth (unReddit) is an account-free, privacy-oriented Reddit client for Android, GPLv3. It never asks for Reddit credentials and stores all user state locally. Namespace `com.cosmos.unreddit` (`buildSrc/src/main/kotlin/Config.kt`), applicationId `com.rohitss.stealth` (debug appends `.dev`), versionCode 35 / `3.0.5`, minSdk 23, compile/targetSdk 33. It is a fork of `cosmosapps/stealth` that kept the upstream namespace but changed the app id/branding. The tree is a single-Activity Hilt app: one `MainActivity` hosts a `NavHostFragment`; all screens are fragments/bottom sheets; five `@InstallIn(SingletonComponent::class)` DI modules wire the network stack, Room, DataStore, dispatchers and an app scope.

## Identity & features
- No login screen, no user account. Local identity = profiles, subscriptions, history, saved posts/comments, all scoped by `profile_id` (no global user state).
- Anonymous, app-only Reddit OAuth token since 2026-10-22 (spoofs Reddit's Android client) — not a user login.
- Renders gallery/image/GIF/video in-app; third-party media via Imgur/Gfycat/Redgifs/Streamable; ExoPlayer for video, Coil for images.
- Room `reddit_db` (schema v4) + DataStore `preferences`.
- Light/Dark/Amoled themes, left/right-handed bottom nav, Privacy Enhancer (regex external-link redirects ON/OFF/ALWAYS_ASK).

## Architecture
```
UI (single Activity): MainActivity → NavHostFragment → PostList/PostDetails/MediaViewer/Subscriptions/Profile/Preferences fragments (+ bottom sheets)
DI: Hilt SingletonComponent — NetworkModule, DatabaseModule, PreferencesModule, DispatchersModule, CoroutinesScopesModule
Data: Repositories → CurrentSource (REDDIT | Teddit | RedditScrapingSource) → Room / DataStore / third-party APIs / old.reddit HTML
```
Entry points: `UnredditApplication` (`@HiltAndroidApp`), `MainActivity`/fragments (`@AndroidEntryPoint`), `MediaDownloadWorker` (`@HiltWorker`).

## End-to-end runtime flow (process start → rendered feed)
1. `UnredditApplication.onCreate()` — `runBlocking { getNightMode().first() }` reads theme from DataStore, applies it, installs `FileUncaughtExceptionHandler` (writes log, `exitProcess(0)`), builds Coil loader, supplies Hilt `HiltWorkerFactory`.
2. `MainActivity.onCreate()` — `setTheme(appTheme)` **before** `super.onCreate`; edge-to-edge; `initNavigation()` builds NavHost + floating bottom nav; collects `UiViewModel` flows (nav visibility, left-handed rebuild, policy-disclaimer Snackbar after 5 s). Nav shown only for the 4 tab destinations.
3. `NetworkModule.provideRedditOkHttpClient()` — builds the official client chain.
4. `RedditOAuthInterceptor.intercept()` — mints the app-only token on first use, adds headers, refreshes once on `401`.
5. `CurrentSource` — delegates to Reddit/Teddit/scraper per `reddit_source` pref; 3 methods hard-wired to official source.
6. Feed paging — `PostListViewModel → PostListRepository → SmartPostListDataSource`: ≤100 subs one joined request; >100 split into chunks, parallel, per-chunk `after` keys, merged by sort (NEW→date, TOP→score, else interlace).
7. Mapping — `PostMapper2 → PostUtil.filterPosts` (drops NSFW unless `show_nsfw`, marks seen/saved).
8. Profile scope — `BaseViewModel.currentProfile` re-issues every DAO query with the active `profileId`.
9. Post detail — two listings (post + comments); "more comments" always hits official `/api%2fmorechildren`.
10. Media — `LinkHandler → MediaViewerViewModel → MediaViewerAdapter`: classify → resolve → play.
11. Download — `MediaDownloadWorker` downloads video+audio separately, muxes on API 29+.
12. Privacy — `LinkHandler.openLink() → LinkRedirector` rewrites external hosts.

## The official Reddit path (app-only OAuth, 2026-10-22)
Reddit began blocking anonymous `.json` server-side with a `403` network-policy page; no cookie change bypasses it. The fix (from Redlib's `MobileSpoof`) mints an anonymous, app-only token by spoofing Reddit's Android client:
- `RedditOAuthManager.kt` (`@Singleton`): persists a device UUID in `reddit_oauth` prefs; `POST https://www.reddit.com/auth/v2/oauth/access-token/loid` with Basic `base64("ohXpoqrZYub1kg:")` + Android client headers; caches Bearer token + `x-reddit-loid`/`x-reddit-session`/QoS/expiry in memory; refreshes before expiry; `invalidate()` forces fresh. Token ~24 h, memory-only.
- `RedditOAuthInterceptor.kt`: adds `Authorization: Bearer ...` + spoofed headers; on `401` invalidates and retries once.
- `RedditPathInterceptor.kt`: percent-encodes the path-based-blocked routes — `/r/<sub>/comments` → `/r/<sub>%2fcomments`, `/api/morechildren` → `/api%2fmorechildren`, `/api/info` → `/api%2finfo`. Bare comment IDs (not fullnames) accepted.
- `RedditApi.kt`: `BASE_URL = https://oauth.reddit.com/` (old: `old.reddit.com`).
- `@RedditOkHttp` chain: `RawJsonInterceptor → JsonInterceptor → RedditPathInterceptor → RedditOAuthInterceptor`; no cookie jar.
- Listings/about/user/search return `200`; only thread routes need the `%2f` bypass. ToS-sensitive (private client ID + spoofed headers), may break anytime.

Historical (removed 2026-10-22): the earlier `RedditCookieInitializationInterceptor` solved a browser JS challenge for `token_v2`; it failed because Reddit also blocks anonymous `.json` server-side even with the cookie. The interim `RedditCookieJar` (memory cache + `CookieManager`) was reverted; `RedditCookieJar` now lives only on the old-Reddit scraper client.

## Source strategy (runtime switch)
`DataPreferences.RedditSource` maps DataStore key `reddit_source`: 0 `REDDIT` (default, Retrofit+Moshi over `www.reddit.com` JSON), 1 `TEDDIT` (Retrofit over a Teddit instance), 2 `REDDIT_SCRAP` (Jsoup over `old.reddit.com` HTML). `CurrentSource` (`@Singleton`) delegates most methods to the active source, but `getMoreChildren()`, `searchPost()`, `searchUser()` are hard-wired to official `RedditSource` because Teddit throws `UnsupportedOperationException` and the scraper returns empty listings. Source switch is immediate (`PreferencesViewModel.setRedditSource` updates pref + live delegate); the Teddit base URL is captured once at `@Singleton` provisioning, so changing the instance needs a restart.

## Media pipeline (classify → resolve → render)
- **Classify** is data-driven: `PostData.mediaType` decides the type from API fields, not the tapped URL (`is_self`→NO_MEDIA, `is_gallery`→REDDIT_GALLERY, `is_video`→REDDIT_VIDEO/GIF, Imgur/Gfycat/Redgifs/Streamable/v.redd.it/i.redd.it → respective types).
- **Route**: `LinkHandler.handleLink()` — subreddit/user/permalink → navigation; media types → `MediaViewerFragment`; everything else → Privacy Enhancer → Custom Tab.
- **Resolve**: `MediaViewerViewModel.retrieveMedia()` — galleries re-fetch by permalink; third-party APIs called; `v.reddit` gets a separate audio URL via `LinkUtil.getRedditSoundTrack()` (matches `DASH_`/`CMAF_`, replaces with `_AUDIO_128`, preserving format).
- **Render**: `MediaViewerAdapter` — Coil for images; ExoPlayer with `MergingMediaSource(video, audio)` for video+audio, video-only retry on 403/404; 100 MB `SimpleCache`.
- **Download**: reuses resolved `GalleryMedia`, forwards `sound`; muxes on API 29+ via `MediaMuxer` (fails silently below API 29).

## Redlib media + inline verification (the investigation, v3.0.5)
Redlib (a public/self-hosted Reddit HTML proxy) sits behind a Cloudflare/Anubis bot challenge. The work (commit `5605018`, plus `6e24506` and `72fa56d`) made redlib video play in ExoPlayer and solved the challenge inside the app:
- **Challenge is request-fingerprint-bound** (browser UA + `Accept`/`Accept-Language`/`Sec-Fetch-*` headers + `cf_clearance` cookie), NOT TLS/JA3-bound. `BrowserLikeInterceptor` rewrites only HTTP headers; the app's OkHttp and ExoPlayer share the same JA3.
- **Media path**: `OkHttpDataSource` routes the m3u8 playlist and every segment through the browser-like `@TedditOkHttp` client (browser UA + headers + `cf_clearance`). Honors `DataSpec.position/length` via HTTP `Range` (Reddit CMAF HLS addresses one `.mp4` with byte ranges); returns `C.RESULT_END_OF_INPUT` at stream end; `RedlibLoadErrorPolicy` returns no retry on 403/429 so the viewer shows verification instead of retrying ~50 s. `ExoPlayerHelper` is NOT a singleton — it takes the browser-like OkHttp client as a ctor arg.
- **Detection**: `RedlibChallenge.isChallenge(code, body)` (status 403/429 + body-marker scan for Cloudflare/Anubis/Gandalf) → `RedlibBlockedException` carrying the listing URL.
- **Inline overlay**: `PostListFragment` opens `RedlibVerificationFragment` (WebView over the feed); `RedlibWebView` provides a WebView-identical UA (`BrowserUserAgent`), cookie enablement, and a locally-served Anubis proof-of-work worker. Solving sets `cf_clearance` in `CookieManager` → flush → reuse via `RedditCookieJar` on every OkHttp request.
- **Media verification**: `MediaViewerAdapter.handleChallenge()` — on 403/429 against a redlib URL, launches `RedlibVerificationActivity` for the instance host; on completion, flushes cookies and `retryPendingRedlib()` re-prepares the player; retry bar hides on `STATE_READY`.
- **Feed image posts on media-stripped redlib builds**: listings carry only a 140px proxied `post_thumbnail` + a `<span>` label (`image`/`video`); `RedlibPostScraper.toPost()` classifies `image`-labelled proxied thumbnails as `MediaType.IMAGE` (link-post labels like `bsky.app` are unaffected; `video` label intentionally not mapped — no URL in the stripped feed). Detail pages still carry full-size media.
- **Full-size viewer**: opening a `/preview/` image fetches the detail page via `getPost(permalink)` and swaps in the full-size URL when `postType == IMAGE` and the URL isn't another `/preview/`; failure keeps the preview.
- **Instance build label**: `RedlibScraper.extractInstanceVersion()` selects `#version, a#source` and stores the text AS-IS (upstream `<span id="version">v0.40.0</span>`, forks `<a id="source">82235ec</a>`); `RedlibInstanceInfo` is an in-memory `StateFlow<Map<host,version>>`, no persistence.
- **Gandalf** (nerdvpn's self-hosted botblocker) is a different challenge variant but the existing inline WebView solves it as-is; no code change needed.
- **Search**: some redlib instances don't serve `/search` (404) → friendly message; user search unsupported everywhere.

## Working principles (invariants)
1. **Account-free, but not request-free** — no user identity; local profiles replace the account; the official client still needs the app-only OAuth token + route encoding.
2. **One Activity, one Hilt graph, fragments for screens** — ViewModels expose flows; fragments collect; `BaseViewModel` supplies profile-scoped flows.
3. **Remote access is a runtime strategy** — three sources selected by pref; 3 methods hard-wired to official.
4. **The profile is the data tenant boundary** — `profile_id` in composite PKs + cascading FK; DAOs always take a `profileId`; preferences are global, only the selected profile is per-profile.
5. **Remote pages are disposable; explicit actions are durable** — Paging 3, no `RemoteMediator`; only viewing/saving/subscribing write to Room; NSFW filtering is client-side (downloaded then discarded).
6. **Media has one classify→resolve→render pipeline**.
7. **Bootstrapping is synchronous and process-wide** — several singletons read DataStore / mint tokens on the calling thread at construction (`UnredditApplication`, `CurrentSource`, `LinkRedirector`, `provideTedditApi`, `MediaViewerFragment`).

## Evolution
- **Upstream (2020-2025)**: `cosmosapps/stealth` fork; feature-by-feature build; tags 0.1.0→2.0.0; key upstream fixes (Reddit audio `DASH_AUDIO_128`, Redgifs `vthumbnail` removal, Android 14 launch fix, policy disclaimer 2023-06-10).
- **Fork line (2026-07-17 → HEAD)**: `59295de` rebrand (appId `com.rohitss.stealth`), `45fc13e` cookie hotfix (later removed), `bcfe7c2` CMAF audio, `dfa795f` downloaded-audio mux, `302922c` keystore move, `1349c1f` kapt JDK16+ flags, `e216456` v3.0.0, `b8474ec` post shortcut buttons, `07577e5` merge PR#6 (the baseline the `Knowledge/` docs were generated at).
  - **What changed since v3.0.0 (`07577e5` to HEAD `c0aad3f`, v3.0.5):**
    - **OAuth workaround (`85980e1`, 2026-10-22 emulator investigation)** — Reddit blocks anonymous `.json` server-side; removed the broken token-based bypass and added the app-only OAuth flow (`RedditOAuthManager` mints an anonymous app token via `POST …/auth/v2/oauth/access-token/loid` with client id `ohXpoqrZYub1kg`, `RedditOAuthInterceptor`, `RedditPathInterceptor` `%2f` route encoding, `RedditApi.BASE_URL = oauth.reddit.com`).
    - **Redlib HTML scraping source (`74203ef`)** — added the `scraper/redlib/` package (`RedlibScraper` + post/comment/subreddit/user/search scrapers), rewrote `TedditSource`/`TedditApi` to scrape Redlib HTML routes, wired `BrowserLikeInterceptor` + `@RedlibBaseUrl`, added `RedlibScraperTest`. Default source → Redlib.
    - **Dynamic instances + custom UA (`925abf6`)** — `RedlibInstancesRepository` downloads/clearnet-filters the instance list; `custom_user_agent` DataStore key; `BrowserLikeInterceptor` takes a per-request UA provider.
    - **Active-instance home bar (`1c4120d`)** — subtitle under "STEALTH" shows the saved Redlib instance.
    - **Legacy-class removal (`ecd4e0f`)** — dead-code cleanup: `TargetRedditInterceptor`, `CommentType`, `PagerHelper`, `RedditUri`, `Over18Scraper`, `TedditUser`, empty `PollPostViewHolder`, `RedditCookieJar.cookies`.
    - **NSFW controls + prefs sync (`42e9b25`)** — fixed the Redlib feed hang/empty-feed crash by syncing a content preference with Redlib; added NSFW controls.
    - **HLS video playback behind bot challenge + inline verification (`5605018`)** — `OkHttpDataSource` routes the m3u8/segments through the browser-like client; `RedlibChallenge`/`RedlibBlockedException` detection; `RedlibVerificationFragment`/`RedlibWebView` inline Cloudflare/Anubis solving; `RedlibLoadErrorPolicy` (no retry on 403/429).
    - **Search friendly message (`6e24506`)** — instances without `/search` get a friendly error + taller info bar.
    - **v3.0.5 media-stripped feed fixes (`72fa56d`)** — feed image posts classified via the `post_thumbnail` `<span>` label; full-size `/preview/` image resolution in the viewer; `RedlibInstanceInfo` in-memory instance-build label (`extractInstanceVersion()`).
    - Version bumps: v3.0.1 (`cb19d7a`), v3.0.2 (`8243e83`), v3.0.3 (`7147b4d`), v3.0.4 (`1f277bd`), v3.0.5 (`c0aad3f`).
- Note: `versionCode 35` but changelogs still end at `23.txt` (no `24.txt`–`30.txt`).

## Issue → fix register
- **3.1 Reddit anonymous JSON block (Oct 2026)** — tried cookie bootstrap + challenge solving; ruled out UA/Accept/DNS/TLS/IP; server still `403`s anonymous `.json`; chose app-only OAuth + `%2f` route encoding.
- **3.2 v.reddit audio playback (`bcfe7c2`)** — `DASH_(\d+)` → `(DASH|CMAF)_(\d+)`, audio name `_AUDIO_128` preserving format.
- **3.3 Downloaded audio (`dfa795f`)** — pass `sound` to worker, mux on API 29+; API<29 reports failure (limitation).
- **3.4 Release signing (`302922c`)** — keystore at `keys/keystore.properties` relative to `app/`; absent → unsigned release.
- **3.5 kapt JDK16+ (`1349c1f`)** — `--add-exports/--add-opens` in both `app/build.gradle.kts` and `gradle.properties`.
- **3.6 Post shortcut buttons (`b8474ec`)** — user/subreddit nav from post surfaces; `r/` prefix stripped.

## Open issues & known limits (current tree)
- Download notification retry/cancel never fire — `DownloadManagerReceiver` requires `KEY_SOUND`; pending intents omit it.
- OAuth uses a private Android client ID + spoofed headers — Reddit may revoke anytime; ToS/anti-abuse risk.
- Thread routes rely on a `%2f` path-bypass — breaks again if Reddit normalizes the encoded slash.
- OAuth token is memory-only, refreshed lazily; no background refresh.
- Reddit still blocks anonymous `.json` — workaround depends on the private token flow.
- Scraper client never bootstraps cookies — `old.reddit.com` scraping still login-walled.
- Download mux unavailable below API 29.
- Teddit/scraped sources incomplete — 3 ops hard-wired to official; in-subreddit search empty when scraping.
- Teddit host changes require a restart.
- Redgifs bearer token never refreshes.
- NSFW filtering is client-side (downloaded then discarded).
- Blocking DataStore reads on hot paths.
- No CI; only `ExampleUnitTest`, `ExampleInstrumentedTest`, `DateUtilTest`.
- Version/changelog mismatch (`35`/`3.0.5` vs changelog `23.txt`).

## Covers
- app/src/main/java/com/cosmos/unreddit
- app/src/main/java/com/cosmos/unreddit/di
- app/src/main/java/com/cosmos/unreddit/util
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib
- app/src/main/java/com/cosmos/unreddit/ui/mediaviewer
- app/src/main/java/com/cosmos/unreddit/ui/redditsource
- buildSrc
- ./*
Listing: 2b89584ee8ac

## Files
- app/src/main/java/com/cosmos/unreddit/MainActivity.kt 5abbf4a689d6
- app/src/main/java/com/cosmos/unreddit/UnredditApplication.kt 7afb07446ff4
- app/src/main/java/com/cosmos/unreddit/UiViewModel.kt 9956c64d9631
- app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt 9d8331fdc3ad
- app/src/main/java/com/cosmos/unreddit/di/DatabaseModule.kt a81cd0c0d0f0
- app/src/main/java/com/cosmos/unreddit/di/PreferencesModule.kt daef21ffa0a1
- app/src/main/java/com/cosmos/unreddit/di/DispatchersModule.kt 2e76083b3ed0
- app/src/main/java/com/cosmos/unreddit/di/CoroutinesScopesModule.kt 9c5729d81a4b
- app/src/main/java/com/cosmos/unreddit/util/LinkHandler.kt 319e3593d017
- app/src/main/java/com/cosmos/unreddit/util/LinkRedirector.kt 2be56e380dba
- app/src/main/java/com/cosmos/unreddit/util/LinkUtil.kt c074edc0b19c
- app/src/main/java/com/cosmos/unreddit/util/ExoPlayerHelper.kt 54120e034636
- app/src/main/java/com/cosmos/unreddit/util/OkHttpDataSource.kt 82a0ff5b0ad3
- app/src/main/java/com/cosmos/unreddit/util/PostUtil.kt 176b70ec814c
- app/src/main/java/com/cosmos/unreddit/ui/mediaviewer/MediaViewerAdapter.kt ea6e42db3006
- app/src/main/java/com/cosmos/unreddit/ui/mediaviewer/MediaViewerViewModel.kt 07b2b12e8880
- app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedlibVerificationFragment.kt c0719390f35b
- app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedlibWebView.kt 7b1bd051da8b
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditOAuthManager.kt 780c993b3735
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditOAuthInterceptor.kt f309d399132b
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditPathInterceptor.kt dd8cecd8ef98
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditApi.kt 6131cade4532
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibPostScraper.kt 44185eae6132
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibChallenge.kt 692b884b68f8
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibScraper.kt 426d926686a2
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedlibInstanceInfo.kt d71a6e9c7484
- app/src/main/java/com/cosmos/unreddit/data/local/RedditDatabase.kt a59fb8d67a3f
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/CurrentSource.kt 894f821b6a6c
- buildSrc/src/main/kotlin/Config.kt 4cf1a474a1f6
- buildSrc/src/main/kotlin/Dependencies.kt 0e8efd0e0ce0
- build.gradle.kts 4bfa4fab0db4
- settings.gradle.kts e2b1f43980b5
- gradle.properties 91827aa447c1
