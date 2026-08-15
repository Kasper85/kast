# Proposal: Fix user-reported errors (7 issues)

## Intent

Existing installs can no longer open their DB: MIGRATION_4_5 creates UNIQUE indexes on flixcorn_series and flixcorn_episode_cache that the entities never declared, so Room post-migration validation throws `Migration didn't properly handle: flixcorn_series`. Every data-touching screen fails on device (user issues 2–7: TV crash, favorites, history, settings, episode servers). Issue 1 is a real product gap: search shows mixed media while tabs promise filtered browsing. Fix the root cause so the app works on device and the unit suite compiles again.

## Scope

**In**
- DB v7: add `@Index(unique = true)` to FlixcornSeriesEntity + FlixcornEpisodeCacheEntity; MIGRATION_6_7 creates missing indices and rebuilds favorites dropping legacy `media_type` (preserving rows).
- Fix stale test fakes/assertions; add 5→6→7 and legacy 3→…→7 migration regression tests.
- Search: Movies/Series/Flixcorn tabs filter SearchResults.

**Out**
- TMDB init race (deferred, separate change).
- E2E androidTest suite.
- Features beyond the 7 reported issues.

## Capabilities

**New**: None.

**Modified**
- `data-local-storage`: Room v7 schema, flixcorn indices, favorites rebuild.
- `home-search-browsing`: tab-filtered search results.
- `migration-testing`: regression tests for 5→6→7 + legacy path.

## Approach

1. Add `@Index(unique=true)` to both flixcorn entities; bump DB version to 7.
2. MIGRATION_6_7: `CREATE INDEX IF NOT EXISTS` for the two unique indices; rebuild favorites (CREATE new table w/o media_type → INSERT INTO…SELECT → DROP → RENAME) so legacy device DBs (user_version=3, path 3→4→5→6→7) validate.
3. Fix stale fakes (FlixcornEpisodeViewModelTest 139/146/149, FlixcornSeriesDetailViewModelTest 73/103, HomeViewModelTest 124) and assertions (KastLgDatabaseTest 85/131); add migration regression tests asserting v7 schema for fresh and legacy paths.
4. Filter SearchResults by selectedTab (HomeScreen.kt ~584–696).
5. Verify: `assembleDebug` + `testDebugUnitTest`; device reinstall; manual pass of all 7 issues.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| data/local/FlixcornSeriesEntity.kt | Modified | + @Index(unique) on slug |
| data/local/FlixcornEpisodeCacheEntity.kt | Modified | + @Index(unique) on episodeUrl |
| data/local/DatabaseMigrations.kt | Modified | MIGRATION_6_7 |
| data/local/KastLgDatabase.kt | Modified | version 6→7 |
| data/local/FavoriteEntity.kt | Modified | align with favorites rebuild |
| presentation/home/HomeScreen.kt | Modified | search tab filter |
| app/src/test/… | Modified | stale fakes, assertions, new migration tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Migration drops user data | Med | INSERT…SELECT rebuild; regression tests on legacy fixtures |
| Unique index creation fails on existing duplicates | Low | Verify device data; fallback drop+rebuild index |
| Review budget (800 lines) | Med | Work-unit commits; chained PR if forecast high |

## Rollback Plan

Revert commit → DB back to v6 schema (entities+version). App today is broken on device, so reverting to 401f48e baseline is acceptable; worst case uninstall/reinstall. Data loss limited to rebuilt favorites table.

## Dependencies

None external.

## Success Criteria

- [ ] `testDebugUnitTest` green incl. new migration tests
- [ ] `assembleDebug` builds
- [ ] Fresh + existing installs open DB at v7; issues 2–7 resolved on device
- [ ] Search tabs filter results (issue 1)
- [ ] All 7 issues verified on device reinstall

## Proposal question round (assumptions to review)

- A: add @Index to entities vs drop index lines from MIGRATION_4_5 — chosen: add @Index (keeps 4_5 intent, fast lookups). If rejected, scope shrinks to removing two index lines.
- B: drop `media_type` column vs add it to FavoriteEntity — chosen: drop (matches current entity contract).
- Assumption: device favorites `media_type` column carries no data worth keeping.