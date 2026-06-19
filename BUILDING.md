# Building KastLG

## Requirements

* Windows PowerShell 5.1 or newer.
* Android SDK Platform 35 and Build Tools 35.0.0.
* A local JDK 17 or newer.

The Windows Gradle wrapper automatically selects a compatible JDK. It checks, in order:

1. `KASTLG_JAVA_HOME`.
2. `JAVA_HOME`.
3. Common user-local, Gradle-managed, Android Studio, Eclipse Adoptium, and system JDK locations.
4. JDK installations exposed by `javac.exe` or `java.exe` on `PATH`.

Candidates must contain both `bin\java.exe` and `bin\javac.exe` and report Java 17 or newer. This means the build remains usable when `JAVA_HOME` is empty and `java` on `PATH` is Java 8.

If automatic discovery is not appropriate for a machine, set `KASTLG_JAVA_HOME` to that machine's JDK root. No JDK binaries or machine-specific absolute paths are stored in the repository.

## Debug build

From a clean PowerShell session:

```powershell
$env:JAVA_HOME = ""
.\gradlew.bat :app:assembleDebug --console=plain
```

The Android SDK location remains machine-local in the ignored `local.properties` file.

## TMDB credentials

Phase 2 uses a TMDB API Read Access Token (v4 bearer token). Provide it through either:

```properties
# local.properties (preferred for local Android development)
TMDB_ACCESS_TOKEN=your_api_read_access_token
```

or the `TMDB_ACCESS_TOKEN` environment variable before invoking Gradle.

`local.properties` is ignored and must never be committed. The build intentionally succeeds
without a token. In that case the app opens normally and explains how to configure the
credential instead of issuing unauthenticated requests or crashing.

Like every credential embedded in a client application, a token included in an APK can be
extracted by a determined user. Use a restricted, non-user TMDB read token and never reuse a
credential that grants access to unrelated systems.

## Phase 2 verification

From a clean PowerShell session:

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Unit tests use fakes and do not require a TMDB credential. A real-device or emulator
end-to-end TMDB check remains pending when no token is configured locally.

## Phase 4 persistence verification

Room exports the versioned schema to `app/schemas`. The initial database version is 1.
Future database changes must add explicit migrations to `DatabaseMigrations`; destructive
migration fallback is intentionally disabled.

The JVM suite uses Robolectric and an in-memory/on-disk Room database, so persistence,
toggle, ordering, and history deduplication can be verified without a TMDB token:

```powershell
.\gradlew.bat clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain
```

Instrumented tests require a connected Android device or emulator.
