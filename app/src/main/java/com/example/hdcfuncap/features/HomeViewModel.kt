package com.example.hdcfuncap.features

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.OcorrenciaRefeicaoResponse
import com.example.hdcfuncap.network.refeicoesDisponiveis
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
import kotlinx.coroutines.coroutineScope
import com.example.hdcfuncap.util.rethrowIfCancellation

class HomeViewModel(private val authApi: AuthApi) : ViewModel() {
    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 60_000L
    }

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _refeicoes = MutableStateFlow<List<OcorrenciaRefeicaoResponse>>(emptyList())
    val refeicoes: StateFlow<List<OcorrenciaRefeicaoResponse>> = _refeicoes.asStateFlow()

    private val _refeicoesErrorMessage = MutableStateFlow<String?>(null)
    val refeicoesErrorMessage: StateFlow<String?> = _refeicoesErrorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMedicamentos = MutableStateFlow(false)
    val isLoadingMedicamentos: StateFlow<Boolean> = _isLoadingMedicamentos.asStateFlow()

    private val _isLoadingRefeicoes = MutableStateFlow(false)
    val isLoadingRefeicoes: StateFlow<Boolean> = _isLoadingRefeicoes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _pacienteNome = MutableStateFlow("João da Silva")
    val pacienteNome: StateFlow<String> = _pacienteNome.asStateFlow()

    private var lastLoadedAtMillis = 0L
    private var lastPacienteId: Long? = null
    private var lastLoadedDate: String? = null
    private var lastRefeicoesVersion = -1L

    fun carregarDados(pacienteId: Long, forceRefresh: Boolean = false) {
        if (_isLoading.value) return
        if (!forceRefresh && isCacheFresh(pacienteId)) return

        viewModelScope.launch {
            _isLoading.value = true
            _isLoadingMedicamentos.value = true
            _isLoadingRefeicoes.value = true
            _errorMessage.value = null
            _refeicoesErrorMessage.value = null
            val hoje = dataAtualIso()
            val refeicoesVersion = AtualizacoesRefeicoes.versao
            if (lastPacienteId != pacienteId || lastLoadedDate != hoje) {
                _medicamentos.value = emptyList()
                _refeicoes.value = emptyList()
                lastLoadedAtMillis = 0L
            }
            lastPacienteId = pacienteId
            lastLoadedDate = hoje
            try {
                coroutineScope {
                    launch {
                        try {
                            carregarMedicamentos(pacienteId, hoje)
                        } finally {
                            _isLoadingMedicamentos.value = false
                        }
                    }
                    launch {
                        try {
                            _refeicoes.value = authApi.getRefeicoesHoje().refeicoesDisponiveis()
                            lastRefeicoesVersion = refeicoesVersion
                        } catch (e: Exception) {
                            e.rethrowIfCancellation()
                            _refeicoesErrorMessage.value =
                                "Não foi possível carregar sua alimentação de hoje. Puxe para atualizar."
                        } finally {
                            _isLoadingRefeicoes.value = false
                        }
                    }
                }
                if (_errorMessage.value == null && _refeicoesErrorMessage.value == null) {
                    lastLoadedAtMillis = SystemClock.elapsedRealtime()
                }
            } finally {
                _isLoading.value = false
                _isLoadingMedicamentos.value = false
                _isLoadingRefeicoes.value = false
            }
        }
    }

    private suspend fun carregarMedicamentos(pacienteId: Long, hoje: String) {
        try {
            val prescricoesAtivas = authApi
                .getPrescricoesMedicamentos(pacienteId)
                .apenasPrescricoesMedicamentosAtivas(hoje)
            val medicamentosHoje = try {
                authApi
                    .getMedicamentosHoje(pacienteId, hoje)
                    .toMedicamentosHoje()
                    .apenasDasPrescricoes(prescricoesAtivas)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _errorMessage.value = "Não foi possível consultar os registros de hoje. Puxe para atualizar e tente novamente."
                emptyList()
            }

            _medicamentos.value = medicamentosHoje.ifEmpty {
                prescricoesAtivas.toMedicamentosHojeFallback(hoje)
            }
        } catch (e: Exception) {
            e.rethrowIfCancellation()
            _errorMessage.value = "Não foi possível carregar seus medicamentos de hoje."
        }
    }

    private fun isCacheFresh(pacienteId: Long): Boolean {
        return lastPacienteId == pacienteId &&
            lastRefeicoesVersion == AtualizacoesRefeicoes.versao &&
            lastLoadedDate == dataAtualIso() &&
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
