# Proposal: Fix user-reported errors (v8)

## Status

**Proposed** — change `fix-user-errors-v8`, ready for specs.

## Executive Summary

Five user-reported failures. **#1 confirmed**: `FlixcornScraper.searchSeries()` builds `$BASE_URL/search?q=${query.trim()}` without URL-encoding, so any title with spaces/accents (e.g. "Bestias Divinas") makes OkHttp throw `IllegalArgumentException`, which `executeWithRetry` maps to `FlixcornError.UNREACHABLE` → "No se pudo buscar la serie en Flixcorn. Intenta de nuevo." **#2–#5** (favorites, watched, history, TV-settings crash) share one strong hypothesis: the device DB is at v9 while this code expects v8; Room has no 9→8 migration, so the first DB access throws and every Room-backed screen fails. Code, schema `8.json` (identityHash `c0bd8631373c455c088f4a307af2f591`), DAOs and the 108 green unit tests are internally consistent. Also: bump `versionName` to 2.0 and re-verify Flixcorn server priority ("Ver Online" `/player/` primary, "Link Directo" `/external/` fallback) against live HTML.

## Justification (why now)

The installed build is unusable on device for all data screens, and series→episode search fails for most TMDB titles. Root causes are diagnosed and small; every week deferred keeps the app broken in production.

## Objectives

- **US-1**: As a user, I can search a TMDB series title with spaces/accents (e.g. "Bestias Divinas") and Flixcorn results load instead of an error.
- **US-2**: As a user, favorites, watched, history and TV settings work after a clean install; a stale/newer local DB shows an actionable message with a reset option — never a crash or generic error.
- **US-3**: As a user, episode playback prefers the "Ver Online" server (`/player/`); "Link Directo" (`/external/`) is used only when no online server exists.
- **US-4**: The app reports `versionName 2.0`.
- **Acceptance**: all 108 existing tests stay green; new tests cover the fixes; `assembleDebug` builds; manual device pass on a clean install.

## Scope

**In**
- URL-encode the search query in `FlixcornScraper.searchSeries()`; audit the other URL constructions (slug, token, episode path) for the same defect.
- Verify `StreamingServer.sortedOnlineFirst()` against live HTML rows exposing ONLY `/external/` (e.g. Vidmoly); adjust ordering/parser if needed.
- DB open resilience: guard first access, catch version-mismatch `IllegalStateException`, surface an actionable message plus user-confirmed reset (no silent data loss).
- Device verification plan: `adb shell run-as com.kastlg.app` / logcat (device `4dd3d45`) or clean uninstall+install to confirm the 9→8 hypothesis.
- `versionName = "2.0"` in `app/build.gradle.kts`.
- Unit tests for all of the above.

**Out**
- TMDB init race and unrelated reported issues.
- New features; androidTest E2E suite.
- Schema changes or new migrations (none needed).

## Capabilities

**New**
- `flixcorn-search`: URL-safe query construction + search-result contract (spaces, accents, unicode).
- `database-open-resilience`: guarded open, actionable error state, user-confirmed destructive reset.

**Modified**
- `flixcorn-server-list`: add ordering-priority requirement — `/player/` ("Ver Online") first, `/external/` ("Link Directo") fallback, incl. rows with only direct links.

## Approach (affected code)

| File | Change |
|------|--------|
| `data/remote/flixcorn/FlixcornScraper.kt` | URL-encode query (OkHttp `HttpUrl`); audit slug/token/episode URLs |
| `data/remote/flixcorn/StreamingServer.kt` | Verify/adjust `sortedOnlineFirst` vs live HTML |
| `data/remote/flixcorn/FlixcornHtmlParser.kt` | Fix selectors only if `/external/`-only rows mis-parse |
| `data/local/KastLgDatabase.kt`, `di/AppContainer.kt` | Guarded DB open + resilience wrapper |
| `presentation/` (favorites, history, tvsettings, detail) | Map DB-open failure to actionable message + reset action |
| `app/build.gradle.kts` | `versionName = "2.0"` |
| `app/src/test/...` | New tests: encoding (spaces+accents), sort with `/external/`-only rows, DB-open resilience (in-memory where viable) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| #2–#5 root cause is NOT the DB version | Med | Confirm on device FIRST (adb / clean install) before wider UI work |
| Reset option loses v9 data | Med | User-confirmed destructive only; never auto-wipe |
| Flixcorn HTML changed since fixtures | Med | Re-verify live; parser stays isolated + fixture-tested |
| Ordering assumption wrong for `/external/`-only rows | Low | Vidmoly-style rows covered in sort tests |

## Rollback Plan

Revert the change commit. Fixes are additive: no schema/version change, no destructive default — reset fires only on explicit user action. Worst case: uninstall/reinstall on device.

## Dependencies

- Device `4dd3d45` (Redmi) reachable via adb, or a user-performed clean install for verification.
- No external libraries.

## Success Criteria

- [ ] `./gradlew testDebugUnitTest` green (108 existing + new)
- [ ] `./gradlew assembleDebug` builds
- [ ] Search with spaces/accents returns Flixcorn results (MockWebServer test + device)
- [ ] Server order: `/player/` first; `/external/`-only rows sorted correctly
- [ ] Stale v9 DB shows actionable message with reset; clean install fixes all 5 issues
- [ ] `versionName 2.0` visible in app

## Open questions (resolved by diagnosis unless noted)

- Q1 (pending): #2–#5 root cause confirmed on device? — verification is part of this scope.
- Q2: `versionCode` — keep `1` (no release cadence yet) unless user requests a bump.
- Q3: reset UX — inline dialog vs settings entry; recommend inline, decide in design.