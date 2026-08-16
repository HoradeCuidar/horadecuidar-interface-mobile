package com.example.hdcfuncap.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.hdcfuncap.network.AuthApi
import com.example.hdcfuncap.network.OcorrenciaMedicamentoResponse
import com.example.hdcfuncap.network.RegistroRealizacaoFuncionalResponse
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoricoResumoSemanaUi(
    val label: String,
    val medicamentosPercentual: Int,
    val alimentacaoPercentual: Int? = null,
    val exerciciosPercentual: Int? = null
)

data class HistoricoRegistroUi(
    val data: String,
    val nome: String,
    val status: String,
    val categoria: String
)

data class HistoricoUiState(
    val mesAno: String = "",
    val semanas: List<HistoricoResumoSemanaUi> = emptyList(),
    val registrosRecentes: List<HistoricoRegistroUi> = emptyList(),
    val canCarregarMesAnterior: Boolean = true,
    val canCarregarProximoMes: Boolean = false
)

class HistoricoViewModel(private val authApi: AuthApi) : ViewModel() {
    private companion object {
        const val EXERCICIOS_PAGE_SIZE = 100
        const val MAX_EXERCICIOS_PAGES = 4
    }

    private val mesSelecionado = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }

    private val _uiState = MutableStateFlow(HistoricoUiState())
    val uiState: StateFlow<HistoricoUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun carregarHistorico(pacienteId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val mesReferencia = mesSelecionado.clone() as Calendar
                val diasDoMes = datasDoMes(mesReferencia)
                val ocorrenciasPorData = mutableMapOf<String, List<OcorrenciaMedicamentoResponse>>()
                var avisoParcial: String? = null

                diasDoMes.forEach { data ->
                    val response = authApi.getMedicamentosHoje(pacienteId, data)
                    ocorrenciasPorData[data] = response.ocorrencias.orEmpty()
                }

                val registrosExercicios = try {
                    carregarRegistrosExercicioDoMes(
                        pacienteId = pacienteId,
                        dataInicio = diasDoMes.firstOrNull().orEmpty(),
                        dataFim = diasDoMes.lastOrNull().orEmpty()
                    )
                } catch (e: Exception) {
                    avisoParcial = "Não foi possível carregar os dados de exercícios."
                    emptyList()
                }

                val datasRecentes = datasRecentes(mesReferencia, diasDoMes)
                val registrosRecentes = montarRegistrosRecentes(
                    datasRecentes = datasRecentes,
                    ocorrenciasPorData = ocorrenciasPorData,
                    registrosExercicios = registrosExercicios
                )

                _uiState.value = HistoricoUiState(
                    mesAno = mesAno(mesReferencia),
                    semanas = calcularSemanas(diasDoMes, ocorrenciasPorData, registrosExercicios),
                    registrosRecentes = registrosRecentes,
                    canCarregarMesAnterior = podeCarregarMesAnterior(mesReferencia),
                    canCarregarProximoMes = podeCarregarProximoMes(mesReferencia)
                )
                _errorMessage.value = avisoParcial
            } catch (e: Exception) {
                _errorMessage.value = "Não foi possível carregar seu histórico."
                _uiState.value = HistoricoUiState(
                    mesAno = mesAno(mesSelecionado),
                    canCarregarMesAnterior = podeCarregarMesAnterior(mesSelecionado),
                    canCarregarProximoMes = podeCarregarProximoMes(mesSelecionado)
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun carregarMesAnterior(pacienteId: Long) {
        if (!podeCarregarMesAnterior(mesSelecionado)) return
        mesSelecionado.add(Calendar.MONTH, -1)
        carregarHistorico(pacienteId)
    }

    fun carregarProximoMes(pacienteId: Long) {
        if (!podeCarregarProximoMes(mesSelecionado)) return
        mesSelecionado.add(Calendar.MONTH, 1)
        carregarHistorico(pacienteId)
    }

    private fun podeCarregarMesAnterior(mesReferencia: Calendar): Boolean {
        return mesesAntesDoAtual(mesReferencia) < 2
    }

    private fun podeCarregarProximoMes(mesReferencia: Calendar): Boolean {
        return mesesAntesDoAtual(mesReferencia) > 0
    }

    private fun mesesAntesDoAtual(mesReferencia: Calendar): Int {
        val atual = Calendar.getInstance()
        return (atual.get(Calendar.YEAR) * 12 + atual.get(Calendar.MONTH)) -
            (mesReferencia.get(Calendar.YEAR) * 12 + mesReferencia.get(Calendar.MONTH))
    }

    private fun calcularSemanas(
        diasDoMes: List<String>,
        ocorrenciasPorData: Map<String, List<OcorrenciaMedicamentoResponse>>,
        registrosExercicios: List<RegistroRealizacaoFuncionalResponse>
    ): List<HistoricoResumoSemanaUi> {
        return dividirMesEmQuatroPeriodos(diasDoMes)
            .mapIndexed { index, datas ->
                val datasDoPeriodo = datas.toSet()
                val ocorrencias = datas.flatMap { data -> ocorrenciasPorData[data].orEmpty() }
                val esperados = ocorrencias.count { it.status != "CANCELADO" }
                val realizados = ocorrencias.count { it.status == "REALIZADO" }
                val percentualMedicamentos = if (esperados > 0) {
                    ((realizados.toFloat() / esperados.toFloat()) * 100).toInt()
                } else {
                    0
                }
                val exerciciosDoPeriodo = registrosExercicios.filter { registro ->
                    extrairDataIso(registro.dataRegistro) in datasDoPeriodo
                }
                val percentualExercicios = calcularPercentualExercicios(exerciciosDoPeriodo)

                HistoricoResumoSemanaUi(
                    label = "Sem ${index + 1}",
                    medicamentosPercentual = percentualMedicamentos.coerceIn(0, 100),
                    exerciciosPercentual = percentualExercicios
                )
            }
    }

    private suspend fun carregarRegistrosExercicioDoMes(
        pacienteId: Long,
        dataInicio: String,
        dataFim: String
    ): List<RegistroRealizacaoFuncionalResponse> {
        if (dataInicio.isBlank() || dataFim.isBlank()) return emptyList()

        val registros = mutableListOf<RegistroRealizacaoFuncionalResponse>()

        for (pagina in 0 until MAX_EXERCICIOS_PAGES) {
            val content = authApi.getHistoricoRealizacaoFuncional(
                pacienteId = pacienteId,
                numberPage = pagina,
                pageSize = EXERCICIOS_PAGE_SIZE
            ).content.orEmpty()

            if (content.isEmpty()) break

            registros += content.filter { registro ->
                val dataRegistro = extrairDataIso(registro.dataRegistro)
                dataRegistro in dataInicio..dataFim
            }

            val chegouEmRegistrosAntigos = content.any { registro ->
                val dataRegistro = extrairDataIso(registro.dataRegistro)
                dataRegistro.isNotBlank() && dataRegistro < dataInicio
            }

            if (chegouEmRegistrosAntigos || content.size < EXERCICIOS_PAGE_SIZE) break
        }

        return registros
    }

    private fun calcularPercentualExercicios(
        registros: List<RegistroRealizacaoFuncionalResponse>
    ): Int {
        if (registros.isEmpty()) return 0

        val pontuacao = registros.sumOf { registro ->
            when (registro.status) {
                "REALIZADO" -> 1.0
                "PARCIALMENTE_REALIZADO" -> 0.5
                else -> 0.0
            }
        }

        return ((pontuacao / registros.size.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun montarRegistrosRecentes(
        datasRecentes: List<String>,
        ocorrenciasPorData: Map<String, List<OcorrenciaMedicamentoResponse>>,
        registrosExercicios: List<RegistroRealizacaoFuncionalResponse>
    ): List<HistoricoRegistroUi> {
        return datasRecentes.flatMap { data ->
            val medicamentos = ocorrenciasPorData[data].orEmpty()
                .filter { it.status == "REALIZADO" || it.status == "NAO_REALIZADO" }
                .mapNotNull { ocorrencia ->
                    val nome = ocorrencia.itemMedicacao?.nomeMedicamento
                        .orEmpty()
                        .ifBlank { return@mapNotNull null }

                    HistoricoRegistroUi(
                        data = formatarDiaMes(data),
                        nome = nome,
                        status = ocorrencia.status.orEmpty(),
                        categoria = "medicamento"
                    )
                }

            val exercicios = registrosExercicios
                .filter { registro -> extrairDataIso(registro.dataRegistro) == data }
                .mapNotNull { registro ->
                    val nome = registro.nomeOrientacao
                        .orEmpty()
                        .ifBlank { return@mapNotNull null }

                    HistoricoRegistroUi(
                        data = formatarDiaMes(data),
                        nome = nome,
                        status = registro.status.orEmpty(),
                        categoria = "exercicio"
                    )
                }

            medicamentos + exercicios
        }
    }

    private fun dividirMesEmQuatroPeriodos(diasDoMes: List<String>): List<List<String>> {
        if (diasDoMes.isEmpty()) return List(4) { emptyList() }

        return (0 until 4).map { index ->
            val inicio = index * diasDoMes.size / 4
            val fim = (index + 1) * diasDoMes.size / 4
            diasDoMes.subList(inicio, fim)
        }
    }

    private fun datasDoMes(mesReferencia: Calendar): List<String> {
        val calendar = mesReferencia.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val ultimoDia = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val datas = mutableListOf<String>()

        for (dia in 1..ultimoDia) {
            calendar.set(Calendar.DAY_OF_MONTH, dia)
            datas.add(formatter.format(calendar.time))
        }

        return datas
    }

    private fun datasRecentes(mesReferencia: Calendar, diasDoMes: List<String>): List<String> {
        val mesAtual = Calendar.getInstance()
        val mesmoMesAtual = mesReferencia.get(Calendar.YEAR) == mesAtual.get(Calendar.YEAR) &&
            mesReferencia.get(Calendar.MONTH) == mesAtual.get(Calendar.MONTH)

        return if (mesmoMesAtual) {
            listOf(dataAtualIso(), dataRelativaIso(-1))
        } else {
            diasDoMes.takeLast(2).asReversed()
        }
    }

    private fun dataAtualIso(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }

    private fun dataRelativaIso(dias: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, dias)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun formatarDiaMes(data: String): String {
        val partes = data.split("-")
        return if (partes.size == 3) "${partes[2]}/${partes[1]}" else data
    }

    private fun extrairDataIso(dataHora: String?): String {
        return dataHora
            .orEmpty()
            .substringBefore("T")
            .substringBefore(" ")
    }

    private fun mesAno(mesReferencia: Calendar): String {
        val texto = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("pt-BR"))
            .format(mesReferencia.time)
        return texto.replaceFirstChar { it.uppercase() }
    }
}

class HistoricoViewModelFactory(private val authApi: AuthApi) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoricoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoricoViewModel(authApi) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
