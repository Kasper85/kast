# Proposal: Fix episode server list stuck in loading forever

## Intent

Episode server lists never render: entering a series → season → episode shows a spinner indefinitely, and neither the server list, the empty state, nor the error state is ever reachable. Root cause is a UI-state bug (not a parsing bug): `FlixcornEpisodeUiState.isLoading` defaults to `true` and is never cleared when `loadServers()` completes, so the screen's `when` branch on `isLoading` always wins. Logs prove the data path works (`almost-paradise s2e1 servers=2`, `wonder-man s1e1 servers=9`).

## Scope

### In Scope
- Fix loading-state transitions in `FlixcornEpisodeViewModel` (default `isLoading=false`; clear it via `.copy()` in Success and Error branches of `loadServers()`).
- Fix the identical bug in `FlixcornSeriesDetailViewModel` (same default-true/never-cleared pattern — verified present).
- Empty state: clear message + back action when an episode has no servers.
- Error state: message + retry button (already partially present in screen; keep + verify).
- Cache: re-entering an already-viewed episode uses cached servers instantly (no re-fetch) — ensure state replacement preserves servers across retries/re-entry.
- Add `FlixcornEpisodeViewModelTest` (+ `FlixcornSeriesDetailViewModelTest`) covering Success, Error, empty, cache-hit, and retry.

### Out of Scope
- Parser/scraper changes (data path proven working).
- Movie/server-list screens — verified clean: `FlixcornHomeViewModel`, `MovieDetailViewModel`, `TvShowDetailViewModel`, `HomeViewModel`, `FavoritesViewModel`, `HistoryViewModel` all set `isLoading=false` on completion.
- Room migration crashes (schemas 6-9), unrelated env issue.

## Capabilities

> Contract with sdd-spec. No existing specs found under `openspec/specs/` — this change introduces one new capability.

### New Capabilities
- `flixcorn-server-list`: episode (and series-detail) server-list loading, empty, error, cache-hit, and retry state behavior.

### Modified Capabilities
- None

## Approach

Minimal state-semantics fix: `isLoading` is an explicit async flag. Set the default to `false`, and on both completion branches of `loadServers()`/`loadSeries()` replace state via `.copy(isLoading = false, ...)` instead of constructing a fresh `FlixcornEpisodeUiState(...)`. Using `.copy` also preserves `selectedLanguage` and TV-message fields (avoiding future regressions). Fix both Flixcorn ViewModels, keep the screen `when`-branch order (loading → error → empty → list) which becomes correct once `isLoading` clears. Add JUnit4 ViewModel tests (`./gradlew testDebugUnitTest`).

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `presentation/flixcorn/FlixcornEpisodeViewModel.kt` | Modified | `isLoading=false` default + `.copy()` in both branches (lines 12-20, 30-44) |
| `presentation/flixcorn/FlixcornSeriesDetailViewModel.kt` | Modified | Same fix (lines 12-16, 24-38) |
| `presentation/flixcorn/FlixcornEpisodeScreen.kt` | Modified | Empty state: add back action (lines 151-162); verify error retry (146) |
| `presentation/flixcorn/FlixcornSeriesDetailScreen.kt` | Modified | Verify error/retry path once isLoading clears |
| `presentation/flixcorn/FlixcornEpisodeViewModelTest.kt` | New | Unit tests (missing today) |
| `presentation/flixcorn/FlixcornSeriesDetailViewModelTest.kt` | New | Unit tests |
| `data/repository/FlixcornRepositoryImpl.kt` | Verify-only | Cache path (lines 61-91) must serve cached servers on re-entry |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Fix lands on top of uncommitted Flixcorn WIP | Med | Keep change surgical; review full diff before apply |
| `.copy` changes behavior for `selectedLanguage`/TV state | Low | Unit tests assert field preservation |
| Room migration crash (schemas 6-9) blocks runtime verification | Med | Rely on unit tests + compile; note for device check |
| Series-detail fix surfaces its own UI issues (both screens were stuck) | Med | Same test coverage pattern applied to both ViewModels |

## Rollback Plan

Revert the two ViewModel files and screen edits (all changes are in-module, isolated to `presentation/flixcorn/`). No schema/data migration involved — `git checkout` of the affected files restores prior behavior. WIP is uncommitted; ensure a stash/backup before applying.

## Dependencies

- None external. Test runner: `./gradlew testDebugUnitTest` (JUnit4 + kotlinx-coroutines-test).

## Success Criteria

- [ ] Entering a series → season → episode renders the server list (or empty/error state) instead of an infinite spinner.
- [ ] Episode with no servers shows clear message + back action.
- [ ] Network/scrape failure shows error + retry; retry recovers.
- [ ] Re-entering an already-viewed episode shows cached servers instantly (no re-fetch).
- [ ] Series-detail screen also resolves its loading state.
- [ ] New ViewModel unit tests pass; `./gradlew testDebugUnitTest` green.
