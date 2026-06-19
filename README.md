<p align="center">
  <img src="docs/logo.png" alt="Kast Logo" width="128" height="128" />
</p>

<h1 align="center">Kast</h1>

<p align="center">
  App Android para descubrir películas y series, y enviarlas a una TV compatible.
</p>

---

## Acerca de

Kast permite buscar películas y series usando TMDB, organizar favoritos e historial, y enviar contenido directamente a una TV LG webOS desde el celular. La app no aloja contenido multimedia ni distribuye archivos de video.

## Capturas

<p align="center">
  <img src="docs/screenshots/home.png" alt="Home" width="250" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/movie-detail.png" alt="Detalle" width="250" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/tv-settings.png" alt="Configuración TV" width="250" />
</p>

## Features

- **Películas y series** — Tendencias, estrenos, mejor valoradas, series populares
- **Búsqueda unificada** — Busca películas y series en una sola barra
- **Detalle completo** — Póster, sinopsis, calificación, año, géneros
- **Series** — Temporadas, episodios con selección
- **Favoritos** — Guarda películas favoritas localmente
- **Historial** — Registra lo que viste y lo que enviaste a la TV
- **Discovery de TVs** — Encuentra TVs LG automáticamente en la red
- **Envío a TV** — Envía películas a una TV LG webOS mediante WSS + SSAP
- **Skeleton loaders** — Carga visual suave
- **Tema oscuro** — Diseño minimalista en negro/gris/blanco

## Tech Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | Retrofit + OkHttp |
| Imágenes | Coil |
| Persistencia | Room |
| TMDB | API v4 (Read Access Token) |
| TV | WebSocket + protocolo SSAP |
| Discovery | UDP multicast (SSDP) |

## Arquitectura

```
app/src/main/java/com/kastlg/app/
├── data/
│   ├── local/        # Room: entities, DAOs, database
│   ├── remote/       # TMDB API: Retrofit, DTOs, token store
│   ├── repository/   # Repository implementations
│   └── tv/           # SSAP client + discovery (UDP multicast)
├── di/               # AppContainer (dependency injection)
├── domain/
│   ├── models/       # Domain models
│   ├── repositories/ # Repository interfaces
│   └── usecases/     # Use cases
└── presentation/
    ├── about/        # Acerca de
    ├── detail/       # Detalle de película
    ├── tvdetail/     # Detalle de serie
    ├── favorites/    # Favoritos
    ├── history/      # Historial
    ├── home/         # Home (carruseles + búsqueda)
    ├── library/      # Componentes compartidos
    ├── navigation/   # Rutas y destinos
    ├── settings/     # Configuración TMDB + TV
    ├── theme/        # Colores, tipografía, tema
    └── tvsettings/   # Configuración TV + discovery
```

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

## Download

[Descargar última versión](../../releases/latest)

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
6. Desde cualquier película, presioná **Ver en TV**

## Desarrollo

### Ejecutar tests

```bash
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

### Estructura de tests

- Unit tests con fakes y Robolectric
- No requieren token de TMDB ni dispositivo conectado
- Room tests con base de datos en memoria

## Créditos

- **Películas y series**: [TMDB](https://www.themoviedb.org/)
- **Reproducción**: [UnlimPlay](https://unlimplay.com/)
- **TV compatible**: LG webOS (WSS + SSAP)

Kast utiliza TMDB para mostrar información de películas y series. La reproducción se realiza mediante UnlimPlay. Kast no aloja contenido multimedia ni distribuye archivos de video.

## Limitaciones

- Solo funciona en orientación vertical
- Búsqueda limitada a la primera página de resultados de TMDB
- El historial registra contenido visto, no reproducción real
- Requiere TV LG webOS en la misma red WiFi
- El pairing requiere aceptar un prompt en la TV la primera vez

## Licencia

MIT License — ver [LICENSE](LICENSE)
