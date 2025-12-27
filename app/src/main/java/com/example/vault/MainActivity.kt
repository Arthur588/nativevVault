package com.example.vault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.vault.ui.theme.VaultTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    VaultNavHost(navController = navController)
                }
            }
        }
    }
}

@Composable
fun VaultNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            val loginViewModel: com.example.vault.ui.login.LoginViewModel = hiltViewModel()
            com.example.vault.ui.screens.LoginScreen(
                viewModel = loginViewModel,
                onUnlock = { navController.navigate("home") }
            )
        }
        composable("home") {
            val homeViewModel: com.example.vault.ui.home.HomeViewModel = hiltViewModel()
            com.example.vault.ui.screens.HomeScreen(
                viewModel = homeViewModel,
                onImport = { navController.navigate("import") },
                onViewToday = { navController.navigate("today") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable("import") {
            val importViewModel: com.example.vault.ui.importing.ImportViewModel = hiltViewModel()
            com.example.vault.ui.screens.ImportScreen(
                viewModel = importViewModel,
                onDone = { navController.popBackStack() }
            )
        }
        composable("today") {
            val todayViewModel: com.example.vault.ui.today.TodayViewModel = hiltViewModel()
            com.example.vault.ui.screens.TodayScreen(
                viewModel = todayViewModel,
                onViewItem = { itemId -> navController.navigate("viewer/$itemId") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("viewer/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")
            val viewerViewModel: com.example.vault.ui.viewer.ViewerViewModel = hiltViewModel()
            // Load the list when entering viewer
            viewerViewModel.load(id)
            com.example.vault.ui.screens.ViewerScreen(
                viewModel = viewerViewModel,
                onClose = {
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            com.example.vault.ui.screens.SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}