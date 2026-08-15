# Delta Specs — fix-user-reported-errors

Root cause (verified on disk): MIGRATION_4_5 (DatabaseMigrations.kt:78,91) creates UNIQUE indexes `index_flixcorn_series_slug` / `index_flixcorn_episode_cache_episode_url`, but FlixcornSeriesEntity/FlixcornEpisodeCacheEntity declare NO @Index — v6 schema (6.json) expects zero indices → Room post-migration validation throws `Migration didn't properly handle: flixcorn_series` on every existing install (user issues 2–7). Legacy device DB (user_version=3) favorites also carries an extra `media_type` column absent from FavoriteEntity (6.json:8-59) → would still fail after index fix. Unit suite does not compile (testDebugUnitTest fails at compileDebugUnitTestKotlin).

## 1. data-local-storage (DB v7)

Context: KastLgDatabase.kt version=6→7; DatabaseMigrations.ALL (5 migrations, DatabaseMigrations.kt:116) gains MIGRATION_6_7.

- DLS-1 (MUST) Entities declare the unique indices MIGRATION_4_5 already creates: @Index(unique=true) on FlixcornSeriesEntity.slug → `index_flixcorn_series_slug`; on FlixcornEpisodeCacheEntity.episodeUrl → `index_flixcorn_episode_cache_episode_url`. Scenario: fresh in-memory v7 build opens without IllegalStateException; getBySlug/getByUrl DAO lookups still resolve.
- DLS-2 (MUST) MIGRATION_6_7 creates both unique indexes via `CREATE UNIQUE INDEX IF NOT EXISTS` with exact names above. Scenario: v6 DB (no indices) → after 6_7, PRAGMA index_list shows both; validation passes. Edge: DB already carrying the indices (v4→5 path) → IF NOT EXISTS, no error.
- DLS-3 (MUST) MIGRATION_6_7 rebuilds `favorites` dropping legacy `media_type`, preserving rows: CREATE `favorites_new` (v6 shape) → INSERT INTO favorites_new (tmdb_id,title,poster_url,overview,release_date,vote_average,favorited_at) SELECT same FROM favorites → DROP favorites → ALTER RENAME. Scenario: v6 with 2 movie favorites (keyed tmdb_id) → 6_7 keeps both rows; schema matches 7.json. Scenario: legacy 3→…→7 with media_type rows → rows preserved, media_type dropped, validates at 7. Edge: empty favorites → empty rebuilt table, no failure. Edge: flixcorn_series_favorites (slug-keyed) untouched by rebuild.
- DLS-4 (MUST) DatabaseMigrations.ALL includes MIGRATION_6_7; version=7. Rollback = revert commit (baseline 401f48e acceptable; app broken on device today).

## 2. migration-testing

Context: stale code prevents compilation. HomeViewModelTest.kt:116-125 (createViewModel omits required `searchFlixcorn` ctor arg), FlixcornEpisodeViewModelTest.kt:135-140 (removed `getEpisodeServers` ctor arg), :142-147 (StreamingServer 4-arg ctor; class now has 6 fields incl. directUrl/serverIconUrl), fakes FlixcornEpisodeViewModelTest.kt:149-169 and FlixcornSeriesDetailViewModelTest.kt:103-123 (missing `resolvePlayerUrl` override — FlixcornRepository.kt:17), FlixcornSeriesDetailViewModelTest.kt:68-74 (removed `getSeriesDetail` ctor arg). Stale assertions KastLgDatabaseTest.kt:85 (version==3 → 7) and :131 (ALL.size==2 → 6).

- MT-1 (MUST) Repair the four stale test files so `gradlew testDebugUnitTest` compiles and runs green.
- MT-2 (MUST) MigrationTestHelper regression 5→6→7 (room-testing 2.7.2 present; 5.json/6.json committed; 7.json generated at build): create at v5 from 5.json, run MIGRATION_5_6 + MIGRATION_6_7, `runMigrationsAndValidate(7)`. Scenario: passes; PRAGMA index_list shows both unique indices. Edge: also validate create-at-v6 + MIGRATION_6_7 only.
- MT-3 (MUST) Legacy path: create at v3 from 3.json, simulate device divergence (ALTER TABLE favorites ADD COLUMN media_type TEXT; insert favorite row with media_type), run full chain 3→4→5→6→7, validate at 7 → row preserved, media_type gone. Edge: empty media_type value.
- MT-4 (MUST) Fresh-install parity: in-memory build at v7 opens without IllegalStateException (entities+indices consistent).
- MT-5 (SHOULD) Commit generated 7.json to app/schemas/ so future diffs are possible.

## 3. home-search-browsing

Context: browse mode already filters by tab (HomeScreen.kt:277-323); SearchResults (HomeScreen.kt:584-696) renders movies+series+flixcorn unconditionally regardless of selectedTab; tab row hidden during search (HomeScreen.kt:141); HomeTab (HomeUiState.kt:8-11) lacks Flixcorn; search() (HomeViewModel.kt:186-232) fires all three use cases and stores all three lists.

- HSB-1 (MUST) HomeTab gains `Flixcorn("Flixcorn")`; tab row MUST remain selectable during search. Mapping: Movies→searchResults, Series→searchTvResults, Flixcorn→flixcornResults. SearchResults renders ONLY the active tab's section (header + items). Scenario: Movies tab + query → "Películas" section only; Series → series only; Flixcorn → flixcorn only.
- HSB-2 (MUST) Empty-state correctness: when active tab's list is empty but others have results → show "No se encontraron resultados"; hasSearchResults evaluated against active tab only.
- HSB-3 (SHOULD) search() invokes only the active tab's use case (no off-tab network calls); tab switch during search re-fires scoped search. Scenario: Movies tab, query "Dune" → searchMovies called; searchTvShows/searchFlixcorn NOT called. Edge: token missing / no network → existing toActionableMessage error path, no crash.
- HSB-4 (MUST) Browse-mode behavior unchanged (genre chips per tab, carousels, tab switch re-render).

## Non-goals

TMDB init race (separate change). E2E androidTest. Flixcorn browse-mode carousel content on Home (Flixcorn tab in browse MAY show placeholder/empty state).

## Out of scope / deferred

Removing MIGRATION_4_5 index lines (rejected alternative A). Adding media_type to FavoriteEntity (rejected alternative B). 4.json schema export. Device-data backfill of media_type. Manual device reinstall pass (apply/verify phases).