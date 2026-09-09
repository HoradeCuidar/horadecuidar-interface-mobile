package com.example.hdcfuncap.features

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.ItemMedicacaoResponse
import com.example.hdcfuncap.network.OrientacaoFuncionalResponse
import com.example.hdcfuncap.network.PrescricaoMedicamentoResponse
import com.example.hdcfuncap.network.apenasPrescricoesMedicamentosAtivas
import com.example.hdcfuncap.network.toMedicamentosHoje
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class PrescricoesViewModel(private val authApi: AuthApi) : ViewModel() {
    private companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 2 * 60_000L
    }

    private val _prescricoesMedicamentos =
        MutableStateFlow<List<PrescricaoMedicamentoResponse>>(emptyList())
    val prescricoesMedicamentos: StateFlow<List<PrescricaoMedicamentoResponse>> =
        _prescricoesMedicamentos.asStateFlow()

    private val _orientacoesFuncionais =
        MutableStateFlow<List<OrientacaoFuncionalResponse>>(emptyList())
    val orientacoesFuncionais: StateFlow<List<OrientacaoFuncionalResponse>> =
        _orientacoesFuncionais.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingOrientacoes = MutableStateFlow(false)
    val isLoadingOrientacoes: StateFlow<Boolean> = _isLoadingOrientacoes.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorMessageOrientacoes = MutableStateFlow<String?>(null)
    val errorMessageOrientacoes: StateFlow<String?> = _errorMessageOrientacoes.asStateFlow()

    private var lastMedicamentosLoadedAtMillis = 0L
    private var lastMedicamentosPacienteId: Long? = null
    private var lastOrientacoesLoadedAtMillis = 0L

    fun carregarPrescricoesMedicamentos(pacienteId: Long, forceRefresh: Boolean = false) {
        if (_isLoading.value) return
        if (!forceRefresh && isMedicamentosCacheFresh(pacienteId)) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val hasCachedData = lastMedicamentosLoadedAtMillis > 0L

            try {
                val pacienteAutenticado = authApi.getPacienteProfile()
                val idParaBusca = pacienteAutenticado.id ?: pacienteId
                _prescricoesMedicamentos.value = try {
                    authApi.getPrescricoesMedicamentos(idParaBusca)
                        .apenasPrescricoesMedicamentosAtivas()
                } catch (e: HttpException) {
                    val detalhe = e.mensagemErro()
                    if (e.code() == 500 && erroDeAtivoNulo(detalhe)) {
                        carregarMedicamentosDoDiaComoPrescricao(idParaBusca)
                    } else {
                        throw PrescricoesHttpException(
                            code = e.code(),
                            detalhe = detalhe
                        )
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = when (e) {
                    is HttpException -> {
                        val detalhe = e.mensagemErro()
                        if (detalhe.isNullOrBlank()) {
                            "Não foi possível carregar suas prescrições. Código ${e.code()}."
                        } else {
                            "Não foi possível carregar suas prescrições. Código ${e.code()}: $detalhe"
                        }
                    }
                    is PrescricoesHttpException -> {
                        if (e.detalhe.isBlank()) {
                            "Não foi possível carregar suas prescrições. Código ${e.code}."
                        } else {
                            "Não foi possível carregar suas prescrições. Código ${e.code}: ${e.detalhe}"
                        }
                    }
                    else -> "Não foi possível carregar suas prescrições."
                }
                if (!hasCachedData) {
                    _prescricoesMedicamentos.value = emptyList()
                }
            } finally {
                if (_errorMessage.value == null) {
                    lastMedicamentosPacienteId = pacienteId
                    lastMedicamentosLoadedAtMillis = SystemClock.elapsedRealtime()
                }
                _isLoading.value = false
            }
        }
    }

    fun carregarOrientacoesFuncionais(forceRefresh: Boolean = false) {
        if (_isLoadingOrientacoes.value) return
        if (!forceRefresh && isOrientacoesCacheFresh()) return

        viewModelScope.launch {
            _isLoadingOrientacoes.value = true
            _errorMessageOrientacoes.value = null
            val hasCachedData = lastOrientacoesLoadedAtMillis > 0L

            try {
                _orientacoesFuncionais.value = authApi
                    .getOrientacoesFuncionaisPaciente()
                    .content
                    .orEmpty()
                    .filter { it.ativo != false }
                lastOrientacoesLoadedAtMillis = SystemClock.elapsedRealtime()
            } catch (e: Exception) {
                _errorMessageOrientacoes.value = "Não foi possível carregar seus exercícios."
                if (!hasCachedData) {
                    _orientacoesFuncionais.value = emptyList()
                }
            } finally {
                _isLoadingOrientacoes.value = false
            }
        }
    }

    private fun isMedicamentosCacheFresh(pacienteId: Long): Boolean {
        return lastMedicamentosPacienteId == pacienteId &&
            lastMedicamentosLoadedAtMillis > 0L &&
            SystemClock.elapsedRealtime() - lastMedicamentosLoadedAtMillis < AUTO_REFRESH_INTERVAL_MS
    }

    private fun isOrientacoesCacheFresh(): Boolean {
        return lastOrientacoesLoadedAtMillis > 0L &&
            SystemClock.elapsedRealtime() - lastOrientacoesLoadedAtMillis < AUTO_REFRESH_INTERVAL_MS
    }

    private fun extrairMensagemErro(body: String): String {
        return try {
            JSONObject(body).optString("message").ifBlank {
                JSONObject(body).optString("mensagem")
            }
        } catch (e: Exception) {
            body
        }
    }

    private suspend fun carregarMedicamentosDoDiaComoPrescricao(
        pacienteId: Long
    ): List<PrescricaoMedicamentoResponse> {
        val hoje = dataAtualIso()
        val medicamentosHoje = authApi
            .getMedicamentosHoje(pacienteId, hoje)
            .toMedicamentosHoje()

        if (medicamentosHoje.isEmpty()) return emptyList()

        return listOf(
            PrescricaoMedicamentoResponse(
                id = "ocorrencias-$hoje",
                nomeProfissional = null,
                dataInicio = hoje,
                dataFim = null,
                ativo = true,
                itens = medicamentosHoje.map { medicamento ->
                    ItemMedicacaoResponse(
                        itemId = medicamento.itemId,
                        id = medicamento.itemId,
                        nomeMedicamento = medicamento.nomeMedicamento,
                        dosagemFormatada = medicamento.dosagemFormatada,
                        frequencia = medicamento.frequencia,
                        viaAdministracao = medicamento.viaAdministracao,
                        dosagemValor = null,
                        dosagemUnidade = null,
                        quantidadeDoses = null,
                        intervaloValor = null,
                        intervaloTipo = null,
                        ativo = true,
                        observacao = medicamento.observacao
                    )
                },
                medicacoes = null
            )
        )
    }

    private fun erroDeAtivoNulo(detalhe: String): Boolean {
        return detalhe.lowercase(Locale.US).contains("ativo")
    }

    private fun HttpException.mensagemErro(): String {
        return response()?.errorBody()?.string()?.let(::extrairMensagemErro).orEmpty()
    }

    private fun dataAtualIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

private data class PrescricoesHttpException(
    val code: Int,
    val detalhe: String
) : Exception()

class PrescricoesViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrescricoesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrescricoesViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
