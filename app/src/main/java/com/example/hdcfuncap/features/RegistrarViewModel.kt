package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.AtualizarAdesaoRequest
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.OrientacaoFuncionalResponse
import com.example.hdcfuncap.network.RegistroRealizacaoFuncionalRequest
import com.example.hdcfuncap.network.RegistroRealizacaoFuncionalResponse
import com.example.hdcfuncap.network.toMedicamentosHoje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExercicioRegistroUi(
    val orientacao: OrientacaoFuncionalResponse,
    val registroHoje: RegistroRealizacaoFuncionalResponse?
)

class RegistrarViewModel(private val authApi: AuthApi) : ViewModel() {

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _exercicios = MutableStateFlow<List<ExercicioRegistroUi>>(emptyList())
    val exercicios: StateFlow<List<ExercicioRegistroUi>> = _exercicios.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarDados(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val erros = mutableListOf<String>()
            try {
                _medicamentos.value = authApi
                    .getMedicamentosHoje(pacienteId, dataAtualIso())
                    .toMedicamentosHoje()
            } catch (e: Exception) {
                erros.add("Não foi possível carregar os medicamentos de hoje.")
                _medicamentos.value = emptyList()
            }

            try {
                _exercicios.value = carregarExerciciosDoDia(pacienteId)
            } catch (e: Exception) {
                erros.add("Não foi possível carregar os exercícios de hoje.")
                _exercicios.value = emptyList()
            }

            _errorMessage.value = erros.joinToString("\n").ifBlank { null }
            _isLoading.value = false
        }
    }

    private suspend fun carregarExerciciosDoDia(pacienteId: Long): List<ExercicioRegistroUi> {
        val hoje = dataAtualIso()
        val orientacoes = authApi
            .getOrientacoesFuncionaisPaciente()
            .content
            .orEmpty()
            .filter { it.ativo != false && it.id != null }

        val registrosHoje = authApi
            .getHistoricoRealizacaoFuncional(pacienteId = pacienteId)
            .content
            .orEmpty()
            .filter { it.dataRegistro?.substringBefore("T") == hoje }
            .groupBy { it.orientacaoFuncionalId }
            .mapValues { (_, registros) -> registros.firstOrNull() }

        return orientacoes.map { orientacao ->
            ExercicioRegistroUi(
                orientacao = orientacao,
                registroHoje = registrosHoje[orientacao.id]
            )
        }
    }

    fun registrarExercicio(
        pacienteId: Long,
        exercicio: ExercicioRegistroUi,
        status: String,
        duracaoRealizadaMinutos: Int?,
        sensacaoFinal: String?,
        observacao: String?
    ) {
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val orientacaoId = exercicio.orientacao.id ?: return@launch
                val request = RegistroRealizacaoFuncionalRequest(
                    id = orientacaoId,
                    status = status,
                    duracaoRealizadaMinutos = duracaoRealizadaMinutos,
                    sensacaoFinal = sensacaoFinal,
                    observacao = observacao
                )

                val registroId = exercicio.registroHoje?.id
                if (registroId == null) {
                    authApi.registrarRealizacaoFuncional(
                        pacienteId = pacienteId,
                        request = request
                    )
                } else {
                    authApi.alterarRealizacaoFuncional(
                        pacienteId = pacienteId,
                        registroId = registroId,
                        request = request
                    )
                }

                carregarDados(pacienteId)
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível salvar este exercício."
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
