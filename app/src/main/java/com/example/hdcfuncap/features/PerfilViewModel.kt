package com.example.hdcfuncap.features

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.PacienteProfileResponse
import com.example.hdcfuncap.network.PacienteProfileUpdateRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PerfilViewModel(private val authApi: AuthApi) : ViewModel() {
    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 5 * 60_000L
    }

    private val _profile = MutableStateFlow<PacienteProfileResponse?>(null)
    val profile: StateFlow<PacienteProfileResponse?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var lastLoadedAtMillis = 0L

    fun carregarPerfil(forceRefresh: Boolean = false) {
        if (_isLoading.value) return
        if (!forceRefresh && isCacheFresh()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                _profile.value = authApi.getPacienteProfile()
                lastLoadedAtMillis = SystemClock.elapsedRealtime()
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível carregar seu perfil."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun atualizarPerfil(
        request: PacienteProfileUpdateRequest,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null

            try {
                _profile.value = authApi.atualizarPacientePerfil(request)
                lastLoadedAtMillis = SystemClock.elapsedRealtime()
                onSuccess()
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível atualizar seu perfil."
            } finally {
                _isSaving.value = false
            }
        }
    }

    private fun isCacheFresh(): Boolean {
        return _profile.value != null &&
            lastLoadedAtMillis > 0L &&
            SystemClock.elapsedRealtime() - lastLoadedAtMillis < AUTO_REFRESH_INTERVAL_MS
    }
}

class PerfilViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PerfilViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PerfilViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
