package com.example.hdcfuncap.features

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

@Composable
fun RegistrarScreen(
    onNavigate: (String) -> Unit,
    viewModel: RegistrarViewModel
) {
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }
    val pacienteId by userPreferences.pacienteId.collectAsState(initial = null)

    val medicamentos by viewModel.medicamentos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

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

            if (isLoading) {
                item { CircularProgressIndicator(modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)) }
            } else {
                items(medicamentos) { med ->
                    RegistroMedicamentoCard(
                        medicamento = med,
                        onRegistrar = { status ->
                            pacienteId?.let { id ->
                                viewModel.registrarAdesao(
                                    itemId = med.itemId,
                                    adesaoId = med.adesaoId,
                                    status = status,
                                    observacao = med.observacao ?: "",
                                    pacienteId = id
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun RegistroMedicamentoCard(
    medicamento: MedicamentoHojeResponse,
    onRegistrar: (String?) -> Unit
) {
    val isFeito = medicamento.statusAdesaoHoje == "REALIZADO"
    val isNaoFeito = medicamento.statusAdesaoHoje == "NAO_REALIZADO"

    val corCardFundo = when {
        isFeito -> Color(0xFFE8F7ED)
        isNaoFeito -> Color(0xFFFFF0F0)
        else -> Color.White
    }
    val corBorda = when {
        isFeito -> Color(0xFFBCE3C5)
        isNaoFeito -> Color(0xFFFFD6D6)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, corBorda, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = corCardFundo),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFeito || isNaoFeito) 0.dp else 2.dp)
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
                } else if (isNaoFeito) {
                    Box(modifier = Modifier.size(32.dp).background(Color(0xFFFF6B6B), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isFeito) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isFeito) Color(0xFF8DE39D) else Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFeito) "Registrado" else "Marcado como não tomado",
                            fontSize = 14.sp,
                            color = if (isFeito) Color(0xFF8DE39D) else Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onRegistrar(null) }
                    ) {
                        Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Desfazer", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Button(
                        onClick = { onRegistrar("NAO_REALIZADO") },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE5E5))
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF6B6B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Não tomei", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}