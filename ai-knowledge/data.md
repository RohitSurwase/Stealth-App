# Data
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The data layer is the app's single source of truth for the UI. Repositories coordinate three state owners: Room (`reddit_db`, version 4) for persisted local state, DataStore (`preferences`) for settings, and the `CurrentSource` strategy for remote state. Remote state comes three ways — the official JSON API, a Redlib HTML proxy, and Teddit — selected by a preference. Reddit content is fetched with Retrofit and parsed either as Moshi JSON (official) or with Jsoup scrapers (Redlib / old-Reddit). Anti-bot challenges from Redlib instances are detected and failed loudly so the feed shows an error instead of a blank screen.

## Key files
- `app/src/main/java/com/cosmos/unreddit/data/local/RedditDatabase.kt`: Room db v4, 6 entities/DAOs, profile-isolated tables, default "Stealth" profile, migrations 1_2/2_3/3_4.
- `app/src/main/java/com/cosmos/unreddit/data/repository/PreferencesRepository.kt`: only accessor for all DataStore keys; exposes typed flows.
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/CurrentSource.kt`: picks Reddit/Teddit(scraping)/RedditScraping source under a Mutex.
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/TedditSource.kt`: the Redlib source — scrapes Redlib HTML with `scraper.redlib.*` against a dynamic base URL.
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/RedditScrapingSource.kt`: classic Jsoup scrapers on `old.reddit.com`.
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditApi.kt`: Retrofit interface; `BASE_URL=oauth.reddit.com`, `BASE_URL_OLD=old.reddit.com`.
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibScraper.kt`: Redlib Jsoup base; normalises relative links, timestamps, detects challenge pages.
- `app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibChallenge.kt`: flags Cloudflare/Anubis/within.website challenge text, meta-refresh redirect, and 403/429 status.
- `app/src/main/java/com/cosmos/unreddit/data/repository/RedlibInstancesRepository.kt`: downloads the public instance list from redlib-org/redlib-instances, caches it, filters clearnet hosts.
- `app/src/main/java/com/cosmos/unreddit/data/remote/BrowserUserAgent.kt` + `BrowserLikeInterceptor.kt`: browser-like User-Agent/headers shared by the network client.

## Entry points and flows
UI `ViewModel`s call a repository; the repository reads Room/DataStore and delegates remote work to `CurrentSource`. `CurrentSource` holds one active `BaseRedditSource` (REDDIT / TEDDIT / REDDIT_SCRAP), swapped via `setRedditSource(value)` under a `Mutex`. The official source builds `GET /r/{sub}/{sort}` and Moshi-parses the `ResponseBody`; the Redlib source fetches HTML and runs it through `RedlibPostScraper`/`RedlibCommentScraper`/etc., which throw `RedlibBlockedException` on a challenge page (HTTP 200) or on a 403/429 error body. Paging 3 `PagingSource`s in `remote/datasource/` call `CurrentSource`; feeds are cached in memory (`cachedIn`) with no `RemoteMediator`.

## Gotchas
- The TEDDIT slot is now a Redlib HTML proxy, not a JSON mirror — `TedditSource` scrapes whatever instance `@RedlibBaseUrl` points at, chosen from the cached instance list.
- `getMoreChildren`, `searchPost`, `searchUser` are hard-wired to the official `RedditSource`; Teddit/Redlib and scraping return empty stubs for them.
- Redlib challenge pages answer HTTP 200, so detection lives in the scraper (`RedlibChallenge`), not the network layer; a 404/500 is intentionally treated as real content.
- Media download audio mux works only on API 29+; below Q the worker writes both files but reports `Result.failure()`.
- Room/DataStore/network layers have no automated tests.

## Covers
- app/src/main/java/com/cosmos/unreddit/data
Listing: 1050eddde75f

## Files
- app/src/main/java/com/cosmos/unreddit/data/local/RedditDatabase.kt a59fb8d67a3f
- app/src/main/java/com/cosmos/unreddit/data/repository/PreferencesRepository.kt be396e63fc46
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/CurrentSource.kt 894f821b6a6c
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/TedditSource.kt a685fe299d7b
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/RedditScrapingSource.kt ebe57778c641
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/RedditApi.kt 6131cade4532
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibScraper.kt 426d926686a2
- app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/scraper/redlib/RedlibChallenge.kt 692b884b68f8
- app/src/main/java/com/cosmos/unreddit/data/repository/RedlibInstancesRepository.kt 86137d94b75f
- app/src/main/java/com/cosmos/unreddit/data/remote/BrowserUserAgent.kt 1cb4769f5c9d
