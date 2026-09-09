package com.example.hdcfuncap.features

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.apenasDasPrescricoes
import com.example.hdcfuncap.network.apenasPrescricoesMedicamentosAtivas
import com.example.hdcfuncap.network.toMedicamentosHojeFallback
import com.example.hdcfuncap.network.toMedicamentosHoje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val authApi: AuthApi) : ViewModel() {
    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 60_000L
    }

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pacienteNome = MutableStateFlow("João da Silva")
    val pacienteNome: StateFlow<String> = _pacienteNome.asStateFlow()

    private var lastLoadedAtMillis = 0L
    private var lastPacienteId: Long? = null

    fun carregarDados(pacienteId: Long, forceRefresh: Boolean = false) {
        if (_isLoading.value) return
        if (!forceRefresh && isCacheFresh(pacienteId)) return

        viewModelScope.launch {
            val hasCachedData = lastLoadedAtMillis > 0L
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val hoje = dataAtualIso()
                val prescricoesAtivas = authApi
                    .getPrescricoesMedicamentos(pacienteId)
                    .apenasPrescricoesMedicamentosAtivas(hoje)
                val medicamentosHoje = try {
                    authApi
                        .getMedicamentosHoje(pacienteId, hoje)
                        .toMedicamentosHoje()
                        .apenasDasPrescricoes(prescricoesAtivas)
                } catch (e: Exception) {
                    emptyList()
                }

                _medicamentos.value = medicamentosHoje.ifEmpty {
                    prescricoesAtivas.toMedicamentosHojeFallback(hoje)
                }
                lastPacienteId = pacienteId
                lastLoadedAtMillis = SystemClock.elapsedRealtime()
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível carregar seus medicamentos de hoje."
                if (!hasCachedData) {
                    _medicamentos.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isCacheFresh(pacienteId: Long): Boolean {
        return lastPacienteId == pacienteId &&
            lastLoadedAtMillis > 0L &&
            SystemClock.elapsedRealtime() - lastLoadedAtMillis < AUTO_REFRESH_INTERVAL_MS
    }

    private fun dataAtualIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

class HomeViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
