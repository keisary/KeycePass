package com.ak.keycepass.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ak.keycepass.android.data.local.LocalDatabase
import com.ak.keycepass.android.data.local.SessionManager
import com.ak.keycepass.android.data.network.NetworkClient
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.android.navigation.KeycePassNavHost
import com.ak.keycepass.android.ui.theme.KeycePassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = LocalDatabase.getDatabase(applicationContext)
        val sessionManager = SessionManager(applicationContext)
        val repository = AttendanceRepository(
            sessionManager = sessionManager,
            db = database,
            networkClient = NetworkClient("http://localhost:8080")
        )

        setContent {
            KeycePassTheme {
                KeycePassNavHost(repository = repository, sessionManager = sessionManager)
            }
        }
    }
}
