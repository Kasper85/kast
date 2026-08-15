# Watched Tracking Specification

## Purpose

Specifies persisted watched-state for movies and episodes, the toggle UI that reads and updates it, restart survival, and the Room migration that introduces the storage.

## Requirements

### Requirement: W1 — Movie watched state persisted

The system SHALL persist movie watched state keyed by movie id in a `watched_movies` table with `movieId` as primary key and a `watchedAt` timestamp. The movie detail screen SHALL expose a watched toggle labeled "Marcar como visto" when unwatched and "Quitar de vistos" when watched.

#### Scenario: Movie marked watched

- GIVEN a movie detail screen for a movie not yet watched
- WHEN the user taps "Marcar como visto"
- THEN a row with the movie id and current timestamp is stored
- AND the toggle reads "Quitar de vistos"

#### Scenario: Movie unmarked

- GIVEN a movie detail screen for a watched movie
- WHEN the user taps "Quitar de vistos"
- THEN the watched row is removed
- AND the toggle reads "Marcar como visto"

### Requirement: W2 — Episode watched state persisted

The system SHALL persist episode watched state keyed by (series slug, season, episode) in a `watched_episodes` table with a composite key of slug + season + episode and a `watchedAt` timestamp. The Flixcorn episode server list screen SHALL expose a watched toggle that reflects and updates this state.

#### Scenario: Episode marked from server list

- GIVEN the Flixcorn episode server list for (slug, season, episode)
- WHEN the user activates the watched toggle
- THEN the episode row is stored
- AND the checkmark/toggle reflects watched state

#### Scenario: Episode unmarked

- GIVEN a watched episode on the server list
- WHEN the user activates the watched toggle again
- THEN the episode row is removed
- AND the checkmark/toggle clears

### Requirement: W3 — Watched state survives restart

The system SHALL retain watched state across app restarts via Room persistence. Process death or app relaunch SHALL NOT lose movie or episode watched state.

#### Scenario: Movie watched state persists

- GIVEN a movie marked watched
- WHEN the app is killed and reopened
- THEN the movie detail still shows "Quitar de vistos"

#### Scenario: Episode watched state persists

- GIVEN an episode marked watched from its server list
- WHEN the app restarts
- THEN the episode server list still shows the watched checkmark

### Requirement: W4 — Database migration to version 8

The system SHALL bump the Room database version from 7 to 8 and SHALL register `MIGRATION_7_8` that creates the `watched_movies` and `watched_episodes` tables only. Favorites and history tables MUST remain untouched by the migration.

#### Scenario: Upgrade from v7 preserves favorites

- GIVEN a v7 database containing favorite rows
- WHEN `MIGRATION_7_8` runs
- THEN favorites rows and schema are unchanged
- AND both watched tables are created empty

#### Scenario: Fresh install at v8

- GIVEN a new install
- WHEN the database is created at version 8
- THEN both watched tables exist and are writable
- AND no migration runs
