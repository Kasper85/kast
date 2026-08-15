# Tasks: Watched tracking & series episode flow

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1000 (950–1100) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 |
| Delivery strategy | ask-on-risk |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | DB v8: entities, DAOs, MIGRATION_7_8, schema 8.json + migration tests | PR 1 | `.\gradlew testDebugUnitTest --tests "com.kastlg.app.data.local.*"` | N/A — Robolectric in-memory DB; device pass in Phase 5 | Revert commit; CREATE TABLE only, v7 DB intact |
| 2 | WatchedRepository + RoomWatchedRepository + tests | PR 1 | `.\gradlew testDebugUnitTest --tests "com.kastlg.app.data.repository.RoomWatchedRepositoryTest"` | N/A — Room in-memory; restart check Phase 5 | Revert commit; no UI depends on it yet |
| 3 | Slug resolution + TvShowDetail episode flow + nav wiring | PR 2 | `.\gradlew testDebugUnitTest --tests "com.kastlg.app.domain.usecases.*" --tests "com.kastlg.app.presentation.tvdetail.*"` | Manual: TMDB series → tap episode → Flixcorn servers; no-match notice | Revert commit; MovieDetail + Flixcorn screens untouched |
| 4 | Watched toggle UI + online-first sort + next-episode | PR 3 | `.\gradlew testDebugUnitTest --tests "com.kastlg.app.presentation.flixcorn.*" --tests "com.kastlg.app.presentation.detail.*" --tests "com.kastlg.app.data.remote.flixcorn.*"` | Manual: mark watched → restart persists; Siguiente episodio; Ver Online first | Revert commit; tables unused by UI, no data rewrite |
| 5 | Device fix: uninstall + fresh install (issues 1 & 3) | PR 3 | `.\gradlew testDebugUnitTest` + `.\gradlew assembleDebug` | Manual checklist: favorite save, Settings open, watched persists, episode flow | N/A — no code change; fresh install IS the fix |

## Phase 1: DB v8 foundation (W4)

- [x] 1.1 RED — KastLgDatabaseTest: version 7→8, ALL.size 6→7; fresh-v8 in-memory test: watched tables exist + writable (W4 fresh install)
- [x] 1.2 RED — NEW KastLgMigrationTest (Robolectric + room-testing): v7 with favorite row from 7.json → runMigrationsAndValidate(8,true,*ALL) → favorites preserved, watched tables empty (W4 upgrade)
- [x] 1.3 GREEN — WatchedMovieEntity.kt + WatchedEpisodeEntity.kt (movieId PK; slug+season+episode PK; watchedAt)
- [x] 1.4 GREEN — WatchedMovieDao.kt + WatchedEpisodeDao.kt: observeExists/exists/upsert/delete/toggle (FavoriteDao pattern)
- [x] 1.5 GREEN — KastLgDatabase.kt: version=8; register entities + DAOs
- [x] 1.6 GREEN — DatabaseMigrations.kt: MIGRATION_7_8 (2x CREATE TABLE IF NOT EXISTS) appended to ALL
- [x] 1.7 Commit generated `app/schemas/.../8.json`

## Phase 2: Watched repository

- [x] 2.1 RED — NEW RoomWatchedRepositoryTest: movie toggle/observe, episode toggle/observe, composite key, concurrency (mirror RoomFavoriteRepositoryTest)
- [x] 2.2 GREEN — domain/repositories/WatchedRepository.kt (observeIsMovieWatched/observeIsEpisodeWatched/toggleMovie/toggleEpisode)
- [x] 2.3 GREEN — data/repository/RoomWatchedRepository.kt (Mutex + now(), mirror RoomFavoriteRepository)
- [x] 2.4 GREEN — di/AppContainer.kt: expose `watchedRepository`

## Phase 3: Series episode flow (T1–T5)

- [x] 3.1 RED — NEW ResolveFlixcornSeriesSlugUseCaseTest (fake repo): exact normalized match, first-match fallback, empty→null, Error passthrough
- [x] 3.2 GREEN — domain/usecases/ResolveFlixcornSeriesSlugUseCase.kt (normalize: lowercase, strip accents/punctuation; else first result)
- [x] 3.3 RED — TvShowDetailViewModelTest: episode tap → NavigationEvent; isResolvingEpisode/loading; episodeNotice no-match (T2/T4/T5)
- [x] 3.4 GREEN — TvShowDetailUiState.kt + ViewModel + Factory: episode tap → resolve → NavigationEvent(slug,s,e); loading + notice states
- [x] 3.5 GREEN — TvShowDetailScreen.kt: delete Ver en TV button + TV feedback block (~378–415); episode tap; loading/notice UI (T1)
- [x] 3.6 GREEN — presentation/KastLgApp.kt: collect NavigationEvent → FlixcornEpisodeRoute(slug, season, episode) (T3)
- [x] 3.7 VERIFY — MovieDetailScreen still shows Ver en TV (T1 regression)

## Phase 4: Watched UI + server list (W1/W2, F1–F3)

- [x] 4.1 RED — sortedOnlineFirst tests: player-first, external fallback, no-online, name tiebreak (F1)
- [x] 4.2 GREEN — StreamingServer.kt `sortedOnlineFirst()`; FlixcornHtmlParser.kt replace sortServersByPriority; FlixcornRepositoryImpl.kt re-sort cache hits (F1)
- [x] 4.3 RED — FlixcornEpisodeViewModelTest: next-episode same-season, season-cross, last-season hidden, load-fail retry (F2/F3)
- [x] 4.4 GREEN — FlixcornEpisodeViewModel.kt: isWatched observe/toggle; loadNextEpisode() via getFlixcornSeriesDetail (24h cache) + getEpisodeServers (F2/F3)
- [x] 4.5 GREEN — FlixcornEpisodeScreen.kt: watched chip, "Siguiente episodio" button (hide at end), "Ver Online"/"Link Directo" labels (W2/F1/F2)
- [x] 4.6 RED — MovieDetailViewModelTest: isWatched observe + toggle (W1)
- [x] 4.7 GREEN — MovieDetailUiState + ViewModel + Factory + Screen: full-width "Marcar como visto"/"Quitar de vistos" OutlinedButton (W1)

## Phase 5: Verification + device

- [ ] 5.1 `.\gradlew testDebugUnitTest` full green — existing 76 + new (regression)
- [ ] 5.2 `.\gradlew assembleDebug` builds
- [ ] 5.3 Manual — uninstall + fresh install: favorite save OK (issue 1), Settings opens (issue 3), no DB crash
- [ ] 5.4 Manual — watched persists across restart (W3); movie + episode toggles (W1/W2)
- [ ] 5.5 Manual — episode tap → servers; no-match notice; next-episode; online-first order (T3/T4/F1/F2)

## Dependencies

- Phase 1 → Phase 2 (repo needs DAOs) → Phase 4 (UI toggles need repo)
- Phase 3 independent of Phases 1–2 (only shares AppContainer entry); can run after Phase 1
- Phase 5 needs all prior phases; device step (5.3) independent of code
- 1.1 (ALL.size 6→7) precedes every migration compile
