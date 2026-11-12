# Stealth-App — Redlib Media Playback & Inline Verification (Consolidated Guide)

**Date:** Nov 12–13 2026
**Working dir:** `/mnt/VirtualDisk/Android-Projects/Stealth-App`
**App package:** `com.rohitss.stealth` (Kotlin package `com.cosmos.unreddit`)
**Emulator:** `emulator-5554` (Pixel-9a_API-36_Android-16), display 1080×2424 → adb input taps must be full-res

This file consolidates the former `08-Media-Playback-Investigation.md` and
`09-Redlib-HLS-403-Handoff.md` into a single reference. All items are RESOLVED and
verified on-device; it is kept as context for future regressions or new challenge variants.

---

## 1. GOAL

1. Redlib video posts must play in the app's ExoPlayer media viewer (not just the WebView).
2. Redlib's Cloudflare/Anubis bot challenge must be solved **inside the app** — feed and
   HLS media both — without sending the user to an external browser.

## 2. FINAL STATUS — ALL RESOLVED (verified on-device)

| Issue | Root cause | Fix |
|---|---|---|
| Video post opens blank + "Something went wrong" | `RedlibPostScraper` read `video.src`, but redlib puts the HLS URL on a `<source>` child | Extract m3u8 from `<source>` (m3u8 preferred → any non-empty `<source>` → `video.src`) |
| ExoPlayer gets 403 on `/hls/...` | Challenge is **request-fingerprint-bound** (UA + browser headers + `cf_clearance`), NOT TLS/JA3-bound | New `OkHttpDataSource` routes playlist + every segment through the browser-like `@TedditOkHttp` client |
| Playlist "End of stream" parse error | `OkHttpDataSource.read()` threw `EOFException` at normal stream end | Return `C.RESULT_END_OF_INPUT` per the ExoPlayer 2.x `DataSource` contract |
| `TimestampAdjuster` NPE during fMP4 chunk load | Reddit CMAF HLS addresses one `.mp4` with **byte ranges**; the DataSource ignored `DataSpec.position/length` and fetched the whole file | Send HTTP `Range` header from `DataSpec.position`/`length` in `open()` |
| Feed / post request hit by challenge (403, 429, or HTTP-200 challenge page) | Retrofit threw `HttpException` before the scraper could inspect the body; challenge pages can also parse as empty listings | `RedlibChallenge.isChallenge(code, body)` (status-based + body-marker scan) → `RedlibBlockedException` → inline verification overlay |
| Search returns error on some instances | Instance-side: those redlib instances don't serve `/search` (404 even when authenticated); user search unsupported everywhere | Friendly message "This instance doesn't support search — try another instance." |
| Challenge recovery took ~50 s of silent retries | ExoPlayer's HLS playlist tracker retries load errors silently | `RedlibLoadErrorPolicy`: no retries on 403/429 |

**Crash fix:** `LinkUtil.getRedditSoundTrack()` now returns `String?` (was `error(...)` on
non-reddit video URLs).

## 3. HOW THE CHALLENGE FLOW WORKS (the full chain)

1. **Detection** — feed/post/search requests whose response is a challenge page (Cloudflare
   "Just a moment…", Anubis/within.website markers) or 403/429/empty-200 throw
   `RedlibBlockedException` carrying the exact listing URL (Cloudflare challenges can be
   path-specific).
2. **Inline overlay** — `PostListFragment` opens `RedlibVerificationFragment` (WebView overlay
   over the feed, hosted in `fragment_post.xml`'s `verification_container`), loading the exact
   blocked path. `RedlibWebView` provides shared config: WebView-identical UA (`BrowserUserAgent`),
   cookie enablement, and locally-served Anubis proof-of-work worker.
3. **Clearance cookie** — solving the challenge sets `cf_clearance` into the global
   `android.webkit.CookieManager`; the user taps DONE → cookie flush → fragment result.
4. **Reuse** — `RedditCookieJar` attaches `cf_clearance` to every OkHttp request on that host;
   `BrowserLikeInterceptor` sends the byte-identical browser UA + headers
   (`Accept`/`Accept-Language`/`Sec-Fetch-*`). The cookie is bound to UA+IP and stays valid for
   a long time, so subsequent requests (feed AND the HLS fetches through `OkHttpDataSource`,
   which rides the same client) pass without re-challenge.
5. **Media path** — `MediaViewerAdapter.handleChallenge()`: on `InvalidResponseCodeException`
   403/429 against a redlib URL, launch `RedlibVerificationActivity` for the instance host via
   `MediaViewerFragment.redlibVerificationLauncher`; on completion, flush cookies and
   `retryPendingRedlib()` re-prepares the failed player. Retry bar hides on `STATE_READY`.

**Key facts:**
- `BrowserLikeInterceptor` rewrites only HTTP headers, not TLS. Both the app's OkHttp and
  ExoPlayer's platform stack share the same JA3 — the challenge is **not** JA3-bound (proven:
  a cookieless run 403s, a run with `cf_clearance` 200s, same TLS stack). No TLS tailoring needed.
- A missing/expired `cf_clearance` is the only thing that re-triggers a challenge. The feed's
  own verification repairs the cookie before the user reaches a video, so the media-path
  handler is a safety net for the narrow race (expiry between feed load and playback) and for
  instance-side revocation.
- The manual "Verify instance in browser" button in Settings is kept as a harmless escape hatch.

## 4. EXOPLAYER 2.18.1 CONTRACT FACTS (verified against the AAR sources)

- `DataSource`: implement `open(DataSpec): Long`, `read(ByteArray, Int, Int): Int` (3-arg form
  only), `close()`, `getUri()`, `addTransferListener(TransferListener)`.
- `read()` must return `C.RESULT_END_OF_INPUT` at normal end of stream — throwing
  `EOFException` breaks `HlsPlaylistParser` mid-parse.
- `open()` must return `C.LENGTH_UNSET` when the length is unknown — `0` corrupts chunk state.
- `open()` must honor `DataSpec.position`/`length` via HTTP `Range` — Reddit CMAF playlists
  address a single `.mp4` with byte ranges; the init segment is a small `ftyp`+`moov` range.
- `InvalidResponseCodeException` ctor: `(int, String, IOException cause, Map<String,List<String>>
  headerFields, DataSpec, ByteArray responseBody)` — the `Map` and `ByteArray` are `@NonNull`
  (use `emptyMap()` / `ByteArray(0)`).
- Reddit redlib HLS shape: master `HLSPlaylist.m3u8` → `CMAF_480/720.m3u8` variants →
  `EXT-X-MAP` init (`ftyp`+`moov`, ~1 KB) + `moof`/`mdat` segment ranges into `CMAF_*.mp4`.

## 5. BUILD / TEST / DEBUG COMMANDS (do NOT reinvent)

```bash
# Build (release) — MANDATORY JBR-17: Kotlin 1.6.21 rejects Java 25
export JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14
./gradlew :app:assembleRelease --console=plain     # if resource-merge StackOverflow: ./gradlew clean first

# Unit tests (RedlibScraperTest)
./gradlew :app:testDebugUnitTest --console=plain

# Install + launch (always the explicit component; the intent-action form is ambiguous)
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am force-stop com.rohitss.stealth
adb shell am start com.rohitss.stealth/com.cosmos.unreddit.MainActivity

# Logs (app tags: MediaViewer, OkHttpDataSource)
adb logcat -v time MediaViewer:V OkHttpDataSource:V *:S

# Reset challenge/cookie state entirely
adb shell pm clear com.rohitss.stealth
```

Keep-worthy logs that remain in the code:
`OkHttpDataSource` logs a `Log.w` on any non-2xx (code + content-type + URL);
`MediaViewerAdapter` logs `bind video`, full `onPlayerError` stack traces,
"redlib HLS blocked by bot challenge; launching verification for <host>",
"Verification finished; retrying redlib HLS playback", and the audio-fallback path.

## 6. KEY FILES

- `.../scraper/redlib/RedlibPostScraper.kt` — HLS `<source>` extraction.
- `.../scraper/redlib/RedlibChallenge.kt` / `RedlibBlockedException.kt` — detection + exception.
- `.../source/TedditSource.kt` — challenge→exception conversion; search routes (`/search?type=link`,
  `?type=sr_user`, `/r/<sub>/search`); user search intentionally returns an empty listing.
- `.../util/OkHttpDataSource.kt` — the OkHttp-backed ExoPlayer DataSource + Factory (new).
- `.../util/ExoPlayerHelper.kt` — `redlibMediaSourceFactory` (uncached, `RedlibLoadErrorPolicy`),
  default cached factory for non-redlib hosts.
- `.../di/NetworkModule.kt` — `@TedditOkHttp` = the browser-like client (BrowserLikeInterceptor +
  RedditCookieJar + browser UA). `@RedditScrapOkHttp` is NOT browser-like — never use it here.
- `.../ui/mediaviewer/MediaViewerAdapter.kt` — host-based factory pick, challenge → verification,
  retry-after-verification, retry-bar hide on `STATE_READY`.
- `.../ui/mediaviewer/MediaViewerFragment.kt` — `redlibVerificationLauncher` wiring.
- `.../ui/redditsource/RedlibVerificationFragment.kt` / `RedlibVerificationActivity.kt` /
  `RedlibWebView.kt` — the verification UIs (feed overlay, full-screen fallback, shared config).
- `.../ui/postlist/PostListFragment.kt` — challenge-vs-generic error handling + verification launch.
- `.../res/layout/` — `fragment_redlib_verification.xml` (`fitsSystemWindows` DONE button),
  `fragment_post.xml` (`verification_container` host), `view_info_bar.xml` (12dp vertical padding
  so long messages don't render cramped in the shared RETRY/VERIFY bar).

## 7. KNOWN LIMITATIONS (accepted)

- Emulator-only quirk: the Cloudflare Turnstile auto-slide can loop; on a real device or with a
  manual tap-through the challenge solves normally. The DONE→retry flow is correct regardless.
- Only `TedditSource.getSubreddit` converts HTTP-level challenges to `RedlibBlockedException`;
  other endpoints (`getPost`, `getUserPosts`, …) have the same latent issue for non-2xx
  challenges (out of scope; the inline overlay covers the common feed path).
- Instances that disable `/search` answer 404 — surfaced with the friendly message; user search
  is unsupported on all redlib instances.

## 8. GIT STATE

- `5605018` — Redlib HLS playback behind bot challenge + inline verification (the main change set).
- `6e24506` — Search friendly message + taller info bar.
- No debug/temp code remains in the tree; all diagnostic hooks were removed before commit.
