# 05 - Dependency Injection

The app uses **Dagger Hilt**. All modules install into `SingletonComponent`, and entry points are `@HiltAndroidApp` on `UnredditApplication`, `@AndroidEntryPoint` on `MainActivity` and fragments, and `@HiltWorker` on `MediaDownloadWorker`.

This document reflects the current `NetworkModule` wiring, including `RedditCookieInitializationInterceptor` added by `45fc13e`.

---

## Modules Overview

All modules are in `app/src/main/java/com/cosmos/unreddit/di/`.

| Module | Provides |
|---|---|
| `NetworkModule` | OkHttp clients, Retrofit services, Moshi instances |
| `DatabaseModule` | Room `RedditDatabase` |
| `PreferencesModule` | `DataStore<Preferences>` |
| `DispatchersModule` | Named coroutine dispatchers |
| `CoroutinesScopesModule` | Application-scoped `CoroutineScope` |

---

## NetworkModule (`NetworkModule.kt`)

`NetworkModule` is an `object` annotated with `@Module @InstallIn(SingletonComponent::class)`.

### Qualifiers (9)

| Qualifier | Annotation target |
|---|---|
| `@RedditMoshi` | `Moshi` with Reddit polymorphic/custom adapters |
| `@BasicMoshi` | Plain `Moshi` |
| `@ImgurMoshi` | `Moshi` with `AlbumDataAdapter` |
| `@RedditOkHttp` | Official JSON client |
| `@TedditOkHttp` | Teddit client |
| `@RedditScrapOkHttp` | old.reddit HTML client |
| `@GenericOkHttp` | Media API client |
| `@RedditOfficial` | Retrofit `RedditApi` for `https://www.reddit.com/` |
| `@RedditScrap` | Retrofit `RedditApi` for `https://old.reddit.com/` |

### OkHttp clients (exact chain)

| Provider | Interceptors (order) | CookieJar | Timeouts |
|---|---|---|---|
| `provideRedditOkHttpClient()` (`:125-135`) | `RawJsonInterceptor` → `JsonInterceptor` → `RedditCookieInitializationInterceptor` | `RedditCookieJar` | 60 s |
| `provideTedditOkHttpClient()` (`:140-148`) | `RawJsonInterceptor` → `TargetRedditInterceptor` | — | 60 s |
| `provideRedditScrapOkHttpClient()` (`:164-171`) | — | `RedditCookieJar` | 60 s |
| `provideGenericOkHttpClient()` (`:153-159`) | — | — | 60 s |

The official client is the only one that bootstraps cookies. The scraper client shares the system cookie jar but does not run the initialization interceptor. See [04-Data-Layer.md](04-Data-Layer.md#cookie-bootstrap-hotfix-45fc13e).

### Retrofit services

| Provider | Base URL | Converter(s) |
|---|---|---|
| `provideRedditApi(@RedditMoshi, @RedditOkHttp)` | `RedditApi.BASE_URL` = `https://www.reddit.com/` | `MoshiConverterFactory` + `SortingConverterFactory` |
| `provideRedditScrapingApi(@RedditMoshi, @RedditScrapOkHttp)` | `RedditApi.BASE_URL_OLD` = `https://old.reddit.com/` | same |
| `provideTedditApi(@RedditMoshi, @TedditOkHttp, PreferencesRepository)` | `reddit_source_instance`, validated; fallback `https://teddit.net/` | same |
| `provideImgurApi(@ImgurMoshi, @GenericOkHttp)` | `ImgurApi.BASE_URL` | `MoshiConverterFactory` |
| `provideStreamableApi(@BasicMoshi, @GenericOkHttp)` | `StreamableApi.BASE_URL` | `MoshiConverterFactory` |
| `provideGfycatApi(@BasicMoshi, @GenericOkHttp)` | `GfycatApi.BASE_URL_GFYCAT` | `MoshiConverterFactory` |
| `provideRedgifsApi(@BasicMoshi, @GenericOkHttp)` | `RedgifsApi.BASE_URL` | `MoshiConverterFactory` |

`provideTedditApi()` calls `runBlocking { preferencesRepository.getRedditSourceInstance().firstOrNull() }` and `LinkValidator(url).validUrl` (`:213-221`). Because of `@Singleton`, the instance URL is read **once per process**. Selecting Teddit therefore shows a restart-required dialog (`PreferencesFragment`, `restartRequired = true`) and `Context.restart()` relaunches the task and exits the process (`util/extension/ApplicationExt.kt`). Switching the enum itself is immediate because `PreferencesViewModel.setRedditSource()` also calls `CurrentSource.setRedditSource()`, but a changed Teddit host only takes effect after the restart.

### Moshi configurations

**`@RedditMoshi`** (`:90-103`):

- `PolymorphicJsonAdapterFactory<Child>` on field `kind`:
  - `t1` → `CommentChild`
  - `t2` → `AboutUserChild`
  - `t3` → `PostChild`
  - `t5` → `AboutChild`
  - `more` → `MoreChild`
- `MediaMetadataAdapter.Factory` — converts polymorphic metadata into a map.
- `RepliesAdapter` — handles `replies` as either a `Listing` or a string.
- `EditedAdapter` — handles `edited` as `false`, `true` or a timestamp.
- `NullToEmptyStringAdapter` — converts null strings to `""`.

**`@BasicMoshi`** (`:108-111`): no custom adapters.

**`@ImgurMoshi`** (`:116-120`): `AlbumDataAdapter`.

---

## DatabaseModule

`DatabaseModule.provideRedditDatabase()` (`di/DatabaseModule.kt:19-25`):

```kotlin
Room.databaseBuilder(context, RedditDatabase::class.java, "reddit_db")
    .addCallback(RedditDatabase.Callback())
    .addMigrations(RedditDatabase.MIGRATION_1_2)
    .addMigrations(RedditDatabase.MIGRATION_2_3)
    .addMigrations(RedditDatabase.MIGRATION_3_4)
    .build()
```

- `@Singleton`; database name `reddit_db`.
- `RedditDatabase.Callback()` inserts the default `"Stealth"` profile on first creation.
- DAOs are **not** separately provided; repositories obtain them from `RedditDatabase` (e.g. `redditDatabase.postDao()`).

---

## PreferencesModule

`PreferencesModule.providePreferencesDataStore()` (`di/PreferencesModule.kt:20-27`):

```kotlin
private val Context.preferencesDataStore by preferencesDataStore(name = "preferences")

@Provides @Singleton
fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences>
```

The delegate guarantees a single `DataStore` instance for the `"preferences"` file. All keys are defined in `data/model/preferences/*`.

---

## DispatchersModule

`DispatchersModule` provides four `CoroutineDispatcher`s:

| Qualifier | Value |
|---|---|
| `@DefaultDispatcher` | `Dispatchers.Default` |
| `@IoDispatcher` | `Dispatchers.IO` |
| `@MainDispatcher` | `Dispatchers.Main` |
| `@MainImmediateDispatcher` | `Dispatchers.Main.immediate` |

These are injected into repositories, sources, scrapers and `MediaDownloadWorker`.

---

## CoroutinesScopesModule

`CoroutinesScopesModule` provides:

```kotlin
@ApplicationScope
@Provides @Singleton
fun providesCoroutineScope(@DefaultDispatcher dispatcher: CoroutineDispatcher): CoroutineScope =
    CoroutineScope(SupervisorJob() + dispatcher)
```

`@ApplicationScope` is available for long-lived work, although most feature code uses `viewModelScope`.

---

## Other injection points

| Type | Scope | Notes |
|---|---|---|
| `CurrentSource` | `@Singleton` | Constructor reads `reddit_source` with `runBlocking` |
| `LinkRedirector` | `@Singleton` | Constructor reads `privacy_enhancer` and all `redirect` rows with `runBlocking` |
| `RedgifsToken` | `@Singleton` | In-memory bearer token |
| `LinkHandler` | `@FragmentScoped` | Injects the hosting `Fragment` and `LinkRedirector` |
| `MediaDownloadWorker` | `@HiltWorker` | `@AssistedInject` with app context/params + `@IoDispatcher` |
| `*ViewModel` | Hilt `@HiltViewModel` where applicable | Feature-scoped |
| `*Mapper` | `@Singleton` | Injected with `@DefaultDispatcher` |
