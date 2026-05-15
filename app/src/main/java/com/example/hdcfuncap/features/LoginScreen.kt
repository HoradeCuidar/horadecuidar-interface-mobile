package com.example.hdcfuncap.features

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.example.hdcfuncap.network.LoginRequest
import com.example.hdcfuncap.network.RetrofitClient

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onLoginSuccess: () -> Unit) {

    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    val corFundoInput = Color(0xFFEDF4FF)
    val corTextoCinza = Color(0xFF757575)
    val corBotao = Color(0xFF6B9BFE)
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

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

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            enabled = !isLoading,
            onClick = {
                isLoading = true

                coroutineScope.launch {
                    try {
                        val request = LoginRequest(username = usuario, senha = senha)
                        val response = RetrofitClient.authApi.logar(request)

                        Toast.makeText(context, "Sucesso!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Falha no login", Toast.LENGTH_SHORT).show()
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
                containerColor = androidx.compose.ui.graphics.Color(0xFF6699FF),
                disabledContainerColor = androidx.compose.ui.graphics.Color(0xFF6699FF).copy(alpha = 0.7f),
                contentColor = androidx.compose.ui.graphics.Color.White,
                disabledContentColor = androidx.compose.ui.graphics.Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(text = "Entrar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { },
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

