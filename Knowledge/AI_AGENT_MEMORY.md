# STEALTH-APP — Agent Memory

## Goal (one line)
Make the app reliably load real Reddit content through a Redlib HTML-scraping source, including instance selection/verification and clear status on the home screen.

## Current status — what is being worked on RIGHT NOW
Session 8: **v3.0.2 shipped + preferences synced with Redlib.** Version bumped in `buildSrc/src/main/kotlin/Config.kt` (commit `8243e83`) with short release notes (Redlib default source, instance verification/management, home-screen instance label, custom UA, media fix, cleanup). Resolved a user-reported issue where the feed under the Redlib source would hang/crash: content preferences now sync with Redlib — a matching setting is sent to Redlib so its output matches the app's, with a source-aware summary and a short confirmation dialog when the source is Redlib. Preference rows keep the fixed 64dp card; the summary fits one line. Committed as `42e9b25`. Working tree clean.

Session 7: **Dead-code cleanup done.** Commit `ecd4e0f`. Removed 157 lines across 8 files; `testDebugUnitTest` + `assembleDebug` green.

Session 6: **Home app bar shows the active Redlib instance.** Commit `1c4120d`. The subtitle below "STEALTH" displays the saved instance (e.g. `reddit.nerdvpn.de`), hides for non-Redlib sources, and falls back to `TedditApi.DEFAULT_INSTANCE`. Verified on `emulator-5554`.

## Session 7 — dead-code cleanup
### Removed (no references in Kotlin, XML, navigation or Hilt)
- `data/remote/TargetRedditInterceptor.kt`
- `data/model/CommentType.kt`
- `util/PagerHelper.kt`
- `util/RedditUri.kt`
- `data/remote/api/reddit/scraper/Over18Scraper.kt`
- `data/remote/api/reddit/model/TedditUser.kt`
- `PostViewHolder.PollPostViewHolder` (empty nested class)
- `RedditCookieJar.cookies` (unused field shadowed by the local list)

### Deliberately kept
- `RedditApi.consentOver18` (0 callers, but documents the endpoint), unused `RedlibInstance.onion/i2p` + `RedlibInstances.updated` fields (schema documentation; Moshi ignores them anyway).
- Fragments/widgets/Hilt modules that a naive scan flags: they are used from navigation XML, layout XML or annotation processing.

### JS-challenge findings (probed live)
- `reddit.nerdvpn.de` "Gandalf" is **hCaptcha**, not a PoW: `/.gandalf/check` serves an `h-captcha` widget + `h-captcha-response`; `/.gandalf/confirm` is POST-only and a GET returns **403**. Obfuscated JS + AES-GCM config fetched from `v4/v6.gandalf.nerdvpn.de`.
- `redlib.catsarch.com` Anubis v1.28.2 currently returns a **hard deny** (`anubis_challenge: null`, "Access Denied: error code …"), not a solvable challenge.
- The emulator's `app_webview/Default/Cookies` DB had **0 cookie rows**, so no challenge credential was persisted at inspection time.
- Cookie reuse remains the right model for Cloudflare only; JS/CAPTCHA gates need a WebView-side fetch or a storage bridge.

## Session 6 — instance indicator + custom-instance findings
### Finished
- `layout_appbar.xml`: title + new `instance` subtitle in a vertical `LinearLayout`; title still scrolls to top on click.
- `PostListViewModel.redlibInstance` combines `getRedditSource()` + `getRedditSourceInstance()`; returns the instance for `TEDDIT`, null otherwise.
- `TedditApi.DEFAULT_INSTANCE` centralises the fallback host (`redlib.freedit.eu`) used by both home and preferences.
- Verified visually: app bar shows `STEALTH` with `reddit.nerdvpn.de` underneath.

### Important finding — verification vs. app requests
- `instances.json` is nearly empty (3 clearnet), while `instances.txt` has ~50 clearnet entries. Custom hosts can be typed into the dialog, but are not listed.
- `reddit.nerdvpn.de` verification succeeds in the WebView, but OkHttp receives **HTTP 307 → `/.gandalf/check`**. Refined in Session 7: that gate is **hCaptcha**, not a PoW. `redlib.catsarch.com` currently answers **HTTP 429** with an Anubis reject page (`anubis_challenge: null`).
- Cloudflare cookie reuse works, but JS/CAPTCHA gates cannot be satisfied by OkHttp. Cookie reuse only helps when the gate actually writes a cookie that `CookieManager` persists.
- Search on these instances has not been confirmed in-app because the feed request itself is blocked.

## Previous status
Session 5: **Redlib follow-ups done.** Committed as `925abf6`.

## Session 5 — follow-up requests
### Finished
- Added `RedlibInstances`/`RedlibInstance` Moshi models and `RedlibInstancesRepository`: downloads `https://raw.githubusercontent.com/redlib-org/redlib-instances/main/instances.json` only when the SharedPreferences cache (`redlib_instances`) is empty or a refresh is forced; filters out `.onion`/`.i2p` hosts; caches newline-separated hostnames.
- `PreferencesViewModel` now injects the repository: `redlibInstances` starts from the cache (falling back to the single `TedditApi.BASE_URL` host), auto-downloads only when the cache is empty, and `refreshRedlibInstances(): Boolean` forces a download.
- Source dialog: new "Refresh instance list" button → `KEY_REFRESH` fragment result → `PreferencesFragment` refreshes, shows a failure toast if needed, and reopens the dialog. Refresh/verify buttons are hidden for non-Redlib sources. Extracted `selectedSource()`.
- Removed the hardcoded list from `PreferencesViewModel`; only `TedditApi.BASE_URL` (`redlib.freedit.eu`) remains as a last-resort fallback before the first download.
- User-Agent: new `custom_user_agent` DataStore key + `EditTextPreference` (blank = "Default (WebView)"). `BrowserUserAgent.resolve(context, customUserAgent)` is shared by `RedlibVerificationActivity` and OkHttp. `BrowserLikeInterceptor` now takes a `() -> String` provider evaluated per request, so UA changes apply without an app restart; `Sec-CH-UA` is derived from the UA's Chrome major version.
- `network_retry_message` now reads: "Something went wrong.\nIf this keeps happening, set Reddit Source to Redlib and verify the instance in Settings." `view_info_bar.xml` message is `0dp`/end-constrained so it wraps instead of overlapping RETRY.
- Emulator verification: cached list = `redlib.freedit.eu`, `redlib.cow.rip`, `redlib.privadency.com`; dropdown shows all 3; refresh dismisses + reopens the dialog; custom UA saves/clears with the correct summary; offline error hint wraps correctly; feed still loads after re-enabling network.
- `./gradlew testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**. `lintDebug` still fails only on pre-existing `MissingTranslation` errors (new user-facing strings are English-only).

### Note
- Changing the custom UA invalidates `cf_clearance` (bound to IP+UA); the preference dialog explicitly tells the user to verify the Redlib instance again.
- Search remains a Redlib server bug (see Session 4).

## Tasks
### Finished
- Read brief, Knowledge base, sources, scrapers, models, DI, preferences.
- Baseline build green (JDK 17). Confirmed default `REDDIT` source shows "Something went wrong" on emulator.
- Recovered prior uncommitted edits from OpenCode DB into `/tmp/opencode/prev_edits.txt`.
- Recovered exact Redlib HTML markup from upstream templates into `/tmp/opencode/redlib_templates/` + `/tmp/opencode/redlib_utils.rs`, `/tmp/opencode/redlib_main.rs`.
- Applied config edits: `strings.xml` Teddit→Redlib (label + disclaimer), default source `TEDDIT`, `PreferencesRepository.getRedditSource` default `TEDDIT`, `redlibInstances` in `PreferencesViewModel`, `PreferencesFragment` uses it, `TedditApi.BASE_URL = https://redlib.freedit.eu/`.
- Added `@RedlibBaseUrl` DI qualifier/provider (`NetworkModule`) returning an `HttpUrl`; injects into `TedditSource` for absolute media/permalink URL resolution. Legacy `teddit.net` instance values fall back to the default.
- Wired user-restored `BrowserLikeInterceptor` into the Teddit OkHttp client (and removed obsolete `RawJsonInterceptor`/`TargetRedditInterceptor` from it; `TargetRedditInterceptor` was later deleted entirely in Session 7).
- Rewrote `TedditApi` to return `ResponseBody` and use Redlib HTML routes: `/r/{sub}/{sort}`, `/r/{sub}`, `/r/{sub}/search`, `{permalink}`, `/user/{user}/submitted`, `/user/{user}/comments`, `/user/{user}`, `/search?type=link`, `/search?type=sr_user`.
- Added `RedlibScraper` base (relative URL resolution, Redlib timestamp parsing incl. `"Nov 11 2026, 06:33:12 UTC"` and ISO fallback, score/comment-count parsing, `after=` pagination).
- Added scrapers: `RedlibPostScraper` (list + detail), `RedlibCommentScraper` (nested tree), `RedlibUserCommentScraper`, `RedlibSubredditScraper`, `RedlibUserScraper`, `RedlibSubredditSearchScraper`.
- Rewrote `TedditSource` to scrape all endpoints; normalises sorts per Redlib route; `getMoreChildren` returns empty; `searchUser` returns empty (Redlib has no user search).
- Changed `CurrentSource.getMoreChildren`/`searchPost`/`searchUser` to route to the selected source.
- Added `app/src/test/java/com/cosmos/unreddit/RedlibScraperTest.kt` (synthetic Redlib HTML for list/detail/comments/subreddit/search/user comments). All 5 tests pass.
- `JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 ./gradlew testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL**.
- Installed APK on `emulator-5554`; app launches, no fatal exceptions.

- Fixed instance dropdown: `RedditSourceDialogFragment` now uses a non-filtering `ArrayAdapter` (custom `Filter` returns the full list), passes `ArrayList(instances)` to the bundle, and shows all 3 instances.
- Added `RedlibVerificationActivity` (WebView with OkHttp's exact UA) + `activity_redlib_verification.xml` + manifest entry + "Verify instance in browser" button (`verify_instance`) wired in the dialog; cookies flow via `CookieManager`/`RedditCookieJar`.

- Guarded `RedditCookieJar.loadForRequest` against unparseable cookie fragments (WebView cookies) instead of `!!` NPE.
- Emulator UI verification: instance dropdown lists `redlib.freedit.eu`, `redlib.cow.rip`, `redlib.privadency.com`; “Verify instance in browser” opens the WebView activity. On `redlib.freedit.eu` the full Cloudflare Turnstile → Anubis PoW chain now completes inside the WebView; after **Done**, the app feed loads from Redlib. Confirmed visually: feed, images, post detail + nested comments, correct `1.1k` comment count.

- **Root cause of the verification loop (fixed):** the WebView claimed desktop Chrome/126 while the engine (WebView 156) reported Android through `navigator.userAgentData`. Cloudflare Turnstile looped forever. Fixed by using the real WebView UA (sanitised: remove `; wv`, `Version/4.0`, collapse double spaces) for BOTH WebView and OkHttp, and by dropping desktop `Sec-CH-UA*` headers for mobile UAs. New `BrowserUserAgent` object; `BrowserLikeInterceptor(userAgent)`; `NetworkModule.provideTedditOkHttpClient(@ApplicationContext)`.
- **Root cause of Anubis failure (fixed):** after Cloudflare passed, Anubis' own worker script `/.within.website/x/cmd/anubis/static/js/worker/sha256-*.mjs` returned **HTTP 403** in the WebView, so Anubis showed “Failed to calculate challenge: undefined”. Fixed with `WebViewClient.shouldInterceptRequest` serving a bundled copy (`app/src/main/assets/anubis/sha256-webcrypto.mjs`, upstream Anubis 1.24 worker) for any `.../js/worker/sha256-*.mjs` request. The challenge JSON + server-side validation remain remote. Verification then completes and cookies land in `CookieManager`.
- **Images (fixed):** Coil now uses the `@TedditOkHttp` client via `UnredditApplication.newImageLoader().callFactory(...)`, so Redlib-proxied media gets the browser UA + challenge cookies. Placeholders disappeared.
- **Comment count (fixed):** Redlib detail header renders abbreviated counts (“3.1k comments”); `RedlibScraper.toCommentCount()` now parses k/m suffixes. Unit test updated (`3.1k` → 3100). Verified on emulator: detail header shows `1.1k` (was `1`).
- Removed temporary diagnostics (console/error logging in the activity, `RedlibNet` logging interceptor). Final build: `JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 ./gradlew testDebugUnitTest assembleDebug` → **BUILD SUCCESSFUL** (all 5 Redlib tests pass).

### Session 4 — pending verification
- Cleared app data (`pm clear`), re-ran Settings → Reddit Source → Redlib → Verify; Cloudflare + Anubis complete and the feed loads again.
- **Subreddit info page: works.** `r/memes` header shows icon, title, `/r/Memes the original since 2008`, `36.2m Subscribers`, posts, pagination. `Active` shows `0` because Redlib reports it as 0 (server-side).
- **User profile: works structurally.** Submitted tab lists posts; Comments tab lists comments. Two server-side gaps found by dumping the live HTML:
  - The user panel is empty on freedit: `<p id="user_name">u/</p>`, karma `0`, no created date → app shows `0 / 0 / 11/11/26` (created defaults to now). Selector is correct; Redlib has no data.
  - Every comment body is literally `<p class="comment_body"></p>` in the response, so bodies render empty. Selector is correct.
- **Fixed (app-side):** user comment subtitle previously rendered `r/memes - u/null`. `UserCommentsAdapter` now shows only the subreddit when `linkAuthor` is unknown. **Important:** must call `binding.executePendingBindings()` before overriding the text, otherwise the data-binding expression overwrites it at draw time.
- **Search: broken server-side.** `/search?q=…&type=link` returns Redlib's own error page (“Failed to parse page JSON data … /search.json?q=…&raw_json=1”) with HTTP 404 → app shows “Something went wrong”. Reproduced by the user on other instances and in a mobile browser. `searchSubreddit`/`searchUser` are equally affected; nothing to fix in the scraper.
- Debug technique that worked: temporarily log `document.outerHtml()` in the scraper in 1500-char chunks (`Log.d(tag, chunk)`), pull with `adb logcat -d -v brief | grep -a <tag>`, strip the `D/<tag>(pid): ` prefix per line and join with newlines to reconstruct the HTML. All temporary logging removed afterwards.

### Ongoing
- Instance subtitle and dead-code cleanup are done and committed.
- Next up: the `instances.txt` decision. JS/CAPTCHA gates are understood — see Session 7; do not invest further in cookie reuse for them.

### Remaining (next steps, in order)
1. **Triage the user-reported issue** on v3.0.2 (need the report: steps, source/instance, device/WebView version, logcat).
2. Decide whether to parse `instances.txt` (CSV, ~50 clearnet) in addition to / instead of `instances.json` (3 clearnet) so working instances are selectable from the dropdown.
3. Optional: translate the new strings (de/es) to clear `MissingTranslation` lint errors.
4. If custom JS-gated instances are still required: prototype a WebView-side `fetch` transport or a `localStorage` bridge (see Session 7). Not recommended for `nerdvpn`/`catsarch`.

### Done
- ~~Deferred cleanup~~ — `ecd4e0f`.
- ~~JS-challenge investigation~~ — findings recorded in Session 7.

## Findings so far (source behavior, HTML structure, gotchas, version notes)

### Repo state when this work began (historical)
- The original baseline HEAD `cb19d7a` did not contain the brief's claimed redlib edits (base URL, instance list, logging interceptor). Treated current source as ground truth.
- Prior recovered edits include logging interceptor + logging dependency; brief says remove before commit — **not reintroduced** (kept out).
- Actual HEAD has app-only OAuth workaround (`RedditOAuthManager`, `RedditOAuthInterceptor`, `RedditPathInterceptor`) and `RedditApi.BASE_URL = https://oauth.reddit.com/`; untouched.

### Redlib HTML (verified against upstream templates, and now live on freedit)
- Listing `div.post[id]`; header `.post_subreddit`, `.post_author`, `.created[title]`; title `h2.post_title > a:not(.post_flair)`; flair `a.post_flair`.
- Media: image `div.post_media_content a.post_media_image[href]`; video `div.post_media_content video[src]` + `poster`; gallery `div.gallery figure a[href]`.
- Link posts: `a.post_thumbnail[href]` (external URL) in list; `a#post_url[href]` on detail.
- Score `div.post_score[title]`; comments `a.post_comments[title]` (list) or `#comment_count` (detail).
- Detail page: `div.post.highlighted`, `h1.post_title`, `div.post_body`; permalink in `div.post_footer a[href*="/comments/"]`.
- Comments: `div.thread > div.comment`; author `.comment_author`, score `.comment_score[title]`, `a.created[title]`, body `.comment_body`, replies direct children of `blockquote.replies`; `a.deeper_replies` unsupported.
- User page: `#user_name`, `#user_icon`, `#user_details > div` (karma, created), `div.comment.user-comment` for comments listing, `a.comment_link`, `a.comment_subreddit`.
- Subreddit page: `#sub_title`, `#sub_name`, `#sub_icon`, `#sub_description`, `#sub_details > div` (members, active), `#sidebar_contents`.
- Search: subreddit results `a.search_subreddit[href^="/r/"]` with `.search_subreddit_name`, `.search_subreddit_members[title]`, `.search_subreddit_description`.
- Pagination: footer `a[href*="after="]` (relative query URL; must resolve against base before parsing).
- Live feedback: post detail comment count renders abbreviated text (`1.1k comments`), hence the k/m parsing fix. Media URLs resolve against the Redlib base for proxied images, so Coil must use the Redlib client (browser UA + cookies).

### Cloudflare + Anubis / verification (session 3)
- freedit is behind **both** Cloudflare (Turnstile) and Anubis (PoW, `.within.website/...`, version 1.24.0 at time of test).
- Turnstile in WebView loops if the UA is inconsistent with the engine: never send a desktop UA from an Android WebView. Use `WebSettings.getDefaultUserAgent(context)` sanitised (`BrowserUserAgent.fromWebView`).
- OkHttp and the WebView **must send the exact same UA**, because `cf_clearance` is bound to UA+IP. `@TedditOkHttp` now builds `BrowserLikeInterceptor(BrowserUserAgent.fromWebView(context))`.
- Anubis' worker script is blocked by Cloudflare for WebView subresource requests (HTTP 403) → serve `assets/anubis/sha256-webcrypto.mjs` locally via `shouldInterceptRequest`. Only that script is local; challenge and validation stay server-side.
- Useful adb diagnostics: `adb logcat -d -v brief | grep -aE "RedlibWebView|RedlibNet"` (console + HTTP status). Temporary logging was removed after the fix.
- Emulator WebView version was 156.0.8078.47; Chrome (real app) loaded freedit fine once its own cookies existed.

### Custom instances (session 6, refined in session 7)
- `https://raw.githubusercontent.com/redlib-org/redlib-instances/main/instances.json` currently lists only **3 clearnet** hosts + 1 onion, while `instances.txt` has **~50 clearnet** entries. The dialog accepts free-typed hosts even if they are not listed.
- `reddit.nerdvpn.de` (UA/DE, SFW-only): OkHttp gets `307` → `/.gandalf/check?gandalf_id=…&gandalf_redir=…`. The check page is **hCaptcha** (`h-captcha` widget, `h-captcha-response`, POST-only `/.gandalf/confirm`, GET → 403) with obfuscated JS + an AES-GCM config from `v4/v6.gandalf.nerdvpn.de`. OkHttp cannot pass it; cookie reuse only helps if the WebView fully solves the captcha *and* the pass is stored as a cookie.
- `redlib.catsarch.com` (US): Anubis **v1.28.2** currently returns a **hard deny** (`anubis_challenge: null`, “Access Denied: error code …”), not a PoW challenge — nothing client-side can fix it.
- Emulator `app_webview/Default/Cookies` had **0 rows** at inspection time → no challenge credential was persisted.
- The home app bar subtitle shows the saved instance (verified: `reddit.nerdvpn.de`).

### Source routing (`CurrentSource.kt`)
- All methods now delegate to the selected source.
- `setRedditSource` swaps the delegate under a `Mutex`; initial selection uses `runBlocking { preferences.first() }`.
- Default `reddit_source` is now `TEDDIT` (1) via repository default; `DataPreferences.RedditSource.fromValue` still defaults unknown values to `REDDIT`.

### Build/toolchain
- JDK 17 required (`/home/rohit_surwase/.jdks/jbr-17.0.14`); system Java 25 fails.
- Transient `StackOverflowError` in `hiltJavaCompileDebug` can occur after daemon restart; rerun is green.
- Emulator `emulator-5554`; app `com.rohitss.stealth.dev`, install with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Decisions made (what + why)
- Option B chosen: scrape Redlib HTML (JSON path is dead, reddit.com blocks aggressively).
- Keep `TedditSource`/`TedditApi` names but UI label is **Redlib** (minimal churn).
- Separate `scraper.redlib` package; old old.reddit scrapers untouched.
- `getMoreChildren` intentionally returns empty (no Redlib equivalent); comment expansion is out of scope per brief.
- Dead-code cleanup was deferred to the final phase and completed in Session 7 (`ecd4e0f`).
- `searchUser` returns empty because Redlib does not implement user search.

## File map (paths changed / added)
Added:
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/redlib/RedlibInstances.kt` (@JsonClass models for the online instance list)
- `app/src/main/java/com/cosmos/unreddit/data/repository/RedlibInstancesRepository.kt` (cached download, clearnet filter, manual refresh)
- `app/src/main/java/com/cosmos/unreddit/data/remote/BrowserUserAgent.kt`
- `app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedlibVerificationActivity.kt`
- `app/src/main/res/layout/activity_redlib_verification.xml`
- `app/src/main/assets/anubis/sha256-webcrypto.mjs` (upstream Anubis worker copy)
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibScraper.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibPostScraper.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibCommentScraper.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibUserCommentScraper.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibSubredditScraper.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibUserScraper.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibSubredditSearchScraper.kt`
- `app/src/test/java/com/cosmos/unreddit/RedlibScraperTest.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/BrowserLikeInterceptor.kt` (user-restored, now wired)
- `Knowledge/AI_AGENT_MEMORY.md`, `Knowledge/AI_AGENT_PROMPT.md` (tracked; moved here and committed in `3d96b98`)

Deleted (Session 7, `ecd4e0f`):
- `data/remote/TargetRedditInterceptor.kt`
- `data/model/CommentType.kt`
- `util/PagerHelper.kt`
- `util/RedditUri.kt`
- `data/remote/api/reddit/scraper/Over18Scraper.kt`
- `data/remote/api/reddit/model/TedditUser.kt`
- `PostViewHolder.PollPostViewHolder` (empty nested class)
- `RedditCookieJar.cookies` (unused field)

Modified:
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/TedditApi.kt` (HTML routes + `DEFAULT_INSTANCE`)- `app/src/main/res/layout/layout_appbar.xml` (title container + `instance` subtitle)
- `app/src/main/java/com/cosmos/unreddit/ui/postlist/PostListViewModel.kt` (`redlibInstance` flow)
- `app/src/main/java/com/cosmos/unreddit/ui/postlist/PostListFragment.kt` (bind instance subtitle)
- `app/src/main/java/com/cosmos/unreddit/ui/preferences/PreferencesViewModel.kt` (uses `TedditApi.DEFAULT_INSTANCE`)
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/TedditSource.kt`
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/CurrentSource.kt`
- `app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt`
- `app/src/main/java/com/cosmos/unreddit/data/repository/PreferencesRepository.kt`
- `app/src/main/java/com/cosmos/unreddit/ui/preferences/PreferencesViewModel.kt`
- `app/src/main/java/com/cosmos/unreddit/ui/preferences/PreferencesFragment.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/layout/fragment_reddit_source.xml` (instance dropdown + verify button)
- `app/src/main/AndroidManifest.xml` (`RedlibVerificationActivity`)
- `app/src/main/java/com/cosmos/unreddit/ui/redditsource/RedditSourceDialogFragment.kt` (non-filtering adapter + verify wiring)
- `app/src/main/java/com/cosmos/unreddit/UnredditApplication.kt` (Coil `callFactory`)
- `app/src/main/java/com/cosmos/unreddit/ui/user/UserCommentsAdapter.kt` (hide unknown `linkAuthor`)

## Plan (ranked by complexity)
Redlib content loading is verified on the emulator for `redlib.freedit.eu` (feed, post/comment, subreddit info, user profile). The home app bar shows the active instance. Open items:

1. **JS/CAPTCHA instances** — findings complete (Session 7): `reddit.nerdvpn.de` is an hCaptcha gate, `redlib.catsarch.com` is a hard Anubis deny. Cookie reuse cannot fix either; only a WebView-side fetch/storage bridge could, and it is not recommended.
2. **Instance source** — `instances.json` exposes only 3 clearnet hosts; `instances.txt` has ~50. Consider parsing the CSV/merging both so custom working instances appear in the dropdown.
3. ~~Deferred cleanup~~ — done in `ecd4e0f`.
4. **Translations** — new strings are English-only; `lintDebug` still fails on pre-existing `MissingTranslation` errors.
5. **Search** — broken server-side on `freedit`; not yet confirmed in-app on the two custom instances because their feed requests are blocked.

### Commit log
- `8243e83` — Updated to v3.0.2 (version bump in `buildSrc/src/main/kotlin/Config.kt`).
- `3d96b98` — Updated Knowledge section (moved `AI_AGENT_MEMORY.md` / `AI_AGENT_PROMPT.md` under `Knowledge/`).
- `ecd4e0f` — Remove unused legacy classes and interceptor.
- `1c4120d` — Show the active Redlib instance in the home app bar.
- `925abf6` — Add dynamic Redlib instances and configurable User-Agent.
- `74203ef` — Add Redlib HTML scraping source with browser challenge support.
- The agent docs live in `Knowledge/` and are tracked like normal project docs.

## Blockers / open questions
- **User-reported issue (v3.0.2) — resolved (commit `42e9b25`).** The feed under the Redlib source would hang/crash; fixed by syncing the relevant content preference with Redlib so its output matches the app's (a matching setting is sent to Redlib, with a source-aware summary and a short confirmation dialog when the source is Redlib). See Session 8.
- In-app verification path: Preferences → Reddit Source → instance → **Verify instance in browser** → complete the challenge → **Done** → **Save** (restart when prompted). Cookies are stored by `CookieManager` and reused by `RedditCookieJar` on the `@TedditOkHttp` client (same UA as the WebView).
- If the user's existing install already persisted `reddit_source`, they must select “Redlib” in Settings once (fresh installs default to Redlib).
- **JS/CAPTCHA gates are not reusable by OkHttp.** `reddit.nerdvpn.de` answers OkHttp with **307 → `/.gandalf/check`**, an **hCaptcha** gate (`/confirm` is POST-only; GET → 403). `redlib.catsarch.com` returns an Anubis **hard deny** (`anubis_challenge: null`). Cloudflare's `cf_clearance` reuse does work.
- The emulator's WebView cookie DB had **0 rows** when inspected, so no gate credential persisted; re-check only after a gate actually sets a cookie.
- **Search is a Redlib server bug on `freedit`** (not ours): `/search` fails while building `/search.json` and answers HTTP 404; reproduced in a mobile browser and on other instances. Search on `nerdvpn`/`catsarch` is expected to work but has not been confirmed in-app because the feed request is blocked.
- `cow.rip` was Cloudflare-403 from the agent network; `privadency` was Anubis-200. Only `freedit` has been verified end-to-end for feed + content.
- Untested/less-verified: refresh-failure toast, full verify flow on a second instance after changing the custom UA, and direct on-wire confirmation of a changed UA.
- On `freedit`, the user "about" panel and comment bodies are returned empty by the server; karma/created/cake-day and comment text therefore show defaults/blank. Subreddit `Active` is also reported as `0`.
