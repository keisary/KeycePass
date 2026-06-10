package com.ak.keycepass.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import com.ak.keycepass.desktop.ui.AdminLayout
import com.ak.keycepass.desktop.ui.theme.KeycePassTheme
import kotlinx.coroutines.launch
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.SwingUtilities

fun main() {
    // Demarrage du serveur
    ServerManager.start()

    // Utiliser Swing JFrame directement (contourne Skiko Window)
    SwingUtilities.invokeLater {
        val frame = JFrame("KeycePass — Administration")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(1100, 720)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        // Attendre la fermeture
        frame.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                ServerManager.stop()
            }
        })
    }
}
