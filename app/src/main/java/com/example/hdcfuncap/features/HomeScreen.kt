package com.example.hdcfuncap.features

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.components.HdcBottomBar
import com.example.hdcfuncap.network.MedicamentoHojeResponse
import com.example.hdcfuncap.storage.UserPreferences

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    val pacienteId by userPreferences.pacienteId.collectAsState(initial = null)
    val pacienteNome by userPreferences.pacienteNome.collectAsState(initial = "Paciente")

    val medicamentos by viewModel.medicamentos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(pacienteId) {
        pacienteId?.let { id ->
            android.util.Log.d("HDC_TESTE", "Buscando dados do paciente dinâmico: $id")
            viewModel.carregarDados(id)
        }
    }

    val dataAtual = remember {
        val calendario = java.util.Calendar.getInstance()
        val formatador = java.text.SimpleDateFormat(
            "EEEE, dd 'de' MMMM",
            java.util.Locale("pt", "BR")
        )
        formatador.format(calendario.time).replaceFirstChar { it.uppercase() }
    }

    val corFundo = Color(0xFFF8F9FA)
    val corTextoPrincipal = Color(0xFF1E293B)
    val corTextoSecundario = Color(0xFF757575)
    val azulHdc = Color(0xFF6B9DFE)

    Scaffold(
        bottomBar = {
            HdcBottomBar(
                currentScreen = "home",
                onNavigate = onNavigate
            )
        }
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
                Text(
                    text = "Bom dia, $pacienteNome!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = corTextoPrincipal
                )

                Text(
                    text = dataAtual,
                    fontSize = 14.sp,
                    color = corTextoSecundario
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(azulHdc, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Área do Dashboard Gráfico",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = azulHdc,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Remédios",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = azulHdc)
                    }
                }
            } else {
                items(medicamentos) { med ->
                    MedicamentoCard(medicamento = med)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = Color(0xFFF4A261),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Alimentação",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsWalk,
                        contentDescription = null,
                        tint = Color(0xFF8DE39D),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Exercícios",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = corTextoPrincipal
                    )
                }
            }
        }
    }
}

@Composable
fun MedicamentoCard(medicamento: MedicamentoHojeResponse) {
    val isFeito = medicamento.statusAdesaoHoje == "REALIZADO"
    val corStatus = if (isFeito) Color(0xFF8DE39D) else Color(0xFF6B9DFE)
    val iconeStatus = if (isFeito) Icons.Default.Check else Icons.Outlined.Schedule

    var expandido by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable {
                expandido = !expandido
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(corStatus, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconeStatus,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = medicamento.nomeMedicamento,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Text(
                        text = "${medicamento.dosagemFormatada} • ${medicamento.frequencia}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                if (isFeito) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF8DE39D), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Feito",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .border(1.dp, Color(0xFF6B9DFE), RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Pendente",
                            color = Color(0xFF6B9DFE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (expandido) {
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(
                    thickness = 1.dp,
                    color = Color(0xFFE2E8F0)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Detalhes da Prescrição",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Dosagem: ${medicamento.dosagemFormatada}",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Horário de tomar: ${medicamento.frequencia}",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}