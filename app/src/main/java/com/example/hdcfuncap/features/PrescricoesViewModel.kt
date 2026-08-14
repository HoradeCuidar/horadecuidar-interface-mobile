package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.ItemMedicacaoResponse
import com.example.hdcfuncap.network.OrientacaoFuncionalResponse
import com.example.hdcfuncap.network.PrescricaoMedicamentoResponse
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

    fun carregarPrescricoesMedicamentos(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val pacienteAutenticado = authApi.getPacienteProfile()
                val idParaBusca = pacienteAutenticado.id ?: pacienteId
                _prescricoesMedicamentos.value = try {
                    authApi.getPrescricoesMedicamentos(idParaBusca)
                } catch (e: HttpException) {
                    if (e.code() == 500 && erroDeAtivoNulo(e)) {
                        carregarMedicamentosDoDiaComoPrescricao(idParaBusca)
                    } else {
                        throw e
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = when (e) {
                    is HttpException -> {
                        val detalhe = e.response()?.errorBody()?.string()?.let(::extrairMensagemErro)
                        if (detalhe.isNullOrBlank()) {
                            "Não foi possível carregar suas prescrições. Código ${e.code()}."
                        } else {
                            "Não foi possível carregar suas prescrições. Código ${e.code()}: $detalhe"
                        }
                    }
                    else -> "Não foi possível carregar suas prescrições."
                }
                _prescricoesMedicamentos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarOrientacoesFuncionais() {
        viewModelScope.launch {
            _isLoadingOrientacoes.value = true
            _errorMessageOrientacoes.value = null

            try {
                _orientacoesFuncionais.value = authApi
                    .getOrientacoesFuncionaisPaciente()
                    .content
                    .orEmpty()
                    .filter { it.ativo != false }
            } catch (e: Exception) {
                _errorMessageOrientacoes.value = "Não foi possível carregar seus exercícios."
                _orientacoesFuncionais.value = emptyList()
            } finally {
                _isLoadingOrientacoes.value = false
            }
        }
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
                        intervaloTipo = null
                    )
                },
                medicacoes = null
            )
        )
    }

    private fun erroDeAtivoNulo(e: HttpException): Boolean {
        val detalhe = e.response()?.errorBody()?.string()?.let(::extrairMensagemErro).orEmpty()
        return detalhe.contains("ItemMedicacao.ativo") || detalhe.contains("ativo")
    }

    private fun dataAtualIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}

class PrescricoesViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrescricoesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrescricoesViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
