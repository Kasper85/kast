package com.kastlg.app.presentation.tvsettings

import com.kastlg.app.MainDispatcherRule
import com.kastlg.app.data.tv.SsapClient
import com.kastlg.app.domain.models.TvConfig
import com.kastlg.app.domain.repositories.TvRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TvSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is disconnected with empty IP`() = runTest {
        val repository = FakeTvRepository()
        val viewModel = TvSettingsViewModel(repository)
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertEquals("", state.tvIp)
        assertNull(state.errorMessage)
    }

    @Test
    fun `connect with blank IP shows error`() = runTest {
        val repository = FakeTvRepository()
        val viewModel = TvSettingsViewModel(repository)
        runCurrent()

        viewModel.onIpChanged("  ")
        viewModel.connect()
        runCurrent()

        assertEquals("Ingresa la IP de la TV.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `successful connect updates state to connected`() = runTest {
        val repository = FakeTvRepository()
        val viewModel = TvSettingsViewModel(repository)
        runCurrent()

        viewModel.onIpChanged("192.168.1.100")
        viewModel.connect()
        runCurrent()

        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals("TV conectada", viewModel.uiState.value.successMessage)
        assertEquals("192.168.1.100", repository.lastConnectedIp)
    }

    @Test
    fun `failed connect shows error message`() = runTest {
        val repository = FakeTvRepository(shouldFailConnect = true)
        val viewModel = TvSettingsViewModel(repository)
        runCurrent()

        viewModel.onIpChanged("192.168.1.100")
        viewModel.connect()
        runCurrent()

        assertFalse(viewModel.uiState.value.isConnected)
        assertEquals("Connection refused", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `clearMessages removes messages`() = runTest {
        val repository = FakeTvRepository()
        val viewModel = TvSettingsViewModel(repository)
        runCurrent()

        viewModel.onIpChanged("192.168.1.100")
        viewModel.connect()
        runCurrent()

        viewModel.clearMessages()
        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
    }

    private class FakeTvRepository(
        private val shouldFailConnect: Boolean = false,
    ) : TvRepository {
        private val configFlow = MutableStateFlow<TvConfig?>(null)
        var lastConnectedIp: String? = null
        var lastOpenedUrl: String? = null

        override fun observeConfig(): Flow<TvConfig?> = configFlow

        override fun observeDiagnosticLog(): Flow<List<SsapClient.DiagnosticEntry>> =
            kotlinx.coroutines.flow.flowOf(emptyList())

        override suspend fun getConfig(): TvConfig? = configFlow.value

        override suspend fun saveConfig(config: TvConfig) {
            configFlow.value = config
        }

        override suspend fun deleteConfig() {
            configFlow.value = null
        }

        override suspend fun connectAndRegister(ip: String): Result<String> {
            if (shouldFailConnect) return Result.failure(Exception("Connection refused"))
            lastConnectedIp = ip
            val config = TvConfig(
                tvIp = ip,
                tvName = "Test TV",
                clientKey = "test-key",
                isPaired = true,
            )
            configFlow.value = config
            return Result.success("test-key")
        }

        override suspend fun openUrl(url: String): Result<Unit> {
            if (configFlow.value == null) {
                return Result.failure(Exception("No hay TV configurada. Conectá primero."))
            }
            lastOpenedUrl = url
            return Result.success(Unit)
        }

        override suspend fun disconnect() {
            // no-op
        }
    }
}
