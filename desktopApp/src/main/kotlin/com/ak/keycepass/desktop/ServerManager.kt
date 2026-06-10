package com.ak.keycepass.desktop

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetAddress
import java.net.NetworkInterface

object ServerManager {
    private var server: EmbeddedServer<*, *>? = null
    private var serverScope: CoroutineScope? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val _updates = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val updates: SharedFlow<String> = _updates.asSharedFlow()

    fun start(port: Int = 8080, host: String = "0.0.0.0") {
        if (server != null) return

        serverScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        embeddedServer(Netty, port = port, host = host) {
            install(ContentNegotiation) { json() }
            install(StatusPages) {
                exception<Throwable> { call, cause ->
                    call.respondText("Erreur serveur: ${cause.message}", ContentType.Text.Plain)
                }
            }
            install(WebSockets)

            routing {
                get("/api/health") {
                    call.respond(mapOf("status" to "ok", "app" to "KeycePass"))
                }

                get("/api/seance/info") {
                    call.respond(mapOf(
                        "matiere" to "Ingénierie Logicielle",
                        "classe" to "B2_IT",
                        "statut" to "EN_COURS"
                    ))
                }

                get("/api/attendance/status") {
                    call.respond(mapOf(
                        "presents" to 8,
                        "retards" to 2,
                        "absents" to 2,
                        "total" to 12
                    ))
                }

                webSocket("/ws/live") {
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                _updates.emit(text)
                            }
                        }
                    } catch (e: Exception) {
                        // Client déconnecté
                    }
                }
            }
        }.apply {
            start(wait = false)
            server = this
        }

        val ip = getLocalIp()
        println("🚀 KeycePass Server démarré sur http://$ip:$port")
        println("📡 WebSocket: ws://$ip:$port/ws/live")
    }

    fun stop() {
        server?.stop(1000, 2000)
        serverScope?.cancel()
        server = null
        serverScope = null
        println("🛑 KeycePass Server arrêté")
    }

    private fun getLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is InetAddress && !addr.isLoopbackAddress && addr.hostAddress.contains('.')) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return "127.0.0.1"
    }
}
