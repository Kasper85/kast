# Design: Fix user-reported errors (v8)

## Summary

Fix five production failures: (1) Flixcorn search crashes on queries with spaces/accents because the query is interpolated raw into the URL; (2–5) favorites, watched, history and TV-settings break on a stale DB newer than code (Room migrates only forward; v9 device DB vs v8 code throws `IllegalStateException` on first access). Also verify online-first server ordering and bump `versionName` to 2.0.

## Context & Constraints

- Room can only migrate forward; a device DB at v9 cannot open against v8 code — `IllegalStateException("A migration from 9 to 8 was required but not found")` on first DAO access (Room opens lazily, not at `build()`).
- No schema changes; DB stays v8. Manual DI in `AppContainer` (object); no new libraries.
- All 108 existing tests must stay green → every new constructor parameter gets a test-compatible default.
- Code identifiers English; user-facing strings Spanish (project convention).

## Architecture Decisions

| # | Decision | Options | Tradeoff | Chosen |
|---|----------|---------|----------|--------|
| D1 | Query/URL encoding | a) `URLEncoder.encode(q, "UTF-8")` b) OkHttp `HttpUrl` builder | (a) encodes space as `+` (ambiguous in queries, invalid in path segments) and `+`→`%2B` asymmetrically; (b) RFC 3986 per-segment/per-param, `+` stays `%2B`, spaces `%20` | **b** — `buildUrl(vararg segments, query)` private helper on `HttpUrl`; `fetchHtml(HttpUrl)`; search = `buildUrl("search", query = mapOf("q" to q.trim()))`; detail = `buildUrl("serie", "$slug.html")`; episode = `buildUrl("ver", slug, "temporada-$season", "capitulo-$episode.html")`; player = `buildUrl("player", token)`; external = `buildUrl("external", "$token", query = mapOf("s" to "1"))`. Expose `internal fun episodeUrl(slug, season, episode)` so `FlixcornRepositoryImpl` cache keys and `detailUrl` reuse the same canonical form (no silent cache invalidation). Unsafe slugs/tokens are encoded by the builder — never reach `Request.Builder.url()` raw, so `IllegalArgumentException` → `UNREACHABLE` mapping cannot trigger (REQ-SEARCH-02 AC2) |
| D2 | Guard placement | a) `KastLgDatabase.create()` b) `AppContainer` | (a) `build()` does NOT open the DB — guard would need a forced open inside `create()`, entangling DB factory with app state; (b) keeps the `StateFlow` next to the object graph and forces a single guarded open | **b** — synchronized `openDatabase()` in `AppContainer`: `KastLgDatabase.create(ctx).also { it.openHelper.writableDatabase }` in try/catch; on `IllegalStateException` set `_databaseError` FIRST, then throw typed `DatabaseOpenException`. Repositories keep non-null DB signatures (no invasive nullability). Failed lazy never completes → next access re-attempts (needed for reset) |
| D3 | Error state | a) per-VM detection b) shared `StateFlow<DatabaseError?>` on `AppContainer` | (a) duplicates logic and breaks AC3 consistency; (b) one source of truth, injectable into VMs | **b** — `databaseError: StateFlow<DatabaseError?>` (null = OK, `VersionMismatch` = error). All four VMs inject it with default `MutableStateFlow(null)` (tests untouched) and route to the DB-error UI when non-null |
| D4 | Reset UX | a) inline dialog in affected screens b) Settings entry c) both | (a) satisfies REQ-DB-02, minimal surface; (b) only useful when the app still works (healthy DB); (c) more churn | **a** — two-step: banner "Los datos locales no son compatibles con esta versión de la app" → button "Restablecer datos" → confirmation dialog ("Se eliminarán favoritos, historial y configuración. Esta acción no se puede deshacer." / Cancelar / Restablecer). `AppContainer.resetLocalData()`: close DB → `context.deleteDatabase("kastlg.db")` (name exposed via `KastLgDatabase.delete(context)`) → clear error → null all Room-backed singletons (var-based accessors) so lazies rebuild. After confirm: **relaunch the task** (`finishAffinity()` + launch intent) — deterministic, avoids stale DAO/flow instances across all 4 VMs. Rejected: in-place reload (repository-provider plumbing + `flatMapLatest` in 4 VMs, more test churn) and `Runtime.exit(0)` (reads as a crash). Settings entry deferred (open question) |
| D5 | Server order | a) change `sortedOnlineFirst()` b) tests + live verify only | Code already implements player-first → external-fallback → name tiebreak, and both load paths re-sort (`parseEpisodeServers` line 189; cache read `FlixcornRepositoryImpl` line 68). `parseServerRow` already accepts `onlineHref == null` rows | **b** — no production change; add `/external/`-only (Vidmoly) sort tests + cache re-sort test + live check. Parser selectors unchanged unless live HTML proves a mis-parse (out of scope) |
| D6 | versionName | — | — | `versionName = "2.0"` in `app/build.gradle.kts`; `versionCode` stays 1 (build-only) |

## Components

| File | Action | Change |
|------|--------|--------|
| `data/remote/flixcorn/FlixcornScraper.kt` | Modify | `buildUrl` HttpUrl helper; all 5 endpoint URLs; `fetchHtml(HttpUrl)`; `internal episodeUrl()` for cache keys |
| `data/repository/FlixcornRepositoryImpl.kt` | Modify | cache key + `detailUrl` via scraper canonical URL (keeps keys stable post-encoding) |
| `data/local/KastLgDatabase.kt` | Modify | expose `DATABASE_NAME`; companion `delete(context)` |
| `di/AppContainer.kt` | Modify | `openDatabase()` guard, `databaseError` StateFlow, `DatabaseOpenException`, `resetLocalData()`, Room singletons → synchronized var accessors |
| `presentation/components/DatabaseErrorState.kt` | Create | shared banner + `ResetDatabaseDialog` (Spanish copy) + task-relaunch trigger |
| `presentation/{favorites,history}/…ViewModel.kt` | Modify | inject `databaseError`; `combine` into UiState → DB-error state replaces generic message |
| `presentation/detail/MovieDetailViewModel.kt` | Modify | `databaseError` in UiState; route existing catches (observeFavorite/Watched, recordViewed, getConfig) when `databaseError.value != null` |
| `presentation/tvsettings/TvSettingsViewModel.kt` | Modify | try/catch around `observeConfig()` collect and `autoReconnect()` (`getConfig` throws ISE) → DB-error state, no crash (REQ-DB-01 AC2) |
| `app/build.gradle.kts` | Modify | `versionName = "2.0"` |
| tests (below) | Create/Modify | see Test Plan |

## Data Flow

DB-error + reset flow (config rule: sequence diagram):

```
User opens Favorites
  → favoriteRepository lazy → AppContainer.openDatabase()
  → KastLgDatabase.create(ctx).openHelper.writableDatabase   [forced open]
  → Room: ISE "migration from 9 to 8 required but not found"
  → catch → _databaseError = VersionMismatch → throw DatabaseOpenException
  → FavoritesViewModel combine(databaseError, repo.catch{}) → DB-error UiState
  → UI: banner + dialog → Confirm
  → resetLocalData(): close DB → deleteDatabase("kastlg.db") → error=null → null singletons
  → relaunch task → fresh install state → v8 DB recreated on next access
```

Normal path (clean install / healthy DB): `openDatabase()` succeeds, `databaseError` stays null → zero behavior change; `KastLgDatabaseTest` in-memory builders are untouched.

## Test Plan

| File | New/Update | Cases |
|------|-----------|-------|
| `FlixcornScraperUrlTest.kt` (NEW, pure) | 6 | `q=Bestias%20Divinas`; `Café`→`Caf%C3%A9`; single word unchanged (no double-encode); slug/token with space encoded in path; `/external/x?s=1`; `+` stays `%2B` |
| `FlixcornScraperTest.kt` (NEW, MockWebServer) | 3 | recorded request path/query for search (2 requests; rate limit ≥1 s), episode URL, player/external URL |
| `StreamingServerSortTest.kt` (UPDATE) | 4 | `/external/`-only rows never above `/player/`; mixed row with both URLs; all-external fallback order + none primary; cache-order fixture re-sorts |
| `FlixcornRepositoryTest.kt` (NEW) | 2 | cache read re-sorts online-first (in-memory Room + fake scraper); cache key canonical for slug with space |
| `DatabaseOpenResilienceTest.kt` (NEW, Robolectric) | 3 | file DB with `PRAGMA user_version=9` → guarded open sets `VersionMismatch` + throws `DatabaseOpenException`; `resetLocalData()` → fresh v8 opens, error cleared; clean-install path never sets error |
| `FavoritesViewModelTest` / `HistoryViewModelTest` / `TvSettingsViewModelTest` / `MovieDetailViewModelTest` (UPDATE) | 4 | DB-error state renders instead of generic message; `TvSettingsViewModel.observeConfig` ISE does not crash |
| `BuildConfigTest.kt` (NEW) | 1 | `BuildConfig.VERSION_NAME == "2.0"` |

## Risks & Mitigations

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| #2–#5 root cause is NOT the DB version | Med | Device verification FIRST (step 1 below) before wide UI work |
| Relaunch after reset feels abrupt | Med | Confirmation + "Datos restablecidos. La app se reiniciará." copy before relaunch |
| Rate-limited scraper tests slow/racy | Med | Pure `buildUrl` tests primary; MockWebServer kept to ≤3 requests |
| `AppContainer` object state leaks into tests | Low | Explicit reset in `@Before`/`@After` of resilience tests |
| Flixcorn screens (not in #2–#5) also hit DB error | Med | Shared guard covers them; they fall back to their existing error paths |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary. URL construction is an HTTP client request, not a shell/process boundary.

## Migration / Rollout

No migration. No schema/version change; reset is explicit-user-only, never automatic. Rollback: revert the change commit; worst case clean uninstall/install on device.

## Device Verification (device `4dd3d45` or clean install)

1. Confirm hypothesis first: `adb shell run-as com.kastlg.app ls databases/`; if present, try `adb shell run-as com.kastlg.app sqlite3 databases/kastlg.db "PRAGMA user_version;"` (fallback: reproduce via older APK `adb install -r`); or `adb logcat -s AndroidRuntime` for "migration from 9 to 8".
2. Clean install: `adb uninstall com.kastlg.app && ./gradlew installDebug`; open Favorites, Historial, Ajustes → all work, no crash.
3. Stale-DB path: with a v9 DB present, open Favorites → banner + reset → confirm → app relaunches → features work empty.
4. Search "Bestias Divinas" in Flixcorn → results render (logcat `FlixcornScraper` shows `results=3`).
5. Episode with Vidmoly-style `/external/`-only rows → renders "Link Directo", no primary, below any `/player/` row.
6. App info / about shows versionName 2.0.

## Open Questions

- [ ] Include the Settings-screen reset entry in this change or defer? (design ships inline-only)
- [ ] Device `4dd3d45` reachable for the confirmation step?

## Next Step

Ready for tasks (sdd-tasks). Applicable threat-matrix rows: none.