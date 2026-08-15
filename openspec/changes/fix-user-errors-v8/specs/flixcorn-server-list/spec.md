# Delta for Flixcorn Server List

## MODIFIED Requirements

### Requirement: REQ-SERV-01 — Online-first server ordering (was F1)

The system SHALL order episode servers online-first: servers with a `/player/` `onlineUrl` rank first and render as primary ("Ver Online"), followed by servers with only an `/external/` `directUrl` ("Link Directo"). The system SHALL replace the current name-based sort and SHALL apply this ordering on every load path — fresh scrape and cache read — including rows that expose ONLY a direct link.

Acceptance criteria:
- AC1: A row exposing only `directUrl` (`/external/`, e.g. Vidmoly) renders as "Link Directo" and never ranks above a `/player/` server.
- AC2: When no server has `onlineUrl`, all servers render as "Link Directo" in deterministic fallback order and none is marked primary.
- AC3: Ordering applies to fresh parses (`parseEpisodeServers`) and to cache reads with legacy name-priority order (`getEpisodeServers` re-sort).
- AC4: Verified against live HTML rows exposing only `/external/`.

(Previously: servers with a `/player/` onlineUrl ranked first followed by directUrl-only servers; no explicit coverage of `/external/`-only rows, cache-load paths, or live verification.)

#### Scenario: Online server renders first

- GIVEN 3 servers, one with an `onlineUrl`
- WHEN the episode server list renders
- THEN the `onlineUrl` server renders first as primary
- AND the two `directUrl`-only servers render after it

#### Scenario: Only direct links available

- GIVEN an episode whose HTML exposes ONLY `/external/` links (e.g. Vidmoly)
- WHEN the server list renders
- THEN every server renders as "Link Directo" in stable fallback order
- AND no server is marked primary
- AND no `/player/` server is fabricated by the parser

#### Scenario: No online server available

- GIVEN no server has an `onlineUrl`
- WHEN the episode server list renders
- THEN all servers render in fallback order as "Link Directo"
- AND no server is marked primary

#### Scenario: Cache read re-sorts online-first

- GIVEN a cached episode stored in name-priority order
- WHEN the episode re-opens and servers load from cache
- THEN servers re-sort online-first
- AND `/external/`-only rows stay below any `/player/` row

## Out of Scope

- Parser selector changes unless live HTML proves a mis-parse.
- TMDB-side work and unrelated reported issues.

## Technical Notes

- `sortedOnlineFirst()` in `StreamingServer.kt` already implements `/player/` → `/external/` → name tiebreak, and `parseServerRow` already accepts rows where `onlineHref == null` (only-direct rows parse). Verify both against live HTML; adjust only if a mis-parse is proven.
- Live check target: an episode page with `/external/`-only rows (e.g. Vidmoly).
- Tests: extend `StreamingServerSortTest` with `/external/`-only rows and a cache-path re-sort test.
