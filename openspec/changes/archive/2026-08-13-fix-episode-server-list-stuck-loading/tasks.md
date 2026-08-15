# Tasks: Fix episode server list stuck in loading forever

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~300-340 (2 VM files ~55, screen ~15, 2 test files ~230) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|-----------|----------------------|-----------------|-------------------|
| 1 | Single PR: test seam + state fix + tests + UI back action | PR 1 | `./gradlew testDebugUnitTest` | N/A — JUnit4 VM tests, no device needed | `git checkout` of 5 files (all in-module, isolated to presentation/flixcorn) |

## Phase 1: Test Infrastructure & RED Tests

- [x] 1.1 Create `app/src/test/java/com/kastlg/app/presentation/flixcorn/FlixcornEpisodeViewModelTest.kt` with `MainDispatcherRule` + `runTest`/`runCurrent` and a `FakeFlixcornRepository` (mutable `FlixcornResult` queue + `callCount`) wrapping `GetFlixcornEpisodeServers` — mirror `HomeViewModelTest` pattern.
- [x] 1.2 Add RED tests to `FlixcornEpisodeViewModelTest`: Success → `isLoading=false` + servers; Error → `error` set, retry then Success recovers; empty servers → `isLoading=false`; re-entry → settles with cached servers; `selectedLanguage` survives `loadServers()` (specs: Episode server list, no-servers, failure+retry, cache, language-preservation).
- [x] 1.3 Create `FlixcornSeriesDetailViewModelTest.kt` with same fake-repo pattern and RED tests: Success → `isLoading=false` + series; Error → `isLoading=false` + retry clears error (specs: Series detail success/fail).

## Phase 2: GREEN — ViewModel State Fix

- [x] 2.1 `FlixcornEpisodeViewModel.kt`: add ctor param `getEpisodeServers: GetFlixcornEpisodeServers = AppContainer.getFlixcornEpisodeServers`; change UiState default to `isLoading: Boolean = false`.
- [x] 2.2 `FlixcornEpisodeViewModel.kt` `loadServers()`: load-start `_uiState.value.copy(isLoading = true, error = null)`; Success → `.copy(isLoading = false, servers = result.data)`; Error → `.copy(isLoading = false, error = "...")`. Factories unchanged.
- [x] 2.3 `FlixcornSeriesDetailViewModel.kt`: same three changes — ctor param `getSeriesDetail = AppContainer.getFlixcornSeriesDetail`, UiState default `isLoading=false`, `.copy()` in load-start/Success/Error of `loadSeries()`.

## Phase 3: UI Wiring

- [x] 3.1 `FlixcornEpisodeScreen.kt` empty branch (lines 151-162): add "Volver" button calling `onBack` (top-bar back exists; spec requires in-body action).
- [x] 3.2 Verify-only: `FlixcornSeriesDetailScreen.kt` error/retry (line 112) and `FlixcornRepositoryImpl` cache (lines 61-91) become reachable once `isLoading` clears — no code change expected.

## Phase 4: Verification

- [x] 4.1 Run `./gradlew testDebugUnitTest` — all new RED tests go GREEN; no regressions.
- [x] 4.2 If Room migration crash (schemas 6-9) blocks device run, fallback `./gradlew compileDebugKotlin` for compile proof; note device check pending.
- [x] 4.3 Confirm no production call-site churn: grep that factories still construct VMs with only slug/season/episode args.
