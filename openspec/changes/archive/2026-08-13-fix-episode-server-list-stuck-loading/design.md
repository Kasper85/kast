# Design: Fix episode server list stuck in loading forever

## Technical Approach

Surgical state-model fix for both `FlixcornEpisodeUiState` and `FlixcornSeriesDetailUiState`: default `isLoading = false` and replace state on every async completion branch via `.copy(isLoading = false, ...)` instead of constructing a fresh state (which re-defaults `isLoading = true`). A minimal testability refactor moves the two usecases from static `AppContainer` access into constructor params, matching the `HomeViewModel`/`MovieDetailViewModel` injection pattern. Add JUnit4 ViewModel tests for both paths.

## Architecture Decisions

| Decision | Choice | Alternatives | Rationale |
|----------|--------|--------------|-----------|
| Completion branches | `.copy(isLoading = false, servers/error = ...)` in Success/Error | Fresh `FlixcornUiState(...)` construction | Fresh construction re-applies the `isLoading = true` default (the bug). `.copy` also preserves `selectedLanguage`/TV fields per spec. |
| Loading trigger | `.copy(isLoading = true, error = null)` | Fresh `FlixcornUiState(isLoading = true)` | Fresh construction wipes `selectedLanguage`/TV fields on every retry; `.copy` preserves them and clears stale error so the spinner shows. |
| Test seam | Constructor-inject `GetFlixcornEpisodeServers` / `GetFlixcornSeriesDetail` (default = `AppContainer.<usecase>`) | Keep static `AppContainer`, add an AppContainer override hook | Matches existing convention; zero production-call-site churn (factories unchanged); lets tests pass a fake repo wrapped in a real usecase. `sendToTv` keeps static access (not in test scope). |
| Cache-hit test level | VM test asserts re-entry settles to cached servers (loading=false, servers kept); repository no-fetch stays verify-only | Room in-memory repo test for `getEpisodeServers` | Cache lives in `FlixcornRepositoryImpl:61-91`, out of VM scope; VM-level assertion is the behavioral contract the screen needs. |

## Data Flow

    EpisodeScreen ──LaunchedEffect──> VM.loadServers()
        │                               │  viewModelScope.launch
        │                               ▼
        │                    .copy(isLoading = true, error = null)
        │                               │
        │                               ▼
        │                    GetFlixcornEpisodeServers → FlixcornRepositoryImpl
        │                               │  (Room cache hit → no network)
        │                               ▼
        │                    Success → .copy(isLoading=false, servers=data)
        │                    Error   → .copy(isLoading=false, error="...")
        │                               │
        ▼                               ▼
    when(isLoading → error → servers.isEmpty() → list)
    Error: "Reintentar" → loadServers(); Empty: message + new "Volver" → onBack()

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `presentation/flixcorn/FlixcornEpisodeViewModel.kt` | Modify | Default `isLoading=false`; `.copy()` in load-start/Success/Error; inject `GetFlixcornEpisodeServers` ctor param. |
| `presentation/flixcorn/FlixcornSeriesDetailViewModel.kt` | Modify | Same three changes for `loadSeries()`. |
| `presentation/flixcorn/FlixcornEpisodeScreen.kt` | Modify | Empty branch (151-162): add "Volver" button calling `onBack` (top-bar back already exists; spec wants in-body action). |
| `presentation/flixcorn/FlixcornSeriesDetailScreen.kt` | Verify-only | Branch order becomes reachable; error retry (112) already present. |
| `presentation/flixcorn/FlixcornEpisodeViewModelTest.kt` | Create | New JUnit4 tests (none exist today). |
| `presentation/flixcorn/FlixcornSeriesDetailViewModelTest.kt` | Create | New JUnit4 parity tests. |
| `di/AppContainer.kt` | Verify-only | Usecases already exposed (lines 77-78); no change needed. |

## Interfaces / Contracts

`FlixcornEpisodeUiState` becomes: `isLoading: Boolean = false` (other fields unchanged). Constructor signature: `FlixcornEpisodeViewModel(slug, season, episode, getEpisodeServers: GetFlixcornEpisodeServers = AppContainer.getFlixcornEpisodeServers)`. Series detail analog: `getSeriesDetail: GetFlixcornSeriesDetail = AppContainer.getFlixcornSeriesDetail`. Factories unchanged.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|--------------|----------|
| Unit (VM) | Success→loaded+`isLoading=false`; Error→`error` set + retry recovers; empty servers→`isLoading=false` (empty branch reachable); re-entry→servers settle, no stale loading; field preservation (`selectedLanguage` survives load) | Fake `FlixcornRepository` (mutable result queue, `callCount`), wrapped in real usecase — mirrors `HomeViewModelTest`/`MovieDetailViewModelTest`. `MainDispatcherRule` + `runTest` + `runCurrent`. |
| Unit (VM parity) | Series-detail Success→`isLoading=false`, series set; Error→`isLoading=false` + retry clears error | Same fake-repo pattern. |
| Unit (repo) | Cache-hit no-refetch in `getEpisodeServers` | Verify-only (existing behavior); optional Room in-memory test if budget allows. |

No Room in-memory needed for VM tests — the fake repo isolates the state machine. `FlixcornError` enum + `FlixcornResult` are available for error fixtures.

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration. Fix is in-module (`presentation/flixcorn/`); WIP is uncommitted — stash/backup before apply. Room schemas 6-9 crash is unrelated env risk; rely on `testDebugUnitTest` + `compileDebugKotlin` fallback for device check.

## Open Questions

- [ ] None blocking. Optional: extend coverage to `FlixcornRepositoryImpl` cache-hit (Room in-memory) if review budget allows.
