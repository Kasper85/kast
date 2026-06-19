package com.kastlg.app.data.remote

interface TokenStore {
    suspend fun getToken(): String
    suspend fun saveToken(token: String)
    suspend fun clearToken()
    fun maskToken(token: String): String
}
