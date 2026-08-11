# Flixcorn Integration — Development Roadmap

## Phase 1: Foundation (Days 1–3)

### 1.1 Dependencies and Setup
- [ ] Add Jsoup dependency to `app/build.gradle.kts`
- [ ] Create `FlixcornScraper.kt` with basic HTTP client setup
- [ ] Add User-Agent header and rate limiter (1 req/sec)
- [ ] Create `FlixcornHtmlParser.kt` with Jsoup document loading

### 1.2 Data Models
- [ ] Create `FlixcornSeries.kt` domain model (slug, title, posterUrl, backdropUrl, overview, year, rating, genres, numberOfSeasons, numberOfEpisodes, status)
- [ ] Create `FlixcornEpisode.kt` domain model (episodeNumber, title, synopsis, seasonNumber)
- [ ] Create `StreamingServer.kt` domain model (serverName, quality, language, onlineUrl, directUrl, serverIcon)
- [ ] Create `FlixcornSearchResult.kt` DTO (title, year, posterUrl, slug, genres)

### 1.3 Repository Interface
- [ ] Define `FlixcornRepository.kt` interface in `domain/repositories/`
- [ ] Define `searchSeries(query: String): List<FlixcornSearchResult>` method
- [ ] Define `getSeriesDetail(slug: String): FlixcornSeries` method
- [ ] Define `getEpisodeServers(slug: String, season: Int, episode: Int): List<StreamingServer>` method

## Phase 2: Scraping Engine (Days 4–7)

### 2.1 Search Parser
- [ ] Implement `parseSearchResults(html: String): List<FlixcornSearchResult>`
- [ ] Parse `.media-card` elements for title, year, poster, slug, genres
- [ ] Handle empty results gracefully
- [ ] Write unit tests with sample search HTML fixture

### 2.2 Series Detail Parser
- [ ] Implement `parseSeriesDetail(html: String): FlixcornSeries`
- [ ] Parse `.serie-poster` for poster URL
- [ ] Parse `.serie-backdrop` for backdrop URL (extract from background-image)
- [ ] Parse `.serie-meta-item` elements for year, seasons, rating, status
- [ ] Parse `.genre-link` elements for genres
- [ ] Parse `.ep-row` elements for episode list
- [ ] Write unit tests with sample series detail HTML fixture

### 2.3 Episode Page Parser
- [ ] Implement `parseEpisodeServers(html: String): List<StreamingServer>`
- [ ] Parse `.cap-quality-section` for quality tiers
- [ ] Parse `.cap-qual-badge` for quality label (1080p, 720p)
- [ ] Parse `.cap-lang-node` for language groups
- [ ] Parse `.cap-lang-name` for language name
- [ ] Parse `.cap-server-row` for server rows
- [ ] Parse `.cap-server-name` for server name
- [ ] Parse `.cap-btn--online` href for online player URL
- [ ] Parse `.cap-btn--direct` href for direct link URL
- [ ] Write unit tests with sample episode page HTML fixture

### 2.4 Player Token Parser
- [ ] Implement `parsePlayerToken(html: String): String?`
- [ ] Parse `#plyr-meta[data-link-token]` attribute
- [ ] Handle missing token gracefully

## Phase 3: Repository Implementation (Days 8–10)

### 3.1 FlixcornRepository Implementation
- [ ] Implement `FlixcornRepositoryImpl.kt` in `data/repository/`
- [ ] Wire `FlixcornScraper` with `FlixcornHtmlParser`
- [ ] Add rate limiting (OkHttp interceptor or manual delay)
- [ ] Add retry logic with exponential backoff (max 2 retries)
- [ ] Implement error wrapping (network errors → domain exceptions)

### 3.2 Caching Layer
- [ ] Create `FlixcornSeriesEntity.kt` Room entity
- [ ] Create `FlixcornEpisodeCacheEntity.kt` Room entity
- [ ] Create `FlixcornSeriesDao.kt` with insert/get/delete
- [ ] Create `FlixcornEpisodeCacheDao.kt` with insert/get/delete
- [ ] Add entities to `KastLgDatabase.kt` with migration
- [ ] Implement cache-first strategy in repository

### 3.3 Dependency Injection
- [ ] Add `FlixcornRepository` to `AppContainer.kt`
- [ ] Create `SearchFlixcornUseCase.kt`
- [ ] Create `GetFlixcornSeriesDetail.kt`
- [ ] Create `GetFlixcornEpisodeServers.kt`
- [ ] Wire use cases in `AppContainer`

## Phase 4: Search Integration (Days 11–13)

### 4.1 Unified Search
- [ ] Modify existing `SearchMoviesUseCase` or create `UnifiedSearchUseCase`
- [ ] Run TMDB and Flixcorn searches in parallel
- [ ] Merge results with source tags (`SOURCE_TMDB`, `SOURCE_FLIXCORN`)
- [ ] Add sorting options (relevance, year, source)

### 4.2 Search UI Updates
- [ ] Add source badge to search result cards
- [ ] Differentiate Flixcorn results visually (subtle color or icon)
- [ ] Handle loading state for parallel searches
- [ ] Show error state for individual source failures

## Phase 5: Series Detail Integration (Days 14–17)

### 5.1 Flixcorn Series Detail Screen
- [ ] Create `FlixcornSeriesDetailScreen.kt` (reuse existing `TvShowDetailScreen` patterns)
- [ ] Display poster, backdrop, title, year, rating, genres, synopsis
- [ ] Season selector chips
- [ ] Episode list with number, title, synopsis
- [ ] "Ver en TV" button

### 5.2 Navigation
- [ ] Add `FlixcornSeriesDetailRoutes` to `AppDestination.kt`
- [ ] Add route: `flixcorn/{slug}`
- [ ] Wire navigation from search results to Flixcorn detail
- [ ] Handle deep linking from search to detail

### 5.3 Episode Server Selection
- [ ] Create `FlixcornEpisodeScreen.kt` or inline in detail screen
- [ ] Display quality tiers as sections
- [ ] Display language groups within each tier
- [ ] Display server rows with name, online button, direct button
- [ ] Server priority ordering (Voe > Dsvplay > Vidara > ...)

## Phase 6: TV Playback Integration (Days 18–20)

### 6.1 URL Resolution
- [ ] Implement `resolvePlayerUrl(token: String): String?` in scraper
- [ ] Fetch `/player/{token}` page and parse video source
- [ ] Handle iframe embed URLs
- [ ] Fallback to `/external/{token}` direct link

### 6.2 Send to TV
- [ ] Create `SendFlixcornToTvUseCase.kt`
- [ ] Resolve player URL → send via SSAP
- [ ] Handle resolution failure → try direct link
- [ ] Handle SSAP failure → show error with retry
- [ ] Add to history on successful send

### 6.3 TV Send UI
- [ ] Update "Ver en TV" button flow for Flixcorn content
- [ ] Show server selection dialog before sending
- [ ] Show loading state during URL resolution
- [ ] Show success/error feedback

## Phase 7: Polish and Edge Cases (Days 21–23)

### 7.1 Error Handling
- [ ] Network timeout → retry once → error state
- [ ] Parse failure → fallback to UnlimPlay
- [ ] No servers found → empty state with TMDB option
- [ ] Flixcorn unreachable → hide Flixcorn results

### 7.2 Cache Management
- [ ] Implement cache expiration (1h for episodes, 24h for series)
- [ ] Add cache size limit (max 500 series, 5000 episodes)
- [ ] Clear cache on app update
- [ ] Manual cache clear in settings

### 7.3 Rate Limiting
- [ ] Implement token bucket rate limiter (1 req/sec)
- [ ] Queue requests when rate limited
- [ ] Show loading indicator during queue wait

## Phase 8: Testing (Days 24–26)

### 8.1 Unit Tests
- [ ] `FlixcornHtmlParserTest` — all parser methods
- [ ] `FlixcornRepositoryTest` — caching, error handling
- [ ] `SearchFlixcornUseCaseTest` — search flow
- [ ] `GetFlixcornEpisodeServersTest` — server resolution
- [ ] `SendFlixcornToTvUseCaseTest` — TV send flow

### 8.2 Test Fixtures
- [ ] `search_results.html` — sample search page
- [ ] `series_detail.html` — sample series page
- [ ] `episode_page.html` — sample episode page
- [ ] `player_page.html` — sample player page

### 8.3 Integration Tests
- [ ] `FlixcornScraperIntegrationTest` — real HTTP (manual)
- [ ] `FlixcornSearchFlowTest` — search → detail → servers

## Phase 9: Documentation and Release (Day 27)

### 9.1 Documentation
- [ ] Update `README.md` with Flixcorn feature
- [ ] Update `COMPARISON.md` with Flixcorn as content source
- [ ] Add `FLIXCORN-ARCHITECTURE.md` with data flow diagram

### 9.2 Release
- [ ] Update version in `build.gradle.kts`
- [ ] Update `CHANGELOG.md`
- [ ] Build release APK
- [ ] Test on physical LG webOS TV

---

## Dependency Graph

```
Phase 1 (Foundation)
    └── Phase 2 (Scraping Engine)
        └── Phase 3 (Repository)
            ├── Phase 4 (Search Integration)
            └── Phase 5 (Series Detail)
                └── Phase 6 (TV Playback)
                    └── Phase 7 (Polish)
                        └── Phase 8 (Testing)
                            └── Phase 9 (Release)
```

## Estimated Total: 27 working days

## Risk Register

| Risk | Impact | Mitigation |
|------|--------|------------|
| Flixcorn HTML structure changes | High | Version parsers, add fallback selectors, cache valid structures |
| Rate limiting blocks scraping | Medium | Respect 1 req/sec, add caching, queue requests |
| Player URL resolution fails | Medium | Fallback to direct link, then UnlimPlay |
| Cloudflare protection blocks requests | High | Rotate User-Agent, add delay between requests, consider proxy |
| Legal concerns around scraping | High | No content hosting, no modification, fair use metadata only |
