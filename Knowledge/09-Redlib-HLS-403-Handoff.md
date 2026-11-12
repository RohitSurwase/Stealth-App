# Handoff: redlib HLS video 403 — findings & next fix

Compiled pre-compaction so the cause is not reinvented. Do not re-derive from scratch;
start from this.

## Objective
Redlib video posts must play in the app's ExoPlayer media viewer (not just the WebView).

## Confirmed root cause: Cloudflare/Anubis bot challenge bound to TLS fingerprint
- redlib video posts serve HLS at:
  `https://<redlib-host>/hls/<id>/HLSPlaylist.m3u8?a=...&v=1&f=sd`
- ExoPlayer gets **403** (`HttpDataSource$InvalidResponseCodeException: Response code: 403`,
  `ExoPlaybackException: Source error`, `code=2004`). The feed on the same domain returns 200.
- The `cf_clearance` cookie **is present** in the app's global `android.webkit.CookieManager`
  and **is sent** by the fix (verified via debug log). So the cookie is NOT missing.
- Diagnosis: `cf_clearance` is bound to the TLS fingerprint (JA3) of the client that passed
  the challenge. ExoPlayer's `DefaultHttpDataSource` rides Android platform
  `HttpURLConnection`/conscrypt → different JA3 than the WebView/Chromium or the app's
  browser-like OkHttp that obtained the cookie → Cloudflare rejects it as a bot (403).
  Anubis is also in front (`techaro.lol-anubis-auth` cookie present); it needs a JS challenge
  that ExoPlayer cannot execute.
- streamable.com / redgifs / reddit videos play fine (non-Cloudflare hosts) → ExoPlayer itself
  works; only Cloudflare-gated redlib HLS fails.

## What is already committed / in place
- `77154db` — extract HLS `<source>` (m3u8) URL for redlib video posts (`RedlibPostScraper.kt`).
  Prefer m3u8 `<source>` → any non-empty `<source>` → `video.src`.
- Cookie-wiring fix in `MediaViewerAdapter.kt` (redlib branch): sends
  `CookieManager.getInstance().getCookie(url)` as a `Cookie` header via
  `exoPlayerHelper.setRequestProperties(mapOf("Cookie" to cookieHeader))`.
  - Import is `android.webkit.CookieManager` (NOT `android.net.CookieManager` — that class does
    NOT exist in this SDK; using it fails compilation).
  - This fix sends the cookie but does NOT fix the 403 (fingerprint/stack mismatch).

## Uncommitted state at handoff (clean these up)
- `MediaViewerAdapter.kt` has a TEMPORARY debug log to remove before commit:
  `Log.d(TAG, "REDLIB cookie for $url = [$cookieHeader]")`
  (placed right after the `getCookie(...)` call in the redlib branch).

## Environment / commands
- Emulator: `emulator-5554` (Pixel-9a_API-36 / Android-16). Display 1080×2424 → taps must be
  full-res (scaled taps fail).
- Build: `export JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 && ./gradlew assembleRelease`
- Install: `adb install -r app/build/outputs/apk/release/app-release.apk`
- Launch: `adb shell am start com.rohitss.stealth/com.cosmos.unreddit.MainActivity`
- Force-stop before re-testing a fresh build: `adb shell am force-stop com.rohitss.stealth`.

## How to test / verify
- Open media viewer: tap the video thumbnail. In feed the view is
  `com.rohitss.stealth:id/image_post_preview`; in post detail it's
  `com.rohitss.stealth:id/image_post` (both clickable). Tap the center of the thumbnail; the
  video should be reasonably centered on screen for the tap to register.
- Watch logcat tag `MediaViewer`: `bind video: url=...`, the `REDLIB cookie for ... = [...]`
  line, and `onPlayerError ... code=2004 msg=Source error`.
- Tip: `adb logcat -c` then capture immediately; app logs use tags `MediaViewer` / `ExoPlayer`.

## Recommended fix (do NOT keep retesting the Cookie-header approach — it can't work)
Route the redlib HLS fetch through the app's browser-like OkHttp client, which already passes
the challenge (BrowserLikeInterceptor + RedditCookieJar + browser-identical User-Agent).
Two options:
  (a) [Cleanest] Custom ExoPlayer `DataSource` backed by OkHttp; feed it to the HLS media source
      so both the m3u8 playlist and the .ts/.m4s segments go through OkHttp (same fingerprint +
      cookies). Replace/branch the data-source factory for redlib hosts.
  (b) Fetch the m3u8 via OkHttp, parse segment URLs, build ExoPlayer media sources from them.

## IMPLEMENTATION STATUS (post-compaction resume point)
Implemented so far (NOT yet built/tested):
- NEW `.../util/OkHttpDataSource.kt`: `OkHttpDataSource(okHttpClient): DataSource` +
  `OkHttpDataSourceFactory: DataSource.Factory`. GET-only (HLS is GET), honors
  `dataSpec.httpRequestHeaders` (Range), throws `HttpDataSource.InvalidResponseCodeException`
  on non-2xx, throws `EOFException` at end of segment.
- `ExoPlayerHelper` constructor changed to `(context, redlibOkHttpClient: OkHttpClient)`; added
  `redlibMediaSourceFactory by lazy { DefaultMediaSourceFactory(OkHttpDataSourceFactory(...)) }`
  (plain, uncached — playlist URL has a per-load token so caching risks stale reads);
  `getMediaSource(mediaItem, factory = defaultMediaSourceFactory)` now takes a factory param.

STILL TO DO (in order):
1. `MediaViewerAdapter.kt`:
   - Add `okHttpClient: OkHttpClient` constructor param; pass to `ExoPlayerHelper(context, okHttpClient)`.
   - In `VideoViewHolder.bind`, replace the temp verification block + temp `REDLIB cookie` debug log
     + the Cookie-header `setRequestProperties` block with: pick
     `val factory = if (url.host.contains("redlib", true)) exoPlayerHelper.redlibMediaSourceFactory
                    else exoPlayerHelper.defaultMediaSourceFactory`; build player with
     `.setMediaSourceFactory(factory)`; build sources with `exoPlayerHelper.getMediaSource(item, factory)`.
   - Remove now-unused imports: `android.webkit.CookieManager`, `okhttp3.*` verification imports,
     `java.net.HttpURLConnection` (only used by temp block) — keep `HttpDataSource` (used by
     isErrorFromAudio).
2. `MediaViewerFragment.kt`: `@Inject lateinit var teditOkHttpClient: OkHttpClient` annotated
   `@NetworkModule.TedditOkHttp`; pass it into the `MediaViewerAdapter(...)` call.
3. Build/install/test on emulator. SUCCESS = video plays (no 403). If it STILL 403s, the
   browser-like OkHttp client's TLS fingerprint does NOT match the one that solved the challenge
   → pivot (TLS-fingerprinting OkHttp layer, e.g. a custom SSLSocketFactory / a JA3-mimicking
   lib, or route the m3u8+segments through the verification WebView and hand segment URLs to
   ExoPlayer). Do NOT assume — the on-device test is ground truth.

## STEP-BY-STEP IMPLEMENTATION PLAN (approved to proceed)

Feasibility rationale: `BrowserLikeInterceptor` only rewrites HTTP headers (UA/Accept/
Accept-Language/Sec-Fetch-*); it does NOT touch TLS. So the browser-like OkHttp client and
ExoPlayer's `DefaultHttpDataSource` both ride the platform conscrypt stack → same JA3. If the
challenge were purely JA3-bound, the browser-like OkHttp client would also fail on the feed —
but the feed returns 200. Therefore the challenge is bound to **User-Agent + browser request
headers + clearance cookie**, and routing redlib HLS through the browser-like OkHttp client
should pass. The on-device test (Phase 3) is the decisive feasibility gate.

### Phase 1 — Implementation (code changes)
1. `MediaViewerAdapter.kt`:
   - Add `okHttpClient: OkHttpClient` constructor param; pass it to
     `ExoPlayerHelper(context, okHttpClient)`.
   - In `VideoViewHolder.bind`, DELETE the temp verification block (lines ~213-239), the temp
     `REDLIB cookie` debug log (line ~245), and the Cookie-header `setRequestProperties` block
     (lines ~244-248). Replace with a host-based factory pick:
     ```kotlin
     val factory = if (url.host.contains("redlib", ignoreCase = true))
                     exoPlayerHelper.redlibMediaSourceFactory
                   else
                     exoPlayerHelper.defaultMediaSourceFactory
     ```
     Build the player with `.setMediaSourceFactory(factory)`. In the single-source branch
     (`video.sound == null`, the redlib path) build the source with
     `exoPlayerHelper.getMediaSource(videoItem, factory)`. The merged-source branch
     (`video.sound != null`, non-redlib reddit/streamable/redgifs) keeps using the default
     factory for both sources (correct — those hosts are non-Cloudflare).
   - Remove now-unused imports: `android.webkit.CookieManager`, the okhttp verification imports
     (`okhttp3.HttpUrl.Companion.toHttpUrlOrNull` is still needed for `url`), and
     `java.net.HttpURLConnection` (only used by `isErrorFromAudio`'s `HTTP_FORBIDDEN`/
     `HTTP_NOT_FOUND` constants — KEEP it; only drop the parts unused). Keep `HttpDataSource`.
2. `MediaViewerFragment.kt`:
   - Add `@Inject lateinit var teditOkHttpClient: OkHttpClient` annotated `@NetworkModule.TedditOkHttp`.
   - Pass `teditOkHttpClient` into the `MediaViewerAdapter(...)` call (new ctor param).

### Phase 2 — Build / install
- `export JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 && ./gradlew assembleRelease`
- `adb install -r app/build/outputs/apk/release/app-release.apk`
- `adb shell am force-stop com.rohitss.stealth` (fresh state)
- `adb shell am start com.rohitss.stealth/com.cosmos.unreddit.MainActivity`

### Phase 3 — Test (feasibility gate — ground truth)
- Navigate to a redlib video post, tap the thumbnail to open the media viewer.
- Watch logcat tag `MediaViewer`: `bind video: url=...`, then whether it plays or errors with
  `code=2004 msg=Source error`.
- **SUCCESS** = video plays (no 403) → approach confirmed feasible; go to Phase 4.
- **STILL 403** = diagnose before pivoting:
  - Confirm the injected `@TedditOkHttp` client itself gets 200 vs 403 on the exact /hls/ URL
    (temporary verification log using the injected client, off the main thread).
  - If `@TedditOkHttp` → 200 but ExoPlayer still 403 → bug in `OkHttpDataSource` (headers/Range/
    EOF handling), not the approach. Fix the DataSource.
  - If `@TedditOkHttp` → 403 too → challenge is genuinely TLS-bound → pivot: JA3-mimicking TLS
    layer (custom `SSLSocketFactory`) OR route the m3u8+segments through the verification WebView
    and hand the resolved segment URLs to ExoPlayer.

### Phase 4 — Cleanup + commit
- Remove any temporary debug/verification logs added in Phase 3.
- Update `Knowledge/08-Media-Playback-Investigation.md` §3.5 and the root-cause section of this
  doc (replace the JA3 claim with the header/UA/cookie finding, if confirmed).
- Commit.

## PROGRESS UPDATE (post on-device test — read before continuing)

### Phase 1–2 — Implementation: DONE, builds clean
The OkHttp-routing fix compiles and the release APK builds successfully. Compile errors fixed
since handoff:
- Removed spurious `androidx.media3:media3-datasource-okhttp` dependency + its import in
  `OkHttpDataSource.kt` (the class implements exoplayer2's `DataSource`, never references media3).
- `MediaViewerAdapter.kt` merged-source audio source: `getMediaSource(video.sound, factory)` had no
  matching overload → changed to
  `getMediaSource(exoPlayerHelper.getMediaItem(video.sound), factory)`.
- `OkHttpDataSource.open()` `InvalidResponseCodeException`: the `Map` and `byte[]` args are
  `@NonNull` in 2.18.1 → pass `emptyMap()` and `ByteArray(0)` (the `IOException cause` stays null).
- `OkHttpDataSource.open()`: `dataSpec.uri` is `android.net.Uri`, not `String` →
  `dataSpec.uri.toString().toHttpUrlOrNull()` (fixed the `toHttpUrlOrNull()` receiver mismatch +
  the cascade `Request.Builder().url(...)` overload ambiguity).
- Corrected the stale JA3 docstrings in `OkHttpDataSource.kt` / `ExoPlayerHelper.kt` to the
  header/UA/cookie theory (see reconciliation below).

### Phase 3 — first run failed, but NOT because of TLS (superseded conclusion)
The first on-device run of the browser-like client returned 403 on `/hls/` and this section
concluded "genuinely TLS-bound → pivot to a JA3-mimicking TLS layer". **That conclusion was wrong.**
The 403 came from the clearance cookie not actually being attached at that moment
(`cf_present=false`, Cloudflare HTML body confirmed later by the `REDLIB-CHK` diagnostic).
Once the cookie was confirmed present, the same client fetched the playlist with HTTP 200 —
no TLS tailoring of any kind is needed. The header/UA/cookie theory is the correct one.

### Final resolution (all verified on-device, emulator-5554, redlib.freedit.eu)
The challenge is **request-fingerprint-bound**: a WebView-identical mobile-Chrome User-Agent +
browser request headers + `cf_clearance` cookie, all supplied by the `@TedditOkHttp` client.
No JA3/TLS work required. Fix chain that made playback work:
1. `OkHttpDataSource` (new): ExoPlayer DataSource routing the m3u8 playlist and every segment
   through the browser-like OkHttp client.
2. ExoPlayer 2.18.1 `DataSource` contract fixes in `OkHttpDataSource`:
   - `read()` must return `C.RESULT_END_OF_INPUT` at normal stream end (throwing `EOFException`
     kills `HlsPlaylistParser` mid-parse with a bogus "End of stream" source error).
   - `open()` must return `C.LENGTH_UNSET` when the content length is unknown (returning `0`
     breaks chunk loading state).
   - `open()` must honor `DataSpec.position`/`length` by sending an HTTP `Range` header: Reddit's
     CMAF HLS playlists address a single `.mp4` file with byte ranges; fetching the whole 3.7 MB
     file from offset 0 made the init-segment feed produce samples and crashed
     `TimestampAdjuster.adjustSampleTimestamp` with an NPE.
3. Media viewer challenge recovery (option 1): on `InvalidResponseCodeException` 403/429 against a
   redlib URL, `MediaViewerAdapter.handleChallenge()` launches `RedlibVerificationActivity`;
   the `redlibVerificationLauncher` in `MediaViewerFragment` flushes cookies and calls
   `retryPendingRedlib()` which re-prepares the failed player. The retry bar hides on
   `STATE_READY`.
4. `RedlibLoadErrorPolicy` on the redlib media-source factory: no silent retries on 403/429 —
   otherwise ExoPlayer's HLS playlist tracker retries the challenge for ~50 s before surfacing
   the error.
End-to-end verified by clearing all cookies at runtime while a video played: playlist → 403 →
verification WebView → DONE → auto-retry → playback resumed.

## ExoPlayer 2.18.1 API facts (verified via javap on the AAR classes.jar — use exactly these)
- `DataSource` extends `DataReader`. Methods to implement:
  `long open(DataSpec): Long`, `int read(byte[] buffer, int offset, int length): Int`,
  `void close()`, `android.net.Uri getUri()`, `void addTransferListener(TransferListener)`.
  (There is NO single-arg `read(ByteArray)` in 2.18.1 — use the 3-arg form.)
- `DataReader.read(byte[], int, int)` — ExoPlayer expects `C.RESULT_END_OF_INPUT` (-1) at the end
  of normal stream (throwing `EOFException` breaks HLS playlist parsing).
- `HttpDataSource.InvalidResponseCodeException` constructor (2.18.1 ONLY):
  `(int responseCode, String message, IOException cause, Map<String,List<String>> headerFields,
   DataSpec dataSpec, byte[] responseBody)`. (Other overloads exist; this is the one used.)
- `DataSpec` fields used: `uri` (android.net.Uri), `httpMethod` (DataSpec.HTTP_METHOD_GET/POST),
  `httpRequestHeaders` (Map<String,String> — apply as OkHttp headers), `length`, `key`.
- jars live under `/mnt/VirtualDisk/Caches/gradle-caches/caches/transforms-3/.../jetified-exoplayer-*-2.18.1/jars/classes.jar`.

## Client identification CORRECTION (do not trust the earlier handoff line)
The browser-like client that passes the challenge is `provideTedditOkHttpClient()` =
`@TedditOkHttp` (has BrowserLikeInterceptor + RedditCookieJar + browser UA). The earlier doc's
claim that `provideRedditScrapOkHttpClient()` (`@RedditScrapOkHttp`) is browser-like is WRONG —
that one has ONLY `cookieJar(RedditCookieJar())`, no BrowserLikeInterceptor and no browser UA,
so it would NOT pass the challenge. Inject `@TedditOkHttp`.

## Confirm before implementing (optional but recommended)
Fetch the exact failing `/hls/` URL through the app's scraping OkHttp client. If OkHttp → 200
and ExoPlayer → 403 with the same cookie, that proves the fingerprint/stack mismatch and
validates option (a)/(b).

## Key files
- `.../ui/mediaviewer/MediaViewerAdapter.kt` — host-based factory pick + challenge → verification
  launch + retry after verification.
- `.../util/OkHttpDataSource.kt` — the OkHttp-backed ExoPlayer DataSource (new).
- `.../util/ExoPlayerHelper.kt` — ExoPlayer data-source factory + `setRequestProperties()` hook.
- `.../di/NetworkModule.kt` — `provideTedditOkHttpClient()` (~line 153, annotated `@TedditOkHttp`)
  is the browser-like client (BrowserLikeInterceptor + RedditCookieJar + browser UA) that passes
  the challenge; inject it into the media viewer to build the OkHttp-backed DataSource.
  (`provideRedditScrapOkHttpClient()` ~line 200, `@RedditScrapOkHttp`, has ONLY `cookieJar()` —
  NOT browser-like; do NOT use it.)
- `.../data/remote/BrowserLikeInterceptor.kt`, `BrowserUserAgent.kt`,
  `.../reddit/RedditCookieJar.kt` — the browser-fingerprint/cookie machinery to reuse.
- `.../reddit/scraper/redlib/RedlibPostScraper.kt` — HLS `<source>` extraction (committed).
- `Knowledge/08-Media-Playback-Investigation.md` — §3.5 cookie caveat (update after fix).
