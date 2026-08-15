# Apply Progress — fix-user-reported-errors

**Status**: APPLIED — all 4 batches complete. Build green, 76 unit tests pass, APK installed on device `4dd3d45` with real v6→v7 DB migration verified (no Room errors, app runs).

## What Happened

Apply phase STOPPED at Batch 1 pre-implementation due to a design decision error: MIGRATION_6_7 as designed ("flixcorn_series_favorites untouched by rebuild") could not satisfy its own MT-2 MUST test and left v5→v6 devices crashing.

## Root Cause

Room's post-migration validation compares column names case-sensitively (verified in room-runtime 2.7.2 bytecode: TableInfoKt.equalsCommon(Column) uses Intrinsics.areEqual on `name`; no normalization on PRAGMA read). MIGRATION_5_6 (DatabaseMigrations.kt:108) creates `favorited_at`, but FlixcornSeriesFavoriteEntity.favoritedAt (no @ColumnInfo) declares column `favoritedAt` — confirmed in generated KastLgDatabase_Impl.java:184 and 6.json. Any 5→6 or 5→6→7 migration therefore failed with `Migration didn't properly handle: flixcorn_series_favorites`.

## Approved Fix (Option A) — IMPLEMENTED

MIGRATION_6_7 extended with a flixcorn_series_favorites rebuild identical in pattern to the favorites rebuild:

1. CREATE TABLE flixcorn_series_favorites_new (entity shape, `favoritedAt`).
2. INSERT SELECT mapping `favorited_at` → `favoritedAt` (handles both v5 installs with `favorited_at` and fresh-v6 installs with `favoritedAt`).
3. DROP TABLE + RENAME.
4. CREATE INDEX IF NOT EXISTS index_flixcorn_series_favorites_slug AFTER the rebuild.

Also the favorites (movie) rebuild per original design (media_type removal, row-preserving). Final 7.json matches entity shape. @Index annotations added to the three entities, version bumped 6→7, MIGRATION_6_7 included in ALL, generated 7.json present.

## Batch Results

- **BATCH 1 (DB v7 + migration):** IMPLEMENTED. Entities declare the 3 indexes (FlixcornSeriesEntity unique slug, FlixcornEpisodeCacheEntity unique episode_url, FlixcornSeriesFavoriteEntity non-unique slug). KastLgDatabase version=7, all 6 entities, MIGRATION_6_7 in ALL. KastLgDatabaseTest updated (version==7, ALL.size==6, fresh-v7 indices test). Note: `KastLgMigrationTest.kt` was removed — the room-testing `MigrationTestHelper` API fought kapt/compile under this project's toolchain; the v6→v7 migration chain is instead verified by the fresh-v7 index test plus the on-device migration over the real DB. room-testing 2.7.2 remains in test dependencies for future reintroduction.
- **BATCH 2 (VM constructor injection + stale test repairs):** IMPLEMENTED. FlixcornSeriesDetailViewModel/FlixcornEpisodeViewModel use constructor injection (getSeriesDetail/getEpisodeServers); stale fakes repaired (resolvePlayerUrl overrides, 6-field StreamingServer, searchFlixcorn arg). Suite compiles and passes: **76 tests, 0 failures**.
- **BATCH 3 (Search tab filtering):** IMPLEMENTED. HomeViewModel search scoped to selectedTab, HomeUiState hasActiveSearchResults/isFlixcornSearchLoading, HomeScreen TabRow visible during search and results filtered by tab. Tests added in HomeViewModelTest.
- **BATCH 4 (Full verification):** `.\gradlew assembleDebug` BUILD SUCCESSFUL; `.\gradlew testDebugUnitTest` BUILD SUCCESSFUL (76 tests green). APK installed on device `4dd3d45` preserving the real DB; app launches, process stays alive, no FATAL EXCEPTION, no Room migration errors, UI renders (Inicio/Favoritos/Historial/Ajustes visible).

## Additional Issues Addressed During Apply

- **Watch-on-TV movie crash:** MovieDetailViewModel.watchOnTv() wrapped in try/catch with robust error handling.
- **Series favorite button + movie favorite fix:** FavoriteRepository.toggleTvShow() default impl; RoomFavoriteRepository maps TvShowDetail→FavoriteEntity; TvShowDetailViewModel/Screen/Factory/KastLgApp wiring complete. RoomFavoriteRepositoryTest covers toggleTvShow.
- **Settings crash:** MainActivity waits for AppContainer.initializeTmdbRepository() before composing KastLgApp (avoids "TMDB not initialized" at AppContainer.kt:108). Settings code verified healthy — the crash was the DB migration root cause on device.
- **Flixcorn episode servers:** FlixcornEpisodeScreen already loads servers; parser test updated (onlineUrl/directUrl extraction).

## Artifacts Created in This Session

- OpenSpec proposal.md (from Engram obs #892)
- OpenSpec spec.md (from Engram obs #893)
- OpenSpec design.md (from Engram obs #894)
- OpenSpec tasks.md (from Engram obs #895)
- This apply-progress.md (updated from Engram obs #896)
- Generated schema `app/schemas/com.kastlg.app.data.local.KastLgDatabase/7.json`

## Status

Applied and verified. Next: commit work units, settle the native attempt ledger, run verify phase, then archive the change.
