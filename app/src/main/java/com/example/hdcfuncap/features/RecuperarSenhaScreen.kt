package com.example.hdcfuncap.features

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hdcfuncap.R
import com.example.hdcfuncap.network.RetrofitClient
import com.example.hdcfuncap.network.SolicitarRecuperacaoSenhaRequest
import kotlinx.coroutines.launch

@Composable
fun RecuperarSenhaScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var emailEnviado by remember { mutableStateOf(false) }
    var isEnviandoEmail by remember { mutableStateOf(false) }
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

        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Voltar",
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
            text = "Alterar senha",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = corTextoPrincipal
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Informe seu e-mail para receber um link de redefinição. A alteração da senha será feita pela página aberta no e-mail.",
            fontSize = 14.sp,
            color = corTextoCinza,
            lineHeight = 20.sp
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
            onValueChange = {
                email = it
                if (emailEnviado) {
                    emailEnviado = false
                    mensagem = null
                }
            },
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
                val emailTratado = email.trim()

                isEnviandoEmail = true
                mensagem = null
                emailEnviado = false

                coroutineScope.launch {
                    try {
                        val response = RetrofitClient.getAuthApi(context)
                            .solicitarRecuperacaoSenha(
                                SolicitarRecuperacaoSenhaRequest(email = emailTratado)
                            )

                        emailEnviado = true
                        mensagem = response.message
                            ?: "Enviamos um link para o seu e-mail. Abra a mensagem para redefinir sua senha pela web."
                    } catch (e: Exception) {
                        mensagem = "Não foi possível enviar o e-mail de recuperação. Tente novamente."
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
                Text("Enviar link por e-mail")
            }
        }

        mensagem?.let { texto ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = texto,
                fontSize = 13.sp,
                color = if (emailEnviado) Color(0xFF2E7D32) else Color(0xFFC62828),
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
