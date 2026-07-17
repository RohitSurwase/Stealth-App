# 05 - Dependency Injection

The app uses **Dagger Hilt** for dependency injection, with modules installed in `SingletonComponent`.

---

## Modules Overview

All modules reside in `app/src/main/java/com/cosmos/unreddit/di/`.

| Module | File | Provides |
|---|---|---|
| `NetworkModule` | `NetworkModule.kt` | OkHttp clients, Retrofit APIs, Moshi instances |
| `DatabaseModule` | `DatabaseModule.kt` | Room `RedditDatabase` singleton |
| `PreferencesModule` | `PreferencesModule.kt` | `DataStore<Preferences>` instance |
| `DispatchersModule` | `DispatchersModule.kt` | Named coroutine dispatchers (IO, Default, Main, MainImmediate) |
| `CoroutinesScopesModule` | `CoroutinesScopesModule.kt` | Application-scoped `CoroutineScope` |

---

## NetworkModule (`NetworkModule.kt`)

The most complex module. Provides:

### Qualifiers Used

| Qualifier | Target |
|---|---|
| `@RedditMoshi` | Moshi with polymorphic Reddit JSON adapters |
| `@BasicMoshi` | Plain Moshi (no custom adapters) |
| `@ImgurMoshi` | Moshi with `AlbumDataAdapter` |
| `@RedditOkHttp` | OkHttp with `RawJsonInterceptor` + `JsonInterceptor` |
| `@TedditOkHttp` | OkHttp with `RawJsonInterceptor` + `TargetRedditInterceptor` |
| `@RedditScrapOkHttp` | OkHttp with `RedditCookieJar` |
| `@GenericOkHttp` | Plain OkHttp (timeouts only) |
| `@RedditOfficial` | Retrofit `RedditApi` targeting `reddit.com` |
| `@RedditScrap` | Retrofit `RedditApi` targeting `old.reddit.com` |

### Provided APIs

- `RedditApi` (two instances: `@RedditOfficial` and `@RedditScrap`)
- `TedditApi` (configurable base URL from preferences)
- `ImgurApi`
- `StreamableApi`
- `GfycatApi`
- `RedgifsApi`

### Moshi Configurations

**`@RedditMoshi`** includes:
- Polymorphic adapter for `Child` (subtypes: `CommentChild`, `AboutUserChild`, `PostChild`, `AboutChild`, `MoreChild`)
- `MediaMetadataAdapter` — custom JSON-to-map adapter
- `RepliesAdapter` — handles polymorphic replies (`Listing` vs `string`)
- `EditedAdapter` — handles `true`/`false`/timestamp for `edited` field
- `NullToEmptyStringAdapter` — converts null strings to empty strings

**`@ImgurMoshi`** includes:
- `AlbumDataAdapter` — handles Imgur album data polymorphism

---

## DatabaseModule

Provides `RedditDatabase` singleton with migration support:
```kotlin
Room.databaseBuilder(context, RedditDatabase::class.java, "reddit_db")
    .addCallback(RedditDatabase.Callback())
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    .build()
```

DAOs are accessed directly through the database instance (no separate DI for each DAO).

---

## PreferencesModule

Provides the `DataStore<Preferences>` singleton using `preferencesDataStore` delegate with name `"preferences"`.

---

## DispatchersModule

Provides named dispatchers via `@DefaultDispatcher`, `@IoDispatcher`, `@MainDispatcher`, `@MainImmediateDispatcher` qualifiers.

---

## CoroutinesScopesModule

Provides `@ApplicationScope` — a `CoroutineScope` backed by `SupervisorJob() + Dispatchers.Default`.
