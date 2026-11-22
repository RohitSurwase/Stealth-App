# Unit tests
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
The `app/src/test` tree holds the JVM unit tests (no Android dependencies). It has two files: `ExampleUnitTest.kt`, a placeholder (`2 + 2 == 4`) with no real coverage, and `RedlibScraperTest.kt`, which is the suite that actually exercises the code. The scraper test drives the Redlib Jsoup scrapers directly with HTML fixtures and `Dispatchers.Unconfined` under `runBlocking`, so it runs on the JVM without a device.

## Key files
- `app/src/test/java/com/cosmos/unreddit/RedlibScraperTest.kt`: 11 `@Test` methods covering post/listing, detail+comments, subreddit info/search, user comments, and every challenge-rejection path.
- `app/src/test/java/com/cosmos/unreddit/ExampleUnitTest.kt`: placeholder `2 + 2 == 4`.

## Entry points and flows
A test builds a scraper with `RedlibPostScraper(Dispatchers.Unconfined, baseUrl)` and calls `.scrap(html)`, asserting on the returned `Listing`/`PostChild`/`CommentChild`. Challenge bodies are passed to `RedlibChallenge.isChallengePage(body)` (non-2xx) and `RedlibChallenge.isChallenge(status, body)` (status-only), asserting they throw/return as expected.

## Gotchas
- `RedlibScraperTest` is the only real unit test; `ExampleUnitTest` adds nothing.
- Challenge detection is split: `isChallengePage` for a body that reached the scraper, `isChallenge(status, body)` for the empty-body case — Cloudflare returns an empty body to non-browser clients (OkHttp), so a 403/429 with `""` must be caught by status alone, while a real 404 must NOT be.
- Test deps: junit 4.13.2 (`app/build.gradle.kts` `testImplementation`).

## Covers
- app/src/test/java/com/cosmos/unreddit
Listing: 787d880a73d0

## Files
- app/src/test/java/com/cosmos/unreddit/ExampleUnitTest.kt 2fdbedbb64e4
- app/src/test/java/com/cosmos/unreddit/RedlibScraperTest.kt b172b244bf16
