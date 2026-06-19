# Arquitectura

## Stack

### UI

* Kotlin
* Jetpack Compose

### Persistencia

* Room

### Networking

* Retrofit
* OkHttp

### Concurrencia

* Coroutines
* Flow

## Arquitectura interna

presentation/
data/
domain/

Patrón:

MVVM + Repository Pattern

## Estructura

app/

presentation/
├── home
├── detail
├── favorites
├── history
└── settings

domain/
├── models
├── repositories
└── usecases

data/
├── remote
├── local
└── repository

## Integración TMDB

Funciones:

* Buscar películas.
* Obtener géneros.
* Obtener detalles.
* Descubrir por género.

## Integración LG

Responsabilidades:

* Guardar IP.
* Conectar por red local.
* Pairing.
* Lanzar navegador.
* Enviar URL.

## Base de datos

favorites

* tmdbId
* title
* posterUrl
* overview
* releaseDate
* voteAverage
* favoritedAt

history

* tmdbId
* title
* posterUrl
* overview
* releaseDate
* voteAverage
* viewedAt

En Fase 4, `history` representa detalles de películas vistos y se registra únicamente
después de cargar correctamente la pantalla de detalle. La reproducción y su URL no se
registran hasta que exista el flujo real de webOS en una fase posterior.

La base comienza en versión 1, exporta su schema y exige migraciones explícitas para
cualquier cambio futuro. No se permite fallback destructivo.
