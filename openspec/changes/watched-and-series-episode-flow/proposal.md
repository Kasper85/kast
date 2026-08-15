# Proposal: Watched tracking & series episode flow

## Intent

Fix 4 user issues. (1) Favorite save error and (3) Settings crash are device-side: installed DB is v9 (old schema), code declares v7, Room refuses 9→7 downgrade, so all DB calls fail. Fix: uninstall + fresh install (no code). (2) Mark movies/episodes watched (persisted) and (4) TMDB series episode flow are code.

## Scope

**In**
- Watched: `watched_movies` + `watched_episodes` tables, DB v7→v8 + MIGRATION_7_8, DAOs, repository methods, ViewModel state, "Marcar como visto" toggle on movie detail + episode server list. Persisted; first slice: mark/unmark only.
- Series flow: remove "Ver en TV" from TvShowDetailScreen (~378–397; movies keep theirs); TMDB episode tap → Flixcorn title search → slug → episode server list; no-match notice; "Siguiente episodio" button (same season, else season+1 episode 1 if more seasons); online-first server ordering ("Ver Online" /player/ URLs, "Link Directo" fallback).

**Out**
- Movie "Ver en TV" stays. No new search UI, offline sync, or watched-filter lists.

## Capabilities

**New**
- `watched-tracking`: persistence + toggle UI. Episode keyed by (series slug, season, episode); movie by id.
- `tmdb-series-episode-flixcorn`: episode tap → slug resolution → server list; no-match notice; "Ver en TV" removal.

**Modified**
- `flixcorn-server-list`: online-first priority + next-episode navigation.

## Approach

1. DB v8: new entities/DAOs; MIGRATION_7_8 (CREATE TABLE only); favorites untouched.
2. Watched repo methods mirroring RoomFavoriteRepository.
3. State in MovieDetailViewModel + FlixcornEpisodeViewModel.
4. Episode tap: `searchSeries(title)` → best-match slug → `getEpisodeServers`; else notice.
5. Sort online-first; next-episode reloads next (season, episode).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| data/local/KastLgDatabase.kt | Modified | v7→8, +entities/DAOs |
| data/local/DatabaseMigrations.kt | Modified | MIGRATION_7_8 |
| data/local/WatchedMovie/WatchedEpisode + DAOs | New | persistence |
| domain/repositories + data/repository | Modified | watched + slug-resolution |
| presentation/detail/MovieDetailScreen.kt | Modified | watched toggle |
| presentation/tvdetail/TvShowDetailScreen.kt | Modified | Ver en TV removal; episode tap |
| presentation/flixcorn/FlixcornEpisodeScreen.kt | Modified | toggle, next-episode, priority |
| app/src/test/… | Modified | migration + flow tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Migration breaks data | Low | CREATE TABLE only; tests |
| Scraper fragility | Med | Rate-limited; error notices |
| Wrong slug match | Med | Best-match; notice |
| Stale episode cache | Low | Keep 60-min TTL |

## Rollback Plan

Revert commit → DB back to v7; existing tables untouched. Reinstall independently fixes issues 1 & 3.

## Dependencies

None external. Fresh device install required for issues 1 & 3.

## Success Criteria

- [ ] `testDebugUnitTest` green incl. MIGRATION_7_8 + fresh v8 validation
- [ ] `assembleDebug` builds
- [ ] Post-reinstall: favorite save + Settings don't crash
- [ ] Watched marks persist across restart
- [ ] Episode tap opens Flixcorn servers; no-match notice shows
- [ ] Next-episode + online-first verified

## Proposal question round (assumptions)

- A: slug = best-match title search (user-approved).
- B: episode keyed (slug, season, episode); movie by id.
- C: next-episode crosses season only if more seasons exist.
- D: online-first = onlineUrl ranked first, directUrl fallback.