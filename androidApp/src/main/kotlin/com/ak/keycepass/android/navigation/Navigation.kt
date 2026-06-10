package com.ak.keycepass.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ak.keycepass.android.data.local.SessionManager
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.android.ui.screens.DelegateScreen
import com.ak.keycepass.android.ui.screens.ScanScreen
import com.ak.keycepass.android.ui.screens.TeacherScreen
import com.ak.keycepass.android.ui.viewmodel.DelegueViewModel
import com.ak.keycepass.android.ui.viewmodel.EnrolementViewModel
import com.ak.keycepass.android.ui.viewmodel.ScanViewModel
import com.ak.keycepass.shared.network.NetworkModels

@Composable
fun KeycePassNavHost(
    repository: AttendanceRepository,
    sessionManager: SessionManager,
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    val context = LocalContext.current

    LaunchedEffect(sessionManager.estEnrole) {
        if (!sessionManager.estEnrole) {
            navController.navigate("enrolement") {
                popUpTo("enrolement") { inclusive = true }
            }
        } else {
            navController.navigate("scan") {
                popUpTo("enrolement") { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = "enrolement"
    ) {
        composable("enrolement") {
            val viewModel: EnrolementViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return EnrolementViewModel(repository, sessionManager) as T
                    }
                }
            )
            com.ak.keycepass.android.ui.screens.LoginScreen(
                viewModel = viewModel,
                navController = navController
            )
        }
        composable("scan") {
            val viewModel: ScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScanViewModel(repository) as T
                    }
                }
            )
            ScanScreen(
                viewModel = viewModel,
                onNavigateDelegate = { navController.navigate("delegate") },
                onNavigateTeacher = { navController.navigate("teacher") },
                onBackToLogin = { sessionManager.effacerSession(); navController.navigate("enrolement") }
            )
        }
        composable("delegate") {
            val viewModel: DelegueViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return DelegueViewModel(repository) as T
                    }
                }
            )
            DelegateScreen(
                viewModel = viewModel,
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable("teacher") {
            val viewModel: ScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScanViewModel(repository) as T
                    }
                }
            )
            TeacherScreen(
                viewModel = viewModel,
                onBackToLogin = { navController.popBackStack() }
            )
        }
    }
}
