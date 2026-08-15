# Design: Watched Tracking & Series Episode Flow

## Technical Approach

Additive Room v7→v8 migration creates `watched_movies` + `watched_episodes` (W1–W4); favorites untouched. New `WatchedRepository` mirrors `RoomFavoriteRepository` and feeds MovieDetail + Flixcorn episode ViewModels. TMDB episode taps resolve a Flixcorn slug via `searchSeries` best-match (T2–T5), then navigate to the existing `FlixcornEpisodeRoute`. Server list sorts online-first (F1); next-episode reloads in place (F2–F3).

## Architecture Decisions

| Option | Tradeoff | Decision |
|---|---|---|
| Two watched tables vs one generic | Two match spec keys (movieId; slug+season+episode), no polymorphic keys | Two entities, one DAO each |
| New `WatchedRepository` vs methods on `FavoriteRepository` | New interface keeps single responsibility, mirrors `RoomFavoriteRepository` | New `WatchedRepository` + `RoomWatchedRepository` |
| Slug resolution: use case + ViewModel nav event vs nav-layer | Use case is unit-testable; ViewModel emits event like `MovieDetailViewModel` | `ResolveFlixcornSeriesSlugUseCase`; `NavigationEvent` |
| Next-episode via cached series detail vs blind ep+1 | Series detail (24h cache) gives exact per-season counts for boundary + hide state | `FlixcornEpisodeViewModel` loads detail, stores `nextEpisode` |
| Online-first sort in parser vs UI | Pure `sortedOnlineFirst()` replaces name-priority sort; repo re-sorts cache hits (legacy caches) | Parser + repo |

## Data Flow

```
Episode tap → TvShowDetailViewModel → ResolveFlixcornSeriesSlugUseCase → searchSeries(title)
  → best-match slug → NavigationEvent → FlixcornEpisodeRoute(slug, s, e)
  → FlixcornEpisodeViewModel → getEpisodeServers → sortedOnlineFirst → ServerList
  watched toggle ⇄ WatchedRepository ⇄ Room watched_* tables
```

## File Changes

| File | Action | Description |
|---|---|---|
| `data/local/WatchedMovieEntity.kt`, `WatchedEpisodeEntity.kt` | Create | Entities: `watched_movies` (movieId PK), `watched_episodes` (slug+season+episode PK) |
| `data/local/WatchedMovieDao.kt`, `WatchedEpisodeDao.kt` | Create | `observeExists`/`exists`/`upsert`/`delete`/`toggle` (FavoriteDao pattern) |
| `data/local/KastLgDatabase.kt` | Modify | version=8; register entities + DAOs |
| `data/local/DatabaseMigrations.kt` | Modify | `MIGRATION_7_8` (CREATE TABLE only); append to `ALL` |
| `domain/repositories/WatchedRepository.kt`, `data/repository/RoomWatchedRepository.kt` | Create | observe + toggle (Mutex, `now()`) |
| `domain/usecases/ResolveFlixcornSeriesSlugUseCase.kt` | Create | searchSeries → best-match slug or null |
| `data/remote/flixcorn/StreamingServer.kt`, `FlixcornHtmlParser.kt`, `data/repository/FlixcornRepositoryImpl.kt` | Modify | `sortedOnlineFirst()`; parser replaces `sortServersByPriority`; repo re-sorts cache hits |
| `presentation/detail/MovieDetailUiState.kt` + `MovieDetailViewModel.kt` + Factory + Screen | Modify | `isWatched`; observe + toggle; full-width "Marcar como visto"/"Quitar de vistos" OutlinedButton |
| `presentation/flixcorn/FlixcornEpisodeViewModel.kt` + Screen | Modify | `isWatched` toggle, `nextEpisode`, `loadNextEpisode()`; watched chip + "Siguiente episodio" button; "Ver Online"/"Link Directo" labels |
| `presentation/tvdetail/TvShowDetailUiState.kt` + ViewModel + Factory + Screen | Modify | delete Ver en TV button + TV feedback (~378–415); episode tap → resolve flow; `isResolvingEpisode`, `episodeNotice`, `NavigationEvent` |
| `presentation/KastLgApp.kt`, `di/AppContainer.kt` | Modify | factory args; collect TvShowDetail nav event; `watchedRepository`, `resolveFlixcornSeriesSlug` |
| `app/schemas/.../8.json` | Create | Room export (build-generated) |
| Tests (see Testing) | Create/Modify | ~6 files |

## Interfaces / Contracts

```sql
CREATE TABLE IF NOT EXISTS `watched_movies` (
  `movieId` INTEGER NOT NULL, `watchedAt` INTEGER NOT NULL, PRIMARY KEY(`movieId`));
CREATE TABLE IF NOT EXISTS `watched_episodes` (
  `slug` TEXT NOT NULL, `season` INTEGER NOT NULL, `episode` INTEGER NOT NULL,
  `watchedAt` INTEGER NOT NULL, PRIMARY KEY(`slug`, `season`, `episode`));
```

DAOs mirror `FavoriteDao`: `@Query("SELECT EXISTS(...)") observeExists(...): Flow<Boolean>` / `suspend exists(...)`; `@Upsert upsert(entity)`; `DELETE WHERE ...`; `@Transaction open suspend fun toggle(entity) { if (exists) delete else upsert }`.

```kotlin
interface WatchedRepository {
    fun observeIsMovieWatched(movieId: Int): Flow<Boolean>
    fun observeIsEpisodeWatched(slug: String, season: Int, episode: Int): Flow<Boolean>
    suspend fun toggleMovie(movieId: Int)
    suspend fun toggleEpisode(slug: String, season: Int, episode: Int)
}
```

```kotlin
fun List<StreamingServer>.sortedOnlineFirst(): List<StreamingServer> = sortedWith(
    compareByDescending<StreamingServer> { it.onlineUrl?.contains("/player/") == true }
        .thenByDescending { it.directUrl?.contains("/external/") == true }
        .thenBy { it.serverName.lowercase() })
```

`ResolveFlixcornSeriesSlugUseCase`: `suspend operator fun invoke(title): FlixcornResult<String?>` — on Success picks `results.firstOrNull { normalize(it.title) == normalize(title) } ?: results.firstOrNull()` (normalize: lowercase, strip accents/punctuation); empty/Error → null / passthrough.

Next-episode: `FlixcornEpisodeViewModel.loadNextEpisode()` → `getFlixcornSeriesDetail(slug)` (cached) → same season ep+1 if present, else season+1 ep1 if `season < numberOfSeasons`, else hide button.

## Testing Strategy

| Layer | What | Approach |
|---|---|---|
| Unit | `sortedOnlineFirst` | player-first, external fallback, no-online, name tiebreak |
| Unit | `bestMatch` | exact normalized, first-match fallback, empty→null, Error passthrough (fake repo) |
| Unit | Next-episode | `FlixcornEpisodeViewModelTest`: same-season, season-cross, last-season hidden, load-fail retry |
| Integration | Migration | Robolectric: v7 with favorite → reopen v8 preserves it, tables writable; fresh v8 (no migration runs) |
| Integration | Watched DAO/repo | `RoomWatchedRepositoryTest`: toggle/observe, concurrency parity, composite key (mirror `RoomFavoriteRepositoryTest`) |
| Regression | Existing 76 | `testDebugUnitTest` green; update `ALL.size` assert 6→7 |

## Threat Matrix

N/A — no network routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary; navigation is in-app Compose only.

## Migration / Rollout

CREATE TABLE only → reversible by dropping `MIGRATION_7_8` from `ALL`; no data rewrite. Rollback: revert commit. Devices stuck on v9 (issues 1/3) still need uninstall/reinstall — Room rejects 9→8 downgrade; out of scope per proposal.

## Open Questions

- [ ] Next-episode: hide vs disable at end-of-series (spec allows either) — default: hide.
- [ ] Confirm Ver en TV removal also drops the TV feedback banner block on `TvShowDetailScreen` (TV messaging becomes unused there).
