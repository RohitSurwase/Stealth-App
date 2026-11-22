# DI
AI-generated, verified against the files listed below on 2026-11-22. Check before relying on it.

## Summary
Dependency injection is Dagger Hilt. All five modules are `object`s that `@InstallIn(SingletonComponent::class)`. Entry points are `@HiltAndroidApp` on `UnredditApplication`, `@AndroidEntryPoint` on `MainActivity`/fragments, and `@HiltWorker` on `MediaDownloadWorker`. `NetworkModule` owns the OkHttp/Retrofit/Moshi wiring; the rest provide the Room database, the DataStore, coroutine dispatchers, and an application scope.

## Key files
- `app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt`: the big one — OkHttp clients, Retrofit APIs, Moshi configs, and qualifiers. `@InstallIn(SingletonComponent::class)`.
- `app/src/main/java/com/cosmos/unreddit/di/DatabaseModule.kt`: builds Room `reddit_db` (callbacks + migrations 1_2/2_3/3_4); DAOs come off the database instance, not separately provided.
- `app/src/main/java/com/cosmos/unreddit/di/PreferencesModule.kt`: single `DataStore<Preferences>` for the `"preferences"` file via a `Context` delegate.
- `app/src/main/java/com/cosmos/unreddit/di/DispatchersModule.kt`: four named dispatchers — `@Default`, `@Io`, `@Main`, `@MainImmediate`.
- `app/src/main/java/com/cosmos/unreddit/di/CoroutinesScopesModule.kt`: `@ApplicationScope` app-scoped `CoroutineScope` on the default dispatcher.

## Entry points and flows
Hilt wires singletons from the moment `UnredditApplication` initializes. `NetworkModule` builds one OkHttp client per qualifier; the official Reddit client is the only one that adds auth/path interceptors. Retrofit APIs are created from those clients + Moshi configs. `@Singleton` providers are read once per process, so a changed Teddit/Redlib host only takes effect after a process restart.

## Gotchas
- The official client chain is `RawJsonInterceptor → JsonInterceptor → RedditPathInterceptor → RedditOAuthInterceptor(oauthManager)`; it no longer uses a cookie jar. The doc's `RedditCookieInitializationInterceptor` was removed and replaced by OAuth.
- Teddit/Redlib uses a `BrowserLikeInterceptor` (browser UA, overridable in Settings) plus a `RedditCookieJar` that injects the `show_nsfw` cookie; the same UA/cookies are shared with the verification WebView so `cf_clearance` is accepted.
- A 10th qualifier, `@RedlibBaseUrl`, now feeds `provideRedlibBaseUrl()`; `TedditApi` is built from it with only `SortingConverterFactory` (no Moshi).
- Because `TedditApi`/`RedlibBaseUrl` are `@Singleton` and read the instance with `runBlocking`, switching the Teddit host requires a restart; switching the source enum itself is immediate via `CurrentSource`.

## Covers
- app/src/main/java/com/cosmos/unreddit/di
Listing: 64485dc92dd8

## Files
- app/src/main/java/com/cosmos/unreddit/di/NetworkModule.kt 9d8331fdc3ad
- app/src/main/java/com/cosmos/unreddit/di/DatabaseModule.kt a81cd0c0d0f0
- app/src/main/java/com/cosmos/unreddit/di/PreferencesModule.kt daef21ffa0a1
- app/src/main/java/com/cosmos/unreddit/di/DispatchersModule.kt 2e76083b3ed0
- app/src/main/java/com/cosmos/unreddit/di/CoroutinesScopesModule.kt 9c5729d81a4b
