package com.ak.keycepass.desktop.data.server

import com.ak.keycepass.desktop.data.database.DatabaseTables.EtudiantTable
import com.ak.keycepass.desktop.data.database.DatabaseTables.EmargementTable
import com.ak.keycepass.desktop.data.database.DatabaseTables.SeanceTable
import com.ak.keycepass.shared.network.ScanPayload
import com.ak.keycepass.shared.network.ScanResponse
import com.ak.keycepass.shared.network.ScanType
import com.ak.keycepass.shared.domain.utils.StatutUtils
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Serveur HTTP léger Ktor pour recevoir les pointages des applications mobiles.
 * Fonctionne aussi bien en réseau local Wi-Fi que via Internet.
 */
object KtorServer {

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    fun start(port: Int = 8080) {
        server = embeddedServer(CIO, port = port) {
            install(ContentNegotiation) {
                json()
            }
            install(CORS) {
                anyHost() // Accepte les connexions depuis n'importe quelle source
            }

            routing {
                // ─── Santé du serveur ────────────────────────────────────────
                get("/api/ping") {
                    call.respondText("KeycePass Server OK")
                }

                // ─── Enrôlement d'un appareil (liaison UUID) ────────────────
                // US_01 : Lie le deviceUuid au matricule de l'étudiant
                post("/api/enrolement") {
                    val payload = call.receive<EnrolementPayload>()
                    val success = transaction {
                        val etudiant = EtudiantTable
                            .selectAll()
                            .where { EtudiantTable.matricule eq payload.matricule }
                            .firstOrNull()

                        if (etudiant == null) return@transaction false

                        // Vérifie si un UUID est déjà lié (anti-fraude)
                        val uuidExistant = etudiant[EtudiantTable.deviceUuid]
                        if (!uuidExistant.isNullOrEmpty() && uuidExistant != payload.deviceUuid) {
                            return@transaction false
                        }

                        EtudiantTable.update({ EtudiantTable.matricule eq payload.matricule }) {
                            it[deviceUuid] = payload.deviceUuid
                        }
                        true
                    }
                    call.respond(EnrolementResponse(success = success))
                }

                // ─── Réception d'un scan de présence ─────────────────────────
                // US_02, US_03 : Réceptionne le premier ou second scan
                post("/api/scan") {
                    val payload = call.receive<ScanPayload>()

                    val response = transaction {
                        // 1. Vérifier que l'appareil est bien lié à l'étudiant
                        val etudiant = EtudiantTable
                            .selectAll()
                            .where { EtudiantTable.matricule eq payload.matricule }
                            .firstOrNull()

                        if (etudiant == null) {
                            return@transaction ScanResponse(false, "ABSENT", "Étudiant introuvable.")
                        }

                        val uuidEnregistre = etudiant[EtudiantTable.deviceUuid]
                        if (uuidEnregistre != payload.deviceUuid) {
                            return@transaction ScanResponse(false, "ABSENT", "Appareil non autorisé (fraude détectée).")
                        }

                        val etudiantId = etudiant[EtudiantTable.idEtudiant]

                        // 2. Traiter selon le type de scan
                        when (payload.scanType) {
                            ScanType.DEBUT -> {
                                val seance = SeanceTable
                                    .selectAll()
                                    .where { SeanceTable.idSeance eq payload.seanceId }
                                    .firstOrNull()
                                    ?: return@transaction ScanResponse(false, "EN_ATTENTE", "Séance introuvable.")

                                // Enregistre le scan de début
                                EmargementTable.insert {
                                    it[EmargementTable.etudiantId] = etudiantId
                                    it[EmargementTable.seanceId] = payload.seanceId
                                    it[EmargementTable.horodatageScanDebut] = payload.timestamp
                                    it[EmargementTable.statutFinal] = "EN_ATTENTE"
                                }
                                ScanResponse(true, "EN_ATTENTE", "Scan de début enregistré.")
                            }

                            ScanType.FIN -> {
                                // Récupère l'émargement existant
                                val emargement = EmargementTable
                                    .selectAll()
                                    .where {
                                        (EmargementTable.etudiantId eq etudiantId) and
                                        (EmargementTable.seanceId eq payload.seanceId)
                                    }
                                    .firstOrNull()
                                    ?: return@transaction ScanResponse(false, "ABSENT", "Aucun scan de début trouvé.")

                                val seance = SeanceTable
                                    .selectAll()
                                    .where { SeanceTable.idSeance eq payload.seanceId }
                                    .first()

                                val scanDebut = emargement[EmargementTable.horodatageScanDebut]
                                val statut = StatutUtils.determinerStatutFinal(
                                    heureDebutCoursStr = seance[SeanceTable.heureDebut],
                                    heurePremierScanStr = scanDebut,
                                    secondScanValide = true
                                )

                                EmargementTable.update({
                                    (EmargementTable.etudiantId eq etudiantId) and
                                    (EmargementTable.seanceId eq payload.seanceId)
                                }) {
                                    it[EmargementTable.horodatageScanFin] = payload.timestamp
                                    it[EmargementTable.statutFinal] = statut.name
                                }

                                ScanResponse(true, statut.name, "Statut calculé : ${statut.name}")
                            }
                        }
                    }
                    call.respond(response)
                }

                // ─── Vérification du statut d'une séance ────────────────────
                // US_04 : L'app mobile vérifie si l'enseignant a clôturé la séance
                get("/api/seance/{seanceId}/statut") {
                    val seanceId = call.parameters["seanceId"]?.toIntOrNull()
                        ?: return@get call.respond(mapOf("erreur" to "ID de séance invalide"))

                    val statut = transaction {
                        SeanceTable
                            .selectAll()
                            .where { SeanceTable.idSeance eq seanceId }
                            .firstOrNull()
                            ?.get(SeanceTable.statutSeance)
                            ?: "INCONNU"
                    }
                    call.respond(mapOf("statutSeance" to statut))
                }

                // ─── Clôture d'une séance par l'enseignant ──────────────────
                // US_04 : L'enseignant signale la fin du cours
                post("/api/seance/{seanceId}/cloturer") {
                    val seanceId = call.parameters["seanceId"]?.toIntOrNull()
                        ?: return@post call.respond(mapOf("success" to false))

                    transaction {
                        SeanceTable.update({ SeanceTable.idSeance eq seanceId }) {
                            it[statutSeance] = "CLOTURE_ENSEIGNANT"
                        }
                    }
                    call.respond(mapOf("success" to true))
                }
            }
        }.start(wait = false)

        println("Serveur KeycePass démarré sur le port $port")
    }

    fun stop() {
        server?.stop(1000, 5000)
        println("Serveur KeycePass arrêté.")
    }
}

@Serializable
data class EnrolementPayload(
    val matricule: String,
    val deviceUuid: String
)

@Serializable
data class EnrolementResponse(
    val success: Boolean,
    val message: String? = null
)
