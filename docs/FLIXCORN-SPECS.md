# Flixcorn Integration — Technical Specifications

## Project Overview

KastLG currently uses TMDB for metadata and UnlimPlay for playback URLs. Flixcorn integration adds a second content source that provides actual streaming links for TV series, enabling direct playback on LG webOS TVs without relying on UnlimPlay's embed system.

Flixcorn is a web-based series catalog that organizes content by series → season → episode, with multiple streaming servers per episode. Each server offers online playback and direct download links. The integration scrapes Flixcorn's structured HTML to extract series metadata, episode lists, and streaming server URLs, then sends the selected stream to the TV via SSAP.

## Goals and Non-Goals

### Goals
- Search Flixcorn for series by title
- Display Flixcorn series details (poster, backdrop, synopsis, seasons, episodes)
- Parse episode pages to extract streaming servers organized by quality and language
- Send selected streaming server URLs to LG webOS TV via SSAP
- Fall back to UnlimPlay when Flixcorn does not have the series
- Cache Flixcorn series metadata locally to reduce scraping frequency

### Non-Goals
- Downloading episodes to local storage
- Hosting or re-streaming Flixcorn content
- Modifying or proxying Flixcorn player pages
- Supporting Flixcorn's download links (only streaming)
- Replacing TMDB as the primary metadata source
- User accounts or authentication with Flixcorn

## Functional Requirements

### FR-1: Flixcorn Search
- User types a query in the existing search bar
- App queries Flixcorn's search endpoint (`/search?q={query}`)
- Results display series cards with poster, title, year, and genres
- Results merge with TMDB search results, Flixcorn results tagged with a source indicator

### FR-2: Flixcorn Series Detail
- Tapping a Flixcorn result opens a detail screen
- Screen displays: title, poster, backdrop, year, rating, genres, synopsis, season count, episode count
- Season selector chips (same UX as existing TMDB series detail)
- Episode list for selected season with episode number, title, and synopsis

### FR-3: Episode Server Resolution
- Tapping an episode scrapes the episode page (`/ver/{slug}/temporada-{N}/capitulo-{N}.html`)
- Parser extracts quality tiers (1080p HD, 720p HD)
- Each tier contains language groups (Español Latino, Subtitulado)
- Each language group contains server rows with: server name, online link (`/player/{token}`), direct link (`/external/{token}`)
- Server priority: Voe > Dsvplay > Vidara > Luluvdoo > Streamtape > Byseqekaho > Vidmoly

### FR-4: Send to TV
- User selects a server from the resolved list
- App resolves the `/player/{token}` or `/external/{token}` URL
- The resolved URL is sent to the LG webOS TV via SSAP `playWithUrl`
- Fallback: if player URL resolution fails, attempt direct link

### FR-5: Source Indicators
- UI shows a badge when content comes from Flixcorn vs TMDB
- Search results distinguish sources visually
- Detail screens show source attribution

## Non-Functional Requirements

### NFR-1: Performance
- Search results appear within 2 seconds on 4G connection
- Episode server resolution completes within 3 seconds
- Parsed HTML cached for 1 hour per episode URL
- Series metadata cached for 24 hours

### NFR-2: Reliability
- Graceful degradation when Flixcorn is unreachable
- Automatic retry with exponential backoff (max 2 retries)
- Parser handles HTML structure changes with fallback selectors
- Empty state UI when no servers are found

### NFR-3: Rate Limiting
- Maximum 1 request per second to Flixcorn
- Batch series detail + episode requests when possible
- Respect Flixcorn's robots.txt (noindex pages are parser-only)

### NFR-4: Security
- No credentials stored for Flixcorn
- All URLs opened in system browser or WebView with user consent
- No content modification or injection
- HTTPS-only connections

## User Stories

### US-1: Search and Find Series
As a user, I want to search for a series by title and see results from both TMDB and Flixcorn, so I can find streaming sources for series that TMDB does not provide playback for.

**Acceptance Criteria:**
- Search input triggers both TMDB and Flixcorn searches in parallel
- Flixcorn results show a source badge
- Results are mergeable and sortable

### US-2: Browse Flixcorn Series
As a user, I want to view a Flixcorn series detail page with poster, synopsis, seasons, and episodes, so I can navigate to the episode I want to watch.

**Acceptance Criteria:**
- Detail screen matches existing TMDB series detail UX
- Season chips load episode lists
- Episodes show number, title, and synopsis

### US-3: Select Streaming Server
As a user, I want to see available streaming servers for an episode, organized by quality and language, so I can choose the best option for my connection and preference.

**Acceptance Criteria:**
- Servers grouped by quality tier
- Servers grouped by language within each tier
- Server icons and names displayed
- Online and direct link options shown

### US-4: Send Stream to TV
As a user, I want to send a selected Flixcorn stream to my LG webOS TV, so I can watch the episode on the big screen.

**Acceptance Criteria:**
- "Ver en TV" button resolves the streaming URL
- URL sent via SSAP to connected TV
- Error shown if resolution fails
- Fallback to direct link if player link fails

### US-5: Fallback to TMDB
As a user, if a series is not found on Flixcorn, I want the app to fall back to TMDB metadata and UnlimPlay playback, so I always have a working path.

**Acceptance Criteria:**
- If Flixcorn search returns no results, TMDB results still show
- If Flixcorn episode page fails to parse, UnlimPlay URL is offered
- User can manually switch between sources on detail screen

## Architecture

### Data Flow

```
User Search
    │
    ├──→ TMDB API ──→ SearchResults (TMDB)
    │
    └──→ Flixcorn Scraper ──→ SearchResults (Flixcorn)
                                    │
                                    ▼
                              SeriesDetail (scraped)
                                    │
                                    ▼
                              EpisodePage (scraped)
                                    │
                                    ▼
                              ServerList (parsed)
                                    │
                                    ▼
                              Player URL Resolution
                                    │
                                    ▼
                              SSAP Send to TV
```

### Module Structure

```
app/src/main/java/com/kastlg/app/
├── data/
│   ├── remote/
│   │   ├── flixcorn/
│   │   │   ├── FlixcornScraper.kt          # HTML parsing engine
│   │   │   ├── FlixcornSearchResult.kt     # Search result DTO
│   │   │   ├── FlixcornSeriesDetail.kt     # Series detail DTO
│   │   │   ├── FlixcornEpisodePage.kt      # Episode page DTO
│   │   │   ├── FlixcornServer.kt           # Server/link model
│   │   │   └── FlixcornHtmlParser.kt       # Jsoup-based parser
│   │   └── TmdbNetwork.kt                  # (existing)
│   ├── repository/
│   │   └── FlixcornRepository.kt           # Flixcorn data source
│   └── local/
│       ├── FlixcornSeriesEntity.kt         # Cached series metadata
│       ├── FlixcornSeriesDao.kt            # Cache access
│       └── KastLgDatabase.kt              # (existing, add entity)
├── domain/
│   ├── models/
│   │   ├── FlixcornSeries.kt              # Domain model
│   │   ├── FlixcornEpisode.kt             # Domain model
│   │   └── StreamingServer.kt             # Server with links
│   ├── repositories/
│   │   └── FlixcornRepository.kt          # Repository interface
│   └── usecases/
│       ├── SearchFlixcornUseCase.kt        # Search Flixcorn
│       ├── GetFlixcornSeriesDetail.kt      # Get series detail
│       ├── GetFlixcornEpisodeServers.kt    # Resolve episode servers
│       └── SendFlixcornToTvUseCase.kt      # Send stream to TV
└── presentation/
    ├── flixcorn/
    │   ├── FlixcornSearchScreen.kt         # Search results
    │   ├── FlixcornSeriesDetailScreen.kt   # Series detail
    │   ├── FlixcornEpisodeScreen.kt        # Episode servers
    │   └── FlixcornViewModel.kt            # Shared ViewModel
    └── navigation/
        └── AppDestination.kt               # (existing, add routes)
```

## Database Design

### FlixcornSeriesEntity
```sql
CREATE TABLE flixcorn_series (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    slug TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    poster_url TEXT,
    backdrop_url TEXT,
    overview TEXT,
    year INTEGER,
    rating REAL,
    genres TEXT,              -- JSON array
    number_of_seasons INTEGER,
    number_of_episodes INTEGER,
    status TEXT,
    detail_url TEXT NOT NULL,
    cached_at INTEGER NOT NULL,  -- epoch millis
    expires_at INTEGER NOT NULL  -- epoch millis
);
```

### FlixcornEpisodeCacheEntity
```sql
CREATE TABLE flixcorn_episode_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    episode_url TEXT NOT NULL UNIQUE,
    servers_json TEXT NOT NULL,   -- JSON serialized servers
    cached_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL
);
```

## API Endpoints (Flixcorn — Scraped)

Flixcorn does not provide a public API. All data is extracted via HTML scraping.

| Endpoint | Method | Response | Cache TTL |
|----------|--------|----------|-----------|
| `/search?q={query}` | GET | Search results HTML | 1 hour |
| `/serie/{slug}.html` | GET | Series detail HTML | 24 hours |
| `/ver/{slug}/temporada-{N}/capitulo-{N}.html` | GET | Episode page HTML | 1 hour |
| `/player/{token}` | GET | Player page with video embed | 15 minutes |
| `/external/{token}` | GET | External redirect page | 5 minutes |

### Scraping Selectors

| Data | CSS Selector |
|------|-------------|
| Search result card | `.media-card` |
| Series title | `h1.fw-bold` |
| Series poster | `.serie-poster` |
| Series backdrop | `.serie-backdrop` (background-image) |
| Year | `.serie-meta-item` (calendar icon) |
| Seasons count | `.serie-meta-item` (collection icon) |
| Rating | `.serie-meta-item` (star icon) |
| Genres | `.genre-link` |
| Season pills | `.ep-pill` |
| Episode row | `.ep-row` |
| Episode title | `.ep-row-title` |
| Episode number | `.ep-row-badge` |
| Quality tier | `.cap-quality-section` |
| Quality badge | `.cap-qual-badge` |
| Language group | `.cap-lang-node` |
| Language name | `.cap-lang-name` |
| Server row | `.cap-server-row` |
| Server name | `.cap-server-name` |
| Online link | `.cap-btn--online` (href) |
| Direct link | `.cap-btn--direct` (href) |
| Player token | `#plyr-meta[data-link-token]` |

## Security Requirements

- All HTTP requests use HTTPS
- No user credentials transmitted to Flixcorn
- Player URLs opened in system browser or trusted WebView
- No content injection or modification
- Scraping respects rate limits (1 req/sec max)
- No persistent storage of streaming URLs beyond cache TTL

## Validation Rules

- Search query: minimum 2 characters, maximum 100 characters
- Series slug: lowercase alphanumeric with hyphens only
- Season number: positive integer
- Episode number: positive integer
- Player token: hex string, 10 characters

## Error Handling

| Error | Handling |
|-------|----------|
| Network timeout | Retry once, then show error with retry button |
| HTML parse failure | Log warning, show "No se pudieron cargar los servidores" |
| No servers found | Show empty state with TMDB fallback option |
| Player URL resolution fails | Try direct link, then show error |
| Rate limit exceeded | Queue requests, show loading indicator |
| Flixcorn unreachable | Hide Flixcorn results, show TMDB only |

## Logging Strategy

- **DEBUG**: Request URLs, response codes, parse durations
- **INFO**: Search queries, series selected, servers resolved, TV sends
- **WARN**: Parse fallbacks triggered, cache misses, retry attempts
- **ERROR**: Network failures, parse exceptions, SSAP send failures

Structured logging format:
```
[FLIXCORN] [LEVEL] operation=? detail=? duration_ms=?
```

## Deployment Requirements

- Jsoup dependency added to `build.gradle.kts`
- No backend changes required
- No new permissions required
- Works on Android API 26+
- No ProGuard rules needed for Jsoup

## Testing Requirements

### Unit Tests
- `FlixcornHtmlParserTest`: parse search results, series detail, episode pages
- `FlixcornRepositoryTest`: mock scraper, test caching logic
- `SearchFlixcornUseCaseTest`: test search flow
- `GetFlixcornEpisodeServersTest`: test server resolution

### Integration Tests
- `FlixcornScraperIntegrationTest`: real HTTP calls to Flixcorn (manual only)
- `FlixcornSendToTvTest`: end-to-end with mock TV

### Test Fixtures
- Sample HTML files for each page type (search, series, episode, player)
- JSON fixtures for parsed server data

## Future Improvements

1. **Flixcorn Movies**: Extend scraping to movie pages if Flixcorn adds them
2. **Server Health Check**: Ping servers before offering them to user
3. **Quality Auto-Select**: Remember user's preferred quality/language
4. **Offline Cache**: Cache episode pages for offline server viewing
5. **WebView Player**: In-app player using Flixcorn's embedded player
6. **Subtitle Extraction**: Parse subtitle tracks from player pages
7. **Resume Playback**: Track last watched episode per series
8. **Recommendations**: Use Flixcorn's "Te puede gustar" for suggestions
