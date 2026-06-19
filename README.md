# Kast

App Android nativa para descubrir películas y series, y reproducirlas en una TV LG webOS desde el celular.

## Qué hace

- Descubre películas y series: tendencias, estrenos, mejor valoradas, series populares.
- Busca películas y series con una barra de búsqueda unificada.
- Muestra detalles completos: póster, sinopsis, calificación, año, géneros.
- Para series: muestra temporadas y episodios con selección.
- Guarda favoritos e historial localmente (Room).
- Descubre automáticamente TVs LG en la red local (SSDP + scan de subnet).
- Envía películas o series a una TV LG webOS conectada a la misma red WiFi.
- La TV abre automáticamente el navegador con la película.
- Reconexión automática al abrir la app si ya hay una TV configurada.
- Skeleton loaders animados y transiciones suaves.

## Stack

- Kotlin + Jetpack Compose
- Material 3 (tema oscuro)
- Retrofit + OkHttp (TMDB API)
- Coil (imágenes)
- Room + DataStore (persistencia local)
- WebSocket nativo (protocolo SSAP para LG webOS)
- UDP multicast (SSDP para descubrimiento de TVs)

## Configurar TMDB

### Opción 1: Desde la app (recomendado)

1. Creá una cuenta en [themoviedb.org](https://www.themoviedb.org/).
2. Generá un **API Read Access Token** (v4) en Settings > API.
3. Abrí la app → pestaña **Ajustes**.
4. Pegá el token en el campo "Token TMDB".
5. Presioná **Guardar token**.

### Opción 2: Desde el código (para desarrollo)

1. Creá o editá `local.properties` en la raíz del proyecto:
   ```properties
   TMDB_ACCESS_TOKEN=tu_token_aquí
   ```
2. Re-compilá el APK.

La app compila sin token y muestra un botón "Configurar token TMDB" en Home.

## Instalar APK

### Requisitos

- Windows PowerShell 5.1+
- Android SDK Platform 35
- JDK 17+ (el wrapper lo detecta automáticamente)

### Build

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
```

El APK se genera en `app/build/outputs/apk/debug/app-debug.apk`.

### Instalar en dispositivo

```powershell
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Configurar TV LG webOS

1. La TV y el celular deben estar en la **misma red WiFi**.
2. En la app, ir a la pestaña **TV**.
3. Presioná **Buscar TVs en la red** o ingresá la IP manualmente.
4. Seleccioná la TV de la lista.
5. Presioná **Conectar**.
6. En la pantalla de la TV, aceptá el permiso de conexión.
7. Una vez conectado, desde cualquier película podés presionar **Ver en TV**.

La configuración (IP y client key) se guardan localmente y sobreviven al cierre de la app.

## Ejecutar tests

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Los tests usan fakes y Robolectric. No requieren token de TMDB ni dispositivo conectado.

## Estructura del proyecto

```
app/src/main/java/com/kastlg/app/
├── data/
│   ├── local/        # Room: entities, DAOs, database, migrations
│   ├── remote/       # TMDB API: Retrofit, DTOs, token store
│   ├── repository/   # Repository implementations
│   └── tv/           # SSAP client + discovery (UDP multicast)
├── di/               # AppContainer (dependency injection)
├── domain/
│   ├── models/       # Domain models (Movie, TvShow, etc.)
│   ├── repositories/ # Repository interfaces
│   └── usecases/     # Use cases
└── presentation/
    ├── detail/       # Movie detail screen
    ├── tvdetail/     # TV series detail screen
    ├── favorites/    # Favorites screen
    ├── history/      # History screen
    ├── home/         # Home screen (carruseles + búsqueda)
    ├── library/      # Shared saved-movies grid
    ├── navigation/   # Routes and destinations
    ├── settings/     # TMDB token configuration
    ├── theme/        # Colors, typography, theme
    └── tvsettings/   # TV configuration + discovery
```

## Limitaciones conocidas

- La app funciona solo en orientación vertical (portrait).
- Textos de UI en español neutro.
- La búsqueda muestra solo la primera página de resultados de TMDB.
- No hay paginación infinita.
- El historial registra "detalle visto" y "enviado a TV", no reproducción real.
- El protocolo SSAP no está documentado oficialmente por LG.
- La TV y el celular deben estar en la misma red WiFi.
- El pairing requiere aceptar un prompt en la TV la primera vez.

## Licencia

MIT License — ver [LICENSE](LICENSE).
