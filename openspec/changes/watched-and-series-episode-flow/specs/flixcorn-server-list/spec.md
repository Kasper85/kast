# Delta for Flixcorn Server List

## ADDED Requirements

### Requirement: F1 — Online-first server ordering

The system SHALL order episode servers online-first: servers with a `/player/` `onlineUrl` rank first and render as primary ("Ver Online"), followed by servers with only an `/external/` `directUrl` ("Link Directo"). The system SHALL replace the current name-based sort.

#### Scenario: Online server renders first

- GIVEN 3 servers, one with an `onlineUrl`
- WHEN the episode server list renders
- THEN the `onlineUrl` server renders first as primary
- AND the two `directUrl`-only servers render after it

#### Scenario: No online server available

- GIVEN no server has an `onlineUrl`
- WHEN the episode server list renders
- THEN all servers render in fallback order as "Link Directo"
- AND no server is marked primary

### Requirement: F2 — Next-episode navigation

The system SHALL show a "Siguiente episodio" action on the episode server list that loads the next episode: same season with episode + 1; when the season's last episode is reached and the series has more seasons, season + 1 with episode 1. The system SHALL hide or disable the action when no next episode exists.

#### Scenario: Next episode within the same season

- GIVEN S1E1 of a 2-season series
- WHEN the user taps "Siguiente episodio"
- THEN the list loads S1E2

#### Scenario: Season boundary crosses to next season

- GIVEN the last episode of season 1 of a 2-season series
- WHEN the user taps "Siguiente episodio"
- THEN the list loads S2E1

#### Scenario: No next episode exists

- GIVEN the last episode of the last known season
- WHEN the episode server list renders
- THEN the "Siguiente episodio" action is hidden or disabled

### Requirement: F3 — Next-episode load reuses server loading contract

The system SHALL load next-episode servers via `getEpisodeServers(slug, nextSeason, nextEpisode)`, reusing the existing cache, and SHALL preserve the existing loading and error states for the reload.

#### Scenario: Next-episode loads with cache

- GIVEN a previously cached next episode
- WHEN the user taps "Siguiente episodio"
- THEN the servers render immediately from cache
- AND no network fetch is triggered

#### Scenario: Next-episode load fails

- GIVEN the next-episode server load fails
- WHEN loading completes with an error
- THEN the existing error state with retry is shown
- AND the user can retry the load
