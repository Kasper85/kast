# TMDB Series Episode to Flixcorn Specification

## Purpose

Specifies the flow from a TMDB series detail screen episode tap to the Flixcorn episode server list, including slug resolution via title search, the no-match notice, and the removal of "Ver en TV" for TMDB series.

## Requirements

### Requirement: T1 — "Ver en TV" removed for TMDB series

The system SHALL NOT render the "Ver en TV" button on `TvShowDetailScreen` for TMDB series. Movie detail screens SHALL keep their "Ver en TV" button.

#### Scenario: Series detail has no button

- GIVEN a TMDB series detail screen
- WHEN it renders
- THEN no "Ver en TV" button is shown

#### Scenario: Movie detail keeps button

- GIVEN a movie detail screen
- WHEN it renders
- THEN the "Ver en TV" button is still shown

### Requirement: T2 — Episode tap resolves Flixcorn slug

The system SHALL, when the user taps an episode on a TMDB series detail screen, run a Flixcorn title search for the series title and SHALL resolve the slug from the best-match result.

#### Scenario: Title match resolves slug

- GIVEN the series "Beastars" with episode S1E1
- WHEN the user taps S1E1
- THEN a title search for the series runs
- AND the best-match result resolves the slug `bestias-divinas`

### Requirement: T3 — Slug resolution navigates to episode server list

The system SHALL, once the slug is resolved, navigate to the episode's Flixcorn server list via `FlixcornEpisodeRoute` with slug, season, and episode.

#### Scenario: Tap S1E1 opens Flixcorn servers

- GIVEN the series "Beastars" with resolved slug `bestias-divinas`
- WHEN the user taps S1E1
- THEN `FlixcornEpisodeRoute` opens for (`bestias-divinas`, season 1, episode 1)
- AND that episode's servers load

### Requirement: T4 — No-match shows notice without navigation

The system SHALL show a notice that the episode was not found on Flixcorn when no best-match result exists, and SHALL NOT navigate.

#### Scenario: No Flixcorn match

- GIVEN a series with no Flixcorn title match
- WHEN the user taps an episode
- THEN a not-found notice is shown
- AND no navigation occurs
- AND back navigation still works

### Requirement: T5 — Episode context shown while resolving

The system SHALL display the tapped episode's title and number as context while the slug is being resolved, and SHALL show a loading state until resolution completes or fails.

#### Scenario: Loading state with episode context

- GIVEN an episode tap in flight
- WHEN the title search is resolving
- THEN the episode title and number are shown with a loading state
- AND the flow resolves to either navigation or the no-match notice
