package com.kastlg.app.data.remote

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tmdbDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tmdb_settings",
)

class TmdbTokenStore(private val context: Context) : TokenStore {
    private val tokenKey = stringPreferencesKey("tmdb_access_token")

    val tokenFlow: Flow<String> = context.tmdbDataStore.data.map { prefs ->
        prefs[tokenKey].orEmpty()
    }

    override suspend fun getToken(): String = tokenFlow.first()

    override suspend fun saveToken(token: String) {
        context.tmdbDataStore.edit { prefs ->
            prefs[tokenKey] = token.trim()
        }
    }

    override suspend fun clearToken() {
        context.tmdbDataStore.edit { prefs ->
            prefs.remove(tokenKey)
        }
    }

    override fun maskToken(token: String): String {
        if (token.length < 12) return "****"
        return "${token.take(6)}...${token.takeLast(4)}"
    }
}
