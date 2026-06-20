<p align="center">
  <img src="docs/logo.png" width="160" alt="Kast Logo">
</p>

<h1 align="center">Kast</h1>

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

## ¿Qué es Kast?

Kast es una aplicación Android de código abierto que te permite buscar películas y series usando [TMDB](https://www.themoviedb.org/), guardar favoritos e historial localmente, y enviar contenido directamente a una TV LG webOS desde tu celular mediante el protocolo SSAP sobre WebSocket.

Kast no aloja, transmite ni distribuye ningún contenido multimedia. Los metadatos provienen de TMDB y la reproducción se realiza a través de [UnlimPlay](https://unlimplay.com/).

### ¿Para quién está pensado?

- Personas que tienen una TV LG webOS en casa
- Usuarios de Android que quieren controlar su TV desde el celular
- Quienes buscan una alternativa a la app ThinQ de LG o a Web Video Caster
- Desarrolladores interesados en el protocolo SSAP de LG webOS

### ¿Cómo funciona?

1. **Busca** películas y series usando TMDB
2. **Guarda** favoritos e historial en tu celular (Room)
3. **Conecta** tu TV LG webOS desde la app
4. **Envía** la película o serie a tu TV con un toque
5. **Tu TV abre** automáticamente el navegador con la reproducción

No necesitas cuenta, login ni backend. Todo funciona localmente en tu dispositivo.

---

## Capturas

<p align="center">
  <img src="docs/screenshots/home.png" width="220" alt="Inicio">
  <img src="docs/screenshots/movie-detail.png" width="220" alt="Detalle de película">
  <img src="docs/screenshots/tv-settings.png" width="220" alt="Configuración TV">
</p>

## Características

- **Películas y series** — Tendencias, estrenos, mejor valoradas, series populares
- **Búsqueda unificada** — Busca películas y series en una sola barra
- **Detalle completo** — Póster, sinopsis, calificación, año, géneros
- **Series** — Temporadas y episodios con selección
- **Favoritos** — Guarda películas favoritas localmente (Room)
- **Historial** — Registra lo que viste y lo que enviaste a la TV
- **Discovery de TVs** — Encuentra TVs LG automáticamente en la red (SSDP)
- **Envío a TV** — Envía películas a una TV LG webOS mediante WSS + SSAP
- **Skeleton loaders** — Carga visual suave
- **Tema oscuro** — Diseño minimalista en negro, gris y blanco
- **Configuración en-app** — Token TMDB configurable sin recompilar

## Descarga

Descarga la última APK desde GitHub Releases:

[**Descargar APK**](../../releases/latest)

## Tech Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | Retrofit + OkHttp |
| Imágenes | Coil |
| Persistencia | Room + DataStore |
| TMDB | API v4 (Read Access Token) |
| TV | WebSocket + protocolo SSAP |
| Discovery | UDP multicast (SSDP) |

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

La configuración (IP y client key) se guardan localmente.

## Desarrollo

### Ejecutar tests

```bash
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

- Unit tests con fakes y Robolectric
- No requieren token de TMDB ni dispositivo conectado
- Room tests con base de datos en memoria

### Arquitectura

```
app/src/main/java/com/kastlg/app/
├── data/           # Room, Retrofit, DTOs, SSAP client, discovery
├── di/             # AppContainer (dependency injection)
├── domain/         # Models, repositories, use cases
└── presentation/   # Compose screens, ViewModels, theme
```

## Comparativas

Ver [COMPARISON.md](COMPARISON.md) para una comparación detallada con alternativas como Web Video Caster, LG ThinQ, Kodi y Plex.

## Preguntas frecuentes

Ver [FAQ.md](FAQ.md) para respuestas a las preguntas más comunes.

## Créditos

- **Películas y series**: [TMDB](https://www.themoviedb.org/)
- **Reproducción**: [UnlimPlay](https://unlimplay.com/)
- **TV compatible**: LG webOS (WSS + SSAP)

> Kast utiliza TMDB para mostrar información de películas y series. La reproducción se realiza mediante UnlimPlay. Kast no aloja contenido multimedia ni distribuye archivos de video.

## Licencia

[MIT License](LICENSE)
