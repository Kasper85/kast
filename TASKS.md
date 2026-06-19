# TASKS

## Infraestructura

* [x] Crear proyecto Kotlin
* [x] Configurar Compose
* [x] Configurar Material 3
* [x] Configurar navegación

## Validación Fase 1

* [x] Compilar desde PowerShell sin perfil, con `JAVA_HOME` vacío y Java 8 en `PATH`
* [x] Seleccionar automáticamente un JDK 17+ local
* [x] Ejecutar `lintDebug`
* [x] Instalar el APK en un AVD Android 35 temporal
* [x] Confirmar `MainActivity` reanudada
* [x] Confirmar logcat de la aplicación sin errores fatales

## TMDB

* [x] Configurar Retrofit
* [x] Configurar API Key
* [x] Buscar películas
* [x] Obtener géneros
* [x] Obtener detalles

## Home

* [x] Barra búsqueda
* [x] Lista géneros
* [x] Lista películas

## Detalle

* [x] Poster
* [x] Título
* [x] Sinopsis
* [x] Rating
* [x] Favorito persistente (reemplaza el estado en memoria de Fase 3)
* [x] Ver en TV navega explícitamente a Configuración TV sin simular conexión ni reproducción

## Favoritos

* [x] Crear entidad Room
* [x] Crear DAO
* [x] Guardar favorito
* [x] Eliminar favorito
* [x] Mostrar favoritos persistentes desde Flow/Room

## Historial

* [x] Crear entidad Room
* [x] Registrar detalle visto sin simular reproducción
* [x] Mostrar historial persistente desde Flow/Room

## Configuración

* [x] Pantalla TV
* [x] Guardar IP
* [x] Guardar client key

## LG webOS

* [x] Investigar SSAP
* [x] Pairing
* [x] Abrir navegador
* [x] Enviar URL

## Testing

* [x] Búsqueda
* [x] Géneros
* [x] Favorito persistente
* [x] Historial
* [x] TV LG

## Validación Fase 2

* [x] Verificar que seleccionar un género cancela una búsqueda activa sin reemitirla ni aplicar debounce a discovery
* [x] Verificar recuperación de géneros después de fallo y retry
* [x] Verificar recuperación de películas después de fallo y retry
* [x] Verificar rutas, query params y Authorization Bearer reales con MockWebServer
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` desde PowerShell sin perfil

## Validación Fase 3

* [x] Verificar detalle TMDB, poster, sinopsis, calificación y géneros
* [x] Verificar fallo de detalle, retry y recuperación exitosa
* [x] Verificar `/movie/{id}`, query `language` y Authorization Bearer reales con MockWebServer
* [x] Verificar favorito en memoria, incluido retry sin perder el estado local
* [x] Corregir formato de calificación con locale explícito
* [x] Verificar que Ver en TV abre Configuración TV sin implementar webOS
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` desde PowerShell sin perfil

## Validación Fase 4

* [x] Verificar schema Room v1 exportado y estrategia de migración explícita sin fallback destructivo
* [x] Verificar toggle, eliminación, orden y persistencia de favoritos tras reabrir la base
* [x] Verificar historial de detalles vistos, orden por recencia y deduplicación por TMDB ID
* [x] Verificar repositories y ViewModels sin acceso directo a DAOs desde presentation
* [x] Mantener las regresiones de Fases 2–3 y ejecutar tests sin token TMDB
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` desde PowerShell sin perfil
* [x] Confirmar que no había dispositivo/emulador conectado para instrumented tests

## Correcciones auditoría Fase 4

* [x] Eliminar DataStore ficticio de ARCHITECTURE.md (no existe en código)
* [x] Corregir `details` → `detail` en estructura de ARCHITECTURE.md
* [x] Eliminar referencia a `data/tv/` inexistente de ARCHITECTURE.md
* [x] Mover limpieza de error después del toggle exitoso en MovieDetailViewModel
* [x] Corregir smart-cast de propiedad computada en MovieDetailScreen
* [x] Agregar test de regresión: toggle error → éxito limpia error correctamente
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — exit code 0

## Validación Fase 5

* [x] Implementar SsapClient con OkHttp WebSocket para protocolo SSAP
* [x] Crear TvConfigEntity + TvConfigDao con Room migration 1→2
* [x] Implementar WebOsTvRepository con connect+register y openUrl
* [x] Crear TvSettingsScreen con campo IP, botón Connect, botón Open Google
* [x] Verificar pairing con manifest PROMPT y client-key storage
* [x] Verificar 8 tests de SsapMessage (register, request, parsing, JSON)
* [x] Verificar 7 tests de TvSettingsViewModel (connect, openGoogle, error states)
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — exit code 0

## Validación Fase 6

* [x] Conectar botón "Watch on TV" con TvRepository
* [x] Construir URL UnlimPlay: `https://unlimplay.com/play/embed/movie/{tmdbId}`
* [x] Navegar a TV Settings si no hay TV configurada
* [x] Navegar a TV Settings si la TV no está emparejada
* [x] Mostrar error si openUrl falla
* [x] Mostrar éxito si openUrl funciona
* [x] Verificar 4 tests de watchOnTv (no config, paired, unpaired, failure)
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — exit code 0

## Validación Fase 7

* [x] Traducir todos los textos de UI a español (navegación, pantallas, errores, mensajes)
* [x] Mejorar mensajes de error para TMDB sin token, TV sin configurar, TV no emparejada, fallo WebSocket, fallo al abrir URL
* [x] Agregar sección de ayuda en TV Settings (misma WiFi, IP manual, aceptar permiso)
* [x] Diferenciar historial "detalle visto" vs "enviado a TV" con campo `sent_to_tv`
* [x] Agregar migración Room 2→3 para campo `sent_to_tv`
* [x] Crear README.md con documentación completa del proyecto
* [x] Ejecutar `clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` — exit code 0

## Configuración TMDB en la app

* [x] Agregar DataStore Preferences para persistir token
* [x] Crear interfaz TokenStore + implementación TmdbTokenStore
* [x] Actualizar AppContainer: leer token de DataStore, fallback a BuildConfig, refresh dinámico
* [x] Crear pantalla de Configuración (Settings) con campo token, guardar, borrar, ocultar/mostrar
* [x] Agregar navegación "Ajustes" al bottom nav
* [x] Mostrar botón "Configurar token TMDB" en Home cuando no hay token
* [x] Actualizar MissingTmdbTokenException para indicar Ajustes
* [x] Tests: 7 tests de SettingsViewModel (estado inicial, guardar, borrar, visibilidad, mensajes)
* [x] Verificar 64 tests, 0 failures, 14 suites

## Limpieza UX/UI y Hardening

* [x] Español neutro: todos los textos de UI en español neutro (sin voseo argentino)
* [x] Bloquear orientación vertical en AndroidManifest.xml
* [x] Limpiar TV Settings: quitar diagnóstico de la UI, dejar solo estado, IP, conectar, probar, borrar
* [x] Agregar botón "Borrar configuración" en TV Settings
* [x] Mensajes estandarizados: "Enviado a la TV", "No se pudo abrir en la TV", "TV conectada"
* [x] Enmascarar client-key en logs (primeros 6 + últimos 4 caracteres)
* [x] Corregir openUrl: type=error y returnValue=false son fallo, no éxito
* [x] Verificar 68 tests, 0 failures, 14 suites

## Fase 8: Discovery automático de TVs

* [x] Crear TvDiscoveryManager con SSDP multicast + scan de subnet
* [x] Crear modelo DiscoveredTv
* [x] Agregar botón "Buscar TVs en la red" en TV Settings
* [x] Mostrar lista de TVs encontradas
* [x] Seleccionar TV → guardar IP → conectar
* [x] Mantener ingreso manual como fallback
* [x] Logs solo en Logcat

## Fase 9: Home moderna

* [x] Agregar endpoints TMDB: trending, now_playing, top_rated, tv/popular
* [x] Crear DTOs: TvShowDto, TvShowPageDto, TvShowDetailDto
* [x] Crear modelos: TvShow, TvShowDetail
* [x] Crear use cases: GetTrendingMovies, GetNowPlayingMovies, GetTopRatedMovies, GetPopularTvShows, SearchTvShows, GetTvShowDetail
* [x] Actualizar MovieRepository + TmdbMovieRepository con nuevos métodos
* [x] Rediseñar HomeViewModel: categorías paralelas + debounce en búsqueda
* [x] Rediseñar HomeScreen: carruseles horizontales, cards modernas, búsqueda unificada películas/series
* [x] Agregar botones rápidos: Favoritos e Historial en Home

## Fase 10: Series

* [x] Crear TvShowDetailViewModel + UiState + Factory
* [x] Crear TvShowDetailScreen con poster, sinopsis, rating, géneros, temporadas, botón Ver en TV
* [x] Agregar TvShowDetailRoutes a navegación
* [x] Conectar Home → Series detail
* [x] Conectar "Ver en TV" en detalle de serie con URL UnlimPlay

## Validación Fase 8-10

* [x] Discovery de TVs funciona (SSDP + subnet scan)
* [x] Home moderna muestra carruseles
* [x] Búsqueda unificada películas + series
* [x] Detalle de serie funciona
* [x] Ver en TV funciona desde detalle de serie
* [x] Verificar 66 tests, 0 failures, 14 suites

## v1.2: Series completas + Navigation + AppBar

* [x] Crear endpoint TMDB getTvSeason (tv/{id}/season/{season_number})
* [x] Crear DTOs: SeasonDto, EpisodeDto con toDomain()
* [x] Crear modelos: Season, Episode
* [x] Crear use case: GetTvSeasonUseCase
* [x] Actualizar TvShowDetailViewModel: selectSeason(), selectEpisode()
* [x] Actualizar TvShowDetailScreen: chips de temporadas, cards de episodios
* [x] PlaybackUrlBuilder desacoplado para movies y series
* [x] Actualizar MovieDetailViewModel y TvShowDetailViewModel para usar PlaybackUrlBuilder
* [x] AppBar compacta (48.dp) en MovieDetailScreen y TvShowDetailScreen
* [x] Home moderna separada: tab Películas / Series
* [x] Navigation: bottom bar visible desde detail screens
* [x] Fix test fakes: agregar getTvSeason a todos los FakeRepository
* [x] Verificar 66 tests, 0 failures, 14 suites

## v1.3: Discovery profesional + estabilidad

* [x] Auto-reconnect: reconexión automática al abrir app si hay TV configurada
* [x] Health check: indicador visual siempre visible (● Conectada / ● Desconectada)
* [x] Hardening: mensajes de error para timeout, IP inválida, TV apagada, pairing rechazado

## v1.4: UX/UI Premium

* [x] AnimatedContent en HomeScreen con fadeIn/fadeOut para transiciones
* [x] Skeleton loaders: ShimmerBox, MovieCarouselCardSkeleton, CarouselSectionSkeleton
* [x] Empty states modernos en Favoritos e Historial
* [x] Detalle screens: spacing consistente verificado

## Validación v1.3-v1.4

* [x] Auto-reconnect funciona al abrir app
* [x] Health check muestra estado correctamente
* [x] Mensajes de error son claros y amigables
* [x] Transiciones animadas en Home
* [x] Skeleton loaders funcionan
* [x] Empty states modernos
* [x] Verificar 66 tests, 0 failures, 14 suites

## v1.5: UX/A11y Fix Pack

### P0 — Bugs UX
* [x] Genre chips: cambiados a Surface+Text no clickeables (MovieDetail + TvShowDetail)
* [x] Episodio seleccionado: feedback "Temporada X · Episodio Y", botón "Ver episodio en TV"
* [x] Mensajes TV movidos antes de botones (visibles sin scrollear)
* [x] Botón debug "Probar Google" eliminado de TV Settings
* [x] Series en favoritos: placeholder "Próximamente" agregado

### P1 — Accesibilidad
* [x] Contraste TextSecondary: #C9C4B9 → #D4D0C8 (~5.0:1)
* [x] contentDescription: search bar, spinner, botón Ver en TV, status icon TV
* [x] Icono Historial: Star → History

### P2 — Quick wins
* [x] Skeletons integrados en Home cuando carga sin contenido
* [x] Empty states con CTA: "Explorar películas" (Favoritos), "Ir al inicio" (Historial)

### Validación v1.5
* [x] 64 tests, 0 failures, 14 suites
* [x] Home carga con skeletons
* [x] MovieDetail sin chips muertos
* [x] TvShowDetail con feedback de episodio
* [x] Mensajes TV visibles
* [x] TV Settings sin botón debug

## v1.6: Rebranding + Acerca de + Géneros + GitHub

### P0 — Rebranding
* [x] KastLG → Kast en UI (header HomeScreen)
* [x] README actualizado con branding Kast

### P0 — Pantalla Acerca de
* [x] AboutScreen creado con créditos, links, disclaimer
* [x] Agregado a AppDestination y KastLgApp
* [x] Botón "Obtener API Key de TMDB" abre navegador
* [x] Créditos UnlimPlay visibles

### P0 — Config TMDB
* [x] Botón "Obtener API Key de TMDB" en Settings
* [x] Texto de ayuda actualizado

### P0 — Home limpio
* [x] Chips de Favoritos/Historial eliminados (duplicaban bottom nav)

### P0 — Filtros por género
* [x] Chips de género funcionales (Movies + Series)
* [x] Filtrado client-side por genreIds
* [x] Opción "Todos" para limpiar filtro
* [x] GetTvGenresUseCase + endpoint agregados

### P0 — Detalle compacto
* [x] Poster cambiado de 2:3 a 16:9 (más compacto)

### P1 — Paleta neutra
* [x] Accent: gold → light gray (#B0B0B0)
* [x] AccentMuted ajustado

### P1 — Documentación GitHub
* [x] .gitignore actualizado (*.apk, google-services.json)
* [x] README con branding Kast

### P2 — Limpieza logs
* [x] SsapClient: logs verbosos reducidos

### Validación v1.6
* [x] 64 tests, 0 failures, 14 suites
* [x] App muestra nombre Kast
* [x] Home sin chips duplicados
* [x] Home con filtros de género
* [x] Detalle compacto
* [x] Pantalla Acerca de funcional
* [x] Botón API Key abre TMDB

## Release

* [x] README
* [x] CHANGELOG
* [ ] Capturas
* [x] APK debug
