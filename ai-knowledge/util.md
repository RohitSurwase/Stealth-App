# Util
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The `util` package holds the app's helper code: link classification and routing, the Privacy Enhancer redirect engine, HTML parsing into `RedditText`, media-URL helpers, ExoPlayer setup, and a set of small extension functions. It is split into 20 root files and 18 files under `util/extension/`. The Redlib work added one root file (`OkHttpDataSource`) and a redlib branch in `ExoPlayerHelper` so HLS playback can pass the Cloudflare/Anubis bot challenge.

## Key files
- `app/src/main/java/com/cosmos/unreddit/util/LinkHandler.kt`: `@FragmentScoped`; classifies a link via `LinkUtil.getLinkType` and routes it to navigation (subreddit/user/link), media viewer, or browser; drives the Privacy Enhancer redirect flow.
- `app/src/main/java/com/cosmos/unreddit/util/LinkRedirector.kt`: `@Singleton`; Privacy Enhancer — matches a link host against regex redirect rules (`ON`/`OFF`/`ALWAYS_ASK`) under a `Mutex`.
- `app/src/main/java/com/cosmos/unreddit/util/LinkUtil.kt`: media-URL helpers — `USER_AGENT` (desktop Firefox UA), `REDDIT_VIDEO_REGEX` / `_AUDIO_128` soundtrack URL, `getLinkType`, Imgur/Gfycat/Streamable parsers.
- `app/src/main/java/com/cosmos/unreddit/util/ExoPlayerHelper.kt`: builds the default (cached) and redlib HLS media-source factories; 100 MB `SimpleCache`; `RedlibLoadErrorPolicy` bails on 403/429 so the viewer shows verification instead of retrying.
- `app/src/main/java/com/cosmos/unreddit/util/OkHttpDataSource.kt`: ExoPlayer `DataSource` that fetches the m3u8 playlist and segments through the app's browser-like OkHttp client (browser UA + request headers + `cf_clearance` cookie) — the redlib HLS challenge path.
- `app/src/main/java/com/cosmos/unreddit/util/HtmlParser.kt`: parses Reddit HTML into `RedditText` blocks (tables, code, quotes, spoilers).
- `app/src/main/java/com/cosmos/unreddit/util/RedditTagHandler.kt`: `Html.TagHandler` implementing Reddit custom tags (spoiler, code, blockquote, lists, …).
- `app/src/main/java/com/cosmos/unreddit/util/PostUtil.kt`: post mapping/filtering, post-type detection, author gradient color, saved/seen flag filtering.
- `app/src/main/java/com/cosmos/unreddit/util/DateUtil.kt`: relative-time and file-date formatting.
- `app/src/main/java/com/cosmos/unreddit/util/extension/Ext.kt`: common fragment/view extensions (keyboard, image load, listeners, external link).

## Entry points and flows
A fragment calls `LinkHandler.handleLink(url)`; it classifies the link and either navigates, opens the media viewer, or opens the browser. Browser links pass through `LinkRedirector` first, which applies Privacy Enhancer rules. Media URLs are resolved by `LinkUtil` and built into ExoPlayer sources by `ExoPlayerHelper` (default cached factory, or the redlib `OkHttpDataSource` factory when the factory is an `OkHttpDataSourceFactory`). Reddit post/comment HTML is turned into `RedditText` by `HtmlParser` + `RedditTagHandler`; `PostUtil` post-processes the parsed listings.

## Gotchas
- redlib HLS is bound to a browser request fingerprint (UA + headers + clearance cookie), not TLS: `OkHttpDataSource` + `ExoPlayerHelper.redlibMediaSourceFactory` reproduce that fingerprint; the plain factory keeps the desktop Firefox UA for everything else.
- `ExoPlayerHelper` takes the browser-like OkHttp client as a constructor arg — it is not a singleton and must be injected with the cookie-carrying client for redlib.
- The Privacy Enhancer rule match runs on the link host against regex keys; a rule with no match leaves the link untouched.
- Two files in the v3.0.0 docs (`RedditUri.kt`, `PagerHelper.kt`) were removed in `ecd4e0f`; `OkHttpDataSource.kt` was added later and is not in that doc.

## Covers
- app/src/main/java/com/cosmos/unreddit/util
Listing: b7b78caf7191

## Files
- app/src/main/java/com/cosmos/unreddit/util/LinkHandler.kt 319e3593d017
- app/src/main/java/com/cosmos/unreddit/util/LinkRedirector.kt 2be56e380dba
- app/src/main/java/com/cosmos/unreddit/util/LinkUtil.kt c074edc0b19c
- app/src/main/java/com/cosmos/unreddit/util/ExoPlayerHelper.kt 54120e034636
- app/src/main/java/com/cosmos/unreddit/util/OkHttpDataSource.kt 82a0ff5b0ad3
- app/src/main/java/com/cosmos/unreddit/util/HtmlParser.kt dc8cb5a7ebac
- app/src/main/java/com/cosmos/unreddit/util/RedditTagHandler.kt 498da1b4e6bf
- app/src/main/java/com/cosmos/unreddit/util/PostUtil.kt 176b70ec814c
- app/src/main/java/com/cosmos/unreddit/util/DateUtil.kt 80d6070a8e1d
- app/src/main/java/com/cosmos/unreddit/util/extension/Ext.kt 34d67ada430e
