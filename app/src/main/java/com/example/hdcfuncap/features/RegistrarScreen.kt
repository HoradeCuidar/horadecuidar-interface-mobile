package com.example.hdcfuncap.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.components.HdcBottomBar
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.storage.UserPreferences

@Composable
fun RegistrarScreen(
    onNavigate: (String) -> Unit,
    viewModel: RegistrarViewModel
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val pacienteId by userPreferences.pacienteId.collectAsState(initial = null)
    val medicamentos by viewModel.medicamentos.collectAsState()
    val exercicios by viewModel.exercicios.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var exercicioSelecionado by remember { mutableStateOf<ExercicioRegistroUi?>(null) }
    var statusSelecionado by remember { mutableStateOf("REALIZADO") }

    LaunchedEffect(pacienteId) {
        pacienteId?.let { id ->
            viewModel.carregarDados(id)
        }
    }

    val corFundo = Color(0xFFF8F9FA)
    val corTextoPrincipal = Color(0xFF1E293B)

    Scaffold(
        bottomBar = { HdcBottomBar(currentScreen = "registrar", onNavigate = onNavigate) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(corFundo)
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp)
        ) {
            item {
                Text(text = "Registrar agora", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = corTextoPrincipal)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "O que você fez agora?", fontSize = 16.sp, color = Color(0xFF757575))
                Spacer(modifier = Modifier.height(24.dp))
            }

            errorMessage?.let { message ->
                item {
                    Text(
                        text = message,
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            if (isLoading) {
                item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
            } else {
                item {
                    RegistroSectionHeader(
                        title = "Prescrições de hoje",
                        subtitle = "Medicamentos previstos para o seu tratamento"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (medicamentos.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum medicamento previsto para hoje.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 18.dp)
                        )
                    }
                }

                items(medicamentos) { med ->
                    RegistroMedicamentoCard(
                        medicamento = med,
                        onRegistrar = { status ->
                            pacienteId?.let { id ->
                                viewModel.registrarAdesao(
                                    medicamento = med,
                                    status = status,
                                    pacienteId = id
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    RegistroSectionHeader(
                        title = "Recomendações de exercícios",
                        subtitle = "Orientações funcionais para registrar quando realizar"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (exercicios.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhuma orientação de exercício disponível.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                items(exercicios) { exercicio ->
                    RegistroExercicioCard(
                        exercicio = exercicio,
                        onRegistrar = { status ->
                            exercicioSelecionado = exercicio
                            statusSelecionado = status
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    exercicioSelecionado?.let { exercicio ->
        RegistroExercicioDialog(
            exercicio = exercicio,
            status = statusSelecionado,
            onDismiss = { exercicioSelecionado = null },
            onConfirm = { duracao, sensacao, observacao ->
                pacienteId?.let { id ->
                    viewModel.registrarExercicio(
                        pacienteId = id,
                        exercicio = exercicio,
                        status = statusSelecionado,
                        duracaoRealizadaMinutos = duracao,
                        sensacaoFinal = sensacao,
                        observacao = observacao
                    )
                }
                exercicioSelecionado = null
            }
        )
    }
}

@Composable
private fun RegistroSectionHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = Color(0xFF6B7280),
            lineHeight = 18.sp
        )
    }
}

private data class SensacaoOption(
    val value: String,
    val label: String
)

private val sensacoesFinais = listOf(
    SensacaoOption("ME_SUPEREI", "Me superei"),
    SensacaoOption("BEM_FORTE", "Bem forte"),
    SensacaoOption("DE_BOA", "De boa"),
    SensacaoOption("ARRASTADO", "Arrastado"),
    SensacaoOption("QUASE_NAO_DEU", "Quase não deu")
)

@Composable
private fun RegistroExercicioDialog(
    exercicio: ExercicioRegistroUi,
    status: String,
    onDismiss: () -> Unit,
    onConfirm: (Int?, String?, String?) -> Unit
) {
    val azulHdc = Color(0xFF6B9DFE)
    var duracao by remember(exercicio.orientacao.id, status) {
        mutableStateOf(exercicio.registroHoje?.duracaoRealizadaMinutos?.toString().orEmpty())
    }
    var sensacaoSelecionada by remember(exercicio.orientacao.id, status) {
        mutableStateOf(exercicio.registroHoje?.sensacaoFinal.orEmpty())
    }
    var observacao by remember(exercicio.orientacao.id, status) {
        mutableStateOf(exercicio.registroHoje?.observacao.orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        duracao.toIntOrNull(),
                        sensacaoSelecionada.ifBlank { null },
                        observacao.trim().ifBlank { null }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = azulHdc),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Salvar", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        title = {
            Text(
                text = if (status == "REALIZADO") "Exercício realizado" else "Exercício parcial",
                color = Color(0xFF1E293B),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = exercicio.orientacao.nome.orEmpty().ifBlank { "Exercício" },
                    color = Color(0xFF4B5563),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = duracao,
                    onValueChange = { value ->
                        duracao = value.filter { it.isDigit() }.take(3)
                    },
                    label = { Text("Duração em minutos") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = azulHdc,
                        focusedLabelColor = azulHdc,
                        cursorColor = azulHdc
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Como você se sentiu?",
                    color = Color(0xFF1E293B),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sensacoesFinais.forEach { sensacao ->
                        SensacaoFinalOption(
                            label = sensacao.label,
                            selected = sensacaoSelecionada == sensacao.value,
                            onClick = { sensacaoSelecionada = sensacao.value }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it.take(180) },
                    label = { Text("Observação opcional") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = azulHdc,
                        focusedLabelColor = azulHdc,
                        cursorColor = azulHdc
                    )
                )
            }
        },
        shape = RoundedCornerShape(18.dp),
        containerColor = Color.White
    )
}

@Composable
private fun SensacaoFinalOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) Color(0xFFEAF1FF) else Color(0xFFF8F9FA),
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = if (selected) Color(0xFF6B9DFE) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6B9DFE))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = Color(0xFF1E293B),
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun RegistroExercicioCard(
    exercicio: ExercicioRegistroUi,
    onRegistrar: (String) -> Unit
) {
    val status = exercicio.registroHoje?.status
    val isRealizado = status == "REALIZADO"
    val isParcial = status == "PARCIALMENTE_REALIZADO"

    val corCardFundo = when {
        isRealizado -> Color(0xFFE8F7ED)
        isParcial -> Color(0xFFFFF7E8)
        else -> Color.White
    }
    val corBorda = when {
        isRealizado -> Color(0xFFBCE3C5)
        isParcial -> Color(0xFFFFDFA8)
        else -> Color(0xFFE2E8F0)
    }
    val corStatus = when {
        isRealizado -> Color(0xFF57C47A)
        isParcial -> Color(0xFFE6A13A)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, corBorda, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = corCardFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = if (status != null) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFEAF8F0), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = Color(0xFF57C47A),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercicio.orientacao.nome.orEmpty().ifBlank { "Exercício" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = exercicio.orientacao.finalidade
                            .orEmpty()
                            .ifBlank { exercicio.orientacao.descricao.orEmpty() }
                            .ifBlank { "Orientação funcional" },
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 2
                    )
                }

                if (status != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(corStatus, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (status != null) {
                val detalhesRegistro = listOfNotNull(
                    exercicio.registroHoje?.duracaoRealizadaMinutos?.let { "$it min" },
                    formatarSensacaoFinal(exercicio.registroHoje?.sensacaoFinal)
                ).joinToString(" • ")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isRealizado) Color(0xFFE8F7ED) else Color(0xFFFFF2E0),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = corStatus,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isRealizado) "Realizado" else "Parcial",
                                    fontSize = 14.sp,
                                    color = corStatus,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (detalhesRegistro.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detalhesRegistro,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    if (isRealizado) {
                        OutlinedButton(
                            onClick = { onRegistrar("PARCIALMENTE_REALIZADO") },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Marcar parcial",
                                color = Color(0xFFE6A13A),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { onRegistrar("REALIZADO") },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DE39D)),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Marcar realizado",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRegistrar("PARCIALMENTE_REALIZADO") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Parcial", color = Color(0xFFE6A13A), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onRegistrar("REALIZADO") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DE39D))
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fiz", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatarSensacaoFinal(valor: String?): String? {
    return sensacoesFinais.firstOrNull { it.value == valor }?.label
}

@Composable
fun RegistroMedicamentoCard(
    medicamento: MedicamentoHojeResponse,
    onRegistrar: (String) -> Unit
) {
    val isFeito = medicamento.statusAdesaoHoje == "REALIZADO"
    val isNaoFeito = medicamento.statusAdesaoHoje == "NAO_REALIZADO"

    val corCardFundo = when {
        isFeito -> Color(0xFFE8F7ED)
        else -> Color.White
    }
    val corBorda = when {
        isFeito -> Color(0xFFBCE3C5)
        else -> Color(0xFFE2E8F0)
    }
    val statusDescricao = when {
        isFeito -> "Registrado"
        isNaoFeito -> "Não realizado"
        else -> "Ainda não registrado"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, corBorda, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = corCardFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFeito) 0.dp else 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFEDF4FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Medication, contentDescription = null, tint = Color(0xFF6B9DFE), modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = medicamento.nomeMedicamento, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Text(text = medicamento.dosagemFormatada, fontSize = 14.sp, color = Color.Gray)
                }
                if (isFeito) {
                    Box(modifier = Modifier.size(32.dp).background(Color(0xFF8DE39D), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isFeito || isNaoFeito) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                if (isFeito) Color(0xFFE8F7ED) else Color(0xFFF1F5F9),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isFeito) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = if (isFeito) Color(0xFF8DE39D) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = statusDescricao,
                                fontSize = 14.sp,
                                color = if (isFeito) Color(0xFF8DE39D) else Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isFeito) {
                        OutlinedButton(
                            onClick = { onRegistrar("NAO_REALIZADO") },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Desfazer",
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { onRegistrar("REALIZADO") },
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DE39D)),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Marcar tomado",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = statusDescricao,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onRegistrar("NAO_REALIZADO") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFFF6B6B),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Não tomei", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onRegistrar("REALIZADO") },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8DE39D))
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tomei", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
