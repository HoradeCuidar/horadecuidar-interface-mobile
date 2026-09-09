package com.example.hdcfuncap.features

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.network.PacienteProfileResponse
import com.example.hdcfuncap.network.PacienteProfileUpdateRequest

@Composable
fun PerfilEditarScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: PerfilViewModel
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var telefone by rememberSaveable { mutableStateOf("") }
    var genero by rememberSaveable { mutableStateOf("MASCULINO") }
    var dataDeNascimento by rememberSaveable { mutableStateOf("") }
    var rua by rememberSaveable { mutableStateOf("") }
    var numeroDaCasa by rememberSaveable { mutableStateOf("") }
    var bairro by rememberSaveable { mutableStateOf("") }
    var cidade by rememberSaveable { mutableStateOf("") }
    var estado by rememberSaveable { mutableStateOf("") }
    var formError by rememberSaveable { mutableStateOf<String?>(null) }
    var formLoaded by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.carregarPerfil()
    }

    LaunchedEffect(profile?.id) {
        val paciente = profile
        if (paciente != null && !formLoaded) {
            nome = paciente.nome.orEmpty()
            email = paciente.email.orEmpty()
            telefone = paciente.telefone.orEmpty()
            genero = paciente.genero ?: "MASCULINO"
            dataDeNascimento = paciente.dataDeNascimento?.substringBefore("T").orEmpty()
            rua = paciente.rua.orEmpty()
            numeroDaCasa = paciente.numeroDaCasa.orEmpty()
            bairro = paciente.bairro.orEmpty()
            cidade = paciente.cidade.orEmpty()
            estado = paciente.estado.orEmpty()
            formLoaded = true
        }
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
                        text = "Editar perfil",
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
                            EditHeaderCard(paciente)
                        }

                        item {
                            SectionTitle("INFORMAÇÕES PESSOAIS")
                            Spacer(modifier = Modifier.height(8.dp))
                            EditFormCard {
                                EditTextField(Icons.Outlined.Person, "Nome", nome) { nome = it }
                                EditTextField(Icons.Outlined.Email, "E-mail", email, KeyboardType.Email) { email = it }
                                EditTextField(Icons.Outlined.Phone, "Telefone", telefone, KeyboardType.Phone) { telefone = it }
                                EditTextField(
                                    icon = Icons.Outlined.CalendarMonth,
                                    label = "Data de nascimento",
                                    value = dataDeNascimento,
                                    keyboardType = KeyboardType.Number,
                                    placeholder = "yyyy-MM-dd",
                                    onValueChange = { dataDeNascimento = it }
                                )
                                GenderSelector(selected = genero, onSelected = { genero = it })
                            }
                        }

                        item {
                            SectionTitle("ENDEREÇO")
                            Spacer(modifier = Modifier.height(8.dp))
                            EditFormCard {
                                EditTextField(Icons.Outlined.Home, "Rua", rua) { rua = it }
                                EditTextField(Icons.Outlined.Home, "Número", numeroDaCasa) { numeroDaCasa = it }
                                EditTextField(Icons.Outlined.Home, "Bairro", bairro) { bairro = it }
                                EditTextField(Icons.Outlined.Home, "Cidade", cidade) { cidade = it }
                                EditTextField(Icons.Outlined.Home, "Estado", estado) { estado = it.uppercase().take(2) }
                            }
                        }

                        if (formError != null || errorMessage != null) {
                            item {
                                Text(
                                    text = formError ?: errorMessage.orEmpty(),
                                    color = Color(0xFFC62828),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            Button(
                                onClick = {
                                    val request = buildUpdateRequest(
                                        nome = nome,
                                        email = email,
                                        telefone = telefone,
                                        genero = genero,
                                        dataDeNascimento = dataDeNascimento,
                                        rua = rua,
                                        bairro = bairro,
                                        estado = estado,
                                        cidade = cidade,
                                        numeroDaCasa = numeroDaCasa
                                    )

                                    if (request == null) {
                                        formError = "Preencha nome, e-mail, telefone, gênero e nascimento corretamente."
                                    } else {
                                        formError = null
                                        viewModel.atualizarPerfil(request, onSuccess = onSaved)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B9DFE))
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Salvar alterações",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditHeaderCard(paciente: PacienteProfileResponse) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(Color(0xFFEAF1FF), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFF6B9DFE), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialsFromName(paciente.nome.orEmpty()),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = paciente.nome.orEmpty().ifBlank { "Paciente" },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun EditFormCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun EditTextField(
    icon: ImageVector,
    label: String,
    value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF6B9DFE)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF6B9DFE),
            focusedLabelColor = Color(0xFF6B9DFE),
            cursorColor = Color(0xFF6B9DFE)
        )
    )
}

@Composable
private fun GenderSelector(
    selected: String,
    onSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "Gênero",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF6B7280)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GenderOption("MASCULINO", "Masculino", selected, onSelected)
            GenderOption("FEMININO", "Feminino", selected, onSelected)
            GenderOption("OUTRO", "Outro", selected, onSelected)
        }
    }
}

@Composable
private fun RowScope.GenderOption(
    value: String,
    label: String,
    selected: String,
    onSelected: (String) -> Unit
) {
    val isSelected = selected == value

    Box(
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .background(if (isSelected) Color(0xFFEAF1FF) else Color.White, RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF6B9DFE) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onSelected(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF6B9DFE) else Color(0xFF6B7280)
        )
    }
}

private fun buildUpdateRequest(
    nome: String,
    email: String,
    telefone: String,
    genero: String,
    dataDeNascimento: String,
    rua: String,
    bairro: String,
    estado: String,
    cidade: String,
    numeroDaCasa: String
): PacienteProfileUpdateRequest? {
    val telefoneLimpo = telefone.filter { it.isDigit() }
    val dataLimpa = dataDeNascimento.trim()
    val dataRegex = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    if (
        nome.isBlank() ||
        email.isBlank() ||
        telefoneLimpo.length !in 10..11 ||
        genero.isBlank() ||
        !dataRegex.matches(dataLimpa)
    ) {
        return null
    }

    return PacienteProfileUpdateRequest(
        nome = nome.trim(),
        email = email.trim(),
        telefone = telefoneLimpo,
        genero = genero,
        dataDeNascimento = dataLimpa,
        rua = rua.trim().ifBlank { null },
        bairro = bairro.trim().ifBlank { null },
        estado = estado.trim().ifBlank { null },
        cidade = cidade.trim().ifBlank { null },
        numeroDaCasa = numeroDaCasa.trim().ifBlank { null }
    )
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
