package com.kastlg.app.presentation.settings

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.data.remote.TokenStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state has no token`() = runTest {
        val store = FakeTokenStore(initialToken = "")
        val viewModel = SettingsViewModel(store)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.hasToken)
        assertEquals("", state.currentToken)
        assertEquals("", state.maskedToken)
    }

    @Test
    fun `initial state shows existing token`() = runTest {
        val store = FakeTokenStore(initialToken = "eyJhbGciOiJIUzI1NiJ9.test.token")
        val viewModel = SettingsViewModel(store)
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasToken)
        assertEquals("eyJhbG...oken", state.maskedToken)
    }

    @Test
    fun `saveToken stores token and updates state`() = runTest {
        val store = FakeTokenStore()
        val viewModel = SettingsViewModel(store)
        runCurrent()

        viewModel.onInputChanged("my-new-token-12345678")
        viewModel.saveToken()
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasToken)
        assertEquals("my-new-token-12345678", store.savedToken)
        assertEquals("my-new...5678", state.maskedToken)
        assertEquals("", state.inputToken)
        assertEquals("Token guardado correctamente.", state.successMessage)
    }

    @Test
    fun `saveToken with blank input shows error`() = runTest {
        val store = FakeTokenStore()
        val viewModel = SettingsViewModel(store)
        runCurrent()

        viewModel.onInputChanged("   ")
        viewModel.saveToken()
        runCurrent()

        assertEquals("Ingresa un token válido.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearToken removes token and updates state`() = runTest {
        val store = FakeTokenStore(initialToken = "existing-token")
        val viewModel = SettingsViewModel(store)
        runCurrent()

        assertTrue(viewModel.uiState.value.hasToken)

        viewModel.clearToken()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.hasToken)
        assertEquals("", state.currentToken)
        assertTrue(store.isCleared)
    }

    @Test
    fun `toggleTokenVisibility toggles visibility`() = runTest {
        val store = FakeTokenStore()
        val viewModel = SettingsViewModel(store)
        runCurrent()

        assertFalse(viewModel.uiState.value.isTokenVisible)
        viewModel.toggleTokenVisibility()
        assertTrue(viewModel.uiState.value.isTokenVisible)
        viewModel.toggleTokenVisibility()
        assertFalse(viewModel.uiState.value.isTokenVisible)
    }

    @Test
    fun `clearMessages removes messages`() = runTest {
        val store = FakeTokenStore()
        val viewModel = SettingsViewModel(store)
        runCurrent()

        viewModel.onInputChanged("test")
        viewModel.saveToken()
        runCurrent()

        assertTrue(viewModel.uiState.value.successMessage != null)

        viewModel.clearMessages()
        assertTrue(viewModel.uiState.value.successMessage == null)
        assertTrue(viewModel.uiState.value.errorMessage == null)
    }

    private class FakeTokenStore(
        initialToken: String = "",
    ) : TokenStore {
        var savedToken: String? = null
        var isCleared = false
        private var currentToken = initialToken

        override suspend fun getToken(): String = currentToken

        override suspend fun saveToken(token: String) {
            savedToken = token
            currentToken = token
        }

        override suspend fun clearToken() {
            isCleared = true
            currentToken = ""
        }

        override fun maskToken(token: String): String {
            if (token.length < 12) return "****"
            return "${token.take(6)}...${token.takeLast(4)}"
        }
    }
}
