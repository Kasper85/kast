# Design: Fix user-reported errors (DB v7 + search tabs)

## Technical Approach

Three coordinated fixes: (1) make the Room schema declare the three indices the migrations already create, bump to v7 with a 6→7 migration that converges every existing install (v3 legacy, v5, v6) and fresh installs onto one valid v7 schema; (2) repair the four stale unit-test files so `testDebugUnitTest` compiles and runs green, adding migration regression coverage; (3) scope search results to the selected tab. Honors spec DLS/MT/HSB-1..5 with two design corrections (D1, D4).

## Architecture Decisions

| # | Decision | Alternatives | Rationale |
|---|---|---|---|
| D1 | Add @Index to both unique flixcorn entities **plus FlixcornSeriesFavoriteEntity** (`index_flixcorn_series_favorites_slug`, non-unique) | Spec listed only the two unique indices | **Design catch**: MIGRATION_5_6 creates that index (DatabaseMigrations.kt:110); entity declares none; no committed schema lists any index. Room TableInfo compares DB vs entity indices — without D1, migrated installs still throw `Migration didn't properly handle: flixcorn_series_favorites` after the first two are fixed |
| D2 | MIGRATION_6_7: 3× `CREATE [UNIQUE] INDEX IF NOT EXISTS` + favorites rebuild (CREATE new → INSERT…SELECT → DROP → RENAME) | `ALTER TABLE DROP COLUMN` (minSdk 26 ships SQLite \< 3.35); editing MIGRATION_4_5 (rejected, breaks 4_5 intent) | IF NOT EXISTS is a no-op on the v4→5 path (indices exist) and creates them on the fresh-v6 path (never ran 4_5). Required for the favorites index too: fresh-v6 installs lack it |
| D3 | Migration tests pass `*DatabaseMigrations.ALL` to MigrationTestHelper | Expose individual migrations | Runner skips non-matching startVersion (v3 skips 1_2..2_3; v5 skips 1_2..4_5); zero production visibility changes |
| D4 | Restore constructor injection in FlixcornSeriesDetail/Episode VMs (`getSeriesDetail`, `getEpisodeServers`) + factories pass AppContainer use cases | Keep AppContainer singletons (tests compile but fake call-count assertions always fail); AppContainer test hook (invasive) | **Design catch**: current VMs call AppContainer (FlixcornSeriesDetailViewModel.kt:31); tests were written for injection. Restoring matches app convention (Home/MovieDetail VMs) and makes MT-1 meaningful. Factory ctor signatures unchanged → screens untouched |
| D5 | Filter in UI: SearchResults renders only the `selectedTab` section; `hasActiveSearchResults` computed in HomeUiState | VM-derived sealed result list | Mirrors existing browse-mode tab switch (HomeScreen.kt:277-323); state stays a flat data holder; computed property unit-testable without Compose |
| D6 | Tab switch mid-search re-fires scoped search (no debounce); `search()` invokes only the active tab's use case, clears all three lists, sets `flixcornSearchLoading` only on Flixcorn tab | Re-filter cached results (instant but stale cross-tab data; fails HSB-3) | HSB-3 SHOULD; kills off-tab network waste; existing tests unaffected (Movies is default tab) |

## Data Flow

```
v3/v5/v6 ──(migrations)──> 3× CREATE INDEX IF NOT EXISTS (no-op or create) ──> favorites rebuild (media_type dropped) ──> v7 ✓
Fresh install ──> entities create tables + 3 declared indices ──> v7 ✓

search(query) ──> debounce 400ms ──> scoped use case (active tab only)
onTabSelected ──> selectedTab ──> if query non-blank: cancel searchJob, re-fire scoped search
SearchResults ──> spinner if isLoading ──> render active-tab section ──> empty → "No se encontraron resultados"
```

## File Changes

| File | Action | Description |
|---|---|---|
| FlixcornSeriesEntity.kt | Modify | + `indices=[Index(value=["slug"], unique=true)]` |
| FlixcornEpisodeCacheEntity.kt | Modify | + `indices=[Index(value=["episode_url"], unique=true)]` |
| FlixcornSeriesFavoriteEntity.kt | Modify | + `indices=[Index(value=["slug"])]` (D1) |
| KastLgDatabase.kt | Modify | version 6→7 |
| DatabaseMigrations.kt | Modify | + MIGRATION_6_7, appended to ALL |
| FlixcornSeriesDetailViewModel.kt | Modify | ctor + `getSeriesDetail`; factory passes `AppContainer.getFlixcornSeriesDetail` (D4) |
| FlixcornEpisodeViewModel.kt | Modify | ctor + `getEpisodeServers`; factory passes `AppContainer.getFlixcornEpisodeServers` (D4) |
| HomeUiState.kt | Modify | HomeTab + `Flixcorn("Flixcorn")`; `hasActiveSearchResults` replaces `hasSearchResults` |
| HomeViewModel.kt | Modify | scoped `search()`; `onTabSelected` re-fires in search mode |
| HomeScreen.kt | Modify | TabRow always visible; SearchResults renders active tab only; loading/empty use `isLoading`/`hasActiveSearchResults` |
| app/schemas/…/7.json | Add | generated at build; commit (MT-5) |
| KastLgDatabaseTest.kt | Modify | version==7 (L85), ALL.size==6 (L131); + index-presence test |
| KastLgMigrationTest.kt | Add | MT-2/MT-3 MigrationTestHelper suite |
| HomeViewModelTest.kt | Modify | + `searchFlixcorn` ctor arg + FakeFlixcornRepository; HSB tests |
| FlixcornEpisodeViewModelTest.kt | Modify | injection arg, 6-field StreamingServer ctor, `resolvePlayerUrl` override |
| FlixcornSeriesDetailViewModelTest.kt | Modify | injection arg, `resolvePlayerUrl` override |

## Interfaces / Contracts

MIGRATION_6_7 SQL (exact):

```sql
CREATE UNIQUE INDEX IF NOT EXISTS `index_flixcorn_series_slug` ON `flixcorn_series` (`slug`)
CREATE UNIQUE INDEX IF NOT EXISTS `index_flixcorn_episode_cache_episode_url` ON `flixcorn_episode_cache` (`episode_url`)
CREATE INDEX IF NOT EXISTS `index_flixcorn_series_favorites_slug` ON `flixcorn_series_favorites` (`slug`)
CREATE TABLE IF NOT EXISTS `favorites_new` (`tmdb_id` INTEGER NOT NULL, `title` TEXT NOT NULL, `poster_url` TEXT, `overview` TEXT NOT NULL, `release_date` TEXT NOT NULL, `vote_average` REAL NOT NULL, `favorited_at` INTEGER NOT NULL, PRIMARY KEY(`tmdb_id`))
INSERT INTO `favorites_new` (`tmdb_id`,`title`,`poster_url`,`overview`,`release_date`,`vote_average`,`favorited_at`) SELECT `tmdb_id`,`title`,`poster_url`,`overview`,`release_date`,`vote_average`,`favorited_at` FROM `favorites`
DROP TABLE `favorites`
ALTER TABLE `favorites_new` RENAME TO `favorites`
```

HomeUiState: `enum class HomeTab(val label: String) { Movies("Películas"), Series("Series"), Flixcorn("Flixcorn") }`; `val hasActiveSearchResults get() = when (selectedTab) { Movies -> searchResults.isNotEmpty(); Series -> searchTvResults.isNotEmpty(); Flixcorn -> flixcornResults.isNotEmpty() }`.

## Testing Strategy

| Layer | What | How |
|---|---|---|
| Migration | 5→6→7 from 5.json (indices absent → 6_7 creates) | createDatabase(5); runMigrationsAndValidate(7, true, *ALL); PRAGMA index_list asserts all 3 names |
| Migration | create-at-6 + 6_7 only | same helper |
| Migration | legacy 3→7: createDatabase(3) → ALTER favorites ADD media_type + INSERT row → runMigrationsAndValidate(7, *ALL) → row preserved, media_type gone | raw SupportSQLiteDatabase |
| Unit | fresh v7 opens; indices present (MT-4) | in-memory builder + PRAGMA index_list |
| Unit | stale fakes/ctors compile and pass (MT-1) | 3 repaired files |
| Unit | HSB: scoped search per tab; tab-switch re-fire; `hasActiveSearchResults`; flixcorn loading flag | fakes track per-tab call lists |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

Single PR: forecast < 400 changed lines (review budget risk Low, `Decision needed before apply: No`, `Chained PRs recommended: No`). One deploy: v7 with full chain; rollback = revert commit (401f48e baseline acceptable; app broken on device today). Commit 7.json with the entity change.

## Edge Cases

- Duplicates before unique-index creation: DAOs use REPLACE-on-conflict (FlixcornSeriesDao.kt:10, FlixcornEpisodeCacheDao.kt:10) and migrated installs have had the unique index since 4_5 → no dedupe step needed; fresh-v6 inserts always REPLACE.
- favorites missing: impossible (exists since v1 in every chain) → SELECT FROM favorites safe.
- media_type present/absent: rebuild SELECT never references it → both shapes migrate.
- Empty favorites: INSERT…SELECT copies zero rows, no failure.
- flixcorn_series_favorites untouched by rebuild (only `favorites` rebuilt).
- Rapid tab switch: prior searchJob cancelled before re-fire.
- Token missing / no network: existing toActionableMessage path; error UI shown when active list empty.
- Flixcorn browse tab renders current content (disclaimer only) — placeholder allowed per spec non-goals.

## Open Questions

- None blocking. Optional follow-up: Flixcorn browse-tab placeholder copy.