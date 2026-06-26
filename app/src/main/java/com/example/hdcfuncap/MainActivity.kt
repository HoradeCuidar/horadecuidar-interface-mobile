package com.example.hdcfuncap

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.hdcfuncap.features.LoginScreen
import com.example.hdcfuncap.features.WelcomeScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.hdcfuncap.features.HomeScreen
import com.example.hdcfuncap.features.HomeViewModel
import com.example.hdcfuncap.features.HomeViewModelFactory
import com.example.hdcfuncap.features.RegistrarScreen
import com.example.hdcfuncap.features.RegistrarViewModel
import com.example.hdcfuncap.features.RegistrarViewModelFactory
import com.example.hdcfuncap.network.RetrofitClient
import com.example.hdcfuncap.ui.theme.HdcFuncapTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    })
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

