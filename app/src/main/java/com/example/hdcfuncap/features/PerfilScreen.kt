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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.components.HdcBottomBar
import com.example.hdcfuncap.network.PacienteProfileResponse

@Composable
fun PerfilScreen(
    onNavigate: (String) -> Unit,
    onOpenDetails: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
    viewModel: PerfilViewModel
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.carregarPerfil()
    }

    Scaffold(
        bottomBar = {
            HdcBottomBar(
                currentScreen = "perfil",
                onNavigate = onNavigate
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 40.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    text = "Meu Perfil",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            when {
                isLoading && profile == null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF6B9DFE))
                        }
                    }
                }

                errorMessage != null && profile == null -> {
                    item {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = Color(0xFFC62828),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    profile?.let { paciente ->
                        item {
                            ProfileHeaderCard(
                                paciente = paciente,
                                onClick = onOpenDetails,
                                onEditClick = onOpenEdit
                            )
                        }

                        item {
                            SectionTitle("INFORMAÇÕES")
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoCard(paciente)
                        }
                    }
                }
            }

            item {
                SectionTitle("AJUSTES")
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ProfileActionRow(
                            icon = Icons.Outlined.Notifications,
                            iconTint = Color(0xFF6B9DFE),
                            iconBackground = Color(0xFFEAF1FF),
                            label = "Lembretes",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFFF0F2F5))
                        ProfileActionRow(
                            icon = Icons.Outlined.History,
                            iconTint = Color(0xFF6B7280),
                            iconBackground = Color(0xFFF2F5FA),
                            label = "Histórico",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFFF0F2F5))
                        ProfileActionRow(
                            icon = Icons.Outlined.Lock,
                            iconTint = Color(0xFF6B7280),
                            iconBackground = Color(0xFFF2F5FA),
                            label = "Alterar senha",
                            onClick = {}
                        )
                        HorizontalDivider(color = Color(0xFFF0F2F5))
                        ProfileActionRow(
                            icon = Icons.Outlined.Settings,
                            iconTint = Color(0xFF6B9DFE),
                            iconBackground = Color(0xFFEAF1FF),
                            label = "Configurações",
                            onClick = onOpenSettings
                        )
                        HorizontalDivider(color = Color(0xFFF0F2F5))
                        ProfileActionRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            iconTint = Color(0xFFE5484D),
                            iconBackground = Color(0xFFFFECEC),
                            label = "Sair do aplicativo",
                            labelColor = Color(0xFFE5484D),
                            onClick = onLogout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    paciente: PacienteProfileResponse,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(162.dp)
            .clickable(onClick = onClick)
            .background(Color(0xFFEAF1FF), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color(0xFF6B9DFE), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsFromName(paciente.nome.orEmpty()),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(34.dp)
                        .background(Color(0xFF4E86F7), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Editar perfil",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = paciente.nome.orEmpty().ifBlank { "Paciente" },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
fun PerfilDetalhesScreen(
    onBack: () -> Unit,
    viewModel: PerfilViewModel
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.carregarPerfil()
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "Dados do perfil",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }
            }

            when {
                isLoading && profile == null -> {
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
                }

                errorMessage != null && profile == null -> {
                    item {
                        Text(
                            text = errorMessage.orEmpty(),
                            color = Color(0xFFC62828),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    profile?.let { paciente ->
                        item {
                            DetailsHeaderCard(paciente)
                        }

                        item {
                            SectionTitle("INFORMAÇÕES PESSOAIS")
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailsCard(
                                rows = listOf(
                                    DetailRowData(Icons.Outlined.Person, "Nome", paciente.nome.orEmpty()),
                                    DetailRowData(Icons.Outlined.Email, "E-mail", paciente.email.orEmpty()),
                                    DetailRowData(Icons.Outlined.Person, "Usuário", paciente.username.orEmpty()),
                                    DetailRowData(Icons.Outlined.Phone, "Telefone", paciente.telefone.orEmpty()),
                                    DetailRowData(Icons.Outlined.CalendarMonth, "Nascimento", formatDate(paciente.dataDeNascimento)),
                                    DetailRowData(Icons.Outlined.Person, "Gênero", formatGender(paciente.genero))
                                )
                            )
                        }

                        item {
                            SectionTitle("SAÚDE")
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailsCard(
                                rows = listOf(
                                    DetailRowData(
                                        icon = Icons.Outlined.MedicalInformation,
                                        title = "Condições",
                                        value = paciente.doencas
                                            .orEmpty()
                                            .joinToString(" • ") { it.nome.orEmpty() }
                                    ),
                                    DetailRowData(
                                        icon = Icons.AutoMirrored.Outlined.StickyNote2,
                                        title = "Observações",
                                        value = paciente.observacoes.orEmpty()
                                    )
                                )
                            )
                        }

                        item {
                            SectionTitle("ENDEREÇO")
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailsCard(
                                rows = listOf(
                                    DetailRowData(Icons.Outlined.Home, "Rua", paciente.rua.orEmpty()),
                                    DetailRowData(Icons.Outlined.Home, "Número", paciente.numeroDaCasa.orEmpty()),
                                    DetailRowData(Icons.Outlined.Home, "Bairro", paciente.bairro.orEmpty()),
                                    DetailRowData(Icons.Outlined.Home, "Cidade", paciente.cidade.orEmpty()),
                                    DetailRowData(Icons.Outlined.Home, "Estado", paciente.estado.orEmpty())
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsHeaderCard(paciente: PacienteProfileResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color(0xFFEAF1FF), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(Color(0xFF6B9DFE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsFromName(paciente.nome.orEmpty()),
                    color = Color.White,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = paciente.nome.orEmpty().ifBlank { "Paciente" },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

private data class DetailRowData(
    val icon: ImageVector,
    val title: String,
    val value: String
)

@Composable
private fun DetailsCard(rows: List<DetailRowData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            rows.forEachIndexed { index, row ->
                ProfileInfoRow(
                    icon = row.icon,
                    iconTint = Color(0xFF6B9DFE),
                    iconBackground = Color(0xFFEAF1FF),
                    title = row.title,
                    value = row.value.ifBlank { "Não informado" }
                )

                if (index < rows.lastIndex) {
                    HorizontalDivider(color = Color(0xFFF0F2F5))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF6B7280)
    )
}

@Composable
private fun InfoCard(paciente: PacienteProfileResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileInfoRow(
                icon = Icons.Outlined.MedicalInformation,
                iconTint = Color(0xFFE5484D),
                iconBackground = Color(0xFFFFECEC),
                title = "Condições de saúde",
                value = paciente.doencas
                    .orEmpty()
                    .joinToString(" • ") { it.nome.orEmpty() }
                    .ifBlank { "Não informadas" }
            )
            HorizontalDivider(color = Color(0xFFF0F2F5))
            ProfileInfoRow(
                icon = Icons.Outlined.CalendarMonth,
                iconTint = Color(0xFFF4A261),
                iconBackground = Color(0xFFFFF2E8),
                title = "Data de nascimento",
                value = formatDate(paciente.dataDeNascimento).ifBlank { "Não informada" }
            )
        }
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    label: String,
    labelColor: Color = Color(0xFF1E293B),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = if (labelColor == Color(0xFFE5484D)) Color(0xFFE5484D) else Color(0xFF6B7280)
        )
    }
}

private fun initialsFromName(name: String): String {
    val parts = name
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "P"
    }
}

private fun formatGender(value: String?): String {
    return when (value) {
        "MASCULINO" -> "Masculino"
        "FEMININO" -> "Feminino"
        "OUTRO" -> "Outro"
        else -> value.orEmpty()
    }
}

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return ""

    val data = value.substringBefore("T").substringBefore(" ")
    val partes = data.split("-")
    if (partes.size != 3) return value

    val dia = partes[2].toIntOrNull() ?: return value
    val mes = partes[1].toIntOrNull() ?: return value
    val ano = partes[0].toIntOrNull() ?: return value
    val meses = listOf(
        "janeiro",
        "fevereiro",
        "março",
        "abril",
        "maio",
        "junho",
        "julho",
        "agosto",
        "setembro",
        "outubro",
        "novembro",
        "dezembro"
    )

    return if (mes in 1..12) {
        "$dia de ${meses[mes - 1]} de $ano".replaceFirstChar { it.uppercase() }
    } else {
        value
    }
}
