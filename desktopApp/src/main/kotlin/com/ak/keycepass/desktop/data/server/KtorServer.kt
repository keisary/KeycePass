package com.ak.keycepass.desktop.data.server

import com.ak.keycepass.desktop.data.database.EtudiantTable
import com.ak.keycepass.desktop.data.database.EmargementTable
import com.ak.keycepass.desktop.data.database.SeanceTable
import com.ak.keycepass.desktop.data.database.SeanceSemaineTable
import com.ak.keycepass.desktop.data.utils.GeoUtils
import com.ak.keycepass.shared.network.ScanPayload
import com.ak.keycepass.shared.network.ScanResponse
import com.ak.keycepass.shared.network.ScanType
import com.ak.keycepass.shared.network.SessionStatusDto
import com.ak.keycepass.shared.network.SeanceCouranteDto
import io.ktor.http.HttpStatusCode
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

                                // Récupérer la semaine parente pour la vérification GPS
                                val semaine = seance[SeanceTable.semaineId]?.let { sid ->
                                    SeanceSemaineTable
                                        .selectAll()
                                        .where { SeanceSemaineTable.idSemaine eq sid }
                                        .firstOrNull()
                                }

                                // Vérifier la géolocalisation si coords fournies et semaine connue
                                val latScan = payload.lat
                                val lonScan = payload.lon
                                val geoValide = if (latScan != null && lonScan != null && semaine != null) {
                                    GeoUtils.localisationValide(
                                        latScan, lonScan,
                                        semaine[SeanceSemaineTable.latReference],
                                        semaine[SeanceSemaineTable.lonReference],
                                        semaine[SeanceSemaineTable.rayonMetres]
                                    )
                                } else {
                                    true // Pas de coords fournies → on accepte (app non mise à jour)
                                }

                                if (!geoValide) {
                                    return@transaction ScanResponse(
                                        success = false,
                                        statutCalcule = "ABSENT",
                                        message = "Localisation non autorisée. Vous êtes trop loin de la salle.",
                                        localisationRefusee = true
                                    )
                                }

                                // Enregistre le scan de début
                                EmargementTable.insert {
                                    it[EmargementTable.etudiantId] = etudiantId
                                    it[EmargementTable.seanceId] = payload.seanceId
                                    it[EmargementTable.horodatageScanDebut] = payload.timestamp
                                    it[EmargementTable.statutFinal] = "EN_ATTENTE"
                                    it[EmargementTable.latScan] = latScan
                                    it[EmargementTable.lonScan] = lonScan
                                    it[EmargementTable.localisationValide] = geoValide
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

                // ─── Statistiques d'une séance ──────────────────────────────
                get("/api/seance/{seanceId}/stats") {
                    val seanceId = call.parameters["seanceId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("erreur" to "ID de séance invalide"))

                    val stats = transaction {
                        val seance = SeanceTable
                            .selectAll()
                            .where { SeanceTable.idSeance eq seanceId }
                            .firstOrNull()

                        if (seance == null) return@transaction null

                        val classeId = seance[SeanceTable.classeId]
                        val totalInscrits = EtudiantTable
                            .selectAll()
                            .where { EtudiantTable.classeId eq classeId }
                            .count().toInt()

                        val emargements = EmargementTable
                            .selectAll()
                            .where { EmargementTable.seanceId eq seanceId }
                            .toList()

                        val totalPresents = emargements.count { it[EmargementTable.statutFinal] == "PRESENT" }
                        val totalRetards = emargements.count { it[EmargementTable.statutFinal] == "RETARD" }
                        val totalAbsents = emargements.count { it[EmargementTable.statutFinal] == "ABSENT" }
                        val statut = seance[SeanceTable.statutSeance]

                        SessionStatusDto(
                            seanceId = seanceId,
                            totalInscrits = totalInscrits,
                            totalPresents = totalPresents,
                            totalRetards = totalRetards,
                            totalAbsents = totalAbsents,
                            cloture = statut == "CLOTURE_ENSEIGNANT"
                        )
                    }

                    if (stats == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("erreur" to "Séance introuvable"))
                    } else {
                        call.respond(stats)
                    }
                }

                // ─── Séance courante d'une semaine ──────────────────────────
                // L'app mobile scanne le QR hebdomadaire et demande quelle
                // séance est actuellement en cours pour la semaine donnée.
                get("/api/semaine/{semaineId}/seance-courante") {
                    val semaineId = call.parameters["semaineId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("erreur" to "ID semaine invalide"))

                    val maintenant = java.time.LocalTime.now().toString().substring(0, 8) // HH:MM:SS
                    val aujourd_hui = java.time.LocalDate.now().toString()                 // YYYY-MM-DD

                    val seanceCourante = transaction {
                        SeanceTable
                            .selectAll()
                            .where {
                                (SeanceTable.semaineId eq semaineId) and
                                (SeanceTable.dateJour eq aujourd_hui) and
                                (SeanceTable.heureDebut lessEq maintenant) and
                                (SeanceTable.heureFin greaterEq maintenant)
                            }
                            .firstOrNull()
                            ?.let {
                                SeanceCouranteDto(
                                    seanceId = it[SeanceTable.idSeance],
                                    nomMatiere = it[SeanceTable.nomMatiere],
                                    heureDebut = it[SeanceTable.heureDebut],
                                    heureFin = it[SeanceTable.heureFin]
                                )
                            }
                    }

                    if (seanceCourante == null) {
                        call.respond(HttpStatusCode.NotFound, mapOf("erreur" to "Aucune séance en cours pour cette semaine"))
                    } else {
                        call.respond(seanceCourante)
                    }
                }

                // ─── Sync différé mobile → serveur ──────────────────────────
                // Reçoit les scans stockés hors-ligne (Room) quand le réseau revient
                post("/api/sync") {
                    val payloads = call.receive<List<ScanPayload>>()
                    var successCount = 0
                    var failCount = 0

                    transaction {
                        payloads.forEach { payload ->
                            try {
                                val etudiant = EtudiantTable
                                    .selectAll()
                                    .where { EtudiantTable.matricule eq payload.matricule }
                                    .firstOrNull() ?: return@forEach

                                val etudiantId = etudiant[EtudiantTable.idEtudiant]

                                when (payload.scanType) {
                                    com.ak.keycepass.shared.network.ScanType.DEBUT -> {
                                        EmargementTable.insert {
                                            it[EmargementTable.etudiantId] = etudiantId
                                            it[EmargementTable.seanceId] = payload.seanceId
                                            it[EmargementTable.horodatageScanDebut] = payload.timestamp
                                            it[EmargementTable.statutFinal] = "EN_ATTENTE"
                                            it[EmargementTable.latScan] = payload.lat
                                            it[EmargementTable.lonScan] = payload.lon
                                        }
                                        successCount++
                                    }
                                    com.ak.keycepass.shared.network.ScanType.FIN -> {
                                        val emarg = EmargementTable
                                            .selectAll()
                                            .where {
                                                (EmargementTable.etudiantId eq etudiantId) and
                                                (EmargementTable.seanceId eq payload.seanceId)
                                            }
                                            .firstOrNull() ?: return@forEach

                                        val seance = SeanceTable
                                            .selectAll()
                                            .where { SeanceTable.idSeance eq payload.seanceId }
                                            .first()

                                        val statut = com.ak.keycepass.shared.domain.utils.StatutUtils.determinerStatutFinal(
                                            heureDebutCoursStr = seance[SeanceTable.heureDebut],
                                            heurePremierScanStr = emarg[EmargementTable.horodatageScanDebut],
                                            secondScanValide = true
                                        )

                                        EmargementTable.update({
                                            (EmargementTable.etudiantId eq etudiantId) and
                                            (EmargementTable.seanceId eq payload.seanceId)
                                        }) {
                                            it[EmargementTable.horodatageScanFin] = payload.timestamp
                                            it[EmargementTable.statutFinal] = statut.name
                                        }
                                        successCount++
                                    }
                                }
                            } catch (e: Exception) {
                                failCount++
                            }
                        }
                    }

                    call.respond(mapOf(
                        "success" to true,
                        "traites" to successCount,
                        "erreurs" to failCount
                    ))
                }
            }
        }.start(wait = false)

        println("Serveur KeycePass démarré sur le port $port")
    }

    fun stop() {
        server?.stop(1000, 5000)
        println("Serveur KeycePass arrêté.")
    }

    fun getLocalIpAddress(): String {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isLoopback || !networkInterface.isUp || networkInterface.isVirtual) continue
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Ignorer
        }
        return "127.0.0.1"
    }

    fun getServerUrl(): String {
        return "http://${getLocalIpAddress()}:8080"
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
