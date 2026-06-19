<p align="center">
  <img src="docs/logo.png" width="160" alt="Kast Logo">
</p>

<h1 align="center">Kast</h1>

<p align="center">
  Discover movies and TV shows and send them directly to your LG webOS TV.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0+-3DDC84?logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/License-MIT-lightgrey" alt="License">
</p>

<p align="center">
  <a href="../../releases/latest">
    <img src="https://img.shields.io/badge/Download-APK-success?style=for-the-badge&logo=android" alt="Download APK">
  </a>
</p>

---

## About

Kast is an Android app that lets you browse movies and TV shows using TMDB, save favorites and watch history locally, and send content directly to an LG webOS TV on your home network. Kast does not host or distribute any media files.

## Screenshots

<p align="center">
  <img src="docs/screenshots/home.png" width="220" alt="Home">
  <img src="docs/screenshots/movie-detail.png" width="220" alt="Movie Detail">
  <img src="docs/screenshots/tv-settings.png" width="220" alt="TV Settings">
</p>

## Features

- **Movies & TV Shows** — Trending, now playing, top rated, popular series
- **Unified Search** — Search movies and shows in a single bar
- **Full Details** — Poster, synopsis, rating, year, genres
- **Series** — Seasons and episodes with selection
- **Favorites** — Save favorite movies locally
- **History** — Track what you watched and sent to TV
- **TV Discovery** — Automatically find LG TVs on your local network
- **Send to TV** — Send movies to an LG webOS TV via WSS + SSAP
- **Skeleton Loaders** — Smooth loading animations
- **Dark Theme** — Minimalist design in black, grey, and white

## Download

Download the latest APK from GitHub Releases:

[**Download APK**](../../releases/latest)

> The APK will be available from GitHub Releases once the first public release is published.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Networking | Retrofit + OkHttp |
| Images | Coil |
| Persistence | Room + DataStore |
| TMDB | API v4 (Read Access Token) |
| TV | WebSocket + SSAP protocol |
| Discovery | UDP multicast (SSDP) |

## Setup

### Prerequisites

- Android SDK Platform 35
- JDK 17+ (the wrapper detects it automatically)

### Build

```bash
.\gradlew.bat :app:assembleDebug --console=plain
```

### Install on device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Configure TMDB

1. Create an account at [themoviedb.org](https://www.themoviedb.org/)
2. Generate an **API Read Access Token** (v4) at Settings > API
3. Open the app → **Settings** tab → paste the token

The app compiles without a token and shows a button to configure it.

## Configure LG TV

1. TV and phone must be on the **same WiFi network**
2. Go to the **TV** tab in the app
3. Press **Search for TVs** or enter the IP manually
4. Select the TV and press **Connect**
5. Accept the permission prompt on the TV screen
6. From any movie, press **Watch on TV**

## Development

### Run tests

```bash
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

- Unit tests with fakes and Robolectric
- No TMDB token or device required
- Room tests with in-memory database

### Architecture

```
app/src/main/java/com/kastlg/app/
├── data/           # Room, Retrofit, DTOs, SSAP client, discovery
├── di/             # AppContainer (dependency injection)
├── domain/         # Models, repositories, use cases
└── presentation/   # Compose screens, ViewModels, theme
```

## Credits

- **Movies & TV Shows**: [TMDB](https://www.themoviedb.org/)
- **Playback**: [UnlimPlay](https://unlimplay.com/)
- **Compatible TV**: LG webOS (WSS + SSAP)

> Kast uses TMDB to display movie and show information. Playback is provided through UnlimPlay. Kast does not host, stream, or distribute any media content.

## License

[MIT License](LICENSE)
