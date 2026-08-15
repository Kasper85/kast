# Tasks: Fix user-reported errors (DB v7 + search tabs)

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~400 (370–430) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR, 3 work-unit commits |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | DB v7: @Index x3, version 7, MIGRATION_6_7, migration tests | PR 1 | `.\gradlew testDebugUnitTest --tests "com.kastlg.app.data.local.*"` | N/A — Robolectric in-memory DB; device pass in verify | Revert commit; Room migrations transactional, old DB intact |
| 2 | VM ctor injection restore + repair 3 test files (suite compiles, MT-1) | PR 1 | `.\gradlew testDebugUnitTest` | N/A — unit-level; screens untouched (factory sigs unchanged) | Revert commit; UI wiring identical |
| 3 | Search tabs: HomeTab.Flixcorn, scoped search, active-tab SearchResults (HSB) | PR 1 | `.\gradlew testDebugUnitTest --tests "com.kastlg.app.presentation.home.*"` | Manual: search + tab switch on device | Revert commit; VM/UI only, no data layer |

Note: unit tests cannot RUN until Unit 2 lands (pre-existing compile errors in 3 test files block compileDebugUnitTestKotlin). Single PR → verified at PR level.

## Phase 1: DB v7 migration (DLS-1..4)

- [ ] 1.1 RED — KastLgDatabaseTest: version==3→7 (L85), ALL.size==2→6 (L131); add fresh-v7 in-memory test asserting 3 index names via PRAGMA index_list (MT-4)
- [ ] 1.2 RED — NEW KastLgMigrationTest (room-testing 2.7.2, D3): 5→6→7 from 5.json, runMigrationsAndValidate(7,true,*ALL); edge create-at-6 + MIGRATION_6_7 only; legacy v3 + ALTER favorites ADD media_type + INSERT row → full chain → row preserved, media_type gone (MT-2/MT-3)
- [ ] 1.3 GREEN — FlixcornSeriesEntity + FlixcornEpisodeCacheEntity: `indices=[Index(value=["slug"|"episode_url"], unique=true)]`
- [ ] 1.4 GREEN — FlixcornSeriesFavoriteEntity: `indices=[Index(value=["slug"])]` non-unique (D1)
- [ ] 1.5 GREEN — KastLgDatabase.kt: version = 7
- [ ] 1.6 GREEN — DatabaseMigrations.kt: MIGRATION_6_7 (3x CREATE [UNIQUE] INDEX IF NOT EXISTS + favorites_new CREATE/INSERT/DROP/RENAME per design SQL) appended to ALL (D2)
- [ ] 1.7 Commit generated `app/schemas/com.kastlg.app.data.local.KastLgDatabase/7.json` (MT-5)

## Phase 2: Suite enablement + VM injection (MT-1, D4)

- [ ] 2.1 GREEN — FlixcornSeriesDetailViewModel: ctor + `getSeriesDetail`; factory passes `AppContainer.getFlixcornSeriesDetail` (factory sig unchanged)
- [ ] 2.2 GREEN — FlixcornEpisodeViewModel: ctor + `getEpisodeServers`; factory passes `AppContainer.getFlixcornEpisodeServers`
- [ ] 2.3 GREEN — FlixcornSeriesDetailViewModelTest: ctor arg; `resolvePlayerUrl` override in fake
- [ ] 2.4 GREEN — FlixcornEpisodeViewModelTest: ctor arg; 6-field StreamingServer (+directUrl, +serverIconUrl); `resolvePlayerUrl` override
- [ ] 2.5 GREEN — HomeViewModelTest: + `searchFlixcorn` ctor arg + minimal FakeFlixcornRepository (compile repair only)
- [ ] 2.6 VERIFY — `.\gradlew testDebugUnitTest` compiles and runs green (MT-1)

## Phase 3: Search tabs (HSB-1..4, D5-D6)

- [ ] 3.1 RED — HomeViewModelTest HSB: Movies tab + query → searchMovies called, searchTvShows/searchFlixcorn NOT; tab switch mid-search re-fires scoped search; hasActiveSearchResults per-tab; flixcornSearchLoading only on Flixcorn tab
- [ ] 3.2 GREEN — HomeUiState: `HomeTab.Flixcorn("Flixcorn")`; `hasActiveSearchResults` (D5)
- [ ] 3.3 GREEN — HomeViewModel: scoped `search()` (active-tab use case only, clears all 3 lists, loading flag per D6); `onTabSelected` cancels searchJob + re-fires when query non-blank
- [ ] 3.4 GREEN — HomeScreen: TabRow always visible (incl. search); SearchResults renders active-tab section only + "No se encontraron resultados"; loading/empty via `isLoading`/`hasActiveSearchResults` (D5)
- [ ] 3.5 VERIFY — browse-mode unchanged (HSB-4): genre chips/carousels per tab

## Phase 4: Verification

- [ ] 4.1 `.\gradlew assembleDebug` + full `.\gradlew testDebugUnitTest` green
- [ ] 4.2 Update migration-chain comments; confirm 7.json committed