package com.example.project.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project.R
import com.example.project.core.util.UiText
import com.example.project.data.mock.MockData
import com.example.project.data.remote.api.AuthException
import com.example.project.domain.repository.AuthRepository
import com.example.project.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
)

sealed interface AuthEffect {
    data class Message(val text: UiText) : AuthEffect
}

/**
 * Auth screen ViewModel (UDF). Login / register call the Melodify backend; demo sign-in stays
 * local for offline demos. Session flag in DataStore switches the app root to [MainScreen].
 */
class AuthViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AuthEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun setMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    fun toggleMode() {
        _uiState.update {
            it.copy(
                mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
            )
        }
    }

    fun login(handle: String, password: String) {
        if (_uiState.value.isLoading) return
        val cleanHandle = handle.trim().removePrefix("@")
        when {
            cleanHandle.isBlank() -> emitError(UiText.from(R.string.auth_error_empty_handle))
            password.length < 6 -> emitError(UiText.from(R.string.auth_error_password_short))
            else -> submit {
                authRepository.login(cleanHandle, password)
            }
        }
    }

    fun register(displayName: String, handle: String, password: String) {
        if (_uiState.value.isLoading) return
        val cleanName = displayName.trim()
        val cleanHandle = handle.trim().removePrefix("@")
        when {
            cleanName.isBlank() -> emitError(UiText.from(R.string.auth_error_empty_name))
            cleanHandle.isBlank() -> emitError(UiText.from(R.string.auth_error_empty_handle))
            password.length < 6 -> emitError(UiText.from(R.string.auth_error_password_short))
            else -> submit {
                authRepository.register(cleanName, cleanHandle, password)
            }
        }
    }

    /** Offline demo path — does not hit the backend. */
    fun continueAsDemo() {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            settingsRepository.login(
                name = MockData.currentUser.displayName,
                handle = MockData.currentUser.handle,
                avatarUrl = MockData.currentUser.avatarUrl,
            )
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun submit(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = block()
            _uiState.update { it.copy(isLoading = false) }
            result.onFailure { error ->
                val text = (error as? AuthException)?.uiText
                    ?: UiText.from(R.string.auth_error_unknown)
                _effects.send(AuthEffect.Message(text))
            }
        }
    }

    private fun emitError(text: UiText) {
        viewModelScope.launch { _effects.send(AuthEffect.Message(text)) }
    }
}
