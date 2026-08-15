# Flixcorn Server List Specification

## Purpose

Specifies loading, empty, error, cache-hit, and retry behavior for the Flixcorn episode and series-detail server lists. Both entry points share a state contract: `isLoading` defaults to `false` and is cleared on every async completion branch, so the screen branch order (loading → error → empty → list) is actually reachable.

## Requirements

### Requirement: Episode server list shows servers after load completes

The system SHALL default `isLoading` to `false` in `FlixcornEpisodeUiState` and SHALL clear it on both success and error completion of server loading, so the list (or its empty/error state) renders instead of an infinite spinner.

#### Scenario: Servers load successfully

- GIVEN an episode with 2 available servers
- WHEN the user opens the episode and `loadServers()` completes successfully
- THEN the server list renders both servers
- AND `isLoading` is cleared

#### Scenario: Episode has no servers

- GIVEN an episode with zero available servers
- WHEN `loadServers()` completes with an empty result
- THEN an empty state with a clear message and a back action is shown
- AND `isLoading` is cleared

### Requirement: Load failure shows error with retry

The system SHALL set `isLoading` to `false` and expose an error state when server loading fails, and SHALL re-trigger loading when the user retries.

#### Scenario: Failure then successful retry

- GIVEN server loading fails due to a network/scrape error
- WHEN the error state renders and the user taps retry
- THEN `loadServers()` runs again
- AND if the retry succeeds, the server list renders

### Requirement: Cached servers served without re-fetch

The system SHALL reuse cached servers for an already-viewed episode so re-entry renders servers instantly without a network fetch.

#### Scenario: Re-entering a viewed episode

- GIVEN an episode whose servers were successfully loaded earlier
- WHEN the user re-enters the episode
- THEN the servers render immediately from cache
- AND no network fetch is triggered

### Requirement: Series detail list shares loading-state correctness

The system SHALL apply the same default-`false` and cleared-on-completion `isLoading` semantics in `FlixcornSeriesDetailViewModel`, so the series-detail server list resolves to list/empty/error states.

#### Scenario: Series detail loads successfully

- GIVEN the series-detail screen with available servers
- WHEN `loadSeries()` completes successfully
- THEN the server list renders and `isLoading` is cleared

#### Scenario: Series detail load fails

- GIVEN server loading fails on the series-detail screen
- WHEN the load completes with an error
- THEN the error state is shown with a retry action
- AND `isLoading` is cleared

### Requirement: State transitions preserve non-server fields

The system SHALL preserve `selectedLanguage` and TV-related fields when replacing state on async completion.

#### Scenario: Language selection survives server load

- GIVEN the user selected a language before servers loaded
- WHEN `loadServers()` completes and state is replaced
- THEN `selectedLanguage` and other non-server fields remain unchanged
