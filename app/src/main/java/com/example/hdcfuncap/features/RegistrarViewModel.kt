package com.example.hdcfuncap.features

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.AtualizarAdesaoRequest
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.network.OrientacaoFuncionalResponse
import com.example.hdcfuncap.network.OcorrenciaRefeicaoResponse
import com.example.hdcfuncap.network.RegistroRefeicaoRequest
import com.example.hdcfuncap.network.StatusRefeicao
import com.example.hdcfuncap.network.comRegistro
import com.example.hdcfuncap.network.opcoesRegistraveis
import com.example.hdcfuncap.network.podeRegistrar
import com.example.hdcfuncap.network.refeicoesDisponiveis
import com.example.hdcfuncap.network.statusRefeicao
import com.example.hdcfuncap.network.RegistroRealizacaoFuncionalRequest
import com.example.hdcfuncap.network.RegistroRealizacaoFuncionalResponse
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
import com.example.hdcfuncap.util.rethrowIfCancellation

data class ExercicioRegistroUi(
    val orientacao: OrientacaoFuncionalResponse,
    val registroHoje: RegistroRealizacaoFuncionalResponse?
)
class RegistrarViewModel(private val authApi: AuthApi) : ViewModel() {
    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 45_000L
    }

    private val _medicamentos = MutableStateFlow<List<MedicamentoHojeResponse>>(emptyList())
    val medicamentos: StateFlow<List<MedicamentoHojeResponse>> = _medicamentos.asStateFlow()

    private val _exercicios = MutableStateFlow<List<ExercicioRegistroUi>>(emptyList())
    val exercicios: StateFlow<List<ExercicioRegistroUi>> = _exercicios.asStateFlow()

    private val _refeicoes = MutableStateFlow<List<OcorrenciaRefeicaoResponse>>(emptyList())
    val refeicoes = _refeicoes.asStateFlow()
    private val _isLoadingRefeicoes = MutableStateFlow(false)
    val isLoadingRefeicoes = _isLoadingRefeicoes.asStateFlow()
    private val _refeicoesErrorMessage = MutableStateFlow<String?>(null)
    val refeicoesErrorMessage = _refeicoesErrorMessage.asStateFlow()
    private val _refeicoesSalvando = MutableStateFlow<Set<Long>>(emptySet())
    val refeicoesSalvando = _refeicoesSalvando.asStateFlow()
    private var lastRefeicoesPacienteId: Long? = null
    private var lastRefeicoesDate: String? = null
    private var lastRefeicoesLoadedAtMillis = 0L
    private var lastRefeicoesVersion = -1L

    fun carregarRefeicoes(pacienteId: Long, forceRefresh: Boolean = false) {
        if (_isLoadingRefeicoes.value || _refeicoesSalvando.value.isNotEmpty()) return
        val hoje = dataAtualIso()
        val version = AtualizacoesRefeicoes.versao
        val samePatientAndDay = lastRefeicoesPacienteId == pacienteId && lastRefeicoesDate == hoje
        if (!forceRefresh && samePatientAndDay && lastRefeicoesVersion == version &&
            _refeicoesErrorMessage.value == null && lastRefeicoesLoadedAtMillis > 0L &&
            SystemClock.elapsedRealtime() - lastRefeicoesLoadedAtMillis < AUTO_REFRESH_INTERVAL_MS
        ) return
        if (!samePatientAndDay) {
            _refeicoes.value = emptyList()
            lastRefeicoesLoadedAtMillis = 0L
        }
        lastRefeicoesPacienteId = pacienteId
        lastRefeicoesDate = hoje
        _isLoadingRefeicoes.value = true
        _refeicoesErrorMessage.value = null
        viewModelScope.launch {
            try {
                _refeicoes.value = authApi.getRefeicoesHoje().refeicoesDisponiveis()
                lastRefeicoesLoadedAtMillis = SystemClock.elapsedRealtime()
                lastRefeicoesVersion = version
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _refeicoesErrorMessage.value = "Não foi possível carregar as refeições. Puxe para atualizar."
            } finally {
                _isLoadingRefeicoes.value = false
            }
        }
    }

    fun registrarRefeicao(
        pacienteId: Long,
        ocorrenciaId: Long,
        opcaoId: Long,
        observacao: String?,
        onSuccess: () -> Unit
    ) {
        if (ocorrenciaId in _refeicoesSalvando.value || _isLoadingRefeicoes.value) return
        val ocorrencia = _refeicoes.value.firstOrNull { it.id == ocorrenciaId }
        if (lastRefeicoesPacienteId != pacienteId || ocorrencia == null ||
            !ocorrencia.podeRegistrar(dataAtualIso()) || ocorrencia.opcoesRegistraveis().none { it.id == opcaoId }
        ) {
            _refeicoesErrorMessage.value = "Esta refeição não está disponível para registro. Atualize a tela."
            return
        }
        _refeicoesSalvando.value += ocorrenciaId
        _refeicoesErrorMessage.value = null
        viewModelScope.launch {
            try {
                val request = RegistroRefeicaoRequest(ocorrenciaId, opcaoId, observacao?.trim()?.ifBlank { null })
                val resposta = if (ocorrencia.statusRefeicao == StatusRefeicao.REALIZADO) {
                    authApi.editarRegistroRefeicao(ocorrenciaId, request)
                } else {
                    authApi.registrarRefeicao(request)
                }
                val atualizada = ocorrencia.comRegistro(resposta, opcaoId)
                _refeicoes.value = _refeicoes.value.map { if (it.id == ocorrenciaId) atualizada else it }
                AtualizacoesRefeicoes.notificarRegistro()
                lastRefeicoesVersion = AtualizacoesRefeicoes.versao
                onSuccess()
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _refeicoesErrorMessage.value =
                    "Não foi possível confirmar o registro da refeição. Verifique sua conexão ou atualize a tela."
            } finally {
                _refeicoesSalvando.value -= ocorrenciaId
            }
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastLoadedAtMillis = 0L
    private var lastPacienteId: Long? = null
    private val registrosEmAndamento = mutableSetOf<String>()

    fun carregarDados(pacienteId: Long, forceRefresh: Boolean = false) {
        if (_isLoading.value) return
        if (!forceRefresh && isCacheFresh(pacienteId)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val erros = mutableListOf<String>()
            val hasCachedData = lastLoadedAtMillis > 0L
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
                    e.rethrowIfCancellation()
                    erros.add("Não foi possível consultar os registros de medicamentos de hoje. Tente atualizar novamente.")
                    emptyList()
                }

                _medicamentos.value = medicamentosHoje.ifEmpty {
                    prescricoesAtivas.toMedicamentosHojeFallback(hoje)
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                erros.add("Não foi possível carregar os medicamentos de hoje.")
                if (!hasCachedData) {
                    _medicamentos.value = emptyList()
                }
            }

            try {
                _exercicios.value = carregarExerciciosDoDia(pacienteId)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                erros.add("Não foi possível carregar os exercícios de hoje.")
                if (!hasCachedData) {
                    _exercicios.value = emptyList()
                }
            }

            if (erros.isEmpty()) {
                lastPacienteId = pacienteId
                lastLoadedAtMillis = SystemClock.elapsedRealtime()
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
            .filter { it.dataRegistro?.trim()?.substringBefore("T")?.substringBefore(" ") == hoje }
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
        val registroKey = "exercicio-$pacienteId-${exercicio.orientacao.id}"
        if (!registrosEmAndamento.add(registroKey)) return
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val orientacaoId = exercicio.orientacao.id ?: return@launch
                val request = RegistroRealizacaoFuncionalRequest(
                    id = orientacaoId,
                    status = status.normalizedApiValue(),
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

                carregarDados(pacienteId, forceRefresh = true)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _errorMessage.value = "Não foi possível salvar este exercício."
            } finally {
                registrosEmAndamento.remove(registroKey)
            }
        }
    }

    fun registrarAdesao(medicamento: MedicamentoHojeResponse, status: String, pacienteId: Long) {
        val registroKey = "medicamento-$pacienteId-${medicamento.ocorrenciaId}"
        if (!registrosEmAndamento.add(registroKey)) return
        viewModelScope.launch {
            _errorMessage.value = null
            try {
                val ocorrenciaId = medicamento.ocorrenciaId
                if (ocorrenciaId == null) {
                    _errorMessage.value =
                        "Este medicamento ainda não possui ocorrência gerada para hoje. Tente novamente mais tarde."
                    return@launch
                }

                val request = AtualizarAdesaoRequest(
                    ocorrenciaId = ocorrenciaId,
                    itemMedicacaoId = medicamento.itemId,
                    ordemNoDia = medicamento.ordemNoDia,
                    dataPrevista = medicamento.dataPrevista,
                    quantidadeDiaria = medicamento.quantidadeDiaria,
                    status = status.normalizedApiValue(),
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

                carregarDados(pacienteId, forceRefresh = true)
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                _errorMessage.value = "Não foi possível salvar este registro."
            } finally {
                registrosEmAndamento.remove(registroKey)
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

private fun String.normalizedApiValue(): String {
    return trim().uppercase(Locale.US)
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
