package com.example.hdcfuncap.features

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.ExameResponse
import com.example.hdcfuncap.network.ExamesRepository
import com.example.hdcfuncap.network.SituacaoExame
import com.example.hdcfuncap.network.situacao
import com.example.hdcfuncap.network.titulo
import com.example.hdcfuncap.util.rethrowIfCancellation
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update
import retrofit2.HttpException

internal data class PdfExame(val file: File, val titulo: String)

internal data class ExamesUiState(
    val exames: List<ExameResponse> = emptyList(),
    val carregando: Boolean = false,
    val carregado: Boolean = false,
    val erro: String? = null,
    val arquivoEmAndamento: Long? = null,
    val pdf: PdfExame? = null,
    val mensagem: String? = null
)

internal class ExamesViewModel(private val repository: ExamesRepository) : ViewModel() {
    private val _state = MutableStateFlow(ExamesUiState())
    val state = _state.asStateFlow()
    private var pacienteId: Long? = null
    private var geracao = 0
    private var carregadoEm = 0L
    private var consulta: Job? = null
    private var arquivo: Job? = null
    private var acao = 0

    fun carregar(patientId: Long?, atualizar: Boolean = false) {
        if (pacienteId != patientId) {
            geracao++
            consulta?.cancel()
            arquivo?.cancel()
            repository.limpar()
            pacienteId = patientId
            carregadoEm = 0L
            _state.value = ExamesUiState()
        }
        if (patientId == null || consulta?.isActive == true) return
        if (!atualizar && carregadoEm > 0 && SystemClock.elapsedRealtime() - carregadoEm < 120_000L) return
        val version = geracao
        _state.update { it.copy(carregando = true, erro = null) }
        consulta = viewModelScope.launch {
            try {
                val exames = repository.listar(patientId)
                if (version == geracao) {
                    _state.update { it.copy(exames = exames, carregado = true) }
                    carregadoEm = SystemClock.elapsedRealtime()
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                if (version == geracao) _state.update { it.copy(erro = mensagemErroExames(e)) }
            } finally {
                if (version == geracao) _state.update { it.copy(carregando = false) }
            }
        }
    }

    fun abrir(exameId: Long) {
        val patient = pacienteId ?: return
        val exame = exameDisponivel(exameId) ?: return
        if (arquivo?.isActive == true) return
        val version = geracao
        val action = ++acao
        _state.update { it.copy(arquivoEmAndamento = exameId) }
        arquivo = viewModelScope.launch {
            var file: File? = null
            var opened = false
            try {
                file = repository.obterPdf(patient, exameId)
                if (version == geracao && action == acao) {
                    fecharPdf()
                    _state.update { it.copy(pdf = PdfExame(file, exame.titulo)) }
                    opened = true
                }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                if (version == geracao) _state.update { it.copy(mensagem = mensagemErroExames(e, arquivo = true)) }
            } finally {
                if (!opened) file?.let(repository::liberar)
                if (version == geracao && action == acao) _state.update { it.copy(arquivoEmAndamento = null) }
            }
        }
    }

    fun baixar(exameId: Long, destination: Uri) {
        val patient = pacienteId ?: return
        if (exameDisponivel(exameId) == null || arquivo?.isActive == true) return
        val version = geracao
        val action = ++acao
        _state.update { it.copy(arquivoEmAndamento = exameId) }
        arquivo = viewModelScope.launch {
            try {
                repository.salvarPdf(patient, exameId, destination)
                if (version == geracao) _state.update { it.copy(mensagem = "PDF salvo no local escolhido.") }
            } catch (e: Exception) {
                e.rethrowIfCancellation()
                if (version == geracao) _state.update { it.copy(mensagem = mensagemErroExames(e, arquivo = true)) }
            } finally {
                if (version == geracao && action == acao) _state.update { it.copy(arquivoEmAndamento = null) }
            }
        }
    }

    fun fecharPdf() {
        _state.value.pdf?.file?.let(repository::liberar)
        _state.update { it.copy(pdf = null) }
    }

    fun consumirMensagem() { _state.update { it.copy(mensagem = null) } }

    fun informarMensagem(message: String) { _state.update { it.copy(mensagem = message) } }

    fun sair() {
        acao++
        arquivo?.cancel()
        fecharPdf()
        _state.update { it.copy(arquivoEmAndamento = null) }
        repository.limpar()
    }

    private fun exameDisponivel(id: Long) = _state.value.exames.firstOrNull {
        it.id == id && it.situacao() == SituacaoExame.PUBLICADO
    }

    override fun onCleared() { repository.fechar() }
}

private fun mensagemErroExames(e: Exception, arquivo: Boolean = false): String = when {
    e is HttpException && e.code() == 401 -> "Sua sessão expirou. Entre novamente para acessar os exames."
    e is HttpException && e.code() == 403 -> "Você não tem permissão para acessar este exame."
    e is HttpException && e.code() == 404 && arquivo -> "Este PDF não está mais disponível. Atualize seus exames."
    arquivo -> "Não foi possível acessar o PDF. Verifique a conexão e tente novamente."
    else -> "Não foi possível carregar seus exames. Puxe para atualizar ou tente novamente."
}

internal class ExamesViewModelFactory(private val api: AuthApi, private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ExamesViewModel::class.java))
        @Suppress("UNCHECKED_CAST")
        return ExamesViewModel(ExamesRepository(api, context.applicationContext)) as T
    }
}
