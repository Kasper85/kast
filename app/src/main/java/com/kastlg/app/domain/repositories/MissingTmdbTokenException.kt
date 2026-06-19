package com.kastlg.app.domain.repositories

class MissingTmdbTokenException : IllegalStateException(
    "Falta el token de TMDB. Configúralo desde Ajustes > Token TMDB.",
)
