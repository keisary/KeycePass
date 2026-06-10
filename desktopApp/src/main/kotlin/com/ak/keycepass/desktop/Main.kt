package com.ak.keycepass.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ak.keycepass.desktop.data.database.DatabaseManager
import com.ak.keycepass.desktop.data.server.KtorServer
import com.ak.keycepass.desktop.ui.screens.DashboardAdmin

/**
 * Point d'entrée de l'application d'administration KeycePass Desktop.
 *
 * Séquence de démarrage :
 * 1. Initialisation de la base de données SQLite centrale
 * 2. Démarrage du serveur Ktor en arrière-plan
 * 3. Lancement de l'interface graphique Compose for Desktop
 */
fun main() = application {
    // 1. Init base de données (crée les tables si elles n'existent pas)
    DatabaseManager.init()

    // 2. Démarrage du serveur HTTP en arrière-plan
    KtorServer.start(port = 8080)

    // 3. Fenêtre principale Compose for Desktop
    Window(
        onCloseRequest = {
            KtorServer.stop()
            exitApplication()
        },
        title = "KeycePass — Administration",
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        DashboardAdmin()
    }
}
