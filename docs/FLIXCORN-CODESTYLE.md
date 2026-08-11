# Flixcorn Integration — Code Style Rules

## General Principles

- All code, comments, identifiers, and documentation in English
- Production-ready code only — no prototypes, no placeholders, no hacks
- Clean Architecture: data / domain / presentation layers strictly separated
- Strong typing everywhere — no `Any`, no `String` where a typed model exists
- Self-documenting code — names explain intent without comments
- Comments only for complex business logic, non-obvious algorithms, or parser edge cases
- Consistent naming across all modules

## Naming Conventions

### Files
- `PascalCase` for all Kotlin files: `FlixcornHtmlParser.kt`
- Suffixes by role: `*Repository.kt`, `*UseCase.kt`, `*ViewModel.kt`, `*Screen.kt`, `*Entity.kt`, `*Dao.kt`, `*Dto.kt`

### Classes and Interfaces
- `PascalCase`: `FlixcornRepository`, `GetEpisodeServersUseCase`
- Interfaces: noun or adjective, no `I` prefix: `FlixcornRepository` not `IFlixcornRepository`
- Data classes: `FlixcornSeries`, `StreamingServer`

### Functions
- `camelCase`: `parseSearchResults()`, `getEpisodeServers()`
- Boolean functions: `is`/`has`/`can` prefix: `isCacheExpired()`, `hasToken()`
- Mutable helpers: internal only, prefixed with underscore or `private`

### Variables
- `camelCase`: `seriesSlug`, `episodeNumber`
- Constants: `UPPER_SNAKE_CASE`: `BASE_URL`, `CACHE_TTL_MS`
- backing property: `_fieldName` for private mutable, `fieldName` for public immutable

### Compose
- Screen composables: `*Route` suffix for navigation entry: `FlixcornSeriesDetailRoute`
- State classes: `*UiState`: `FlixcornSeriesUiState`
- Events: `*Event`: `FlixcornSearchEvent`

## Function Design

- Maximum 30 lines per function
- Single responsibility: one function does one thing
- Maximum 3 parameters; use data class for more
- Early returns over nested if/else
- No side effects in pure functions

```kotlin
// GOOD
fun parseServerRow(element: Element): StreamingServer? {
    val name = element.selectFirst(".cap-server-name")?.text() ?: return null
    val onlineHref = element.selectFirst(".cap-btn--online")?.attr("href")
    val directHref = element.selectFirst(".cap-btn--direct")?.attr("href")

    return StreamingServer(
        serverName = name,
        onlineUrl = onlineHref?.let { "$BASE_URL$it" },
        directUrl = directHref?.let { "$BASE_URL$it" },
    )
}

// BAD
fun parseStuff(el: Element): Any? {
    if (el != null) {
        val n = el.selectFirst(".cap-server-name")
        if (n != null) {
            // ... 50 lines of nested logic
        }
    }
    return null
}
```

## Error Handling

- Use `Result<T>` or sealed classes for error states
- Never catch generic `Exception` — catch specific types
- Log errors with structured context
- Propagate errors to UI layer — never silently swallow

```kotlin
// GOOD
sealed class FlixcornResult<out T> {
    data class Success<T>(val data: T) : FlixcornResult<T>()
    data class Error(val code: FlixcornError) : FlixcornResult<Nothing>()
    data object Loading : FlixcornResult<Nothing>()
}

enum class FlixcornError {
    NETWORK_TIMEOUT,
    PARSE_FAILURE,
    NO_SERVERS_FOUND,
    RATE_LIMITED,
    UNREACHABLE,
}

// BAD
fun fetchSeries(slug: String): Any? {
    try {
        // ...
    } catch (e: Exception) {
        return null  // Silent failure — user sees nothing
    }
}
```

## Parser Rules (Flixcorn-Specific)

- All CSS selectors defined as `companion object` constants
- Selectors versioned with comments when Flixcorn updates HTML
- Null-safe chaining: `element?.selectFirst("...")?.text() ?: ""`
- Never throw from parsers — return null or empty list
- Log parse warnings at WARN level with selector that failed

```kotlin
// GOOD
companion object {
    private const val SELECTOR_SEARCH_CARD = ".media-card"
    private const val SELECTOR_SERIES_TITLE = "h1.fw-bold"
    private const val SELECTOR_EPISODE_ROW = ".ep-row"
}

fun parseSearchResults(html: String): List<FlixcornSearchResult> {
    val doc = Jsoup.parse(html)
    val cards = doc.select(SELECTOR_SEARCH_CARD)

    return cards.mapNotNull { card ->
        val title = card.selectFirst(".media-title")?.text() ?: return@mapNotNull null
        val slug = card.attr("href").removePrefix("/serie/").removeSuffix(".html")
        // ...
    }
}
```

## Caching Rules

- Cache-first: check local before network
- TTL-based expiration: series 24h, episodes 1h, player 15min
- Cache key: URL or slug (never assume stable IDs)
- Cache size limit: 500 series, 5000 episodes max
- Evict oldest on overflow (LRU)

## Rate Limiting

- Maximum 1 request per second to Flixcorn
- Token bucket implementation preferred
- Queue excess requests, do not drop
- Log rate limit events at DEBUG level

## Dependency Injection

- Use `AppContainer` singleton (existing pattern)
- Lazy initialization for all repositories
- No Hilt/Dagger — manual DI via `AppContainer`
- Register new use cases in `AppContainer.applyToken()` or separate init

## Testing Rules

- One test class per production class
- Test file naming: `*Test.kt` (same package)
- HTML fixtures in `src/test/resources/flixcorn/`
- Mock HTTP responses, never hit real Flixcorn in unit tests
- Test edge cases: empty HTML, malformed HTML, missing selectors, network errors
- Minimum 80% line coverage for parsers

```kotlin
// GOOD
@Test
fun `parseSearchResults returns empty list for no results`() {
    val html = loadFixture("flixcorn/search_empty.html")
    val results = parser.parseSearchResults(html)
    assertThat(results).isEmpty()
}

@Test
fun `parseEpisodeServers handles missing online link`() {
    val html = loadFixture("flixcorn/episode_no_online.html")
    val servers = parser.parseEpisodeServers(html)
    assertThat(servers).hasSize(1)
    assertThat(servers[0].onlineUrl).isNull()
    assertThat(servers[0].directUrl).isNotNull()
}
```

## Formatting

- ktlint enforced — run before commit
- 4 spaces indentation, no tabs
- Maximum line length: 120 characters
- Trailing commas on multi-line parameters
- Wildcard imports forbidden
- Unused imports removed

## Security

- No hardcoded URLs except `BASE_URL` constants
- No credentials or tokens in source code
- All URLs use HTTPS
- No `eval()` or dynamic code execution
- No WebView JavaScript injection
- Validate all parsed URLs before use

## Commit Rules

- Conventional commits: `feat:`, `fix:`, `test:`, `refactor:`, `docs:`
- Maximum 72 characters in subject line
- One logical change per commit
- No dead code, no TODOs, no commented-out code
- Format with ktlint before commit

## Prohibited

- No `println()` — use structured logging
- No `Thread.sleep()` — use coroutines with delay
- No `!!` (non-null assertion) — use safe calls or check
- No `runBlocking` in production code
- No `GlobalScope` — use structured concurrency
- No `var` in data classes
- No mutable public state
- No God classes (>200 lines)
