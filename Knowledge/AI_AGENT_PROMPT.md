# STEALTH-APP — Agent Brief: Reliable Reddit Content Loading

> Paste this ENTIRE brief into your local LLM. It is self-contained.
> After reading it, the agent MUST create/update the memory file, produce a ranked plan, then execute complex-first.

---

## 0. HOW TO USE THIS BRIEF (agent protocol)
1. Read this brief fully before touching code.
2. Create/refresh `AI_AGENT_MEMORY.md` at the repo root (template in §8.1).
2.5 Read `Knowledge/README.md` and the linked docs (see KNOWLEDGE BASE section) — your code-grounded reference. Open files as and when required; do not guess about build config, DI wiring, or invariants.
3. Before EVERY session — especially after context compaction — RE-READ `AI_AGENT_MEMORY.md` first and reconcile it with current state.
4. Produce a DETAILED, step-by-step plan (see §8.2). Rank tasks by COMPLEXITY. Execute the MOST COMPLEX / highest-risk task FIRST, then simpler ones.
5. Choose MINIMAL but FULLY PROVEN solutions. Reuse existing patterns; avoid new abstractions, generic sprawl, or unrelated refactors.
6. Do NOT remove or clean up any source, API, or method during planning or implementation. Defer ALL cleanup to the END, after the chosen source is implemented and working (see §8.4). Only then, with concrete evidence of what actually works, decide what is truly useless and remove it.
7. Work in small steps. After each meaningful step, update `AI_AGENT_MEMORY.md`.
8. Never leave the project non-building: build after each change, fix until green, then commit.

---

## 1. GOAL
Make this Android Reddit client reliably load Reddit content (posts, comments, subreddits, search). Currently NO source reliably loads content. Fix the DATA LAYER so the app shows real Reddit data.

---

## 2. READ-FIRST CONSTRAINTS (do not violate)
- **Build with JDK 17 ONLY.** Kotlin 1.6.21/1.7.20 cannot parse Java 25.
  `JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 ./gradlew assembleDebug`
- **You CANNOT test redlib from this environment.** The OpenCode server IP and the emulator's host egress IP are Cloudflare/Anubis-blocked (HTTP 403). Only the USER's real browser can solve Cloudflare challenges. The USER tests on-device.
- Toolchain: Kotlin 1.7.20, OkHttp 4.11.0, Retrofit 2.9.0, AGP 7.3.1, Java 8 source/target.
- Emulator: `emulator-5554`. Launcher: `com.rohitss.stealth.dev/com.cosmos.unreddit.MainActivity`.
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- No curl/wget/python in the emulator shell (only `nc`). Verify network via logcat: `adb logcat -i okhttp`.
- Datastore prefs are NOT written on fresh install (defaults applied lazily) — do not rely on reading source/instance from disk for testing.

---

## 3. PROJECT CONTEXT
"Stealth App" (package `com.rohitss.stealth.dev`, UI package `com.cosmos.unreddit`) is an Android Reddit client (Kotlin, Dagger-Hilt, Retrofit, Moshi, OkHttp, Jsoup). It uses a pluggable "source" architecture: one interface, multiple implementations, selected by user preference.

---

## KNOWLEDGE BASE (read early, and as needed)
There is a code-grounded analysis of this repo at:
`/mnt/VirtualDisk/Android-Projects/Stealth-App/Knowledge/README.md`

It contains 8 linked documents (read the README first, then open the ones you need):
- `01-Project-Overview.md` — purpose, architecture diagram, exact end-to-end runtime flow (incl. cookie bootstrap)
- `02-Build-System.md` — Gradle, `buildSrc`, dependency versions, signing (`keys/`), kapt/JDK flags, build commands
- `03-Source-Structure.md` — package-by-package file inventory
- `04-Data-Layer.md` — Room, DataStore, OkHttp clients, sources, paging, downloads
- `05-Dependency-Injection.md` — Hilt modules, qualifiers, exact client wiring
- `06-UI-Layer.md` — navigation, screens, ViewModels, media player, shortcuts
- `07-Testing.md` — test structure, coverage, how to run tests
- `08-Working-Principles-Evolution-and-Fixes.md` — core invariants, fork timeline, issue-by-issue fix register, open issues

These docs were regenerated from HEAD and verified against source. Treat them as authoritative ground truth for architecture, build flags, and invariants. **Read `Knowledge/README.md` and open the linked files as and when required** — do not guess about build config, DI wiring, or invariants; check the relevant doc first. Where a doc and current source disagree, trust the current source and update the doc.

---

## 4. DEFINITIVE FINDINGS — STATUS OF EACH SOURCE (do not re-derive)
The app's UI only renders typed models (`PostData`, `CommentData`, `Listing`). The problem is purely in the DATA LAYER: how each source produces those models.

- **REDDIT (official JSON API via `RedditApi` + Moshi) — DEAD.** Reddit's JSON API now requires OAuth and aggressively rate-limits/blocks third-party clients. Unreliable.
- **TEDDIT / REDLIB (via `TedditApi` + Moshi) — JSON PATH DEAD.** Current redlib (0.40.0) is HTML-only: its `template()` always returns `text/html`, and `main.rs` registers no client JSON routes. Confirmed live on `redlib.freedit.eu`: even `/r/x/hot?raw_json=1` returns an HTML page. Moshi cannot parse HTML → every Redlib call fails to deserialize. This is NOT a Cloudflare issue and NOT the `?api` param.
- **REDDIT_SCRAP (reddit.com HTML scraping via Jsoup) — WORKS.** This is the proven path: fetch reddit.com HTML, parse it into the app's models.
  - Caveat: reddit.com has been aggressively anti-scraping (login walls, datacenter-IP blocks). REDDIT_SCRAP works today but may degrade over time — weigh this when choosing between consolidating on reddit.com scraping vs. redlib mirrors.

**Conclusion:** The two JSON sources are dead. The proven path is HTML scraping. Redlib CAN work, but only if we scrape its HTML (like REDDIT_SCRAP does for reddit.com) — not via JSON.

---

## 5. ARCHITECTURE MAP
```
CurrentSource (selects one source by preference)
├── RedditSource         (RedditApi + Moshi)          ← JSON API, DEAD
├── TedditSource         (TedditApi + Moshi)          ← JSON via Redlib, DEAD (HTML-only)
└── RedditScrapingSource (RedditApi + Jsoup scrapers) ← reddit.com HTML, WORKS

BaseRedditSource (sealed interface) — the contract all sources implement.
Models: PostData, CommentData, Listing, ListingData, Child(+subtypes), More*, etc.
Scrapers: PostScraper, CommentScraper, SubredditScraper, UserScaper, ... (Jsoup, target reddit.com markup)
```

---

## 6. KEY FILES
**Sources** (`app/src/main/java/com/cosmos/unreddit/data/remote/api/reddit/source/`):
- `BaseRedditSource.kt` — the contract (sealed interface). READ THIS FIRST.
- `CurrentSource.kt` — source selection + preference wiring.
- `RedditSource.kt` — dead JSON source.
- `TedditSource.kt` — dead Redlib JSON source.
- `RedditScrapingSource.kt` — WORKING scraper source (pattern to mirror).

**APIs:**
- `app/.../api/reddit/RedditApi.kt` — Retrofit interface (used by both JSON and scraping paths; scraping calls `.string()`).
- `app/.../api/reddit/TedditApi.kt` — Redlib Retrofit interface (`BASE_URL = https://redlib.freedit.eu/`).

**Models** (`app/.../api/reddit/model/`):
- `PostData.kt`, `CommentData.kt`, `Listing.kt`, `ListingData.kt`, `Child.kt`, `MoreChildren.kt`/`MoreData.kt`, plus supporting models (`Media`, `RichText`, `Tagline`, `Awarding`, `Crosspost`, `Gallery*`, `MediaPreview`, etc.). Read `PostData.kt` and `CommentData.kt` to learn every field the UI expects.

**Scrapers** (`app/.../api/reddit/scraper/`):
- `RedditScraper.kt` (base), `PostScraper.kt`, `CommentScraper.kt`, `SubredditScraper.kt`, `UserScaper.kt`, `SubredditSearchScraper.kt`, `Over18Scraper.kt`.

**Scraping infra:**
- `app/src/main/java/com/cosmos/unreddit/data/remote/scraper/Scraper.kt` — base scraper (`scrap(body): Result`, uses `ioDispatcher`, Jsoup).

**DI:**
- `app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt` — OkHttp clients + Retrofit instances. The Teddit client currently has a temporary `HttpLoggingInterceptor` (HEADERS level) — REMOVE before commit.
- `app/src/main/java/com/cosmos/unreddit/di/DispatchersModule.kt` — `@IoDispatcher`, etc.

**Preferences/UI:**
- `app/src/main/java/com/cosmos/unreddit/ui/preferences/PreferencesViewModel.kt` — hardcoded `redlibInstances` list (3 instances). KEEP these 3; do NOT reintroduce Teddit URLs.
- `app/src/main/java/com/cosmos/unreddit/ui/preferences/PreferencesFragment.kt`

**Shared — DO NOT DELETE (used by PrivacyEnhancer redirect feature):**
- `instances.json`, `AssetsRepository`.

---

## 7. BUILD & TEST COMMANDS
- Build (JDK 17): `JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 ./gradlew assembleDebug`
- Install: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Logcat (network): `adb logcat -i okhttp` (or `| grep -i okhttp`)
- If the build fails, read the error, fix, rebuild. Never commit a non-green build.

---

## 8. WORKING PROTOCOL (mandatory)

### 8.1 PERSISTENT MEMORY — your compaction-survival mechanism (CRITICAL)
Create `AI_AGENT_MEMORY.md` at repo root. This is NOT optional — it is how you keep working across context compactions. Without it, an unexpected compaction loses all your progress.

RULES:
- ALWAYS START every session, and every resume after a compaction, by RE-READING `AI_AGENT_MEMORY.md`. Reconcile it with the current source state before doing anything else.
- ALWAYS UPDATE it after EVERY step: every code change, every build result, every finding, every decision, every blocker.
- If a compaction happens unexpectedly, the very first thing you do is re-read this file and resume from exactly where you left off. No progress may be lost.

Keep it as a PROGRESSIVE, ALWAYS-UPDATED LOG with these sections:
```
# STEALTH-APP — Agent Memory
## Goal (one line)
## Current status — what is being worked on RIGHT NOW
## Tasks
### Finished  (item — one-line result)
### Ongoing   (current step + exact position)
### Remaining (next steps, in order)
## Findings so far  (source behavior, HTML structure, gotchas, version notes)
## Decisions made   (what + why)
## File map  (paths changed / added / removed)
## Plan (ranked by complexity)
## Blockers / open questions
```
Continuously maintain the Tasks section: move items Remaining → Ongoing → Finished, each with a one-line result. This live task log + findings is the core of your persistent context. Update it after every step, not just at session ends.

### 8.2 Planning (before any code)
- Produce a DETAILED, step-by-step plan. Break work into small, independent tasks suitable for a 128k-context model.
- Rank each task by COMPLEXITY (highest risk/effort first).
- Present the plan to the user for approval. Include at least 2 architectural options ranked by complexity:
  - **Option A — Consolidate on the proven scraper (minimal, most reliable):** Make `RedditScrapingSource` the app's single working source and make it robust. Lowest complexity, highest reliability. Risk: depends on reddit.com staying scrapable. (Dead-source cleanup is deferred to §8.4.)
  - **Option B — Redlib via scraping (medium complexity):** Keep a Redlib source but reimplement `TedditSource` to fetch redlib HTML and scrape it with new Jsoup scrapers (mirroring `RedditScrapingSource`). User must test on-device (the agent cannot). More resilient to reddit.com scraping blocks, since it uses redlib mirrors.
  - **Option C — JSON mirror (highest fragility, deprioritize):** Hunt for a legacy libreddit/redlib that still serves `.json`. Unlikely to find a reliable one.
- After approval, execute from the MOST COMPLEX task down to simpler ones (de-risk the hard part first).

### 8.3 Execution rules
- MINIMAL but FULLY PROVEN: reuse existing scrapers/models/patterns.
- One logical change → build → fix → green → commit. Keep git history clean.
- When scraping, match the ACTUAL HTML (verify via the in-app browser `evaluate` if needed; redlib markup is in §9).
- "More (N)" comment expansion won't work through Redlib (no `/api/morechildren`) — initial batch only. Do not chase it.

### 8.4 CLEANUP — DEFERRED TO THE END (final phase)
- Do NOT delete or clean up any source, API, or method during planning or implementation.
- FIRST: implement and get the chosen source working end-to-end (posts, comments, etc.).
- THEN, as the FINAL phase, review the codebase with concrete knowledge of what actually works and what is useless, and remove ONLY what is genuinely unused.
- KEEP everything the app and the new Redlib source actually need to work: shared infra, underlying files, models, and any source still referenced (e.g. `RedditScrapingSource`, the Jsoup scrapers, `RedditApi`, `CurrentSource`, `instances.json`/`AssetsRepository` shared with PrivacyEnhancer). Do NOT delete shared code just because one specific source is dead — delete only when nothing else needs it.
- REMOVE only dead sources/APIs/methods and their now-unused references (in `CurrentSource`, `NetworkModule`, preferences, etc.), and only after confirming nothing else references them.
- This ordering matters: only after implementation do you know exactly what is truly useless, so cleanup stays lean, accurate, and safe — not guessed.

---

## 9. REDLIB HTML REFERENCE (verified live on redlib.freedit.eu)
Only needed if choosing Option B. Structure:
- **Listing:** `<main><div id="posts">` → each post `<div class="post" id="{id}">`:
  - `<a class="post_subreddit" href="/r/x">r/x</a>`, `<a class="post_author" href="/u/…">u/…</a>`, `<span class="created" title="ISO-8601">relative</span>`
  - `<h2 class="post_title">` → `<a class="post_flair">…</a>` (optional) + title `<a href="/r/x/comments/{id}/…">Title</a>`
  - `<div class="post_score" title="{numeric}">`, `<a class="post_comments" title="{n} comments" href="/r/x/comments/{id}/…">`
  - media: `<div class="post_media_content">` (image `a.post_media_image` / `video.post_media_video`)
- **Post page:** `<div class="post highlighted">` (same fields; title in `<h1 class="post_title">`).
- **Comments:** `<div class="thread">` → `<div id="{id}" class="comment">` (nested `<div class="comment">` recursively):
  - `<a class="comment_author …">u/…</a>`, `<a class="created" title="ISO-8601">relative</a>`, `<span class="comment_score" title="{n|Hidden}">`, `<div class="comment_body"><div class="md">…html…</div></div>`
- Permalinks are `/r/{sr}/comments/{id}/{title}/`. Resolve relative links against the instance base URL.

---

## 10. YOUR FIRST ACTIONS
1. Re-read this brief, then create `AI_AGENT_MEMORY.md`.
1.5 Read `Knowledge/README.md` and skim the linked docs (esp. `04-Data-Layer.md`, `05-Dependency-Injection.md`, `08-Working-Principles...`) — open more as needed (see KNOWLEDGE BASE section).
2. Read `BaseRedditSource.kt`, `CurrentSource.kt`, `RedditScrapingSource.kt`, `TedditSource.kt`, `RedditApi.kt`, and `PostData.kt`/`CommentData.kt` to ground yourself.
3. Confirm the build works: `JAVA_HOME=/home/rohit_surwase/.jdks/jbr-17.0.14 ./gradlew assembleDebug`.
4. Propose the ranked plan (Options A/B/C) + task breakdown. WAIT for user approval before coding.
