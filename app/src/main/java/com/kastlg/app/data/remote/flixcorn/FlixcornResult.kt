package com.kastlg.app.data.remote.flixcorn

sealed class FlixcornResult<out T> {
    data class Success<T>(val data: T) : FlixcornResult<T>()
    data class Error(val code: FlixcornError) : FlixcornResult<Nothing>()
    data object Loading : FlixcornResult<Nothing>()
}

enum class FlixcornError {
    NETWORK_TIMEOUT,
    PARSE_FAILURE,
    NO_SERVERS_FOUND,
    RATE_LIMITED,
    UNREACHABLE,
}
