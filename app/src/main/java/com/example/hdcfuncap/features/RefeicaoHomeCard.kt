package com.example.hdcfuncap.features

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.network.OcorrenciaRefeicaoResponse
import com.example.hdcfuncap.network.StatusRefeicao
import com.example.hdcfuncap.network.statusRefeicao
import com.example.hdcfuncap.network.resumoAlimento

@Composable
internal fun RefeicaoHomeCard(
    ocorrencia: OcorrenciaRefeicaoResponse,
    inicialmenteExpandido: Boolean = false
) {
    val refeicao = ocorrencia.refeicao ?: return
    var expandido by rememberSaveable(ocorrencia.id) { mutableStateOf(inicialmenteExpandido) }
    val status = ocorrencia.statusRefeicao
    val corStatus = when (status) {
        StatusRefeicao.REALIZADO -> Color(0xFF278849)
        StatusRefeicao.NAO_REALIZADO -> Color(0xFFC62828)
        StatusRefeicao.PENDENTE -> Color(0xFF6B9DFE)
        else -> Color(0xFF64748B)
    }
    val textoStatus = when (status) {
        StatusRefeicao.REALIZADO -> "Realizada"
        StatusRefeicao.NAO_REALIZADO -> "Não realizada"
        StatusRefeicao.PENDENTE -> "Pendente"
        StatusRefeicao.CANCELADO -> "Cancelada"
        StatusRefeicao.DESCONHECIDO -> "Status indisponível"
    }
    val corFundoStatus = when (status) {
        StatusRefeicao.REALIZADO -> Color(0xFFE6F4EC)
        StatusRefeicao.NAO_REALIZADO -> Color(0xFFFFE5E5)
        StatusRefeicao.PENDENTE -> Color.Transparent
        else -> Color(0xFFF1F5F9)
    }
    val opcoes = refeicao.opcoes.orEmpty().sortedBy { it.ordem ?: Int.MAX_VALUE }

    Card(
        onClick = { expandido = !expandido },
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFFFF0E3), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color(0xFFB86629))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = refeicao.nome?.takeIf { it.isNotBlank() } ?: "Refeição",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                    val resumo = ocorrencia.opcao?.descricao?.takeIf { it.isNotBlank() }
                        ?: when {
                            ocorrencia.opcao != null -> "Opção registrada"
                            opcoes.isEmpty() -> "Sem opções informadas"
                            opcoes.size == 1 -> "1 opção de refeição"
                            else -> "${opcoes.size} opções de refeição"
                        }
                    Text(resumo, fontSize = 12.sp, color = Color(0xFF757575))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .widthIn(max = 132.dp)
                        .background(corFundoStatus, RoundedCornerShape(16.dp))
                        .then(
                            if (status == StatusRefeicao.PENDENTE) {
                                Modifier.border(1.dp, corStatus, RoundedCornerShape(16.dp))
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(textoStatus, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = corStatus)
                }
            }

            if (expandido) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                refeicao.observacoes?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, fontSize = 14.sp, color = Color(0xFF757575))
                }
                opcoes.forEachIndexed { index, opcao ->
                    Spacer(Modifier.height(12.dp))
                    val registrada = opcao.id != null && opcao.id == ocorrencia.opcao?.id
                    Text(
                        text = "Opção ${opcao.ordem ?: index + 1}" + if (registrada) " · Registrada" else "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    )
                    opcao.descricao?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 14.sp, color = Color(0xFF757575))
                    }
                    opcao.alimentos.orEmpty().forEach { alimento ->
                        Text(
                            text = alimento.resumoAlimento(),
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )
                        alimento.observacao?.takeIf { it.isNotBlank() }?.let {
                            Text(it, fontSize = 12.sp, color = Color(0xFF757575))
                        }
                    }
                }
                ocorrencia.observacao?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(12.dp))
                    Text("Observação do registro: $it", fontSize = 14.sp, color = Color(0xFF757575))
                }
            }
        }
    }
}
