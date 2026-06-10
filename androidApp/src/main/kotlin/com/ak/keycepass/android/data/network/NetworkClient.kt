package com.ak.keycepass.android.data.network

import com.ak.keycepass.shared.network.ScanPayload
import com.ak.keycepass.shared.network.ScanResponse
import com.ak.keycepass.shared.network.SessionStatusDto
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

/**
 * Client HTTP configuré pour communiquer avec le serveur Ktor du poste Desktop.
 *
 * L'URL de base est extraite du QR Code d'enrôlement et stockée dans [SessionManager].
 * Format attendu : "http://192.168.1.10:8080" (IP du PC sur le réseau ou adresse internet).
 */
class NetworkClient(private val serverBaseUrl: String) {

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json()
        }
    }

    // ─── Enrôlement (US_01) ──────────────────────────────────────────────────

    /**
     * Envoie la demande d'enrôlement : lie le matricule à l'UUID de l'appareil.
     * @return true si l'enrôlement est accepté par le serveur, false sinon.
     */
    suspend fun enroler(matricule: String, deviceUuid: String): Boolean {
        return try {
            val response: EnrolementResponse = client.post("$serverBaseUrl/api/enrolement") {
                contentType(ContentType.Application.Json)
                setBody(EnrolementRequest(matricule, deviceUuid))
            }.body()
            response.success
        } catch (e: Exception) {
            false
        }
    }

    // ─── Scan de présence (US_02 & US_03) ────────────────────────────────────

    /**
     * Envoie un scan (début ou fin) au serveur Desktop.
     * @return [ScanResponse] contenant le statut calculé ou le message d'erreur.
     */
    suspend fun envoyerScan(payload: ScanPayload): ScanResponse {
        return try {
            client.post("$serverBaseUrl/api/scan") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body()
        } catch (e: Exception) {
            ScanResponse(success = false, statutCalcule = "ABSENT", message = "Erreur réseau : ${e.message}")
        }
    }

    // ─── Statut d'une séance (US_04) ─────────────────────────────────────────

    /**
     * Vérifie si l'enseignant a clôturé la séance (déverrouillage du scan de fin).
     * @return true si le statut de la séance est CLOTURE_ENSEIGNANT.
     */
    suspend fun verifierCloture(seanceId: Int): Boolean {
        return try {
            val response: Map<String, String> = client.get("$serverBaseUrl/api/seance/$seanceId/statut").body()
            response["statutSeance"] == "CLOTURE_ENSEIGNANT"
        } catch (e: Exception) {
            false
        }
    }

    // ─── Clôture de séance par l'enseignant (US_04) ──────────────────────────

    /**
     * L'enseignant signale la fin du cours au serveur.
     */
    suspend fun cloturerSeance(seanceId: Int): Boolean {
        return try {
            val response: Map<String, Boolean> = client.post("$serverBaseUrl/api/seance/$seanceId/cloturer").body()
            response["success"] == true
        } catch (e: Exception) {
            false
        }
    }

    // ─── Statistiques du délégué (US_05) ─────────────────────────────────────

    /**
     * Récupère les statistiques de présence d'une séance (pour le délégué).
     */
    suspend fun getStatistiquesSeance(seanceId: Int): SessionStatusDto? {
        return try {
            client.get("$serverBaseUrl/api/seance/$seanceId/stats").body()
        } catch (e: Exception) {
            null
        }
    }

    fun close() = client.close()
}

// ─── DTOs internes au client Android ─────────────────────────────────────────

@Serializable
private data class EnrolementRequest(
    val matricule: String,
    val deviceUuid: String
)

@Serializable
private data class EnrolementResponse(
    val success: Boolean,
    val message: String? = null
)
