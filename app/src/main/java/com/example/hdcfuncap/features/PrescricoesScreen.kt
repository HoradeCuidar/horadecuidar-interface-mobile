package com.example.hdcfuncap.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.components.HdcBottomBar
import com.example.hdcfuncap.network.ItemMedicacaoResponse
import com.example.hdcfuncap.network.OrientacaoFuncionalResponse
import com.example.hdcfuncap.network.PrescricaoMedicamentoResponse
import com.example.hdcfuncap.storage.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class MedicacaoComPrescricao(
    val item: ItemMedicacaoResponse,
    val prescricao: PrescricaoMedicamentoResponse
)

private enum class TelaPrescricoes {
    CATEGORIAS,
    MEDICAMENTOS,
    EXERCICIOS
}

@Composable
fun PrescricoesScreen(
    onNavigate: (String) -> Unit,
    viewModel: PrescricoesViewModel
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val pacienteId by userPreferences.pacienteId.collectAsState(initial = null)

    val prescricoes by viewModel.prescricoesMedicamentos.collectAsState()
    val orientacoes by viewModel.orientacoesFuncionais.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingOrientacoes by viewModel.isLoadingOrientacoes.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val errorMessageOrientacoes by viewModel.errorMessageOrientacoes.collectAsState()
    var telaAtual by remember { mutableStateOf(TelaPrescricoes.CATEGORIAS) }

    LaunchedEffect(pacienteId) {
        pacienteId?.let { viewModel.carregarPrescricoesMedicamentos(it) }
        viewModel.carregarOrientacoesFuncionais()
    }

    val medicacoes = remember(prescricoes) {
        prescricoes.flatMap { prescricao ->
            prescricao.itensPrescricao().map { item ->
                MedicacaoComPrescricao(item = item, prescricao = prescricao)
            }
        }
    }

    Scaffold(
        bottomBar = {
            HdcBottomBar(
                currentScreen = "prescricoes",
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        when (telaAtual) {
            TelaPrescricoes.MEDICAMENTOS -> {
                MedicamentosPrescritosContent(
                    innerPadding = innerPadding,
                    medicacoes = medicacoes,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    onBack = { telaAtual = TelaPrescricoes.CATEGORIAS }
                )
            }

            TelaPrescricoes.EXERCICIOS -> {
                ExerciciosPrescritosContent(
                    innerPadding = innerPadding,
                    orientacoes = orientacoes,
                    isLoading = isLoadingOrientacoes,
                    errorMessage = errorMessageOrientacoes,
                    onBack = { telaAtual = TelaPrescricoes.CATEGORIAS },
                    onRegistrar = { onNavigate("registrar") }
                )
            }

            TelaPrescricoes.CATEGORIAS -> {
                PrescricoesCategoriasContent(
                    innerPadding = innerPadding,
                    totalMedicamentos = medicacoes.size,
                    totalExercicios = orientacoes.size,
                    nomeProfissional = prescricoes.firstOrNull()?.nomeProfissional,
                    isLoading = isLoading || isLoadingOrientacoes,
                    errorMessage = listOfNotNull(errorMessage, errorMessageOrientacoes)
                        .joinToString("\n")
                        .ifBlank { null },
                    onOpenMedicamentos = { telaAtual = TelaPrescricoes.MEDICAMENTOS },
                    onOpenExercicios = { telaAtual = TelaPrescricoes.EXERCICIOS }
                )
            }
        }
    }
}

@Composable
private fun PrescricoesCategoriasContent(
    innerPadding: PaddingValues,
    totalMedicamentos: Int,
    totalExercicios: Int,
    nomeProfissional: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenMedicamentos: () -> Unit,
    onOpenExercicios: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(innerPadding)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 40.dp, bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Minhas Prescrições",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Prescrições ativas do seu tratamento",
                fontSize = 13.sp,
                color = Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(18.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6B9DFE))
                }
            }
        } else {
            item {
                CategoriaPrescricaoCard(
                    icon = Icons.Outlined.Medication,
                    iconTint = Color(0xFF6B9DFE),
                    iconBackground = Color(0xFFEAF1FF),
                    title = "Medicação",
                    subtitle = "$totalMedicamentos medicamentos ativos",
                    enabled = true,
                    onClick = onOpenMedicamentos
                )

                Spacer(modifier = Modifier.height(12.dp))

                CategoriaPrescricaoCard(
                    icon = Icons.Outlined.Restaurant,
                    iconTint = Color(0xFFF4A261),
                    iconBackground = Color(0xFFFFF2E8),
                    title = "Alimentação",
                    subtitle = "Em breve",
                    enabled = false,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(12.dp))

                CategoriaPrescricaoCard(
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    iconTint = Color(0xFF6FCF97),
                    iconBackground = Color(0xFFEAF8F0),
                    title = "Exercícios",
                    subtitle = "$totalExercicios orientações ativas",
                    enabled = true,
                    onClick = onOpenExercicios
                )

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = Color(0xFFC62828),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEAF1FF), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = if (nomeProfissional.isNullOrBlank()) {
                        "Suas prescrições são atualizadas pelo seu médico. Em caso de dúvida, entre em contato."
                    } else {
                        "Suas prescrições são atualizadas por Dr. $nomeProfissional. Em caso de dúvida, entre em contato."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF1E293B),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun CategoriaPrescricaoCard(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFF1E293B),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF6B7280),
                    fontSize = 13.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (enabled) Color(0xFF6B7280) else Color(0xFFD0D5DD)
            )
        }
    }
}

@Composable
private fun MedicamentosPrescritosContent(
    innerPadding: PaddingValues,
    medicacoes: List<MedicacaoComPrescricao>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(innerPadding)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF1E293B)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Medicação",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = resumoPeriodo(medicacoes),
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6B9DFE))
                }
            }
        } else if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    color = Color(0xFFC62828),
                    fontSize = 14.sp
                )
            }
        } else if (medicacoes.isEmpty()) {
            item {
                Text(
                    text = "Você ainda não possui medicamentos prescritos.",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
            }
        } else {
            items(medicacoes) { medicacao ->
                MedicacaoPrescritaCard(medicacao)
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun ExerciciosPrescritosContent(
    innerPadding: PaddingValues,
    orientacoes: List<OrientacaoFuncionalResponse>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onRegistrar: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(innerPadding)
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 28.dp, bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color(0xFF1E293B)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Exercícios",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = "${orientacoes.size} orientações ativas",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6B9DFE))
                }
            }
        } else if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage,
                    color = Color(0xFFC62828),
                    fontSize = 14.sp
                )
            }
        } else if (orientacoes.isEmpty()) {
            item {
                Text(
                    text = "Você ainda não possui orientações de exercícios.",
                    color = Color(0xFF6B7280),
                    fontSize = 14.sp
                )
            }
        } else {
            items(orientacoes) { orientacao ->
                ExercicioOrientacaoCard(
                    orientacao = orientacao,
                    onRegistrar = onRegistrar
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun ExercicioOrientacaoCard(
    orientacao: OrientacaoFuncionalResponse,
    onRegistrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = orientacao.nome.orEmpty().ifBlank { "Exercício" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            val descricao = orientacao.descricao.orEmpty()
            if (descricao.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = descricao,
                    fontSize = 14.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 19.sp
                )
            }

            val finalidade = orientacao.finalidade.orEmpty()
            if (finalidade.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                InfoMedicamentoRow(
                    icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
                    text = finalidade
                )
            }

            val tags = orientacao.tags.orEmpty()
                .mapNotNull { it.nome }
                .filter { it.isNotBlank() }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = tags.joinToString(" • "),
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEAF8F0), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Orientação ativa",
                        color = Color(0xFF4CAF78),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onRegistrar,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9DFE))
                ) {
                    Text(
                        text = "Registrar",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MedicacaoPrescritaCard(medicacao: MedicacaoComPrescricao) {
    val item = medicacao.item
    val prescricao = medicacao.prescricao

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.nomeMedicamento.orEmpty().ifBlank { "Medicamento" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            InfoMedicamentoRow(
                icon = Icons.Outlined.Medication,
                text = item.dosagemFormatada
                    .orEmpty()
                    .ifBlank { formatarDosagemItem(item) }
                    .ifBlank { "Dosagem não informada" }
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoMedicamentoRow(
                icon = Icons.Outlined.Schedule,
                text = item.frequencia
                    .orEmpty()
                    .ifBlank { formatarFrequenciaItem(item) }
                    .ifBlank { "Frequência não informada" }
            )

            Spacer(modifier = Modifier.height(10.dp))

            InfoMedicamentoRow(
                icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                text = formatarViaItem(item.viaAdministracao).orEmpty().ifBlank { "Via não informada" }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .background(Color(0xFFE8F7ED), RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Prescrição ativa",
                    color = Color(0xFF57C47A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InfoMedicamentoRow(
    icon: ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(Color(0xFFF2F5FA), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(17.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            color = Color(0xFF1E293B),
            fontSize = 14.sp
        )
    }
}

private fun PrescricaoMedicamentoResponse.itensPrescricao(): List<ItemMedicacaoResponse> {
    return itens ?: medicacoes ?: emptyList()
}

private fun formatarDosagemItem(item: ItemMedicacaoResponse): String {
    val valor = item.dosagemValor ?: return ""
    val unidade = item.dosagemUnidade?.lowercase().orEmpty()
    return "${formatarNumero(valor)} $unidade".trim()
}

private fun formatarFrequenciaItem(item: ItemMedicacaoResponse): String {
    val doses = item.quantidadeDoses ?: return ""
    val intervalo = item.intervaloValor ?: 1
    val tipo = item.intervaloTipo ?: return ""
    val vez = if (doses == 1) "vez" else "vezes"

    val unidadeTempo = when (tipo.uppercase()) {
        "HORA" -> if (intervalo == 1) "hora" else "horas"
        "DIA" -> if (intervalo == 1) "dia" else "dias"
        "SEMANA" -> if (intervalo == 1) "semana" else "semanas"
        "MES" -> if (intervalo == 1) "mês" else "meses"
        else -> tipo.lowercase()
    }

    return if (intervalo == 1 && tipo.uppercase() != "HORA") {
        val preposicao = if (tipo.uppercase() == "DIA") "ao" else "por"
        "$doses $vez $preposicao $unidadeTempo"
    } else {
        "$doses $vez a cada $intervalo $unidadeTempo"
    }
}

private fun formatarViaItem(via: String?): String? {
    return when (via) {
        null -> null
        "ORAL" -> "Via oral"
        "SUBLINGUAL" -> "Via sublingual"
        "INJETAVEL" -> "Via injetável"
        "TOPICA" -> "Via tópica"
        "INALATORIA" -> "Via inalatória"
        else -> via
    }
}

private fun formatarNumero(valor: Double): String {
    return if (valor % 1.0 == 0.0) valor.toInt().toString() else valor.toString()
}

private fun resumoPeriodo(medicacoes: List<MedicacaoComPrescricao>): String {
    val primeiraPrescricao = medicacoes.firstOrNull()?.prescricao
        ?: return "Prescrições ativas"

    val inicio = formatarData(primeiraPrescricao.dataInicio)
    val fim = formatarData(primeiraPrescricao.dataFim)

    return if (inicio.isNotBlank() && fim.isNotBlank()) {
        "Prescrição ativa • $inicio a $fim"
    } else {
        "Prescrição ativa"
    }
}

private fun formatarData(valor: String?): String {
    if (valor.isNullOrBlank()) return ""

    val limpo = valor.trim()
    if (limpo.all { it.isDigit() }) {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"))
                .format(Date(limpo.toLong()))
        } catch (e: Exception) {
            ""
        }
    }

    val data = limpo.substringBefore("T").substringBefore(" ")
    val partes = data.split("-")
    return if (partes.size == 3) {
        "${partes[2]}/${partes[1]}/${partes[0]}"
    } else {
        data
    }
}
