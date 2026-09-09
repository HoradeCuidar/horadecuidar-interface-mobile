package com.example.hdcfuncap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hdcfuncap.features.LoginScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.hdcfuncap.features.ConfiguracoesScreen
import com.example.hdcfuncap.features.HistoricoScreen
import com.example.hdcfuncap.features.HistoricoViewModel
import com.example.hdcfuncap.features.HistoricoViewModelFactory
import com.example.hdcfuncap.features.HomeScreen
import com.example.hdcfuncap.features.HomeViewModel
import com.example.hdcfuncap.features.HomeViewModelFactory
import com.example.hdcfuncap.features.PerfilDetalhesScreen
import com.example.hdcfuncap.features.PerfilEditarScreen
import com.example.hdcfuncap.features.PerfilScreen
import com.example.hdcfuncap.features.PerfilViewModel
import com.example.hdcfuncap.features.PerfilViewModelFactory
import com.example.hdcfuncap.features.PrescricoesScreen
import com.example.hdcfuncap.features.PrescricoesViewModel
import com.example.hdcfuncap.features.PrescricoesViewModelFactory
import com.example.hdcfuncap.features.RecuperarSenhaScreen
import com.example.hdcfuncap.features.RegistrarScreen
import com.example.hdcfuncap.features.RegistrarViewModel
import com.example.hdcfuncap.features.RegistrarViewModelFactory
import com.example.hdcfuncap.network.RetrofitClient
import com.example.hdcfuncap.notifications.MedicationNotificationScheduler
import com.example.hdcfuncap.storage.UserPreferences
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
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
            var notificationSetupRequested by remember { mutableStateOf(false) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = {}
            )
            val requestNotificationPermission = {
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } catch (_: Exception) {
                        // A tela não deve ser afetada caso o launcher ainda não esteja pronto.
                    }
                }
            }
            val currentBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = currentBackStackEntry?.destination?.route
            val selectedFontSize by userPreferences.fontSize.collectAsState(initial = "media")
            val currentDensity = LocalDensity.current
            val fontScale = when (selectedFontSize) {
                "pequena" -> 0.88f
                "grande" -> 1.16f
                else -> 1f
            }

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
                                notificationSetupRequested = false
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

            LaunchedEffect(sessionChecked, currentRoute) {
                if (
                    sessionChecked &&
                    currentRoute == "home" &&
                    !notificationSetupRequested
                ) {
                    notificationSetupRequested = true
                    delay(700)
                    requestNotificationPermission()

                    val pacienteId = userPreferences.pacienteId.firstOrNull()
                    if (pacienteId != null) {
                        try {
                            withContext(Dispatchers.IO) {
                                MedicationNotificationScheduler.refreshMedicationNotifications(
                                    context = context.applicationContext,
                                    authApi = RetrofitClient.getAuthApi(context),
                                    pacienteId = pacienteId
                                )
                            }
                        } catch (_: Exception) {
                            // O app continua aberto mesmo se os lembretes não puderem ser atualizados.
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

            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = fontScale
                )
            ) {
                NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(
                        onLoginSuccess = {
                            isHandlingExpiredSession.set(false)
                            notificationSetupRequested = false
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
                        onBack = {
                            if (!navController.popBackStack()) {
                                navController.navigate("login") {
                                    launchSingleTop = true
                                }
                            }
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
                composable("perfil") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }
                    val perfilViewModel: PerfilViewModel = viewModel(
                        factory = PerfilViewModelFactory(authApi)
                    )

                    PerfilScreen(
                        onNavigate = { rota -> navController.navigate(rota) },
                        onOpenDetails = { navController.navigate("perfil-detalhes") },
                        onOpenEdit = { navController.navigate("perfil-editar") },
                        onOpenSettings = { navController.navigate("configuracoes") },
                        onOpenChangePassword = { navController.navigate("recuperar-senha") },
                        onLogout = {
                            isHandlingExpiredSession.set(false)
                            notificationSetupRequested = false
                            CoroutineScope(Dispatchers.Main).launch {
                                userPreferences.clearSession()
                                navController.navigate("login") {
                                    popUpTo("home") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        viewModel = perfilViewModel
                    )
                }
                composable("perfil-editar") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }
                    val perfilViewModel: PerfilViewModel = viewModel(
                        factory = PerfilViewModelFactory(authApi)
                    )

                    PerfilEditarScreen(
                        onBack = {
                            navController.popBackStack("perfil", inclusive = false)
                        },
                        onSaved = {
                            navController.navigate("perfil") {
                                popUpTo("perfil") { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        viewModel = perfilViewModel
                    )
                }
                composable("perfil-detalhes") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }
                    val perfilViewModel: PerfilViewModel = viewModel(
                        factory = PerfilViewModelFactory(authApi)
                    )

                    PerfilDetalhesScreen(
                        onBack = {
                            navController.popBackStack("perfil", inclusive = false)
                        },
                        viewModel = perfilViewModel
                    )
                }
                composable("configuracoes") {
                    ConfiguracoesScreen(
                        onNavigate = { rota -> navController.navigate(rota) },
                        onBack = {
                            navController.popBackStack("perfil", inclusive = false)
                        }
                    )
                }
                composable("historico") {
                    val context = LocalContext.current
                    val authApi = remember { RetrofitClient.getAuthApi(context) }
                    val historicoViewModel: HistoricoViewModel = viewModel(
                        factory = HistoricoViewModelFactory(authApi)
                    )

                    HistoricoScreen(
                        onNavigate = { rota -> navController.navigate(rota) },
                        viewModel = historicoViewModel
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
