package com.ak.keycepass.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ak.keycepass.desktop.data.database.DatabaseManager
import com.ak.keycepass.desktop.data.server.KtorServer
import com.ak.keycepass.desktop.data.server.MdnsService
import com.ak.keycepass.desktop.ui.AdminLayout
import com.ak.keycepass.desktop.ui.theme.KeycePassTheme
import kotlinx.coroutines.launch

fun main() = application {
    val appScope = rememberCoroutineScope()

    // 1. Initialisation base de données SQLite (crée les tables)
    DatabaseManager.init()
    println("[KeycePass] Base de données initialisée")

    // 2. Démarrage du serveur Ktor
    KtorServer.start(port = 8080)
    println("[KeycePass] Serveur démarré sur http://192.168.1.84:8080")

    // 3. Annonce mDNS (keycepass.local)
    MdnsService.start(port = 8080)

    Window(
        onCloseRequest = {
            appScope.launch {
                KtorServer.stop()
                MdnsService.stop()
                println("[KeycePass] Serveur arrêté")
            }
            exitApplication()
        },
        onPreviewKeyEvent = { false },
        title = "KeycePass — Administration",
        icon = painterResource("icons/keycepass_logo.svg"),
        state = rememberWindowState(size = DpSize(980.dp, 650.dp)),
        alwaysOnTop = false,
        resizable = true
    ) {
        LaunchedEffect(Unit) {
            println("[KeycePass] Fenêtre Compose créée")
        }

        KeycePassTheme {
            Surface(Modifier.fillMaxSize()) {
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }
                AdminLayout(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    }
}
