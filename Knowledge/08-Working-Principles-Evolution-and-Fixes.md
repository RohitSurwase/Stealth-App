# 08 - Working Principles, Evolution, Issues and Fixes

This document is the consolidated narrative of the app: the mechanisms that define how it works, the chronological evolution that produced the current tree, and an issue-by-issue register of the problems and fixes applied. It is intentionally code-grounded: every non-obvious claim has a file/function reference.

- **Verified at:** HEAD `07577e5` (merge of `b8474ec`, 2026-07-27), plus the 2026-10-22 emulator investigation and OAuth workaround documented in §3.1.1-§3.1.2 (uncommitted).
- **Path convention:** source paths are relative to `app/src/main/java/com/cosmos/unreddit/` unless written in full.
- **Companion documents:** [01-Project-Overview.md](01-Project-Overview.md) for the end-to-end runtime walkthrough, [04-Data-Layer.md](04-Data-Layer.md) for the data layer, [02-Build-System.md](02-Build-System.md) for build/release details.

---

## 1. Working Principles

These are the invariants that remain true across features. They explain why the code is shaped the way it is, not just what it does.

### 1.1 Account-free, but not request-free

- There is no login screen and no Reddit user account. Since 2026-10-22 the official client obtains an anonymous, app-only OAuth token by spoofing Reddit's Android client (`RedditOAuthManager.kt:30-137`). This is not a user login and still requires no credentials.
- The official client calls `https://oauth.reddit.com/` through `RedditApi` (`RedditApi.kt:113`) with the usual `.json` and `raw_json=1` handling (`JsonInterceptor.kt:12-15`, `RawJsonInterceptor.kt:12-14`).
- `RedditOAuthInterceptor` adds `Authorization: Bearer ...` and the Android client headers to every official request, and refreshes once on `401` (`RedditOAuthInterceptor.kt:12-47`). `RedditPathInterceptor` percent-encodes the `/comments` and `/api/morechildren` routes that Reddit blocks for app-only tokens (`RedditPathInterceptor.kt:24-41`).
- "Account-free" still means no user identity: profiles, subscriptions, history and saved items remain local and scoped by `profile_id`. The pre-existing `token_v2` challenge interceptor was removed on 2026-10-22; `RedditCookieJar` remains only on the old-Reddit scraper client.
- Local identity replaces the account: profiles, subscriptions, history and saved items are stored on-device and scoped by `profile_id`.

### 1.2 One Activity, one Hilt graph, fragments for screens

- `MainActivity` is the only activity and hosts a `NavHostFragment` (`MainActivity.kt:95-103`). All screens are fragments or bottom sheets.
- Hilt injects into `UnredditApplication` (`@HiltAndroidApp`), `MainActivity`/fragments (`@AndroidEntryPoint`) and `MediaDownloadWorker` (`@HiltWorker`). Nearly everything stateful is a `@Singleton` in `SingletonComponent` (`di/NetworkModule.kt`, `di/DatabaseModule.kt`, `di/PreferencesModule.kt`).
- ViewModels expose flows; fragments collect them. `BaseViewModel` supplies profile-scoped flows (`ui/base/BaseViewModel.kt:28-45`).

### 1.3 Remote access is a runtime strategy

`BaseRedditSource` has three implementations, selected by the `reddit_source` DataStore key:

| Preference | Implementation | Transport |
|---|---|---|
| `REDDIT` (0, default) | `RedditSource` | Retrofit + Moshi over `www.reddit.com` JSON |
| `TEDDIT` (1) | `TedditSource` | Retrofit over a Teddit instance |
| `REDDIT_SCRAP` (2) | `RedditScrapingSource` | Retrofit + Jsoup over `old.reddit.com` HTML |

`CurrentSource` is the delegate (`data/remote/api/reddit/source/CurrentSource.kt`): most methods forward to the active source (`:32-57`, `:64-84`), but three methods are deliberately hard-wired to the official `RedditSource` because the alternatives do not implement them:

- `getMoreChildren()` (`:59-62`) — used by "load more comments".
- `searchPost()` (`:86-94`).
- `searchUser()` (`:96-104`).

The alternatives confirm the gap in code: `TedditSource` throws `UnsupportedOperationException` for those methods (`TedditSource.kt:43,74,83`), and `RedditScrapingSource` returns empty/placeholder listings (`RedditScrapingSource.kt:58-60,78-80,113-124`).

### 1.4 The profile is the data tenant boundary

- Room entities that hold user data carry `profile_id` in their composite primary key and a cascading foreign key to `profile(id)` (`data/local/RedditDatabase.kt:22-30`).
- DAO queries always take a `profileId`; there is no global user state. `MIGRATION_2_3` adds the `profile_id` indexes (`RedditDatabase.kt`).
- The default profile `id=1, name="Stealth"` is inserted at database creation (`RedditDatabase.kt:55-60`) and by `MIGRATION_1_2` for upgraded installs.
- `BaseViewModel.currentProfile` maps the DataStore key `current_profile` to a `Profile` and re-flattens every dependent flow with `flatMapLatest` (`ui/base/BaseViewModel.kt:28-45`). Switching profiles therefore re-queries history, subscriptions and saved IDs automatically.
- Preferences (theme, NSFW flags, source, privacy enhancer) are **global**, not per profile; the only per-profile setting is the selected profile itself.

### 1.5 Remote pages are disposable; explicit actions are durable

- Feed screens use Paging 3 `PagingSource` implementations and `cachedIn(viewModelScope)`; there is **no** `RemoteMediator`. Remote pages are not saved to Room.
- Only explicit user actions write to Room: viewing a post inserts history (`PostListRepository.insertPostInHistory()`, `data/repository/PostListRepository.kt:191-193`), saving inserts a full post/comment snapshot, subscribing inserts a subscription row, and profile changes update DataStore.
- NSFW filtering happens **after** download, in `PostUtil.filterPosts()` (`util/PostUtil.kt:22-39`): the request still asks for NSFW content (`over18=1` is forced by `RedditCookieJar`), then the mapper/filter removes it unless `show_nsfw` is set.

### 1.6 Media has a single classification → resolution → rendering pipeline

1. **Classification** is data-driven, not URL-tap-driven: `PostData.mediaType` (`data/remote/api/reddit/model/PostData.kt:128-181`) decides `IMAGE`, `VIDEO`, `REDDIT_VIDEO`, `REDDIT_GALLERY`, `IMGUR_*`, `GFYCAT`, `REDGIFS`, `STREAMABLE`, etc.
2. **Routing** is centralized in `LinkHandler.handleLink()` (`util/LinkHandler.kt:29-67`): media types go to `MediaViewerFragment`, Reddit links go to navigation, everything else goes through the Privacy Enhancer and a Custom Tab.
3. **Resolution** happens in `MediaViewerViewModel.retrieveMedia()` (`ui/mediaviewer/MediaViewerViewModel.kt:85` onward): galleries are re-fetched by permalink (`:180-188`), third-party APIs are called, and `v.reddit` gets a separate audio URL via `LinkUtil.getRedditSoundTrack()` (`util/LinkUtil.kt:57-63`).
4. **Rendering** is `MediaViewerAdapter` (`ui/mediaviewer/MediaViewerAdapter.kt`): Coil for images, ExoPlayer with `MergingMediaSource` for video+audio (`:202-207`), and a video-only retry when the audio track returns 403/404 (`:248-258`).
5. **Download** reuses the same resolved `GalleryMedia` and forwards `sound` to `MediaDownloadWorker.enqueueWork()` (`ui/mediaviewer/MediaViewerFragment.kt:307-312`).

### 1.7 Bootstrapping is synchronous and process-wide

Several singletons read DataStore or run network setup on the calling thread at construction:

| Component | Blocking point |
|---|---|
| `UnredditApplication` | `runBlocking { getNightMode().first() }` (`UnredditApplication.kt:44-47`) |
| `CurrentSource` | `runBlocking { getRedditSource().first() }` (`CurrentSource.kt:27-30`) |
| `LinkRedirector` | `runBlocking` for `privacy_enhancer` and all redirects (`util/LinkRedirector.kt:25-32`) |
| `NetworkModule.provideTedditApi()` | `runBlocking { getRedditSourceInstance().firstOrNull() }` (`di/NetworkModule.kt:212-219`) |
| `MediaViewerFragment` | `runBlocking { viewerViewModel.isVideoMuted.first() }` (`ui/mediaviewer/MediaViewerFragment.kt:173-174`) |

Consequences visible in behavior:

- The first request through `@RedditOkHttp` may perform one extra HTTP round trip while `RedditOAuthManager` mints the app-only token; later requests reuse the in-memory credentials until they expire (`RedditOAuthManager.kt:42-58,73-118`).
- The Teddit base URL is captured when the `@Singleton` Retrofit service is created. Changing the Teddit instance therefore shows the restart dialog (`ui/preferences/PreferencesFragment.kt:199-206,319-345`) and `Context.restart()` relaunches the process (`util/extension/ApplicationExt.kt`).
- Source switching itself is immediate: `PreferencesViewModel.setRedditSource()` updates both the preference and the live delegate (`ui/preferences/PreferencesViewModel.kt:101-105`).

### 1.8 One feed request, concretely

For the default `REDDIT` source, a home feed load follows this exact path:

```text
PostListFragment collects PostListViewModel.postDataFlow
  → PostListViewModel.fetchData (combine subreddit + sorting)          ui/postlist/PostListViewModel.kt:67-76
  → combine with userData (history/saved/NSFW)                         :80-88
  → PostListRepository.getPosts() creates Pager(pageSize = 25)         data/repository/PostListRepository.kt:69-83
  → SmartPostListDataSource.load()                                     data/remote/datasource/SmartPostListDataSource.kt:41-51
      ≤ 100 subs: one joined request                                   :101-111
      > 100 subs: chunk, parallel requests, per-chunk after keys       :59-98
  → CurrentSource.getSubreddit() → RedditSource                        data/remote/api/reddit/source/CurrentSource.kt:32-39
  → @RedditOkHttp: RawJson → Json → Path → OAuth                      di/NetworkModule.kt:124-139
  → Moshi parses Listing → PostUtil.filterPosts() maps/filters         util/PostUtil.kt:22-39
  → PagingData<PostEntity> rendered by PostListAdapter
```

---

## 2. Evolution

### 2.1 Upstream line (2020-2025)

The repository is a fork of [cosmosapps/stealth](https://gitlab.com/cosmosapps/stealth), whose history is preserved. The early commits show the app being built feature by feature:

| Date | Commit / tag | Change |
|---|---|---|
| 2020-05-17 | `a18c495`, `be598c8` | Project initialization; Retrofit + Moshi added |
| 2020-12-10/11 | `f5a1145`, `8c0c248`, `1a7eb46`, `6b4a59b`, `253af91`, `82f5279`, `c2f6e83` | Reddit API, post list, user activity, post details, subreddit activity, subscriptions, preferences |
| 2021-03-27 | tag `0.1.0` | First tagged release line |
| 2021-05-09 | tag `1.0.0` | 1.x stable line |
| 2021-10-25 | tag `1.1.3` | Last 1.x tag |
| 2022-11-26 | tag `2.0.0` | 2.x line |
| 2023-06-04 | `eb67886` | Policy-change disclaimer (`policy_disclaimer_shown`, `PolicyDisclaimerDialogFragment`) |
| 2023-07-30 | `b55fb04` (2.2.2) | Reddit audio URL changed from `DASH_audio` to `DASH_AUDIO_128` |
| 2023-08-15 | `cae3c14`, `e8ff2ce` (2.3.0) | minSdk raised to 23; Android 14 launch fix in `cradle_anim.xml` |
| 2025-06-01 | `b72a8f0` (2.3.1) | Redgifs model fix: removed `vthumbnail` field that the API stopped returning |

Feature milestones visible in `fastlane/metadata/android/en-US/changelogs/` (numbers are **versionCode** entries, not version names):

| Changelog | Feature introduced | Current code evidence |
|---|---|---|
| `3.txt` | Profiles, saved posts/comments, crossposts, mute button | `ProfileManagerDialogFragment`, `PostDao`, `GalleryMedia.sound` |
| `5.txt` | Media downloader | `MediaDownloadWorker`, `DownloadManagerReceiver` |
| `9.txt`, `13.txt` | Teddit source, AMOLED theme, backup/restore, >100 subscriptions | `TedditSource`, `UnredditApplication.appTheme`, `BackupRepository`, `SmartPostListDataSource` |
| `12.txt` | Crash file logging, side bottom navigation with left-handed support | `FileUncaughtExceptionHandler`, `HideBottomViewBehavior` |
| `17.txt` | Privacy Enhancer redirects, HTTPS upgrade | `LinkRedirector`, `LinkUtil.https` |
| `19.txt` | Web-scraping source, API-policy disclaimer | `RedditScrapingSource`, `PolicyDisclaimerDialogFragment` |
| `21.txt` | Missing Reddit audio fix | `LinkUtil.REDDIT_SOUNDTRACK_NAME` |
| `22.txt` | Android 6.0 minimum, Android 14 fix, monochrome icon | `Config.kt:6`, `cradle_anim.xml` |
| `23.txt` | Redgifs link fix | `data/remote/api/redgifs/model/Urls.kt` |

### 2.2 Fork line (2026-07-17 → HEAD)

The fork deliberately keeps the upstream namespace (`com.cosmos.unreddit`) but changes the application ID and branding.

| Commit | Date | Purpose | Current code effect |
|---|---|---|---|
| `59295de` | 2026-07-17 | Initial fork setup: rebrand, repackage, compliance cleanup | `applicationId = "com.rohitss.stealth"` (`buildSrc/src/main/kotlin/Config.kt:4`); README/branding; GitLab logo replaced by GitHub; Reddit Snoo removed; disclaimer added. Version still `2.3.1`/23 |
| `ada4bd9` | 2026-07-17 | Add AI-generated Knowledge section | Previous version of these docs (generated **before** the cookie hotfix, which is why it became stale) |
| `45fc13e` | 2026-07-17 | Hotfix for Reddit JSON restriction | New `RedditCookieInitializationInterceptor`; `RedditCookieJar` rewritten over `CookieManager`; wired in `NetworkModule` |
| `bcfe7c2` | 2026-07-17 | v.reddit CMAF audio playback | `REDDIT_VIDEO_REGEX = "(DASH|CMAF)_(\\d+)"`; generic `_AUDIO_128` (`LinkUtil.kt:16,27`) |
| `dfa795f` | 2026-07-17 | Audio missing from downloaded v.reddit videos | `GalleryMedia.Type.AUDIO`; `KEY_SOUND`; `muxAudioVideo()` in `MediaDownloadWorker` |
| `302922c` | 2026-07-17 | Move keystore config | `app/build.gradle.kts:13` reads `../keys/keystore.properties` |
| `1349c1f` | 2026-07-17 | kapt on modern JDKs | `kapt { javacOptions { ... } }` (`app/build.gradle.kts:24-36`); daemon flags (`gradle.properties:10`) |
| `e216456` | 2026-07-17 | Version 3.0.0 | `versionCode 30`, `versionName "3.0.0"` (`Config.kt:10-11`) |
| `b8474ec` | 2026-07-24 | User/subreddit shortcut buttons | `BaseFragment.onUserClick/onSubredditClick` (`ui/base/BaseFragment.kt`), adapters, `include_post_metrics.xml` |
| `07577e5` | 2026-07-27 | Merge PR #6 | Current HEAD |

Notable release-engineering consequence: the tree is `versionCode 30`, but `fastlane/metadata/android/en-US/changelogs/` still ends at `23.txt`. The version bump `e216456` did not add changelogs `24.txt`–`30.txt`.

---

## 3. Issue → Fix Register

### 3.1 Reddit JSON API restriction — `45fc13e`

> **Historical.** The cookie bootstrap described in this subsection was removed on 2026-10-22 when the OAuth workaround replaced it. It is retained because it explains why the anonymous JSON path failed. The current implementation is in §3.1.2.

**Symptom.** Unauthenticated `.json` requests stopped returning data. Every feed, post and comment request through `@RedditOkHttp` was affected.

**Root cause.** Reddit began serving an anti-bot JavaScript challenge instead of the expected JSON to clients without a browser cookie.

**Fix.** Simulate the browser flow once per process and persist the resulting cookies.

Before/after:

| Component | Before `45fc13e` | After `45fc13e` |
|---|---|---|
| `RedditCookieInitializationInterceptor` | Did not exist | Checks for `token_v2`; if absent, fetches the HTML challenge, solves it, and requests the cookie URL (`RedditCookieInitializationInterceptor.kt:12-57`) |
| `RedditCookieJar` | Returned only hardcoded `over18=1`; `saveFromResponse()` ignored cookies | Merges an in-memory cache with Android `CookieManager`, forces `over18=1`, and exposes `hasCookie()` (`RedditCookieJar.kt:9-70`) |
| `NetworkModule` `@RedditOkHttp` | No cookie jar | Adds the interceptor and `.cookieJar(RedditCookieJar)` (`di/NetworkModule.kt:125-134`) |

Exact acquisition sequence (`RedditCookieInitializationInterceptor.kt`):

1. `hasRequiredCookies()` asks `RedditCookieJar.hasCookie("token_v2")`, which checks the in-memory cache first and then `CookieManager` (`:12-14`, `RedditCookieJar.kt:60-69`).
2. If absent, `initializeCookies()` runs before the original request (`:24-57`):
   - `GET https://www.reddit.com/` (`:26-34`).
   - `Jsoup.parse()`; select `script[nonce]`, extract the regex `await(async <param>=>(<operation>))("<baseValue>")` (`:36-39,59-73`).
   - Evaluate the operation (`:78-90`): only `param + param` means `baseValue + baseValue`; everything else falls back to `baseValue`.
   - Read `input[name=token]` **or** `input[name=jsc_token]` and keep the actual name (`:40-42`). Reddit's October 2026 challenge uses `jsc_token`.
   - `GET https://www.reddit.com/?solution=<solution>&js_challenge=1&<tokenName>=<tokenValue>&jsc_orig_r=` (`:43-50`).
3. OkHttp invokes `RedditCookieJar.saveFromResponse()` for response cookies, which writes them to the in-memory cache and `CookieManager`, then calls `flush()` (`RedditCookieJar.kt:47-58`).
4. The original request proceeds (`:21`); `loadForRequest()` merges memory + system cookies and forces `over18=1` (`RedditCookieJar.kt:21-45`).

**Why it is not authentication.** No username, password, OAuth token or Reddit account is involved. The interceptor obtains only an anonymous browser-session cookie. The official API client still has no user identity; profiles remain local.

**Residual risks of the historical cookie path.**

- The regex/operation evaluator supports only the observed `x + x` challenge shape; a changed challenge produces an invalid solution (`:59-90`).
- All exceptions are swallowed and logged (`:54-56`); the original request proceeds and may fail with Reddit's block page.
- `Cookie.parse()` failures are now skipped instead of crashing (`RedditCookieJar.kt:34`).
- `CookieManager.setCookie()` is asynchronous; the in-memory cache makes the just-received cookies visible immediately, removing the previous "launch several times" workaround (`RedditCookieJar.kt:11,47-58`).
- `saveFromResponse()` stores all cookies from Reddit responses, including unrelated ones.
- `@RedditScrapOkHttp` shares the jar but does **not** run the interceptor (`di/NetworkModule.kt:164-171`).

Full runtime details and failure modes: [04-Data-Layer.md §Cookie bootstrap](04-Data-Layer.md#cookie-bootstrap-hotfix-45fc13e).

### 3.1.1 Reddit anonymous JSON block — October 2026 investigation

**Status:** superseded by §3.1.2. Reddit still rejects anonymous `.json` requests at the server with `403` and a network-policy block page, and no `CookieJar` change can bypass that. The final fix therefore uses an app-only OAuth token and route encoding. This section retains the full record of what was tried, what was ruled out, and why the cookie approach was abandoned.

#### Environment and reproduction

| Item | Value |
|---|---|
| Date | 2026-10-22 |
| Device | Android emulator `Pixel-9a_API-36_Android-16`, `emulator-5554` |
| App under test | `com.rohitss.stealth.dev`, versionCode `30`, versionName `3.0.0` |
| Source setting | `REDDIT` (`DataPreferences.RedditSource.REDDIT`) |
| Build used for tests | `app/build/outputs/apk/debug/app-debug.apk`, built with Temurin JDK 17 |
| Device access | `adb` from the portable Android SDK; `adb root` unavailable, debug `run-as` works |

Cold-start reproduction:

1. `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. `adb shell pm clear com.rohitss.stealth.dev`
3. `adb logcat -c`
4. Launch the launcher activity with `monkey`.
5. Wait for the first feed request.

Observed UI: the feed shows **“Something went wrong”** with a **RETRY** button, plus the existing “Stealth might stop working soon” policy snackbar. The app does not crash.

Initial device logs:

```text
D/CookieGen: Cookies Generated!
E/CookieInterceptor: Failed to initialize cookies
E/CookieInterceptor: java.io.IOException: Canceled
```

The second line appeared about 9 seconds after the first on the user's device. That is the interceptor's `catch`, not an HTTP status.

#### What was tried, in order

| # | Action | Result | Conclusion |
|---|---|---|---|
| 1 | Pulled the app's WebView cookie database (`app_webview/Default/Cookies`) | Only `csv` and `edgebucket` existed for `.reddit.com`; **no `token_v2`** | The bootstrap was not completing successfully, or its cookies were not being persisted |
| 2 | Fetched `https://www.reddit.com/` live and inspected the challenge HTML | Page returned `200 text/html` (~8.4 KB). The form was `<form hidden method="GET" action="/">` with `solution`, `js_challenge`, **`jsc_token`**, and `jsc_orig_r` | The challenge field had been renamed; this is a concrete client-side break |
| 3 | Compared the live form with the interceptor | Code selected `input[name=token]` and built `...&token=<value>&...`; the live form's input is `input[name=jsc_token]` and it submits `jsc_token=` | The app sent an empty/irrelevant `token` parameter, so Reddit never issued `token_v2` |
| 4 | Solved the live challenge manually with `curl` | Following the correct `jsc_token` flow returned `Set-Cookie` for `loid`, `session_tracker`, `csrf_token`, **`token_v2`**, `csv`, and `edgebucket` | The challenge itself still works; only the field name was wrong |
| 5 | Patched the interceptor to accept `input[name=token]` **or** `input[name=jsc_token]` and to send the actual field name | Rebuilt and reinstalled; logs showed `token=jsc_token tokenLen=64 solutionLen=32`, then five `Set-Cookie` cookies, then `Cookies Generated!` | The client can now acquire `token_v2` on the first launch |
| 6 | Inspected the JSON request's cookies immediately after the challenge | Log line: `load /r/popular/hot/.json cookies=[edgebucket, loid, session_tracker, csrf_token, token_v2, csv, over18]` | The request **did** carry `token_v2`; the remaining failure is not a missing cookie |
| 7 | Read the actual JSON response | `403 text/html`; body is Reddit's block page (~190 KB in the first tests, ~1.5 KB in a later request), containing *“Your request has been blocked due to a network policy”* and *“blocked by network security”* | Reddit is refusing anonymous JSON even with a valid anonymous session |
| 8 | Tested User-Agent / `Accept` permutations from the host with the same cookies | Chrome desktop, Chrome mobile, and `okhttp/4.10.0` UAs; `Accept: application/json` and `Accept: */*`; all six combinations returned the same `403` block page | UA and `Accept` are not the cause |
| 9 | Tested alternative Reddit endpoints | `https://www.reddit.com/r/popular/hot.rss` → `200 application/atom+xml` (~48 KB); `old.reddit.com/r/popular/hot` → `200` but final URL is `/login/?reason=lor2&dest=...`; `old.reddit.com/.../.json` → HTML login/challenge, not JSON | RSS is the only anonymous door left; old-Reddit scraping and the scraper source are now login-walled |
| 10 | Tested the default Teddit fallback | `https://teddit.net/` failed at TLS (`tlsv1 alert internal error`) | The built-in `TEDDIT` fallback is not currently usable |
| 11 | Replaced the cookie jar with an app-wide in-memory cache merged with `CookieManager` | First JSON request after the challenge carried `token_v2` without relaunching; no crash | This removes the `CookieManager.setCookie()` asynchronous-read race and the old “launch several times” workaround |
| 12 | Re-ran the cold-start reproduction with the fixed build | UI still shows “Something went wrong”; JSON still returns the server-side `403`; no client exception | The only remaining blocker is Reddit's anonymous-JSON policy |

#### Exact evidence

Live challenge form observed on 2026-10-22:

```html
<form hidden method="GET" action="/">
  <input type="hidden" name="solution" />
  <input type="hidden" name="js_challenge" value="1"/>
  <input type="hidden" name="jsc_token" value="<64-char value>"/>
  <input type="hidden" name="jsc_orig_r" value=""/>
</form>
```

The related script matched the existing regex and produced a 32-character solution:

```js
await(async e=>e+e)("497ec22160b885b9")
```

After the fix, the app logged:

```text
challenge status=200 type=text/html size=8406
form token=jsc_token tokenLen=64 solutionLen=32
saved cookies: loid,session_tracker,csrf_token,token_v2,csv
challenge response status=200 cookies=5
Cookies Generated!
load /r/popular/hot/.json cookies=[edgebucket, loid, session_tracker, csrf_token, token_v2, csv, over18]
request /r/popular/hot/.json -> 403 text/html
```

The `403` body's relevant text:

```text
Your request has been blocked due to a network policy.
Try logging in or creating an account here to get back to browsing.
If you're running a script or application, please register or sign in with your
developer credentials here.
```

Response headers included `retry-after: 0`, `server: snooserv`, and `content-type: text/html`. A later request from the same host returned a smaller “whoa there, pardner!” block page with the same policy text and a support code; the exact page size varies, but the outcome is the same.

#### Root causes

1. **Challenge field rename (client, was fixed, later removed).** The challenge now uses `jsc_token`. The old selector `input[name=token]` returned `null`, so the follow-up URL contained `token=`. The interim interceptor was patched to use the actual input name; that whole interceptor was removed when OAuth replaced it.
2. **Asynchronous cookie visibility (client, was hardened, later reverted).** `CookieManager.setCookie()` and `flush()` are asynchronous, so the old jar could miss a just-written `token_v2` on the first request. The interim jar added a memory cache; the final implementation does not use Reddit cookies for the official source, and `RedditCookieJar.kt` is back to its pre-investigation form for the old-Reddit scraper client only.
3. **Anonymous `.json` is blocked (server, bypassed by OAuth).** Even with `token_v2`, `loid`, `session_tracker`, `csrf_token`, `csv`, and `edgebucket` present, Reddit returns `403` for `.json`. The block page explicitly asks for a login or developer credentials. This is not a cookie, User-Agent, `Accept`, DNS, or TLS problem:
   - the same client gets `200` for the HTML challenge;
   - the same cookies get `200` for `.rss`;
   - UA and `Accept` permutations do not change the `403`;
   - `old.reddit.com` sends the same client to `/login/`.

#### What was ruled out

| Hypothesis | Test | Result |
|---|---|---|
| Wrong challenge solution | Logged `solutionLen=32`; script matched the existing `await(async x=>x+x)` regex | Solution was produced; not the failure |
| Cookies not persisted | Pulled the cookie DB and logged `loadForRequest()`; `token_v2` present and sent | Persistence works after the fix |
| Cookies not sent to `.json` | Logged the cookie names loaded for `/r/popular/hot/.json` | `token_v2` was in the request |
| User-Agent block | Chrome desktop/mobile and OkHttp UAs | All `403` |
| `Accept` header block | `application/json` vs `*/*` | All `403` |
| Whole network/IP blocked | Homepage and challenge return `200`; RSS returns `200` | Not a blanket IP block; only anonymous JSON is refused |
| DNS/TLS failure | Host `curl` and emulator challenge both complete | Not a transport failure |
| Old-Reddit scraping still works | `old.reddit.com/r/popular/hot` | Redirects to `/login/?reason=lor2` |
| Teddit fallback works | Default `https://teddit.net/` | TLS handshake failure |
| Multiple launches still needed | Cold start with fixed build | First JSON request carried `token_v2`; relaunch not needed |

#### Why the app showed “Something went wrong” during the investigation

The feed request reached Reddit and received `403 text/html`. Retrofit/Moshi expected JSON, so the response could not be parsed; the ViewModel surfaced its generic error state. There was no client exception in logcat. `CookieGen` succeeding only meant the challenge/cookie step completed; it did not mean the JSON endpoint accepted the session. The final OAuth build no longer takes this path.

#### Interim client changes (reverted)

| File | Interim change | Outcome |
|---|---|---|
| `RedditCookieInitializationInterceptor.kt` | Select `input[name=token], input[name=jsc_token]`; keep the actual name and use it in the challenge URL | Challenge returned `token_v2`, but JSON still `403`; interceptor deleted on 2026-10-22 |
| `RedditCookieInitializationInterceptor.kt` | `hasRequiredCookies()` called `RedditCookieJar.hasCookie("token_v2")` | Reverted with the interceptor |
| `RedditCookieJar.kt` | App-wide `object` with a `CopyOnWriteArrayList` memory cache, merge/dedupe with `CookieManager`, `hasCookie()` | Reverted to pre-investigation `class RedditCookieJar`; remains only on the scraper client |
| `di/NetworkModule.kt` | Both Reddit clients used `.cookieJar(RedditCookieJar)` | Official client no longer uses a cookie jar; scraper keeps `RedditCookieJar()` |

These changes fixed the client-side cookie failure and the relaunch workaround, but could not fix the server-side `403`. They were removed once §3.1.2 worked.

#### Options for restoring the official source

| Option | What it requires | Trade-off |
|---|---|---|
| **App-only OAuth, Redlib-style (chosen)** | Mint an anonymous token from Reddit's Android OAuth endpoint, call `oauth.reddit.com`, encode blocked thread routes | Restores listings/comments/search with no user login; uses Reddit's private Android client ID and spoofed headers, so it is ToS-sensitive and may break |
| Optional Reddit login | Open a WebView login, let the user authenticate, then reuse the resulting `reddit_session` cookie in `RedditCookieJar` | Restores full JSON; requires a Reddit account and a new login UI; changes the account-free premise |
| Registered developer app | Register a Reddit app, obtain a client ID, add a token endpoint/authenticator, call `oauth.reddit.com` | Legitimate app registration path; Reddit has stopped new app creation, so not currently available |
| User-supplied cookies | Add a settings screen to paste `reddit_session`/`token_v2` cookies | No embedded credentials; still requires the user to log in elsewhere; poor UX and security handling |
| RSS-only fallback | Parse `www.reddit.com/.../.rss` for listings | Works anonymously today, but listings only (no comments/search/scores), and Reddit has announced anonymous RSS support ending 2026-11-13 |
| Redlib instance | Point the app at a public/self-hosted Redlib HTML instance | Instance HTML is scrapeable, but every public instance tested was bot-protected/unreachable from this network; self-hosting is required for reliability |
| Switch to Teddit | Use `TEDDIT` with a live instance | No code change, but the default instance is unreachable and the project is unmaintained |

#### Reproduction and verification commands

```bash
# Build (JDK 17 is required for this Gradle/AGP version)
JAVA_HOME=/tmp/opencode/jdk17 PATH=/tmp/opencode/jdk17/bin:$PATH   ./gradlew :app:assembleDebug --no-daemon --console=plain

# Install + cold start
ADB=/mnt/VirtualDisk/Portable-Apps/Tarballs/Android-Sdk/platform-tools/adb
$ADB install -r app/build/outputs/apk/debug/app-debug.apk
$ADB shell pm clear com.rohitss.stealth.dev
$ADB logcat -c
$ADB shell monkey -p com.rohitss.stealth.dev -c android.intent.category.LAUNCHER 1
$ADB logcat -d -v time | grep -E 'CookieGen|CookieInterceptor|CookieDebug'
```

Useful manual checks:

```bash
curl -sS -o /dev/null -w '%{http_code} %{content_type}\n' \
  'https://www.reddit.com/r/popular/hot.json?raw_json=1'
# -> 403 text/html

curl -sS -o /dev/null -w '%{http_code} %{content_type}\n' \
  'https://www.reddit.com/r/popular/hot.rss'
# -> 200 application/atom+xml
```

#### Do not retry without new information

The following were tested and did not change the outcome:

- changing the User-Agent or `Accept` header;
- re-running the challenge or deleting cookies and relaunching;
- retrying the same `.json` request with a fresh anonymous `token_v2`;
- switching to `old.reddit.com` HTML/JSON;
- using the default `teddit.net` instance.

A client-side attempt against the anonymous `.json` path is not worth repeating; the successful approach is the app-only OAuth flow in §3.1.2.

### 3.1.2 OAuth workaround — implemented 2026-10-22

**Status:** working on the emulator. The `REDDIT` source loads listings, post details, comments and “more comments” again, with no user login.

**Where the idea came from.** Redlib's `MobileSpoof` backend mints an anonymous token from Reddit's Android OAuth endpoint and calls `oauth.reddit.com` with Android client headers (`/tmp/opencode/redlib/src/oauth.rs`). Redlib also documents the current thread-page block and the `%2f` workaround in [redlib-org/redlib#591](https://github.com/redlib-org/redlib/issues/591) and [PR #592](https://github.com/redlib-org/redlib/pull/592).

**Token acquisition.**

```http
POST https://www.reddit.com/auth/v2/oauth/access-token/loid
Authorization: Basic base64("ohXpoqrZYub1kg:")
Content-Type: application/json; charset=UTF-8
User-Agent: Reddit/Version 2024.47.0/Build 2029755/Android 14
x-reddit-retry: algo=no-retries
x-reddit-compression: 1
x-reddit-qos: <random 1.000-100.000>
x-reddit-media-codecs: available-codecs=video/avc, video/hevc
client-vendor-id: <persistent UUID>
X-Reddit-Device-Id: <same UUID>

{"scopes":["*","email","pii"]}
```

Observed response: `200 application/json`, `expires_in: 86399`, `scope: ["*","email","pii"]`, plus `x-reddit-loid` and `x-reddit-session` headers that are replayed on API calls. No user credentials are involved.

**Why thread routes needed an extra fix.** With the token, listings, `about`, user pages and search return `200`, but the literal `/r/<sub>/comments/...` and `/api/morechildren` paths still return the HTML network-policy block page. The block is path-based, not token- or header-based. Percent-encoding the slash before `comments` bypasses it:

| Request | Result |
|---|---|
| `oauth.reddit.com/r/popular/hot?raw_json=1` | `200 application/json` |
| `oauth.reddit.com/r/Android/comments/1wzvqsb/.../.json` | `403 text/html`, 189908-byte block page |
| `oauth.reddit.com/r/Android%2fcomments/1wzvqsb/.../.json?raw_json=1` | `200 application/json`, 20419 bytes |
| `oauth.reddit.com/api/morechildren?api_type=json&children=pega7r8&link_id=t3_1wzvqsb` | `403 text/html` |
| `oauth.reddit.com/api%2fmorechildren?api_type=json&children=pega7r8&link_id=t3_1wzvqsb` | `200 application/json`, 9000 bytes |
| `oauth.reddit.com/api/info?id=t3_1wzvqsb` | `403 text/html` |
| `oauth.reddit.com/api%2finfo?id=t3_1wzvqsb` | `200 application/json` |

`/api/morechildren` accepts bare comment IDs (`pega7r8`), not fullnames (`t1_pega7r8`); the app already sends the bare `MoreChild.children` IDs, so no DTO change was needed.

**Implementation.**

| File | Change |
|---|---|
| `data/remote/api/reddit/RedditOAuthManager.kt` | `@Singleton` token manager. Persists a random device UUID in `reddit_oauth` prefs; posts the token request with the headers above; caches `Bearer` token, `x-reddit-loid`, `x-reddit-session`, QoS and expiry in memory; refreshes 120 s before expiry; `invalidate()` forces a fresh token (`:30-137`) |
| `data/remote/api/reddit/RedditOAuthInterceptor.kt` | Adds `Authorization`, `User-Agent`, `x-reddit-*`, `client-vendor-id` and device headers to every official request; on `401`, closes the response, invalidates the token and retries once (`:12-47`) |
| `data/remote/api/reddit/RedditPathInterceptor.kt` | Rewrites `/r/<sub>/comments...` to `/r/<sub>%2fcomments...`; rewrites `/api/morechildren` and `/api/info` to `%2f` forms; leaves everything else unchanged (`:24-41`) |
| `data/remote/api/reddit/RedditApi.kt` | `BASE_URL` is now `https://oauth.reddit.com/`; `BASE_URL_OLD` is still `https://old.reddit.com/` (`:113-114`) |
| `di/NetworkModule.kt` | `@RedditOkHttp` chain is `RawJsonInterceptor` → `JsonInterceptor` → `RedditPathInterceptor` → `RedditOAuthInterceptor`; no cookie jar (`:124-139`) |
| `RedditCookieInitializationInterceptor.kt` | Deleted; the official client no longer performs the browser challenge |
| `RedditCookieJar.kt` | Restored to its pre-investigation form; it remains only on the old-Reddit scraper client (`di/NetworkModule.kt:168-174`) |

**Emulator verification.**

- Cold start after `pm clear`: home feed rendered `r/masseffect` and `r/MadeMeSmile` posts with scores and media; no “Something went wrong”.
- Tapped the first post: post body and comments rendered from the encoded thread route.
- Tapped “More (59)”: additional comments loaded through the encoded `/api%2fmorechildren` route.
- Logcat had no `RedditOAuth` errors, no `403`, no `401`, and no crash.
- Manual endpoint matrix above was checked with the same token; listings, user/about, search, thread JSON and more-children JSON all returned `200`.

**Caveats and limits.**

- The client ID `ohXpoqrZYub1kg` and Android headers are copied from Reddit's own Android client via Redlib. This is not an officially registered app; Reddit's terms and anti-abuse systems may reject or revoke it at any time.
- The token is app-only and anonymous. It is not a user login, so write actions and account-specific endpoints are still unavailable; the app remains read-only.
- Tokens last about 24 hours and are cached only in memory. A cold start performs one token request; a `401` triggers one refresh-and-retry. There is no background refresh or persistent token.
- Reddit rate-limits app-only tokens (Redlib budgets 99 requests per token). One token per install is fine for normal paging; aggressive prefetching could hit the limit.
- The `%2f` trick is a path-normalization bypass. If Reddit normalizes the encoded slash, thread pages break again; the Redlib PR is still open, so this is a known temporary workaround, not a stable API.
- Public Redlib instances are not part of this fix. All seven clearnet instances tested on 2026-10-22 were unreachable or bot-challenged from this network; the app talks to Reddit directly.

**Re-check commands.**

```bash
# Mint a token
TOKEN=$(curl -sS 'https://www.reddit.com/auth/v2/oauth/access-token/loid' \
  -H 'Authorization: Basic b2hYcG9xclpZdWIxa2c6' \
  -H 'Content-Type: application/json; charset=UTF-8' \
  -H 'User-Agent: Reddit/Version 2024.47.0/Build 2029755/Android 14' \
  -H 'client-vendor-id: 00000000-0000-0000-0000-000000000000' \
  -H 'X-Reddit-Device-Id: 00000000-0000-0000-0000-000000000000' \
  -d '{"scopes":["*","email","pii"]}' | jq -r .access_token)

# Listings
curl -sS -o /dev/null -w '%{http_code} %{content_type}\n' \
  -H "Authorization: Bearer $TOKEN" 'https://oauth.reddit.com/r/popular/hot?raw_json=1'

# Thread: literal path is blocked, encoded path works
curl -sS -o /dev/null -w '%{http_code} %{content_type}\n' \
  -H "Authorization: Bearer $TOKEN" \
  'https://oauth.reddit.com/r/Android/comments/1wzvqsb/we_are_kustom_industries_and_smart_launcher_we/.json?raw_json=1'
curl -sS -o /dev/null -w '%{http_code} %{content_type}\n' \
  -H "Authorization: Bearer $TOKEN" \
  'https://oauth.reddit.com/r/Android%2fcomments/1wzvqsb/we_are_kustom_industries_and_smart_launcher_we/.json?raw_json=1'
```

### 3.2 v.reddit audio playback — `bcfe7c2`

**Symptom.** v.reddit videos played without audio.

**Root cause.** Reddit switched from `DASH_*` to `CMAF_*` segment names, but the app only matched `DASH_(\d+)` and always built the audio name `DASH_AUDIO_128`.

**Fix.**

| Before | After |
|---|---|
| `Regex("DASH_(\\d+)")` | `Regex("(DASH|CMAF)_(\\d+)")` (`LinkUtil.kt:16`) |
| `REDDIT_SOUNDTRACK_NAME = "DASH_AUDIO_128"` | `REDDIT_SOUNDTRACK_NAME = "_AUDIO_128"` (`LinkUtil.kt:27`) |
| `link.replace(regex, "DASH_AUDIO_128")` | Match first, read the format group, replace with `"$format_AUDIO_128"` (`LinkUtil.kt:57-63`) |

Playback still uses `MergingMediaSource` (`MediaViewerAdapter.kt:202-207`) and falls back to video-only if the audio URL returns 403/404 (`:248-258`). The download path uses the same audio URL; see 3.3.

**Verification.** `getRedditSoundTrack()` is a pure string function and is the best unit-test target in the current tree. Its contract is: throw on a URL without `DASH_`/`CMAF_`; otherwise preserve the matched prefix.

### 3.3 Downloaded v.reddit audio — `dfa795f`

**Symptom.** Downloaded v.reddit MP4s had video but no audio.

**Root cause.** The worker downloaded only the video URL. The separate `_AUDIO_128` URL was known to the player but never passed through the download pipeline.

**Fix.**

1. `GalleryMedia.Type.AUDIO` added (`data/model/GalleryMedia.kt:19-20`).
2. `MediaViewerFragment` passes `it.sound` to `MediaDownloadWorker.enqueueWork()` (`ui/mediaviewer/MediaViewerFragment.kt:307-312`).
3. Worker reads `KEY_SOUND`, downloads the audio, and muxes on Android Q+ (`MediaDownloadWorker.kt:70-130`).
4. `muxAudioVideo()` creates a pending `video/mp4` MediaStore entry, copies the first video and audio tracks with `MediaExtractor`/`MediaMuxer`, clears `IS_PENDING`, and deletes the two temporary entries (`MediaDownloadWorker.kt:407-496`).

**Limitation.** On API < 29 the mux branch is skipped (`:114-129`), `mergedUri` stays `null`, and `doWork()` reports `Result.failure()` even though both files were written. This is current behavior, not an intended fallback.

**Regression introduced by this change (open).** `DownloadManagerReceiver.onReceive()` now requires `KEY_SOUND` for **every** intent:

```kotlin
val sound = intent.extras?.getString(KEY_SOUND) ?: return   // DownloadManagerReceiver.kt:16
```

But neither `getRetryPendingIntent()` (`:49-66`) nor `getCancelPendingIntent()` (`:68-83`) writes `KEY_SOUND`. Both notification actions therefore return before reaching the `when` block (`:18-27`): retry and cancel do not fire. The worker creates those intents in `getRetryAction()` / `getCancelAction()` (`MediaDownloadWorker.kt:360-373`). A correct fix would make `sound` nullable (`getString(KEY_SOUND)`) and add `KEY_SOUND` to the retry intent so audio is preserved on retry.

### 3.4 Release signing path — `302922c`

**Problem.** Keystore configuration lived in a path that was awkward to keep out of version control.

**Fix.** The release config now reads `../keys/keystore.properties` relative to `app/` (`app/build.gradle.kts:13`), i.e. `keys/keystore.properties` at the repository root. If absent, `storeFile` falls back to `"keystore.jks"` (`:19`) and the empty credentials make a signed release impossible, while debug builds still work. `.gitignore` excludes `keystore.properties`, `*.jks` and `*.keystore` (`:27,48-49`).

### 3.5 kapt on JDK 16+ — `1349c1f`

**Problem.** AGP 7.3.1 / Kotlin 1.7.20 kapt fails on JDK 16+ without access to internal `javac` packages (KT-45545).

**Fix.** Two layers:

- Module-level `kapt { javacOptions { ... } }`: `-Xss4m` plus `-J--add-exports`/`-J--add-opens` for `com.sun.tools.javac.{main,tree,util,code}` (`app/build.gradle.kts:24-36`).
- Gradle daemon JVM args with the same exports/opens plus `-Xmx2048m -Xss4m` (`gradle.properties:10`).

### 3.6 Post shortcut buttons — `b8474ec`

Feature, not a bug fix: adds user/subreddit navigation from post surfaces.

- `BaseFragment.onUserClick()` → `openUser(post.author)`; `onSubredditClick()` → `openSubreddit(post.subreddit.removePrefix("r/"))` (`ui/base/BaseFragment.kt`, `b8474ec` diff).
- Click listeners are wired in `PostAdapter` (details), `PostListAdapter`/`PostViewHolder` (feed) and `ProfileSavedAdapter` (saved posts).
- `include_post_metrics.xml` gains `buttonUser` / `buttonSubreddit`; `ic_profile_post.xml` is the new icon.
- The `r/` prefix is stripped because `PostEntity.subreddit` stores the prefixed form while the navigation argument expects the bare name.

### 3.7 Pre-fork fixes that shaped the current code

These landed upstream but directly explain current functions:

| Commit | Fix | Current artifact |
|---|---|---|
| `b55fb04` | Reddit audio filename changed to `DASH_AUDIO_128` | `LinkUtil.REDDIT_SOUNDTRACK_NAME` history; later generalized by `bcfe7c2` |
| `b72a8f0` | Redgifs links failed because the API stopped returning `vthumbnail` | `data/remote/api/redgifs/model/Urls.kt` no longer declares it |
| `e8ff2ce` | App crashed at launch on Android 14 | `res/drawable/cradle_anim.xml` |
| `eb67886` | Reddit API-policy changes blocked normal use | Policy disclaimer, hard-coded `2023-06-10`, 5 s delay, `policy_disclaimer_shown` (`MainActivity.kt:84-88,239-244`) |

---

## 4. Open Issues and Known Limits

These are observable in the current tree and are **not** fixed by the commits above.

| Issue | Evidence | Impact |
|---|---|---|
| Download notification retry/cancel never fire | `DownloadManagerReceiver.kt:16` requires `KEY_SOUND`; pending intents at `:49-83` omit it; actions built at `MediaDownloadWorker.kt:360-373` | User cannot retry a failed download or cancel from the notification |
| OAuth uses a private Android client ID and spoofed headers | `RedditOAuthManager.kt:125-136`; `RedditOAuthInterceptor.kt:33-46` | Reddit can reject or revoke the client at any time; ToS/anti-abuse risk |
| Thread routes rely on a path-encoding bypass | `RedditPathInterceptor.kt:24-41`; [redlib#591](https://github.com/redlib-org/redlib/issues/591) | If Reddit normalizes `%2f`, comments and “more comments” return `403` again |
| OAuth token is memory-only and refreshed lazily | `RedditOAuthManager.kt:39-64`; `RedditOAuthInterceptor.kt:12-27` | Each cold start mints a token; no background refresh; expiry during idle is handled only on the next request |
| Reddit still blocks anonymous `.json` | Live `403` on `/r/popular/hot/.json`; `200` via app-only OAuth token on `oauth.reddit.com` | The workaround depends on the private token flow, not on a public API |
| Scraper client never bootstraps cookies | `di/NetworkModule.kt:168-174` attaches `RedditCookieJar()` only | `old.reddit.com` scraping still hits the login wall |
| Download mux unavailable below API 29 | `MediaDownloadWorker.kt:114-129` | Download reports failure even though separate video/audio files exist |
| Teddit and scraped sources are incomplete | `TedditSource.kt:43,74,83`; `RedditScrapingSource.kt:58-60,78-80,113-124`; `CurrentSource.kt:59-62,86-94,96-104` | Three operations are hard-wired to the official source; in-subreddit search is empty when scraping |
| Teddit host changes require a restart | `@Singleton` `provideTedditApi()` reads the URL once (`di/NetworkModule.kt:205-229`); restart dialog (`ui/preferences/PreferencesFragment.kt:199-206,319-345`) | Instance change does not take effect until process restart |
| Redgifs bearer token never refreshes | `data/remote/api/redgifs/RedgifsToken.kt:9-21` | Once expired, Redgifs requests keep sending the stale token for the process lifetime |
| NSFW filtering is client-side | `util/PostUtil.kt:30-32`; official requests send `include_over_18=1`; `RedditCookieJar` forces `over18=1` only on the scraper client | NSFW data is downloaded then discarded |
| Blocking DataStore reads on hot paths | `UnredditApplication.kt:44-47`, `CurrentSource.kt:27-30`, `LinkRedirector.kt:25-32`, `NetworkModule.kt:212-219`, `MediaViewerFragment.kt:173-174` | Startup/first use can block the calling thread |
| No CI and minimal tests | No CI files; only `ExampleUnitTest`, `ExampleInstrumentedTest`, `DateUtilTest` | Regressions such as 3.3 are not caught automatically |
| Version/changelog mismatch | `Config.kt:10-11` is `30`/`3.0.0`; newest changelog is `23.txt` | Store metadata does not describe the current release |

A fully annotated list of data-layer caveats also lives in [04-Data-Layer.md §Data-Layer Caveats](04-Data-Layer.md#8-data-layer-caveats).

---

## 5. How These Claims Were Verified

The evolution and fix claims can be re-checked without reading the whole tree:

```bash
# Fork commits and merge
git log --oneline 2.3.1..v3.0.0
git show --stat 59295de 45fc13e bcfe7c2 dfa795f 302922c 1349c1f e216456 b8474ec

# Cookie fix before/after
git show 45fc13e -- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit
git show 45fc13e -- app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt

# CMAF fix
git show bcfe7c2 -- app/src/main/java/com/cosmos/unreddit/util/LinkUtil.kt

# Download/mux fix
git show dfa795f --stat
git show dfa795f -- app/src/main/java/com/cosmos/unreddit/data/worker/MediaDownloadWorker.kt

# Current Reddit wiring
grep -n "RedditPathInterceptor\|RedditOAuthInterceptor\|cookieJar" \
  app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt
grep -n "BASE_URL" app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditApi.kt
grep -n "CLIENT_ID\|TOKEN_URL\|Bearer" \
  app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditOAuthManager.kt \
  app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditOAuthInterceptor.kt

# Download notification paths
grep -n "KEY_SOUND\|getRetryPendingIntent\|getCancelPendingIntent" \
  app/src/main/java/com/cosmos/unreddit/data/receiver/DownloadManagerReceiver.kt \
  app/src/main/java/com/cosmos/unreddit/data/worker/MediaDownloadWorker.kt
```

---

## 6. Document Map

| Question | Document |
|---|---|
| What does the app do and how does a request flow end to end? | [01-Project-Overview.md](01-Project-Overview.md) |
| How is it built, signed and released? | [02-Build-System.md](02-Build-System.md) |
| Which classes exist in which package? | [03-Source-Structure.md](03-Source-Structure.md) |
| How are rooms, preferences, APIs, sources and downloads wired? | [04-Data-Layer.md](04-Data-Layer.md) |
| Which Hilt modules and interceptors are used? | [05-Dependency-Injection.md](05-Dependency-Injection.md) |
| How do navigation, screens and the media player work? | [06-UI-Layer.md](06-UI-Layer.md) |
| What is tested and what is not? | [07-Testing.md](07-Testing.md) |
