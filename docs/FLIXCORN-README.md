# Kast — App Android para TVs LG webOS

<p align="center">
  <img src="docs/logo.png" width="160" alt="Kast Logo">
</p>

<p align="center">
  Aplicación Android para descubrir películas y series, y enviarlas directamente a tu TV LG webOS.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-MIT-lightgrey" alt="License">
</p>

<p align="center">
  <a href="../../releases/latest">
    <img src="https://img.shields.io/badge/Descargar-APK-success?style=for-the-badge&logo=android" alt="Descargar APK">
  </a>
</p>

---

Kast es una aplicación Android de código abierto que permite descubrir películas y series mediante [TMDB](https://www.themoviedb.org/) y [Flixcorn](https://www.flixcorn.net/), y enviarlas directamente a televisores LG webOS usando el protocolo nativo SSAP. No requiere cuenta, no requiere backend, y no aloja contenido multimedia.

---

## ¿Qué es Kast?

Kast es una app Android pensada para usuarios que tienen una TV LG webOS en casa y quieren buscar películas o series desde su celular y enviarlas a la TV de forma rápida. Usa TMDB para obtener la información de películas y series, y Flixcorn como fuente de streaming para series con múltiples servidores y calidades. Se comunica con la TV mediante el protocolo SSAP sobre WebSocket (WSS).

No necesitás cuenta, login ni servidor multimedia. Todo funciona de forma local: la app busca en TMDB y Flixcorn, guarda favoritos e historial en tu celular, y envía la URL de reproducción a tu TV LG webOS.

Kast no aloja, transmite ni distribuye ningún contenido multimedia. Los metadatos provienen de TMDB, el streaming de Flixcorn, y la reproducción se realiza a través de la TV.

## ¿Por qué existe Kast?

Las soluciones existentes para controlar TVs LG webOS suelen depender de DLNA, Chromecast, servidores multimedia (como Plex o Kodi) o aplicaciones propietarias como LG ThinQ. Muchas de estas alternativas requieren configuración compleja, cuentas, o no están diseñadas específicamente para el flujo buscar → ver → enviar a TV.

Kast está pensado para usuarios Android que quieren un camino directo: buscan una película o serie, la revisan, y la envían a su TV LG webOS con un toque. No requiere cuenta, no requiere backend, no requiere servidor multimedia. Usa TMDB para los metadatos, Flixcorn para el streaming de series, y SSAP para comunicarse con la TV, manteniendo la experiencia simple y local.

## Características

### Contenido
- **Películas y series** — Tendencias, estrenos, mejor valoradas, series populares desde TMDB
- **Streaming de series** — Múltiples servidores de streaming por episodio desde Flixcorn
- **Calidades** — 1080p HD, 720p HD disponibles por servidor
- **Idiomas** — Español Latino, Subtitulado por servidor
- **Búsqueda unificada** — Busca películas y series en una sola barra, resultados de TMDB y Flixcorn combinados

### Experiencia
- **Detalle completo** — Póster, sinopsis, calificación, año, géneros
- **Series TMDB** — Temporadas y episodios con selección (via UnlimPlay)
- **Series Flixcorn** — Temporadas, episodios y selección de servidor de streaming
- **Favoritos** — Guarda películas favoritas localmente (Room)
- **Historial** — Registra lo que viste y lo que enviaste a la TV

### Conexión con TV
- **Discovery de TVs** — Encuentra TVs LG automáticamente en la red (SSDP)
- **Envío a TV** — Envía películas y series a una TV LG webOS mediante WSS + SSAP
- **Selección de servidor** — Elegí el servidor de streaming antes de enviar a la TV

### UI/UX
- **Skeleton loaders** — Carga visual suave
- **Tema oscuro** — Diseño minimalista en negro, gris y blanco
- **Configuración en-app** — Token TMDB configurable sin recompilar
- **Indicadores de fuente** — Sabés si el contenido viene de TMDB o Flixcorn

---

## Capturas

<p align="center">
  <img src="docs/screenshots/home.png" width="220" alt="Inicio">
  <img src="docs/screenshots/movie-detail.png" width="220" alt="Detalle de película">
  <img src="docs/screenshots/tv-settings.png" width="220" alt="Configuración TV">
</p>

---

## Tech Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | Retrofit + OkHttp |
| HTML Scraping | Jsoup |
| Imágenes | Coil |
| Persistencia | Room + DataStore |
| TMDB | API v4 (Read Access Token) |
| Streaming | Flixcorn (HTML scraping) |
| TV | WebSocket + protocolo SSAP |
| Discovery | UDP multicast (SSDP) |

---

## Arquitectura

```
app/src/main/java/com/kastlg/app/
├── data/
│   ├── local/           # Room database, DAOs, entities
│   ├── remote/
│   │   ├── tmdb/        # Retrofit API, DTOs
│   │   └── flixcorn/    # HTML scraper, parsers
│   ├── repository/      # Repository implementations
│   └── tv/              # SSAP client, TV discovery
├── di/                  # AppContainer (dependency injection)
├── domain/
│   ├── models/          # Domain models
│   ├── repositories/    # Repository interfaces
│   └── usecases/        # Business logic
└── presentation/
    ├── components/      # Reusable UI components
    ├── detail/          # Movie detail screen
    ├── flixcorn/        # Flixcorn search, series, episodes
    ├── home/            # Home screen
    ├── navigation/      # Navigation routes
    ├── tvdetail/        # TV show detail screen
    └── theme/           # Material 3 theme
```

### Data Flow — Flixcorn

```
Search Query
    │
    ├──→ TMDB API ──→ Results (TMDB)
    │
    └──→ Flixcorn Scraper ──→ Results (Flixcorn)
                                    │
                                    ▼
                              Series Detail (scraped)
                                    │
                                    ▼
                              Episode Servers (parsed)
                                    │
                                    ▼
                              Player URL Resolution
                                    │
                                    ▼
                              SSAP Send to TV
```

---

## Instalación

### Requisitos

- Android SDK Platform 35
- JDK 17+ (el wrapper lo detecta automáticamente)

### Build

```bash
.\gradlew.bat :app:assembleDebug --console=plain
```

### Instalar en dispositivo

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Configurar TMDB

1. Creá una cuenta en [themoviedb.org](https://www.themoviedb.org/)
2. Generá un **API Read Access Token** (v4) en Settings > API
3. Abrí la app → pestaña **Ajustes** → pegá el token

La app compila sin token y muestra un botón para configurarlo.

## Configurar TV LG

1. La TV y el celular deben estar en la **misma red WiFi**
2. Ir a la pestaña **TV** en la app
3. Presioná **Buscar TVs** o ingresá la IP manualmente
4. Seleccioná la TV y presioná **Conectar**
5. Aceptá el permiso en la pantalla de la TV
6. Desde cualquier película o serie, presioná **Ver en TV**

La configuración (IP y client key) se guardan localmente.

---

## Uso

### Buscar Series

1. Tocá el ícono de búsqueda en la barra de navegación
2. Escribí el nombre de una serie
3. Los resultados muestran series de TMDB y Flixcorn
4. Los resultados de Flixcorn tienen un badge indicando la fuente

### Ver Detalle de Serie Flixcorn

1. Tocá una serie con badge de Flixcorn
2. Navegá entre temporadas usando los chips
3. Seleccioná un episodio
4. Se muestran los servidores disponibles organizados por calidad e idioma

### Enviar a TV

1. Conectá la TV (ver Configurar TV LG)
2. En la pantalla de detalle, tocá "Ver en TV"
3. Elegí el servidor de streaming
4. La app envía la URL a la TV vía SSAP
5. La serie comienza a reproducirse en la TV

---

## Kast vs alternativas

| | Kast | Web Video Caster | LG ThinQ |
|---|------|-----------------|----------|
| Open source | ✅ MIT | ❌ | ❌ |
| Sin cuenta | ✅ | ✅ | ❌ |
| Enfocado en LG webOS | ✅ | ❌ Multiplataforma | ✅ |
| TMDB integrado | ✅ | ❌ | ❌ |
| Flixcorn streaming | ✅ | ❌ | ❌ |
| Favoritos e historial | ✅ | ❌ | ❌ |
| Temporadas y episodios | ✅ | ❌ | ❌ |
| Selección de servidor | ✅ | ❌ | ❌ |
| Usa SSAP nativo | ✅ | ❌ DLNA | ✅ |

---

## Preguntas frecuentes

**¿Kast funciona con cualquier TV?**
No. Kast solo funciona con TVs LG que ejecuten webOS y soporten el protocolo SSAP. La mayoría de las TVs LG de 2014 en adelante son compatibles.

**¿Necesito una cuenta?**
No. Kast funciona sin cuenta, login ni registro. Solo necesitás un token de TMDB (gratuito).

**¿Kast almacena películas o series?**
No. Kast es solo un navegador de metadatos (TMDB/Flixcorn) y un controlador remoto. No almacena, transmite ni distribuye contenido multimedia.

**¿Cómo funciona el streaming de Flixcorn?**
Kast busca series en Flixcorn, parsea las páginas HTML para extraer los servidores disponibles, y te permite elegir uno para enviar a tu TV. No se aloja ni modifica contenido.

**¿Necesito un token de TMDB?**
Sí. TMDB es la fuente principal de metadatos. Necesitás un API Read Access Token v4, que es gratuito y se configura desde la app.

**¿Kast funciona con Chromecast o Samsung TV?**
No. Kast usa el protocolo SSAP nativo de LG webOS. No compatible con Chromecast, Samsung, Sony, ni otros fabricantes.

---

## Desarrollo

### Ejecutar tests

```bash
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

- Unit tests con fakes y Robolectric
- No requieren token de TMDB ni dispositivo conectado
- Room tests con base de datos en memoria
- Tests de parsing HTML con fixtures

### Agregar tests de Flixcorn

Los tests de scraping usan HTML fixtures en `src/test/resources/flixcorn/`:

- `search_results.html` — resultados de búsqueda
- `series_detail.html` — detalle de serie
- `episode_page.html` — página de episodio con servidores

---

## Créditos

- **Metadatos de películas**: [TMDB](https://www.themoviedb.org/)
- **Streaming de series**: [Flixcorn](https://www.flixcorn.net/)
- **TV compatible**: LG webOS (WSS + SSAP)

> Kast utiliza TMDB para información de películas y series, y Flixcorn como fuente de streaming para series. Kast no aloja contenido multimedia ni distribuye archivos de video.

## Licencia

[MIT License](LICENSE)
