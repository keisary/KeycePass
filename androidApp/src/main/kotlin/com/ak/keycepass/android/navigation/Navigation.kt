package com.ak.keycepass.android.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ak.keycepass.android.data.local.SessionManager
import com.ak.keycepass.android.data.local.UserRole
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.android.ui.screens.ScanScreen
import com.ak.keycepass.android.ui.screens.TeacherScreen
import com.ak.keycepass.android.ui.viewmodel.EnrolementViewModel
import com.ak.keycepass.android.ui.viewmodel.ScanViewModel

@Composable
fun KeycePassNavHost(
    repository: AttendanceRepository,
    sessionManager: SessionManager,
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    val context = LocalContext.current
    val startDest = remember {
        if (sessionManager.estEnrole) {
            if (sessionManager.role == UserRole.ENSEIGNANT) "teacher" else "scan"
        } else {
            "enrolement"
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDest
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
                        return ScanViewModel(repository, sessionManager) as T
                    }
                }
            )
            ScanScreen(
                viewModel = viewModel,
                onBackToLogin = { sessionManager.effacerSession(); navController.navigate("enrolement") }
            )
        }
        composable("teacher") {
            val viewModel: ScanViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ScanViewModel(repository, sessionManager) as T
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
