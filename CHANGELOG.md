# Changelog

## 2026-06-18 — Kast v1.6: Rebranding + GitHub Prep

### P0 — Rebranding
* KastLG → Kast en UI (header HomeScreen).

### P0 — Pantalla Acerca de
* Nuevo AboutScreen con: nombre, versión, desarrollador, créditos TMDB/UnlimPlay, disclaimer.
* Botón "Obtener API Key de TMDB" que abre navegador.
* Links a TMDB y UnlimPlay.
* Accesible desde Ajustes.

### P0 — Home limpio
* Chips de Favoritos/Historial eliminados (duplicaban bottom nav).

### P0 — Filtros por género
* Chips de género funcionales en Home (Películas: 7 géneros, Series: 6 géneros).
* Filtrado client-side por genreIds.
* Opción "Todos" para limpiar filtro.

### P0 — Detalle compacto
* Poster cambiado de 2:3 a 16:9 (más compacto, menos scroll).

### P1 — Paleta neutra
* Accent: gold → light gray (#B0B0B0).
* AccentMuted ajustado a #2A2D33.

### P1 — Documentación GitHub
* .gitignore actualizado (*.apk, google-services.json).
* README con branding Kast.

### P2 — Limpieza logs
* SsapClient: logs verbosos de JSON/payload eliminados.

### Verificación
* 64 unit tests, 0 failures, 14 suites.
* APK SHA-256: `187FEBC173B795E600C35911A2859A5B33BD689B0A636EC84A3D6991197715A7`.

## 2026-06-18 — KastLG v1.5: UX/A11y Fix Pack

### P0 — Bugs UX corregidos
* Genre chips: cambiados de AssistChip clickeable a Surface+Text informativo (no más dead affordance).
* Episodio seleccionado: feedback "Temporada X · Episodio Y", botón cambia a "Ver episodio en TV".
* Mensajes TV movidos antes de los botones de acción (visibles sin scrollear).
* Botón debug "Probar abrir Google" eliminado de TV Settings.
* Series en favoritos: placeholder "Próximamente" agregado en TvShowDetail.

### P1 — Accesibilidad
* Contraste TextSecondary: #C9C4B9 → #D4D0C8 (ratio ~5.0:1 contra BackgroundRaised).
* contentDescription agregado: search bar, spinner, botón Ver en TV, status icon TV.
* Icono Historial: Star → History (semántica correcta).

### P2 — Quick wins
* Skeletons integrados en Home cuando carga sin contenido.
* Empty states con CTA: "Explorar películas" (Favoritos), "Ir al inicio" (Historial).

### Verificación
* 64 unit tests, 0 failures, 14 suites.
* APK SHA-256: `D9831B74314637A2D25C7D501227F144E7E4969AE6DF52818A92A3A746E5C009`.

## 2026-06-18 — KastLG v1.3 + v1.4: Discovery profesional + UX Premium

### v1.3 — Discovery + Estabilidad
* Auto-reconnect: reconexión automática al abrir app si hay TV previamente configurada.
* Health check: indicador visual siempre visible (Conectada/Desconectada) en TV Settings.
* Hardening: mensajes de error específicos para timeout ("La TV no responde"), IP inválida, y pairing rechazado.

### v1.4 — UX/UI Premium
* AnimatedContent con fadeIn/fadeOut en HomeScreen para transiciones entre estados.
* Skeleton loaders animados (ShimmerBox, MovieCarouselCardSkeleton, CarouselSectionSkeleton).
* Empty states modernos en Favoritos e Historial con iconos y mejor diseño.
* Detalle screens: spacing consistente verificado (24dp horizontal).

### Verificación
* 66 unit tests, 0 failures, 14 suites.
* APK SHA-256: `4A31D55E677C8349D2E67850A23C3C81693DA750D3831E6A08BF27336E2729EC`.

## 2026-06-18 — KastLG v1.2: Series completas + Navigation + AppBar

### P0 — Series completas
* Nuevo endpoint TMDB: `GET /tv/{id}/season/{season_number}` para obtener episodios.
* Nuevos DTOs: `SeasonDto`, `EpisodeDto` con mappers a dominio.
* Nuevos modelos: `Season`, `Episode` con campos: número, título, fecha, imagen, descripción.
* Nuevo use case: `GetTvSeasonUseCase`.
* TvShowDetailScreen: chips de temporadas (ignora season_number=0), cards de episodios con imagen/título/fecha/descripción.
* Selección de episodio habilita "Ver en TV" con URL específica.
* `PlaybackUrlBuilder`: clase desacoplada para construir URLs de UnlimPlay (movies y series).
* MovieDetailViewModel y TvShowDetailViewModel ahora usan `PlaybackUrlBuilder`.

### P0 — Navigation
* Bottom bar visible desde MovieDetail y TvShowDetail.
* Navegación Inicio/Favoritos/Historial/TV funciona desde cualquier pantalla.

### P0 — AppBar compacta
* TopAppBar reducida a 48.dp en MovieDetailScreen y TvShowDetailScreen.

### P1 — Home moderna
* Tabs "Películas" / "Series" en HomeScreen.
* Películas: Tendencias, Estrenos, Mejor valoradas.
* Series: populares, mejor valoradas, tendencia.

### Verificación
* 66 unit tests, 0 failures, 14 suites.
* APK SHA-256: `E24D599E8F5AF23561292F890E2389A84E4C2F42DB1F98DC32004D2A18D26F84`.

## 2026-06-18 — KastLG v1.1: Discovery + Home Moderna + Series

### Fase 8: Discovery automático de TVs
* TvDiscoveryManager: SSDP multicast + scan de subnet para descubrir TVs LG en la red local.
* Botón "Buscar TVs en la red" en TV Settings con lista de resultados seleccionables.
* Ingreso manual de IP mantenido como fallback.

### Fase 9: Home moderna
* Nuevos endpoints TMDB: trending, now_playing, top_rated, tv/popular.
* Nuevos DTOs y modelos: TvShowDto, TvShowDetailDto, TvShow, TvShowDetail.
* Nuevos use cases: GetTrendingMovies, GetNowPlayingMovies, GetTopRatedMovies, GetPopularTvShows, SearchTvShows, GetTvShowDetail.
* HomeScreen rediseñado con carruseles horizontales, cards modernas, búsqueda unificada películas/series.
* Botones rápidos de Favoritos e Historial en Home.

### Fase 10: Series
* TvShowDetailScreen con poster, sinopsis, rating, géneros, temporadas.
* Botón "Ver en TV" en detalle de serie con URL UnlimPlay.
* Navegación Home → Series detail.
* Búsqueda unificada que muestra películas y series.

### Verificación
* 66 unit tests, 0 failures, 14 suites.
* APK SHA-256: `0EE3024E4D35AF2AFA78A9662E7AA844989E4DE83FEA5F9C613F4726F8A12645`.

## 2026-06-18 — Limpieza UX/UI y Hardening

### Completed

* Neutralized all Argentine Spanish (voseo) to neutral Spanish across all screens and error messages.
* Locked portrait orientation in AndroidManifest.xml — app no longer rotates.
* Cleaned TV Settings screen: removed diagnostic log from UI, simplified to status/IP/connect/test/delete.
* Added "Borrar configuración" button in TV Settings.
* Standardized messages: "Enviado a la TV", "No se pudo abrir en la TV", "TV conectada".
* Fixed openUrl error handling: `type=error` and `returnValue=false` are now proper failures.
* Masked client-key in SsapClient logs (first 6 + last 4 characters).
* Updated all unit tests to match new neutral Spanish strings.
* Verified 68 unit tests across 14 suites: all passed.
* Rebuilt `app-debug.apk` (18,891,505 bytes, SHA-256 `8B963589F7CB314085BA5C849E626A68E539065511A6E12FF8D9966B161A62DF`).

## 2026-06-18 — Configuración TMDB en-app

### Completed

* Added DataStore Preferences dependency for persistent token storage.
* Created `TokenStore` interface + `TmdbTokenStore` implementation using DataStore.
* Updated `AppContainer`: reads token from DataStore first, falls back to BuildConfig, supports dynamic refresh via `refreshTmdbRepository()`.
* Created `SettingsScreen` with: token input (with show/hide toggle), save button, delete button, current token status display, help text explaining token format.
* Added "Ajustes" tab to bottom navigation bar.
* Updated `HomeScreen`: shows "Configurar token TMDB" button when no token is configured.
* Updated `MissingTmdbTokenException` message to direct users to Settings.
* Added 7 unit tests for `SettingsViewModel`: initial state, save, blank validation, clear, visibility toggle, clear messages.
* Verified 64 unit tests across 14 suites: all passed.
* Rebuilt `app-debug.apk` (19,567,149 bytes, SHA-256 `9D8762AA543DEDC8C149E4D82D998AD9359250A8923993D668A5BB18067CE97F`).

## 2026-06-18 — Phase 7: Pulido MVP

### Completed

* Translated all UI texts to Spanish: navigation labels, screens, error messages, help sections, content descriptions.
* Improved error messages for: TMDB without token, TV not configured, TV not paired, WebSocket failure, URL open failure.
* Added help section in TV Settings explaining: same WiFi requirement, manual IP entry, permission acceptance on TV.
* Added `sent_to_tv` boolean field to history entity to differentiate "viewed detail" from "sent to TV".
* Added Room migration 2→3 for the new `sent_to_tv` column.
* History screen now shows a TV icon next to entries that were sent to a TV.
* Created `README.md` with: app description, TMDB setup, APK installation, TV configuration, project structure, known limitations.
* Updated all unit tests to match new Spanish error messages and added `recordSentToTv` test.
* Verified 57 unit tests across 13 suites: all passed.
* Rebuilt `app-debug.apk` (18,300,450 bytes, SHA-256 `CFB1DB5C0F2638AED8098917190DEEDF5C27374BFBB71446F7218C5A856C8ABA`).

## 2026-06-18 — Phase 6: Watch on TV

### Completed

* Connected "Watch on TV" button in MovieDetailScreen to TvRepository.
* Button now builds URL `https://unlimplay.com/play/embed/movie/{tmdbId}` and sends to TV via SSAP.
* If no TV is configured, navigates to TV Settings automatically.
* If TV is not paired, navigates to TV Settings for pairing.
* If openUrl fails, shows error message on detail screen.
* If openUrl succeeds, shows "Playing on {TV name}" success message.
* Added `TvRepository` dependency to `MovieDetailViewModel` and factory.
* Added `watchOnTv()` method with navigation events for settings redirect.
* Added TV message display (error/success) in MovieDetailScreen after action buttons.
* Added 4 unit tests: no config, paired TV, unpaired TV, openUrl failure.

### Verified

* Ran `.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`; exit code 0.
* Verified 56 unit tests across 13 suites: all passed.
* Verified lint with 0 errors.
* Rebuilt `app-debug.apk` (18,326,442 bytes, SHA-256 `C55709262AA89B04A3C2A8BCF80CE34423B816836B7B62223D8D46C531A06F08`).

## 2026-06-18 — Phase 5: SSAP PoC + TV Settings

### Completed

* Implemented SSAP (Smart Service Access Protocol) client using OkHttp WebSocket.
* Created `SsapMessage` data class for JSON protocol messages (register, request).
* Created `SsapClient` with connection lifecycle, pairing flow, and command execution.
* Created `TvConfigEntity` + `TvConfigDao` with Room migration 1→2.
* Created `WebOsTvRepository` combining SSAP client with Room persistence.
* Created `TvSettingsScreen` with IP input, Connect button, and Open Google button.
* Implemented pairing flow: PROMPT type with manifest, client-key storage and reconnection.
* Registered TV repository in AppContainer with proper dependency injection.
* Added 8 unit tests for SsapMessage and 7 for TvSettingsViewModel.

## 2026-06-18 — LG webOS research

### Completed

* Created `LG_WEBOS_RESEARCH.md` documenting SSAP/WebOS WebSocket protocol feasibility for native Android TV control.
* Verified SSAP protocol viability: JSON over WebSocket on port 3000/3001, well-characterized by open-source community.
* Verified browser launch capability: `ssap://system.launcher/open` with URL payload.
* Verified pairing flow: PROMPT or PIN types, client-key storage for reconnection.
* Verified library landscape: hobbyquaker/lgtv2 (Node.js, 344 stars), jareksedy/WebOSClient (Swift, 38 stars), ConnectSDK (archived).
* Documented webOS version compatibility (4.0+ covers 2014–2025 models).
* Documented limitations: same WiFi required, user interaction for pairing, no official Android SDK.
* Decision: CONTINUAR with SSAP/WebSocket using existing OkHttp dependency. No new libraries needed.

## 2026-06-18 — Phase 4 audit fixes

### Completed

* Removed non-existent DataStore references from ARCHITECTURE.md; DataStore is not implemented and has no dependency in the project.
* Fixed directory listing in ARCHITECTURE.md: `details` → `detail`, removed `data/tv/` which does not exist.
* Moved `favoriteErrorMessage` clear after successful toggle in `MovieDetailViewModel.toggleFavorite()` to prevent error state loss on cancellation.
* Fixed smart-cast compilation error on computed property `persistenceErrorMessage` in `MovieDetailScreen` by extracting to a local variable.
* Added regression test `toggle cancellation after previous error preserves error state` proving that a failed toggle error is cleared only on subsequent success.
* Verified 37 unit tests across 11 suites: 37 passed, 0 failed, 0 errors.

### Verified

* Ran `.\gradlew.bat --stop` then `.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`; exit code 0.
* Verified 37 unit tests across 11 suites: all passed.
* Verified lint with 0 errors.
* Rebuilt `app-debug.apk` (18,234,914 bytes, SHA-256 `EB6719DAEC51769BBBA760FB3190DCB5ED68A38FA460E110CB720B44CF1DE412`).

## 2026-06-18 — Phase 4

### Completed

* Added Room 2.7.2 with a version 1 database, exported schema, explicit migration registry, and no destructive migration fallback.
* Added persistent favorite and history entities, DAOs, domain repository contracts, and Room repository implementations.
* Replaced the in-memory detail favorite with a real Room-backed toggle that survives ViewModel, activity, and process recreation.
* Replaced the Favorites placeholder with a Flow-backed screen that updates from Room and opens movie details.
* Defined Phase 4 history as successfully viewed movie details, because real playback does not exist before the webOS phases.
* Added recency-ordered, TMDB-ID-deduplicated history and replaced the History placeholder with a Flow-backed screen.
* Kept DAO access inside data repositories; presentation depends only on domain repository interfaces.
* Added Room, repository, detail ViewModel, Favorites ViewModel, and History ViewModel regression coverage.

### Persistence and migration decisions

* `tmdb_id` is the primary key for both tables, so favorite toggles and history revisits cannot create duplicates.
* Favorites are ordered by `favorited_at`; history is ordered by `viewed_at`.
* A successful detail load records or refreshes the corresponding history entry. No playback URL or fake TV event is recorded.
* The checked-in `app/schemas/com.kastlg.app.data.local.KastLgDatabase/1.json` file is the migration baseline.
* Future schema versions must add explicit entries to `DatabaseMigrations.ALL`.

### Verified

* The first clean attempt found locked lint-cache files; ran `.\gradlew.bat --stop` and repeated cleanly.
* Ran `.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`; exit code 0.
* Verified 28 unit/Robolectric tests across 11 suites: 28 passed, 0 failed, 0 errors, 0 skipped.
* Verified Room insertion, deletion, Flow observation, ordering, history deduplication, and persistence after closing and reopening the database.
* Verified lint with 0 errors and 11 non-blocking dependency/KAPT warnings.
* Rebuilt `app-debug.apk` (18,234,914 bytes, SHA-256 `E80AE7BCE14CEEF5E5178D8E8C42FB66F6404A4F3589EDF78797C0EF1DCC5B2A`).
* Confirmed ADB was available but no device or emulator was connected, so instrumented tests were not run.

### Pending

* Create `LG_WEBOS_RESEARCH.md` before starting Phase 5.
* Phase 5 TV configuration, IP persistence, connection testing, and TV status remain pending.
* Phase 6 webOS discovery, pairing, client-key storage, browser launch, and playback URL delivery remain pending.
* Run a real TMDB end-to-end check on an emulator or physical device with a locally configured `TMDB_ACCESS_TOKEN`.

### Risks

* History currently means “movie details viewed,” not playback history. It must only become playback history when a real webOS playback flow exists.
* KAPT is retained for Room annotation processing and emits a performance warning recommending KSP; this does not affect correctness.
* Instrumented UI behavior was not executed because no Android target was connected.

## 2026-06-18 — Phase 3

### Completed

* Added the TMDB movie-detail endpoint, DTO mapping, repository/use-case path, detail navigation, and complete detail screen.
* Added poster, title, synopsis, release year, rating, genres, actionable loading/error states, and retry.
* Added an in-memory favorite toggle for Phase 3 without Room or persistence.
* Connected `Watch on TV` to the existing TV Settings destination. It now gives an explicit Phase 5 setup response instead of doing nothing or pretending to connect/play.
* Fixed locale-sensitive rating formatting by using an explicit US locale, matching TMDB decimal presentation.
* Extended the production MockWebServer contract to verify `/movie/550`, `language=en-US`, `Accept`, and the real bearer authorization header.
* Added detail failure → retry → success coverage and verified that the in-memory favorite survives a detail retry.

### Verified

* The first clean attempt found locked lint-cache files; ran `.\gradlew.bat --stop` and repeated cleanly.
* Ran `.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain`; exit code 0.
* Verified 20 unit tests across 6 suites: 20 passed, 0 failed, 0 errors, 0 skipped.
* Verified lint with 0 errors and 5 dependency-update warnings.
* Rebuilt `app-debug.apk` (17,935,187 bytes, SHA-256 `8A6BA9C5C865DD24E9F80838F10D76963DDAF588EE0E621586AD48DA32051974`).
* Manually audited the detail action wiring: the button invokes the injected callback and navigation opens the existing `tv-settings` destination.

### Pending

* Phase 4 Room persistence for favorites and history remains pending.
* Phase 5 TV configuration and connection testing remain pending.
* Phase 6 webOS pairing, client-key storage, browser launch, and playback URL delivery remain pending.
* Run a real TMDB end-to-end check on an emulator or physical device with a locally configured `TMDB_ACCESS_TOKEN`.

### Risks

* Phase 3 favorites are intentionally process-memory only and do not survive recreation or app restart.
* `Watch on TV` currently routes to the explicit TV Settings placeholder; it does not connect to a TV or simulate playback.
* Runtime detail content depends on TMDB availability, rate limits, and image CDN connectivity.

## 2026-06-18 — Phase 2

### Completed

* Replaced the Phase 1 in-memory Home content with the real TMDB movie API.
* Added Retrofit 3.0.0, OkHttp 4.12.0, Gson conversion, and Coil 3.0.4 image loading.
* Added movie-only search, official movie genres, popular discovery, and genre discovery.
* Added TMDB DTO mapping into domain `Movie` and `Genre` models behind a repository and use cases.
* Added a Flow-based Home ViewModel with a real 400 ms search debounce and stale-request cancellation while keeping genre discovery immediate.
* Added responsive Compose movie cards with TMDB posters, release year, rating, loading, empty, and actionable error states.
* Added retry coverage for genre loading failures and recovery.
* Fixed movie retry so the visible action relaunches the current search or discovery request after a failure.
* Added a MockWebServer boundary test for production TMDB routes, relevant query parameters, and bearer authorization.
* Added safe v4 bearer-token configuration through ignored `local.properties` or `TMDB_ACCESS_TOKEN`.
* Added a non-crashing missing-token state; builds and unit tests do not require credentials.
* Kept movie detail navigation as a pending callback. No detail endpoint or screen was implemented.
* Added Phase 2 unit coverage for DTO mapping, repository behavior, genres, initial discovery, genre selection, search debounce, cancellation, missing credentials, retry recovery, and the real HTTP boundary.
* Documented credential setup, client-token risk, clean verification, and pending real TMDB E2E validation in `BUILDING.md`.

### Verified

* Ran `.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain` from PowerShell without a profile; exit code 0.
* Verified 19 unit tests across the current workspace: 19 passed, 0 failed, 0 skipped.
* Verified lint with 0 errors and 6 warnings.
* Rebuilt `app-debug.apk` (17,935,187 bytes, SHA-256 `47A949FD5C1AAFE55E7FB1822F16824307913C24124EF5AACAFB5A338BA11268`).
* Verified the missing-token path with fakes: no TMDB call is attempted and the UI exposes an actionable configuration message.

### Pending

* Run a real TMDB end-to-end check on an emulator or physical device with a locally configured `TMDB_ACCESS_TOKEN`.
* Implement movie details in Phase 3; `Obtener detalles` intentionally remains incomplete.
* Add pagination if a later product phase requires browsing beyond TMDB's first result page.

### Risks

* A bearer token embedded in an Android APK can be extracted. Use a dedicated read-only TMDB token and never reuse a sensitive credential.
* Runtime content depends on TMDB availability, rate limits, and image CDN connectivity.
* Phase 2 currently displays the first TMDB result page, which matches the defined phase scope but limits deep browsing.

## 2026-06-18 — Phase 1

### Completed

* Created the native Android Kotlin project with package and namespace `com.kastlg.app`.
* Configured Jetpack Compose, Material 3, Navigation Compose, Coroutines, and Flow.
* Added the `presentation`, `domain`, and `data` layers with a minimal MVVM and Repository implementation.
* Added a sober, accessible dark movie theme.
* Added a functional Home screen and bottom navigation.
* Added explicit Phase 1 placeholders for Favorites, History, and TV Settings.
* Added a Windows Gradle bootstrap that discovers a local JDK 17+ without storing JDK binaries or machine-specific paths.
* Documented the supported JDK discovery locations and the `KASTLG_JAVA_HOME` override in `BUILDING.md`.
* Generated the Gradle wrapper and produced a debug APK successfully.

### Verified

* Ran `.\gradlew.bat :app:assembleDebug --console=plain` from PowerShell without a profile, with an empty `JAVA_HOME` and Java 8 first on `PATH`.
* Confirmed that the wrapper selected Eclipse Temurin 17.0.19 for Gradle 8.9.
* Ran `:app:lintDebug` successfully with no blocking findings.
* Installed the debug APK on a temporary headless Android 35 Google APIs x86_64 AVD.
* Verified the final APK SHA-256: `5BBBA0BF31072B317167FFDA8352B3E6BFD581B5E404481200841137B60D9DC2`.
* Cold-launched `com.kastlg.app/.MainActivity`; Android reported the activity as `topResumedActivity`.
* Checked the final application process logcat after launch: 36 lines inspected and zero fatal matches.
* Removed the temporary AVD after the smoke test.

### Pending

* Add automated unit and UI test source sets; `:app:testDebugUnitTest` currently succeeds with no test sources.
* Validate Phase 1 on a physical Android device in addition to the verified emulator run.

### Risks

* The bootstrap selects an installed JDK but intentionally does not download one. Machines without a discoverable JDK 17+ must install one or set `KASTLG_JAVA_HOME`.
* Runtime evidence currently covers one Android 35 x86_64 emulator configuration.
