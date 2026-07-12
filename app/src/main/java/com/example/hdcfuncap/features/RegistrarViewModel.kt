package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.AtualizarAdesaoRequest
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrarViewModel(private val authApi: AuthApi) : ViewModel() {

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun carregarDados(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val medicamentosHoje = authApi.getMedicamentosHoje(pacienteId)
                val medicamentosPendentes = medicamentosHoje.filter { it.statusAdesaoHoje == null }

                if (medicamentosPendentes.isNotEmpty()) {
                    medicamentosPendentes.forEach { medicamento ->
                        val request = AtualizarAdesaoRequest(
                            itemMedicacaoId = medicamento.itemId,
                            status = "NAO_REALIZADO",
                            observacao = medicamento.observacao ?: ""
                        )

                        if (medicamento.adesaoId != null) {
                            authApi.registrarAdesao(
                                pacienteId = pacienteId,
                                adesaoId = medicamento.adesaoId,
                                request = request
                            )
                        } else {
                            authApi.criarAdesao(
                                pacienteId = pacienteId,
                                request = request
                            )
                        }
                    }

                    _medicamentos.value = authApi.getMedicamentosHoje(pacienteId)
                } else {
                    _medicamentos.value = medicamentosHoje
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarAdesao(itemId: Long, adesaoId: Long?, status: String, observacao: String, pacienteId: Long) {
        viewModelScope.launch {
            try {
                val request = AtualizarAdesaoRequest(
                    itemMedicacaoId = itemId,
                    status = status,
                    observacao = observacao
                )

                if (adesaoId != null) {
                    authApi.registrarAdesao(pacienteId = pacienteId, adesaoId = adesaoId, request = request)
                } else {
                    authApi.criarAdesao(pacienteId = pacienteId, request = request)
                }

                carregarDados(pacienteId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class RegistrarViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegistrarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegistrarViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
