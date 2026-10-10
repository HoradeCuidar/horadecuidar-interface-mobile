package com.example.hdcfuncap.features

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.network.OcorrenciaRefeicaoResponse
import com.example.hdcfuncap.network.StatusRefeicao
import com.example.hdcfuncap.network.opcoesRegistraveis
import com.example.hdcfuncap.network.statusRefeicao
import com.example.hdcfuncap.network.resumoAlimento

@Composable
internal fun RegistroRefeicaoCard(
    ocorrencia: OcorrenciaRefeicaoResponse,
    podeRegistrar: Boolean,
    salvando: Boolean,
    onRegistrar: () -> Unit
) {
    val realizada = ocorrencia.statusRefeicao == StatusRefeicao.REALIZADO
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (realizada) Color(0xFFE8F7ED) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color(0xFFFFF0E3), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color(0xFFB86629))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        ocorrencia.refeicao?.nome?.takeIf { it.isNotBlank() } ?: "Refeição",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
                    )
                    Text(
                        when (ocorrencia.statusRefeicao) {
                            StatusRefeicao.REALIZADO -> "Realizada"
                            StatusRefeicao.NAO_REALIZADO -> "Não realizada"
                            StatusRefeicao.PENDENTE -> "Ainda não registrada"
                            else -> "Status indisponível"
                        },
                        fontSize = 13.sp,
                        color = if (realizada) Color(0xFF278849) else Color(0xFF64748B)
                    )
                }
            }
            if (realizada) {
                val opcao = ocorrencia.opcao
                Text(
                    opcao?.descricao?.takeIf { it.isNotBlank() }
                        ?: opcao?.ordem?.let { "Opção $it registrada" } ?: "Opção registrada",
                    fontSize = 14.sp, color = Color(0xFF475569)
                )
            }
            if (!podeRegistrar && !salvando) {
                Text("Registro indisponível. Atualize a tela.", fontSize = 13.sp, color = Color(0xFF64748B))
            }
            Button(
                onClick = onRegistrar,
                enabled = podeRegistrar && !salvando,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9DFE))
            ) {
                if (salvando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(if (realizada) Icons.Outlined.Edit else Icons.Default.Check, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (salvando) "Salvando..." else if (realizada) "Editar registro" else "Registrar refeição")
            }
        }
    }
}

@Composable
internal fun RegistroRefeicaoDialog(
    ocorrencia: OcorrenciaRefeicaoResponse,
    salvando: Boolean,
    enabled: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Long, String?) -> Unit
) {
    val opcoes = ocorrencia.opcoesRegistraveis()
    var opcaoId by rememberSaveable(ocorrencia.id) {
        mutableStateOf(ocorrencia.opcao?.id?.takeIf { id -> opcoes.any { it.id == id } } ?: opcoes.singleOrNull()?.id)
    }
    var observacao by rememberSaveable(ocorrencia.id) { mutableStateOf(ocorrencia.observacao.orEmpty()) }
    AlertDialog(
        onDismissRequest = { if (!salvando) onDismiss() },
        title = {
            Text(ocorrencia.refeicao?.nome?.takeIf { it.isNotBlank() } ?: "Registrar refeição")
        },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ocorrencia.refeicao?.observacoes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 13.sp, color = Color(0xFF64748B))
                }
                Text("Opção consumida", fontWeight = FontWeight.SemiBold, color = Color(0xFF1E293B))
                opcoes.forEachIndexed { index, opcao ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !salvando && enabled) { opcaoId = opcao.id },
                        verticalAlignment = Alignment.Top
                    ) {
                        RadioButton(
                            selected = opcaoId == opcao.id,
                            onClick = { opcaoId = opcao.id },
                            enabled = !salvando && enabled,
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF6B9DFE))
                        )
                        Column(Modifier.weight(1f).padding(top = 12.dp)) {
                            Text(
                                opcao.descricao?.takeIf { it.isNotBlank() } ?: "Opção ${opcao.ordem ?: index + 1}",
                                fontWeight = FontWeight.SemiBold
                            )
                            opcao.alimentos.orEmpty().forEach { alimento ->
                                Text(alimento.resumoAlimento(), fontSize = 13.sp, color = Color(0xFF64748B))
                                alimento.observacao?.takeIf { it.isNotBlank() }?.let {
                                    Text(it, fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }
                OutlinedTextField(
                    value = observacao,
                    onValueChange = { observacao = it.take(180) },
                    label = { Text("Observação (opcional)") },
                    enabled = !salvando && enabled,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
                errorMessage?.let { Text(it, fontSize = 13.sp, color = Color(0xFFC62828)) }
            }
        },
        confirmButton = {
            Button(
                onClick = { opcaoId?.let { onConfirm(it, observacao.ifBlank { null }) } },
                enabled = !salvando && enabled && opcoes.any { it.id == opcaoId },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9DFE))
            ) {
                if (salvando) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (salvando) "Salvando..." else "Salvar registro")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !salvando) { Text("Cancelar") }
        }
    )
}
