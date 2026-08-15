# Delta for Flixcorn Search

## ADDED Requirements

### Requirement: REQ-SEARCH-01 — URL-safe search query encoding

The system MUST percent-encode the trimmed search query (RFC 3986) before interpolating it into the Flixcorn search URL, so no raw space, accent, or reserved character reaches `Request.Builder.url()`. The system MUST preserve the existing query length bounds (2–100) and rate limiting.

Acceptance criteria:
- AC1: `searchSeries("Bestias Divinas")` produces a request URL containing `q=Bestias%20Divinas`.
- AC2: Queries with accents/unicode (e.g. "Café") are percent-encoded as UTF-8 escapes.
- AC3: Single-word safe queries ("bestias") produce the same URL as today — no double-encoding.
- AC4: `Request.Builder.url(...)` never throws `IllegalArgumentException` for any in-range query.

#### Scenario: Query with spaces loads results

- GIVEN the user searches "Bestias Divinas" from the TV-series detail screen
- WHEN `searchSeries()` builds the request URL
- THEN the query is encoded as `q=Bestias%20Divinas`
- AND no `IllegalArgumentException` is thrown
- AND Flixcorn results render instead of the UNREACHABLE error message

#### Scenario: Query with accents

- GIVEN the user searches "Café"
- WHEN the search URL is built
- THEN the accent is percent-encoded (UTF-8)
- AND the request fails only on genuine network errors

#### Scenario: Already-safe query unchanged

- GIVEN a single-word query "bestias"
- WHEN the search URL is built
- THEN the URL matches the current encoding exactly (no double-encoding)

### Requirement: REQ-SEARCH-02 — URL-safe construction for all Flixcorn endpoints

The system MUST build every Flixcorn request URL — search, `/serie/<slug>.html`, `/ver/<slug>/temporada-<season>/capitulo-<episode>.html`, `/player/<token>`, and `/external/<token>?s=1` — without raw spaces or unencoded reserved characters. Slugs and tokens derived from server HTML MUST be validated or encoded before interpolation; an unsafe value MUST NOT surface `FlixcornError.UNREACHABLE`.

Acceptance criteria:
- AC1: Every endpoint URL construction is audited and unit-tested to assert no raw space can appear.
- AC2: An unsafe slug/token is encoded or rejected as `PARSE_FAILURE` — never an `IllegalArgumentException` mapped to `UNREACHABLE`.

#### Scenario: Unsafe slug from server data

- GIVEN `getSeriesDetail` receives a slug containing a space or reserved character
- WHEN the request URL is built
- THEN the URL is encoded or the call fails as `PARSE_FAILURE`
- AND `executeWithRetry` never maps the failure to `UNREACHABLE`

### Requirement: REQ-SEARCH-03 — Failure UX unchanged

The system MUST NOT change the user-facing error contract for genuine failures: an unreachable Flixcorn still shows "No se pudo buscar la serie en Flixcorn. Intenta de nuevo."; the encoding fix only removes encoding-triggered false failures.

#### Scenario: Genuine network failure

- GIVEN the Flixcorn site is unreachable
- WHEN `searchSeries()` fails with a network error
- THEN the existing UNREACHABLE error message renders unchanged

## Out of Scope

- TMDB initialization race and unrelated reported issues.
- Parser selector changes (the `.media-card` / `.media-title` parsing contract is already correct).
- New search features (pagination, fuzzy matching).

## Technical Notes

- Defect confirmed at `FlixcornScraper.searchSeries()` line 38: `val url = "$BASE_URL/search?q=${query.trim()}"`. `fetchHtml` passes the raw string to OkHttp, which throws `IllegalArgumentException` on spaces; `executeWithRetry`'s `catch (Exception)` maps it to `FlixcornError.UNREACHABLE`.
- Preferred fix: OkHttp `HttpUrl` builder or `URLEncoder` (UTF-8) — decision in design.
- Live check: the site answers `q=bestias` with 3 `.media-card` / `.media-title` results, href `/serie/<slug>.html`.
- Tests: MockWebServer asserting the encoded path/query plus multi-word and accented query cases.
- `versionName = "2.0"` (app/build.gradle.kts, versionCode stays 1) is cross-cutting build metadata tracked in the proposal; it is not part of this domain spec.
