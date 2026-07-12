package com.example.hdcfuncap

import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hdcfuncap.features.LoginScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hdcfuncap.features.HomeScreen
import com.example.hdcfuncap.features.HomeViewModel
import com.example.hdcfuncap.features.HomeViewModelFactory
import com.example.hdcfuncap.features.PrescricoesScreen
import com.example.hdcfuncap.features.PrescricoesViewModel
import com.example.hdcfuncap.features.PrescricoesViewModelFactory
import com.example.hdcfuncap.features.RecuperarSenhaScreen
import com.example.hdcfuncap.features.RegistrarScreen
import com.example.hdcfuncap.features.RegistrarViewModel
import com.example.hdcfuncap.features.RegistrarViewModelFactory
import com.example.hdcfuncap.network.RetrofitClient
import com.example.hdcfuncap.storage.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val userPreferences = remember { UserPreferences(context) }
            val navController = rememberNavController()
            val isHandlingExpiredSession = remember { AtomicBoolean(false) }
            var sessionChecked by remember { mutableStateOf(false) }
            var startDestination by remember { mutableStateOf("login") }

            LaunchedEffect(Unit) {
                val token = userPreferences.getAuthToken()
                if (isJwtTokenValid(token)) {
                    startDestination = "home"
                } else {
                    userPreferences.clearSession()
                    startDestination = "login"
                }
                sessionChecked = true
            }

            LaunchedEffect(navController) {
                RetrofitClient.setSessionExpiredHandler {
                    if (isHandlingExpiredSession.compareAndSet(false, true)) {
                        runOnUiThread {
                            CoroutineScope(Dispatchers.Main).launch {
                                userPreferences.clearSession()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                    }
                }
            }

            if (!sessionChecked) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@setContent
            }

            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            isHandlingExpiredSession.set(false)
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        },
                        onForgotPassword = {
                            navController.navigate("recuperar-senha")
                        }
                    )
                }
                composable("recuperar-senha") {
                    RecuperarSenhaScreen(
                        onBackToLogin = {
                            navController.popBackStack("login", inclusive = false)
                        }
                    )
                }
                composable("home") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }
                    val homeViewModel: HomeViewModel = viewModel(
                        factory = HomeViewModelFactory(authApi)
                    )
                    HomeScreen(
                        onNavigate = { rota -> navController.navigate(rota) },
                        viewModel = homeViewModel
                    )
                }
                composable("prescricoes") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }
                    val prescricoesViewModel: PrescricoesViewModel = viewModel(
                        factory = PrescricoesViewModelFactory(authApi)
                    )

                    PrescricoesScreen(
                        onNavigate = { rota -> navController.navigate(rota) },
                        viewModel = prescricoesViewModel
                    )
                }
                composable("registrar") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }

                    val registrarViewModel: RegistrarViewModel = viewModel(
                        factory = RegistrarViewModelFactory(authApi)
                    )

                    RegistrarScreen(
                        onNavigate = { rota -> navController.navigate(rota) },
                        viewModel = registrarViewModel
                    )
                }
            }
        }
    }
}

private fun isJwtTokenValid(token: String?): Boolean {
    if (token.isNullOrBlank()) return false

    return try {
        val payload = token.split(".").getOrNull(1) ?: return false
        val decodedPayload = String(
            Base64.decode(
                payload,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
            ),
            Charsets.UTF_8
        )
        val expirationSeconds = JSONObject(decodedPayload).optLong("exp", 0L)
        val expirationMillis = expirationSeconds * 1000

        expirationMillis > System.currentTimeMillis() + 30_000
    } catch (e: Exception) {
        false
    }
}
