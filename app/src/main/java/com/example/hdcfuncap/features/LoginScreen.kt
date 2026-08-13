package com.example.hdcfuncap.features

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.R
import com.example.hdcfuncap.network.LoginRequest
import com.example.hdcfuncap.network.RetrofitClient

 import com.example.hdcfuncap.storage.UserPreferences
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit
) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
     val userPreferences = remember { UserPreferences(context) }
     val savedUser by userPreferences.savedUsername.collectAsState(initial = "")
     val isRemember by userPreferences.isRememberMeChecked.collectAsState(initial = false)
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var lembrarDeMim by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loginMessage by remember { mutableStateOf<String?>(null) }

    val corFundoInput = Color(0xFFEDF4FF)
    val corTextoCinza = Color(0xFF757575)
    val corBotao = Color(0xFF6B9BFE)


    LaunchedEffect(savedUser, isRemember) {
        if (savedUser.isNotEmpty()) {
            usuario = savedUser
        }
        lembrarDeMim = isRemember
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Image(
            painter = painterResource(id = R.drawable.hdc_second_logo),
            contentDescription = "Logo Hora de Cuidar",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Olá! Faça seu login",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Use as informações enviadas pelo seu médico",
            fontSize = 14.sp,
            color = corTextoCinza
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Usuário",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = corTextoCinza
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = usuario,
            onValueChange = { usuario = it },
            placeholder = { Text("Seu usuário", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = corFundoInput,
                unfocusedContainerColor = corFundoInput,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Senha",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = corTextoCinza
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextField(
            value = senha,
            onValueChange = { senha = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Sua senha", color = Color.Gray) },
            shape = RoundedCornerShape(12.dp),
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (senhaVisivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                val description = if (senhaVisivel) "Esconder senha" else "Mostrar senha"

                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                    Icon(imageVector = image, contentDescription = description, tint = corTextoCinza)
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = corFundoInput,
                unfocusedContainerColor = corFundoInput,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = lembrarDeMim,
                onCheckedChange = { lembrarDeMim = it }
            )
            Text(text = "Lembrar de mim", color = corTextoCinza, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        loginMessage?.let { message ->
            LoginFeedbackMessage(message = message)
            Spacer(modifier = Modifier.height(14.dp))
        }

        Button(
            enabled = !isLoading,
            onClick = {
                isLoading = true
                loginMessage = null

                coroutineScope.launch {
                    try {
                        val request = LoginRequest(username = usuario, senha = senha)
                        val response = RetrofitClient.getAuthApi(context).logar(request)
                        val token = response.token
                        val pacienteId = response.id

                        if (token.isNullOrBlank() || pacienteId == null) {
                            loginMessage = "Não foi possível validar sua sessão. Tente novamente."
                            return@launch
                        }

                        userPreferences.saveToken(token)
                        userPreferences.savePaciente(id = pacienteId, nome = usuario)
                        userPreferences.saveUser(username = usuario, remember = lembrarDeMim)

                        onLoginSuccess()
                    } catch (e: Exception) {
                        loginMessage = "Usuário ou senha inválidos. Confira os dados enviados pelo seu médico."
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = corBotao,
                disabledContainerColor = corBotao.copy(alpha = 0.7f),
                contentColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(text = "Entrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onForgotPassword,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Esqueci minha senha",
                color = corBotao,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun LoginFeedbackMessage(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFECEC), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            color = Color(0xFFE5484D),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp
        )
    }
}
