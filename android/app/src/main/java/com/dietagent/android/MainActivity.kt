package com.dietagent.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dietagent.android.ui.auth.LoginScreen
import com.dietagent.android.ui.auth.RegisterScreen
import com.dietagent.android.ui.chat.ChatViewModel
import com.dietagent.android.ui.home.HomeScreen
import com.dietagent.android.ui.theme.NutriMuseTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NutriMuseTheme {
                val app = application as DietAgentApp
                var start by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    start = if (runBlocking { app.tokenStore.getToken() }.isNullOrBlank()) "login" else "home"
                }

                val destination = start
                if (destination != null) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = destination) {
                        composable("login") { LoginScreen(navController) }
                        composable("register") { RegisterScreen(navController) }
                        composable("home") { HomeScreen(app, navController) }
                    }
                }
            }
        }
    }
}

/** ViewModel 工厂：从 Application 手动 DI 容器取仓库 */
fun chatViewModelFactory(app: DietAgentApp) = viewModelFactory {
    initializer { ChatViewModel(app.agentRepository, app.analysisRepository) }
}
