# Delta for Database Open Resilience

## ADDED Requirements

### Requirement: REQ-DB-01 — Guarded database open with actionable error state

The system MUST detect a database that cannot be opened (Room `IllegalStateException` on version mismatch — device DB newer than code) at first access and MUST surface an actionable error state instead of crashing or showing a generic error. Room-backed screens (favorites, watched, history, TV settings) MUST degrade to this state.

Acceptance criteria:
- AC1: First DB access on a version-mismatched DB renders the actionable message — no app crash.
- AC2: `TvSettingsViewModel.observeConfig` completes with the error state instead of throwing.
- AC3: The state is consistent across favorites, watched, history, and TV settings screens.

#### Scenario: Stale newer database on launch

- GIVEN the device DB is at a version newer than the code (e.g. v9 vs v8)
- WHEN the user opens Favorites
- THEN an actionable message renders ("Los datos locales no son compatibles con esta versión de la app")
- AND the app does not crash

#### Scenario: TV settings open with stale database

- GIVEN the same version-mismatched DB
- WHEN the user opens Ajustes
- THEN the settings screen shows the actionable state
- AND no crash occurs in `observeConfig`

### Requirement: REQ-DB-02 — User-confirmed destructive reset

The system MUST offer a reset action that deletes the local database and recreates it, and MUST require explicit user confirmation before any data deletion. The system MUST NOT auto-delete or silently wipe data.

Acceptance criteria:
- AC1: Reset is a two-step action: trigger + explicit confirmation dialog with Spanish copy.
- AC2: Cancel leaves the DB untouched.
- AC3: Confirming reset deletes the DB file; the app recreates it on next access.
- AC4: After reset, all Room-backed features work with empty data.

#### Scenario: Reset confirmed

- GIVEN the actionable error state is shown
- WHEN the user taps reset and confirms
- THEN the local DB is deleted and recreated
- AND favorites/watched/history/TV-config work with empty data

#### Scenario: Reset cancelled

- GIVEN the confirmation dialog is shown
- WHEN the user cancels
- THEN no data is deleted
- AND the error state remains

### Requirement: REQ-DB-03 — Clean install unaffected

The system MUST preserve current behavior on a clean install: a fresh DB opens at version 8 with no error state, and the existing 108 unit tests stay green.

#### Scenario: Clean install

- GIVEN a fresh install with no existing DB
- WHEN the app first accesses Room
- THEN the DB opens normally at v8
- AND favorites, watched, history, and TV settings work without the error state

## Out of Scope

- Schema changes or new Room migrations (none needed; code stays at v8).
- Silent or automatic data migration or wipe.
- androidTest E2E suite.

## Technical Notes

- Hypothesis for issues #2–#5 (favorites, watched, history, TV-settings crash): the device DB was left at v9 by an `-r` install while the code expects v8. Room migrates only forward; a newer version cannot be downgraded → `IllegalStateException` on first access. Code, `8.json` schema (identityHash `c0bd8631373c455c088f4a307af2f591`), DAOs and the 108 unit tests are internally consistent.
- Device verification is part of scope (proposal Q1): confirm via `adb shell run-as com.kastlg.app` / logcat on device `4dd3d45`, or a clean uninstall+install, BEFORE wide UI work.
- Reset deletes the DB file (Room recreates it on next access); `DatabaseMigrations.ALL` is untouched. Guard belongs in `KastLgDatabase.create()` / `di/AppContainer.kt`; UI mapping in `presentation/` (favorites, history, tvsettings, detail).
- Reset UX: inline dialog on the first affected screen — decision in design.
