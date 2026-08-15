# Tasks: Fix user-reported errors (v8)

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

Estimated changed lines: 700–1000 (prod 260–400; tests 445–635).
Suggested split: PR 1 → PR 2 → PR 3 (stacked to main).

### Suggested Work Units

| Unit | Goal | Likely PR | Focused test command | Runtime harness | Rollback boundary |
|---|---|---|---|---|---|
| 1 | URL safety (D1) + versionName (D6) | PR 1 | `testDebugUnitTest --tests "*FlixcornScraper*"` | `./gradlew testDebugUnitTest` | Revert scraper/repo/gradle only |
| 2 | DB guard + reset (D2/D3) | PR 2 | `testDebugUnitTest --tests "*DatabaseOpenResilienceTest*"` | `./gradlew testDebugUnitTest` | Revert DB/DI files only |
| 3 | DB-error UI (D4) + server tests (D5) | PR 3 | `./gradlew testDebugUnitTest` | Full suite + `assembleDebug` + device | Revert DatabaseErrorState + 4 VMs |

## Phase 1: URL Safety (D1, D6)

- [ ] 1.1 — RED+fix: `FlixcornScraperUrlTest` (6: spaces, accents, no double-encode, slug/token path, `/external/x?s=1`, `+`→`%2B`) → `buildUrl` + `fetchHtml(HttpUrl)` + 5 endpoints + `episodeUrl()`. Files: `FlixcornScraperUrlTest.kt` (new), `FlixcornScraper.kt`. Acceptance: URL tests green; no raw space in `url()`.
- [ ] 1.2 — MockWebServer test (≤3 req): encoded search path/query (rate-limit ≥1 s), episode, player/external. Files: `FlixcornScraperTest.kt` (new). Acceptance: recorded paths match; ≤3 requests.
- [ ] 1.3 — Repository: cache key via `episodeUrl()` + cache re-sort test (slug w/ space). Files: `FlixcornRepositoryTest.kt` (new), `FlixcornRepositoryImpl.kt`. Acceptance: tests green.
- [ ] 1.4 — Set `versionName = "2.0"` + `BuildConfigTest`. Files: `app/build.gradle.kts`, `BuildConfigTest.kt` (new). Acceptance: test green.

## Phase 2: DB Open Resilience (D2, D3)

- [ ] 2.1 — Gate: confirm v9 on device (adb `PRAGMA user_version` / logcat); stop if disproven. Files: device `4dd3d45`. Acceptance: confirmed.
- [ ] 2.2 — RED test: v9 file DB → `VersionMismatch` + `DatabaseOpenException`; `resetLocalData()` → v8 reopens, error cleared; clean install → no error. Files: `DatabaseOpenResilienceTest.kt` (new). Acceptance: 3 cases green.
- [ ] 2.3 — GREEN: `KastLgDatabase` — expose `DATABASE_NAME`, `delete(context)`; `AppContainer` — synchronized `openDatabase()` (force open; ISE → `_databaseError` → throw), `databaseError: StateFlow<DatabaseError?>`, `resetLocalData()` (close → delete → clear → null singletons), Room lazies → var accessors. Files: `KastLgDatabase.kt`, `AppContainer.kt`. Acceptance: 2.2 green; lazies rebuild.

## Phase 3: UI Integration & Reset UX (D4)

- [ ] 3.1 — Component: banner "Los datos locales no son compatibles con esta versión de la app" + "Restablecer datos" + dialog (se eliminarán favoritos, historial y configuración; Cancelar / Restablecer) + relaunch (`finishAffinity()` + intent). Files: `DatabaseErrorState.kt` (new). Acceptance: two-step; cancel leaves DB untouched.
- [ ] 3.2 — Favorites/History VMs + tests: inject `databaseError` (default `MutableStateFlow(null)`); combine → DB-error UiState. Files: 2 VMs + 2 tests. Acceptance: DB-error replaces generic message; existing tests untouched.
- [ ] 3.3 — TvSettings VM + test: try/catch `observeConfig()` + `autoReconnect()` → DB-error state. Files: VM + test. Acceptance: ISE never crashes (AC2).
- [ ] 3.4 — MovieDetail VM + test: `databaseError` in UiState; route favorite/watched/history/getConfig on error. Files: VM + test. Acceptance: DB-error state, no crash.

## Phase 4: Server Order Verification (D5 — tests only)

- [ ] 4.1 — Extend `StreamingServerSortTest`: `/external/`-only never above `/player/`; mixed row; all-external fallback (none primary); cache-order re-sort. Files: `StreamingServerSortTest.kt`. Acceptance: 4 cases green (REQ-SERV-01).
- [ ] 4.2 — Live check: Vidmoly episode → "Link Directo", no primary, below any `/player/` row. Files: device / live HTML. Acceptance: AC1–AC4 verified.

## Phase 5: Full Verification

- [ ] 5.1 — `testDebugUnitTest` + `assembleDebug`. Files: —. Acceptance: 108 existing + new green; build OK.
- [ ] 5.2 — Device pass: clean install all 5 screens OK; stale DB → banner → reset → relaunch → empty works; search "Bestias Divinas"; versionName 2.0. Files: device `4dd3d45`. Acceptance: proposal criteria met.

## Risks

- Root cause not DB version → gate 2.1.
- Reset deletes v9 data → user-confirmed only.
- Flixcorn HTML drift → live verify.
- Scraper tests slow/racy → pure URL tests primary; ≤3 requests.
- AppContainer state leaks into tests → reset in `@Before`/`@After`.