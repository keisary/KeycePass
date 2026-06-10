package com.ak.keycepass.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ak.keycepass.desktop.ui.AdminLayout
import com.ak.keycepass.desktop.ui.theme.KeycePassTheme
import kotlinx.coroutines.launch

fun main() = application {
    val appScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ServerManager.start()
    }

    Window(
        onCloseRequest = {
            appScope.launch { ServerManager.stop() }
            exitApplication()
        },
        title = "KeycePass — Administration",
        state = rememberWindowState(
            size = DpSize(1280.dp, 800.dp),
            position = WindowPosition(Alignment.Center)
        ),
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
    )
}

enum class Screen(val label: String) {
    DASHBOARD("Dashboard"),
    QR_MANAGEMENT("QR Codes"),
    ENROLEMENT("Pairages"),
    HISTORIQUE("Historique")
}
