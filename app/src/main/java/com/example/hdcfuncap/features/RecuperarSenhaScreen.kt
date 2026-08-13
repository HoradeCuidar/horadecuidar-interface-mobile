package com.example.hdcfuncap.features

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.R
import com.example.hdcfuncap.network.ResetarSenhaRequest
import com.example.hdcfuncap.network.RetrofitClient
import com.example.hdcfuncap.network.SolicitarRecuperacaoSenhaRequest
import kotlinx.coroutines.launch

@Composable
fun RecuperarSenhaScreen(
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var novaSenha by remember { mutableStateOf("") }
    var confirmacao by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var confirmacaoVisivel by remember { mutableStateOf(false) }
    var emailEnviado by remember { mutableStateOf(false) }
    var isEnviandoEmail by remember { mutableStateOf(false) }
    var isRedefinindoSenha by remember { mutableStateOf(false) }
    var mensagem by remember { mutableStateOf<String?>(null) }

    val corFundo = Color(0xFFF8F9FA)
    val corFundoInput = Color(0xFFEDF4FF)
    val corTextoPrincipal = Color(0xFF1E293B)
    val corTextoCinza = Color(0xFF757575)
    val corBotao = Color(0xFF6B9BFE)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(corFundo)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        IconButton(onClick = onBackToLogin) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar para o login",
                tint = corTextoPrincipal
            )
        }

        Image(
            painter = painterResource(id = R.drawable.hdc_second_logo),
            contentDescription = "Logo Hora de Cuidar",
            modifier = Modifier
                .size(148.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Recuperar senha",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = corTextoPrincipal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Informe seu e-mail para receber o link de redefinição.",
            fontSize = 14.sp,
            color = corTextoCinza
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "E-mail",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = corTextoCinza
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("seuemail@exemplo.com", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = corFundoInput,
                unfocusedContainerColor = corFundoInput,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            enabled = !isEnviandoEmail && email.isNotBlank(),
            onClick = {
                isEnviandoEmail = true
                mensagem = null
                emailEnviado = false

                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.getAuthApi(context)
                            .solicitarRecuperacaoSenha(
                                SolicitarRecuperacaoSenhaRequest(email = email.trim())
                            )

                        emailEnviado = true
                        mensagem = response.message ?: "Enviamos as instruções para o seu e-mail."
                        Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        mensagem = "Não foi possível solicitar a recuperação. Tente novamente."
                        Toast.makeText(context, mensagem, Toast.LENGTH_LONG).show()
                    } finally {
                        isEnviandoEmail = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = corBotao,
                disabledContainerColor = corBotao.copy(alpha = 0.55f),
                contentColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            if (isEnviandoEmail) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text("Enviar link")
            }
        }

        mensagem?.let { texto ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = texto,
                fontSize = 13.sp,
                color = if (emailEnviado) Color(0xFF2E7D32) else Color(0xFFC62828)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Já tenho o token",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = corTextoPrincipal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cole o token ou o link recebido no e-mail e escolha uma nova senha.",
            fontSize = 14.sp,
            color = corTextoCinza
        )

        Spacer(modifier = Modifier.height(20.dp))

        TextField(
            value = token,
            onValueChange = { token = it },
            placeholder = { Text("Token ou link de recuperação", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = corFundoInput,
                unfocusedContainerColor = corFundoInput,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = novaSenha,
            onValueChange = { novaSenha = it },
            label = "Nova senha",
            isVisible = senhaVisivel,
            onVisibilityChange = { senhaVisivel = !senhaVisivel },
            containerColor = corFundoInput,
            textColor = corTextoCinza
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordField(
            value = confirmacao,
            onValueChange = { confirmacao = it },
            label = "Confirmar nova senha",
            isVisible = confirmacaoVisivel,
            onVisibilityChange = { confirmacaoVisivel = !confirmacaoVisivel },
            containerColor = corFundoInput,
            textColor = corTextoCinza
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackToLogin,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar")
            }

            Button(
                enabled = !isRedefinindoSenha &&
                        token.isNotBlank() &&
                        novaSenha.isNotBlank() &&
                        confirmacao.isNotBlank(),
                onClick = {
                    val tokenLimpo = extrairTokenRecuperacao(token)

                    if (novaSenha != confirmacao) {
                        Toast.makeText(
                            context,
                            "A confirmação precisa ser igual à nova senha.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }

                    isRedefinindoSenha = true

                    coroutineScope.launch {
                        try {
                            val response = RetrofitClient.getAuthApi(context)
                                .resetarSenha(
                                    ResetarSenhaRequest(
                                        token = tokenLimpo,
                                        novaSenha = novaSenha,
                                        confirmacao = confirmacao
                                    )
                                )

                            Toast.makeText(
                                context,
                                response.message ?: "Senha redefinida com sucesso.",
                                Toast.LENGTH_LONG
                            ).show()
                            onBackToLogin()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                "Não foi possível redefinir a senha. Verifique o token.",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            isRedefinindoSenha = false
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = corBotao,
                    disabledContainerColor = corBotao.copy(alpha = 0.55f),
                    contentColor = Color.White,
                    disabledContentColor = Color.White
                )
            ) {
                if (isRedefinindoSenha) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text("Salvar")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    containerColor: Color,
    textColor: Color
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.Gray) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (isVisible) "Esconder senha" else "Mostrar senha"

            IconButton(onClick = onVisibilityChange) {
                Icon(imageVector = image, contentDescription = description, tint = textColor)
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        singleLine = true
    )
}

private fun extrairTokenRecuperacao(valor: String): String {
    val texto = valor.trim()
    return if (texto.contains("token=")) {
        texto.substringAfter("token=").substringBefore("&").trim()
    } else {
        texto
    }
}
