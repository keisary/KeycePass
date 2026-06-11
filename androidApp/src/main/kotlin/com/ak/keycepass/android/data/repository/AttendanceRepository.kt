package com.ak.keycepass.android.data.repository

import com.ak.keycepass.android.data.local.LocalDatabase
import com.ak.keycepass.android.data.local.SessionManager
import com.ak.keycepass.android.data.local.UserRole
import com.ak.keycepass.android.data.local.entities.EmargementLocal
import com.ak.keycepass.android.data.network.NetworkClient
import com.ak.keycepass.shared.domain.utils.StatutUtils
import com.ak.keycepass.shared.network.ScanPayload
import com.ak.keycepass.shared.network.ScanType
import com.ak.keycepass.shared.network.SeanceCouranteDto
import com.ak.keycepass.shared.network.SessionStatusDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dépôt central de la logique métier de l'application mobile.
 *
 * Orchestre toutes les opérations d'émargement en faisant communiquer :
 * - [SessionManager] : gestion de l'identité locale chiffrée
 * - [LocalDatabase] : stockage temporaire des scans en attente
 * - [NetworkClient] : communication HTTP avec le serveur Desktop
 * - [StatutUtils] : calcul du statut de présence (module :shared)
 */
class AttendanceRepository(
    private val sessionManager: SessionManager,
    private val db: LocalDatabase,
    private val networkClient: NetworkClient
) {
    private val etudiantDao = db.etudiantDao()

    // ─── US_01 : Enrôlement initial ──────────────────────────────────────────

    /**
     * Enrôle l'utilisateur en liant son matricule à l'UUID de son appareil.
     * Analyse le contenu du QR Code d'enrôlement pour extraire l'URL du serveur,
     * l'ID de classe et le rôle.
     *
     * Format QR Code attendu :
     * keycepass://enrolement?classeId=B2_IT&token=XXX&serverUrl=http://192.168.1.10:8080&role=ETUDIANT
     *
     * @param matricule Matricule saisi par l'utilisateur.
     * @param deviceUuid UUID extrait de l'appareil par [SessionManager].
     * @param contenuQr Contenu brut du QR Code d'enrôlement scanné.
     * @return [EnrolementResult] succès ou erreur avec message.
     */
    suspend fun enroler(
        matricule: String,
        nom: String,
        prenom: String,
        deviceUuid: String,
        contenuQr: String
    ): EnrolementResult = withContext(Dispatchers.IO) {
        // 1. Parser le QR Code d'enrôlement
        val params = parseQrParams(contenuQr)
        val serverUrl = params["serverUrl"]
            ?: return@withContext EnrolementResult.Erreur("QR Code invalide : URL du serveur manquante.")
        val roleStr = params["role"] ?: "ETUDIANT"
        val role = runCatching { UserRole.valueOf(roleStr) }.getOrDefault(UserRole.ETUDIANT)

        // 2. Appel réseau vers le serveur Desktop
        networkClient.serverBaseUrl = serverUrl
        val succes = networkClient.enroler(matricule, deviceUuid)
        if (!succes) {
            return@withContext EnrolementResult.Erreur(
                "Enrôlement refusé. Vérifiez votre matricule ou contactez l'administration."
            )
        }

        // 3. Sauvegarder la session localement de façon chiffrée
        sessionManager.sauvegarderSession(
            matricule = matricule,
            nom = nom,
            prenom = prenom,
            deviceUuid = deviceUuid,
            role = role,
            serverUrl = serverUrl
        )
        // Sauvegarder l'étudiant en base locale pour suivi offline
        etudiantDao.insert(
            com.ak.keycepass.android.data.local.entities.EtudiantLocal(
                matricule = matricule,
                identifiantClasse = params["classeId"].orEmpty(),
                identifiantAppareil = deviceUuid,
                role = role.name
            )
        )

        EnrolementResult.Succes(role)
    }

    // ─── US_02 : Premier scan (arrivée en cours) ─────────────────────────────

    /**
     * Traite le premier scan du QR Code de présence (arrivée en cours).
     *
     * Nouveau format QR Code de présence (hebdomadaire) :
     * keycepass://presence?semaineId=3&classeId=B2_IT&token=XXX&serverUrl=http://...
     *
     * L'app résout d'abord la séance courante via [resolveSeanceCourante], puis
     * envoie le scan avec les coordonnées GPS optionnelles.
     *
     * @param contenuQr Contenu du QR Code de présence scanné.
     * @param lat Latitude GPS de l'étudiant au moment du scan (nullable).
     * @param lon Longitude GPS de l'étudiant au moment du scan (nullable).
     * @return [ScanResult] avec le statut provisoire ou une erreur.
     */
    suspend fun enregistrerPremierScan(
        contenuQr: String,
        lat: Double? = null,
        lon: Double? = null
    ): ScanResult = withContext(Dispatchers.IO) {
        val params = parseQrParams(contenuQr)
        var seanceId = params["seanceId"]?.toIntOrNull()

        if (seanceId == null) {
            val seanceCourante = resolveSeanceCourante(contenuQr)
                ?: return@withContext ScanResult.Erreur("Aucun cours en cours actuellement.")
            seanceId = seanceCourante.seanceId

            // Sauvegarder la séance en cache local
            val dateJour = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            db.seanceDao().insert(
                com.ak.keycepass.android.data.local.entities.SeanceLocal(
                    idSeance = seanceCourante.seanceId,
                    nomMatiere = seanceCourante.nomMatiere,
                    classeId = params["classeId"].orEmpty(),
                    dateJour = dateJour,
                    heureDebut = seanceCourante.heureDebut,
                    heureFin = seanceCourante.heureFin,
                    statut = "EN_COURS"
                )
            )
        }

        val matricule = sessionManager.matricule
            ?: return@withContext ScanResult.Erreur("Session non initialisée. Veuillez vous enrôler.")
        val deviceUuid = sessionManager.deviceUuid
            ?: return@withContext ScanResult.Erreur("UUID appareil introuvable.")

        // Vérifier si un scan de début existe déjà pour cette séance
        val dejaScan = db.emargementDao().findBySeanceId(seanceId)
        if (dejaScan != null) {
            return@withContext ScanResult.Erreur("Vous avez déjà effectué votre scan d'arrivée pour ce cours.")
        }

        // Récupérer la séance locale pour connaître l'heure officielle
        val seanceLocale = db.seanceDao().findById(seanceId)
            ?: return@withContext ScanResult.Erreur("Séance introuvable pour ce scan.")

        val heureActuelle = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())

        // Calculer le statut provisoire (règle des 15 min)
        val minutesDebut = StatutUtils.parseTimeToMinutes(seanceLocale.heureDebut)
        val minutesScan = StatutUtils.parseTimeToMinutes(heureActuelle)
        val statutProvisoire = if (minutesScan - minutesDebut <= 15) "A_L_HEURE" else "EN_RETARD"

        // Sauvegarder localement en Room
        db.emargementDao().insert(
            EmargementLocal(
                seanceId = seanceId,
                heureScanDebut = heureActuelle,
                statutProvisoire = statutProvisoire
            )
        )

        // Envoyer le scan de début au serveur Desktop (avec coordonnées GPS)
        val payload = ScanPayload(
            matricule = matricule,
            deviceUuid = deviceUuid,
            seanceId = seanceId,
            timestamp = heureActuelle,
            scanType = ScanType.DEBUT,
            lat = lat,
            lon = lon
        )
        val response = networkClient.envoyerScan(payload)

        // Vérifier si le serveur a refusé pour cause de localisation
        if (response.localisationRefusee) {
            db.emargementDao().supprimerParSeanceId(seanceId)
            return@withContext ScanResult.RefusGeo
        }

        ScanResult.ScanDebutEnregistre(statutProvisoire)
    }

    // ─── US_04 : Vérifier si l'enseignant a clôturé la séance ───────────────

    /**
     * Interroge le serveur pour savoir si la séance est déverrouillée.
     * @return true si le scan de fin est autorisé.
     */
    suspend fun verifierCloture(seanceId: Int): Boolean = withContext(Dispatchers.IO) {
        networkClient.verifierCloture(seanceId)
    }

    // ─── US_03 : Second scan (fin de cours) ──────────────────────────────────

    /**
     * Traite le second scan (fin de cours), uniquement si la séance est clôturée
     * par l'enseignant. Calcule et transmet le statut final au serveur.
     *
     * @param contenuQr Contenu du QR Code de présence scanné.
     * @param lat Latitude GPS (optionnel pour la vérification anti-fraude).
     * @param lon Longitude GPS (optionnel).
     * @return [ScanResult] avec le statut final (PRESENT / RETARD) ou une erreur.
     */
    suspend fun enregistrerSecondScan(
        contenuQr: String,
        lat: Double? = null,
        lon: Double? = null
    ): ScanResult = withContext(Dispatchers.IO) {
        val params = parseQrParams(contenuQr)
        val seanceId = if (contenuQr.startsWith("teacher-close-")) {
            contenuQr.substringAfter("teacher-close-").toIntOrNull()
        } else {
            params["seanceId"]?.toIntOrNull()
        } ?: return@withContext ScanResult.Erreur("QR Code de fin de cours invalide.")

        val matricule = sessionManager.matricule ?: return@withContext ScanResult.Erreur("Session non initialisée.")
        val deviceUuid = sessionManager.deviceUuid ?: return@withContext ScanResult.Erreur("UUID introuvable.")

        // Vérifier que la séance est bien clôturée par l'enseignant
        val cloture = networkClient.verifierCloture(seanceId)
        if (!cloture) {
            return@withContext ScanResult.Erreur("L'enseignant n'a pas encore clôturé le cours. Veuillez patienter.")
        }

        // Récupérer le premier scan depuis Room
        val premierScan = db.emargementDao().findBySeanceId(seanceId)
            ?: return@withContext ScanResult.Erreur("Aucun scan d'arrivée trouvé pour cette séance.")

        val seanceLocale = db.seanceDao().findById(seanceId)
            ?: return@withContext ScanResult.Erreur("Séance introuvable.")

        val heureActuelle = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())

        // Calculer le statut final (via StatutUtils du module :shared)
        val statutFinal = StatutUtils.determinerStatutFinal(
            heureDebutCoursStr = seanceLocale.heureDebut,
            heurePremierScanStr = premierScan.heureScanDebut,
            secondScanValide = true
        )

        // Envoyer le scan de fin au serveur Desktop (avec coordonnées GPS)
        val payload = ScanPayload(
            matricule = matricule,
            deviceUuid = deviceUuid,
            seanceId = seanceId,
            timestamp = heureActuelle,
            scanType = ScanType.FIN,
            lat = lat,
            lon = lon
        )
        val response = networkClient.envoyerScan(payload)

        if (response.localisationRefusee) {
            return@withContext ScanResult.RefusGeo
        }

        // Nettoyer la base locale
        if (response.success) {
            db.emargementDao().marquerEnvoiConfirme(seanceId)
        }

        ScanResult.StatutFinalObtenu(statutFinal.name)
    }

    // ─── US_04 (Enseignant) : Clôturer la séance ─────────────────────────────

    /**
     * L'enseignant clôture la séance depuis son application.
     * Déverrouille le scan de fin pour tous les étudiants.
     */
    suspend fun cloturerSeance(seanceId: Int): Boolean = withContext(Dispatchers.IO) {
        networkClient.cloturerSeance(seanceId)
    }

    // ─── US_05 (Délégué) : Statistiques de présence ──────────────────────────

    /**
     * Récupère les statistiques de présence de la séance pour le délégué.
     */
    suspend fun getStatistiquesSeance(seanceId: Int): SessionStatusDto? = withContext(Dispatchers.IO) {
        networkClient.getStatistiquesSeance(seanceId)
    }

    // ─── Résolution de la séance courante (après scan QR hebdomadaire) ─────────

    /**
     * Après que l'étudiant a scanné le QR Code hebdomadaire, interroge le serveur
     * pour connaître quelle séance est en cours à l'instant T.
     *
     * Format du QR Code hebdomadaire :
     * keycepass://presence?semaineId=3&classeId=B2_IT&token=XXX&serverUrl=http://...
     *
     * @param contenuQr Contenu du QR Code hebdomadaire scanné
     * @return [SeanceCouranteDto] ou null si aucune séance n'est en cours
     */
    suspend fun resolveSeanceCourante(contenuQr: String): SeanceCouranteDto? = withContext(Dispatchers.IO) {
        val params = parseQrParams(contenuQr)
        val semaineId = params["semaineId"]?.toIntOrNull() ?: return@withContext null
        val serverUrl = params["serverUrl"]
        if (serverUrl != null) {
            networkClient.serverBaseUrl = serverUrl
        }
        networkClient.getSeanceCourante(semaineId)
    }

    // ─── Utilitaires internes ─────────────────────────────────────────────────

    /**
     * Parse les paramètres d'un QR Code au format URI scheme.
     * Exemple : "keycepass://enrolement?classeId=B2_IT&token=XXX" → Map
     */
    private fun parseQrParams(contenu: String): Map<String, String> {
        return try {
            val query = contenu.substringAfter("?", "")
            if (query.isEmpty()) return emptyMap()
            query.split("&").associate { param ->
                val (key, value) = param.split("=", limit = 2)
                val decodedValue = java.net.URLDecoder.decode(value, "UTF-8")
                key to decodedValue
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    suspend fun obtenirToutesLesSeances(): List<com.ak.keycepass.android.data.local.entities.SeanceLocal> = withContext(Dispatchers.IO) {
        db.seanceDao().obtenirToutesLesSeances()
    }

    suspend fun insererSeance(seance: com.ak.keycepass.android.data.local.entities.SeanceLocal) = withContext(Dispatchers.IO) {
        db.seanceDao().insert(seance)
    }

    suspend fun mettreAJourStatutSeance(seanceId: Int, statut: String) = withContext(Dispatchers.IO) {
        db.seanceDao().mettreAJourStatut(seanceId, statut)
    }

    /**
     * Synchronise les séances en interrogeant le serveur Desktop et met à jour Room.
     */
    suspend fun synchroniserSeances(): List<com.ak.keycepass.android.data.local.entities.SeanceLocal> = withContext(Dispatchers.IO) {
        try {
            val serverList = networkClient.getToutesLesSeances()
            if (serverList.isNotEmpty()) {
                serverList.forEach {
                    db.seanceDao().insert(
                        com.ak.keycepass.android.data.local.entities.SeanceLocal(
                            idSeance = it.idSeance,
                            nomMatiere = it.nomMatiere,
                            classeId = it.classeId,
                            dateJour = it.dateJour,
                            heureDebut = it.heureDebut,
                            heureFin = it.heureFin,
                            statut = it.statutSeance
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignorer l'erreur réseau et continuer
        }
        db.seanceDao().obtenirToutesLesSeances()
    }
}

// ─── Résultats typés ──────────────────────────────────────────────────────────

sealed class EnrolementResult {
    data class Succes(val role: UserRole) : EnrolementResult()
    data class Erreur(val message: String) : EnrolementResult()
}

sealed class ScanResult {
    data class ScanDebutEnregistre(val statutProvisoire: String) : ScanResult()
    data class StatutFinalObtenu(val statut: String) : ScanResult()
    data class Erreur(val message: String) : ScanResult()
    /** Refus spécifique : l'étudiant est en dehors du périmètre autorisé (>200 m). */
    data object RefusGeo : ScanResult()
}
