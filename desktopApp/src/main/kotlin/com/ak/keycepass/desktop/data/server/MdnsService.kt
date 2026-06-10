package com.ak.keycepass.desktop.data.server

import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import java.net.InetAddress

/**
 * Service mDNS — annonce le serveur KeycePass sur le réseau
 * sous le nom "KeycePass" (résolvable en keycepass.local).
 */
object MdnsService {

    private var jmdns: JmDNS? = null

    fun start(port: Int = 8080) {
        try {
            val hostname = "keycepass"
            jmdns = JmDNS.create(InetAddress.getLocalHost(), hostname)

            val serviceInfo = ServiceInfo.create(
                "_http._tcp.local.",
                "KeycePass",
                port,
                "path=/"
            )
            jmdns?.registerService(serviceInfo)

            println("[mDNS] keycepass.local annoncé sur le réseau (port $port)")
        } catch (e: Exception) {
            println("[mDNS] ⚠ Impossible de démarrer mDNS : ${e.message}")
        }
    }

    fun stop() {
        try {
            jmdns?.unregisterAllServices()
            jmdns?.close()
            jmdns = null
            println("[mDNS] Service arrêté")
        } catch (e: Exception) {
            println("[mDNS] ⚠ Erreur arrêt : ${e.message}")
        }
    }
}
