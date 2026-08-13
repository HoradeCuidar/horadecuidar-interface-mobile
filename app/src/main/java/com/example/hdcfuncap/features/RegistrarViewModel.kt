package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.AtualizarAdesaoRequest
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.toMedicamentosHoje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrarViewModel(private val authApi: AuthApi) : ViewModel() {

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarDados(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _medicamentos.value = authApi
                    .getMedicamentosHoje(pacienteId, dataAtualIso())
                    .toMedicamentosHoje()
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível carregar os registros de hoje."
                _medicamentos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun registrarAdesao(medicamento: MedicamentoHojeResponse, status: String, pacienteId: Long) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val ocorrenciaId = medicamento.ocorrenciaId ?: return@launch
                val request = AtualizarAdesaoRequest(
                    ocorrenciaId = ocorrenciaId,
                    itemMedicacaoId = medicamento.itemId,
                    ordemNoDia = medicamento.ordemNoDia,
                    dataPrevista = medicamento.dataPrevista,
                    quantidadeDiaria = medicamento.quantidadeDiaria,
                    status = status,
                    observacao = medicamento.observacao ?: ""
                )

                if (medicamento.adesaoId != null) {
                    authApi.registrarAdesao(
                        pacienteId = pacienteId,
                        adesaoId = medicamento.adesaoId,
                        request = request
                    )
                } else {
                    authApi.criarAdesao(pacienteId = pacienteId, request = request)
                }

                carregarDados(pacienteId)
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível salvar este registro."
            }
        }
    }

    private fun dataAtualIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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
