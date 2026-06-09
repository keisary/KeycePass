package com.ak.keycepass.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ak.keycepass.desktop.ui.screens.DashboardScreen
import com.ak.keycepass.desktop.ui.screens.GestionEnrolementScreen
import com.ak.keycepass.desktop.ui.screens.QRManagementScreen
import com.ak.keycepass.desktop.ui.theme.KeycePassTheme
import kotlinx.coroutines.launch

// Point d'entrée de l'application desktop KeycePass
fun main() = application {
    val appScope = rememberCoroutineScope()

    // Démarrage du serveur Ktor embarqué
    LaunchedEffect(Unit) {
        ServerManager.start()
    }

    Window(
        onCloseRequest = {
            appScope.launch {
                ServerManager.stop()
            }
            exitApplication()
        },
        title = "KeycePass — Administration",
        state = rememberWindowState(size = DpSize(1280.dp, 800.dp))
    ) {
        KeycePassTheme {
            Surface(Modifier.fillMaxSize()) {
                App()
            }
        }
    }
}

@Composable
fun App() {
    var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

    AdminLayout(
        currentScreen = currentScreen,
        onNavigate = { currentScreen = it }
    ) {
        when (currentScreen) {
            Screen.DASHBOARD -> DashboardScreen()
            Screen.QR_MANAGEMENT -> QRManagementScreen()
            Screen.ENROLEMENT -> GestionEnrolementScreen()
        }
    }
}

enum class Screen(val label: String, val icon: String) {
    DASHBOARD("Dashboard", "📊"),
    QR_MANAGEMENT("QR Codes", "📱"),
    ENROLEMENT("Pairages", "🔐")
}
