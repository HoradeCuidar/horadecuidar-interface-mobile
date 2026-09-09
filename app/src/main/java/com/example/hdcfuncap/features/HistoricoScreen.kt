package com.example.hdcfuncap.features

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.hdcfuncap.components.HdcBottomBar
import com.example.hdcfuncap.components.HdcPullToRefresh
import com.example.hdcfuncap.storage.UserPreferences

@Composable
fun HistoricoScreen(
    onNavigate: (String) -> Unit,
    viewModel: HistoricoViewModel
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val pacienteId by userPreferences.pacienteId.collectAsState(initial = null)
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isInitialLoading = isLoading && uiState.mesAno.isBlank()

    LaunchedEffect(pacienteId) {
        pacienteId?.let { viewModel.carregarHistorico(it) }
    }

    Scaffold(
        bottomBar = {
            HdcBottomBar(
                currentScreen = "historico",
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        HdcPullToRefresh(
            isRefreshing = isLoading,
            onRefresh = {
                pacienteId?.let { viewModel.carregarHistorico(it, forceRefresh = true) }
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentPadding = PaddingValues(top = 40.dp, bottom = 32.dp)
            ) {
                item {
                    Text(
                        text = "Meu Histórico",
                        color = Color(0xFF1E293B),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (uiState.canCarregarMesAnterior) {
                            IconButton(
                                onClick = {
                                    pacienteId?.let { viewModel.carregarMesAnterior(it) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Mês anterior",
                                    tint = Color(0xFF1E293B)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(32.dp))
                        }

                        Text(
                            text = uiState.mesAno.ifBlank { "Mês atual" },
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (uiState.canCarregarProximoMes) {
                            IconButton(
                                onClick = {
                                    pacienteId?.let { viewModel.carregarProximoMes(it) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Voltar para mês mais recente",
                                    tint = Color(0xFF1E293B)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (isInitialLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF6B9DFE))
                        }
                    }
                } else {
                    errorMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = Color(0xFFC62828),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 14.dp)
                            )
                        }
                    }

                    item {
                        HistoricoGraficoCard(semanas = uiState.semanas)

                        Spacer(modifier = Modifier.height(22.dp))

                        Text(
                            text = "Registros recentes",
                            color = Color(0xFF1E293B),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    if (uiState.registrosRecentes.isEmpty()) {
                        item {
                            Text(
                                text = "Nenhum registro encontrado hoje ou ontem.",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        item {
                            RegistrosRecentesCard(registros = uiState.registrosRecentes)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoricoGraficoCard(semanas: List<HistoricoResumoSemanaUi>) {
    val dados = if (semanas.isEmpty()) {
        List(4) { index -> HistoricoResumoSemanaUi(label = "Sem ${index + 1}", medicamentosPercentual = 0) }
    } else {
        semanas
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EixoPercentual()

                dados.forEach { semana ->
                    SemanaBarGroup(semana = semana)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color(0xFFE2E8F0))

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendaItem("Remédios", Color(0xFF6B9DFE))
                LegendaItem("Alimentação", Color(0xFFF4A261), enabled = false)
                LegendaItem("Exercícios", Color(0xFF8DE39D))
            }
        }
    }
}

@Composable
private fun EixoPercentual() {
    Column(
        modifier = Modifier.height(112.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text("100%", color = Color(0xFF6B7280), fontSize = 11.sp)
        Text("50%", color = Color(0xFF6B7280), fontSize = 11.sp)
        Text("0%", color = Color(0xFF6B7280), fontSize = 11.sp)
    }
}

@Composable
private fun SemanaBarGroup(semana: HistoricoResumoSemanaUi) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.height(104.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            PercentBar(
                percent = semana.medicamentosPercentual,
                color = Color(0xFF6B9DFE)
            )
            PercentBar(
                percent = semana.alimentacaoPercentual ?: 0,
                color = Color(0xFFF4A261),
                muted = semana.alimentacaoPercentual == null
            )
            PercentBar(
                percent = semana.exerciciosPercentual ?: 0,
                color = Color(0xFF8DE39D),
                muted = semana.exerciciosPercentual == null
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = semana.label,
            color = Color(0xFF6B7280),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun PercentBar(
    percent: Int,
    color: Color,
    muted: Boolean = false
) {
    Box(
        modifier = Modifier
            .width(11.dp)
            .height(96.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF1F5F9)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .width(11.dp)
                .height(((percent.coerceIn(0, 100) / 100f) * 96).dp)
                .clip(RoundedCornerShape(50))
                .background(if (muted) color.copy(alpha = 0.18f) else color)
        )
    }
}

@Composable
private fun LegendaItem(
    label: String,
    color: Color,
    enabled: Boolean = true
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (enabled) color else color.copy(alpha = 0.28f), CircleShape)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = if (enabled) Color(0xFF6B7280) else Color(0xFF94A3B8),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun RegistrosRecentesCard(registros: List<HistoricoRegistroUi>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            registros.forEachIndexed { index, registro ->
                RegistroRecenteRow(registro = registro)
                if (index < registros.lastIndex) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }
            }
        }
    }
}

@Composable
private fun RegistroRecenteRow(registro: HistoricoRegistroUi) {
    val isRealizado = registro.status == "REALIZADO"
    val isParcial = registro.status == "PARCIALMENTE_REALIZADO"
    val iconBackground = when {
        isRealizado -> Color(0xFFE8F7ED)
        isParcial -> Color(0xFFFFF2E0)
        else -> Color(0xFFFFECEC)
    }
    val iconTint = when {
        isRealizado -> Color(0xFF8DE39D)
        isParcial -> Color(0xFFE6A13A)
        else -> Color(0xFFFF6B6B)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = registro.data,
            color = Color(0xFF6B7280),
            fontSize = 13.sp,
            modifier = Modifier.width(54.dp)
        )

        Text(
            text = registro.nome,
            color = Color(0xFF1E293B),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isRealizado || isParcial) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
