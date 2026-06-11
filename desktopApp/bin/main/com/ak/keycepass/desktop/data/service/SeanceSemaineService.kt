package com.ak.keycepass.desktop.data.service

import com.ak.keycepass.desktop.data.database.SeanceSemaineTable
import com.ak.keycepass.desktop.data.database.SeanceTable
import com.ak.keycepass.desktop.data.database.EnseignantTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Service de gestion des semaines d'enseignement et de leur QR code.
 *
 * Workflow :
 * 1. En début de semaine, l'admin crée une [SeanceSemaine] pour chaque classe
 *    en saisissant les coordonnées GPS de référence du lieu de cours.
 * 2. Un token HMAC-SHA256 est généré automatiquement — c'est ce token qui est
 *    encodé dans le QR code hebdomadaire affiché par l'administration.
 * 3. Les séances individuelles (cours + prof) sont associées à cette semaine.
 * 4. L'app mobile scanne le QR une fois par semaine pour connaître la séance
 *    en cours (via GET /api/semaine/{semaineId}/seance-courante).
 */
object SeanceSemaineService {

    /**
     * Clé secrète interne pour la signature HMAC.
     * Dans un système de production, cette clé serait stockée dans un
     * fichier de configuration sécurisé ou un keystore.
     */
    private const val HMAC_SECRET = "KeycePass-Internal-Secret-2026"

    // ─── Création d'une semaine ───────────────────────────────────────────────

    /**
     * Crée une nouvelle semaine d'enseignement pour une classe.
     *
     * @param classeId Identifiant de la classe (ex. "B2_IT")
     * @param semaineIso Semaine au format ISO 8601 (ex. "2026-W24")
     * @param latRef Latitude GPS de référence saisie par l'admin
     * @param lonRef Longitude GPS de référence saisie par l'admin
     * @param rayonM Rayon de tolérance en mètres (défaut global : 200)
     * @return L'identifiant de la semaine créée, ou null si elle existe déjà
     */
    fun creerSemaine(
        classeId: String,
        semaineIso: String,
        latRef: Double,
        lonRef: Double,
        rayonM: Int = 200
    ): Int? {
        val token = genererTokenSemaine(classeId, semaineIso)

        return transaction {
            // Vérifier si la semaine existe déjà pour cette classe
            val existante = SeanceSemaineTable
                .selectAll()
                .where {
                    (SeanceSemaineTable.classeId eq classeId) and
                    (SeanceSemaineTable.semaineIso eq semaineIso)
                }
                .firstOrNull()

            if (existante != null) return@transaction null

            val result = SeanceSemaineTable.insert {
                it[SeanceSemaineTable.classeId] = classeId
                it[SeanceSemaineTable.semaineIso] = semaineIso
                it[SeanceSemaineTable.latReference] = latRef
                it[SeanceSemaineTable.lonReference] = lonRef
                it[SeanceSemaineTable.rayonMetres] = rayonM
                it[SeanceSemaineTable.tokenSemaine] = token
            }
            result[SeanceSemaineTable.idSemaine]
        }
    }

    // ─── Ajout d'une séance à une semaine ────────────────────────────────────

    /**
     * Ajoute une séance de cours (cours + horaires + prof) à une semaine existante.
     *
     * @param semaineId Identifiant de la semaine parente
     * @param nomMatiere Nom de la matière (ex. "Algorithmique avancée")
     * @param classeId Classe concernée
     * @param dateJour Date du cours (format YYYY-MM-DD)
     * @param heureDebut Heure de début (format HH:MM:SS)
     * @param heureFin Heure de fin (format HH:MM:SS)
     * @param enseignantId Identifiant de l'enseignant (nullable)
     * @return L'identifiant de la séance créée
     */
    fun ajouterSeanceALaSemaine(
        semaineId: Int,
        nomMatiere: String,
        classeId: String,
        dateJour: String,
        heureDebut: String,
        heureFin: String,
        enseignantId: Int? = null
    ): Int {
        return transaction {
            val result = SeanceTable.insert {
                it[SeanceTable.nomMatiere] = nomMatiere
                it[SeanceTable.classeId] = classeId
                it[SeanceTable.dateJour] = dateJour
                it[SeanceTable.heureDebut] = heureDebut
                it[SeanceTable.heureFin] = heureFin
                it[SeanceTable.statutSeance] = "PLANIFIE"
                it[SeanceTable.semaineId] = semaineId
                it[SeanceTable.enseignantId] = enseignantId
            }
            result[SeanceTable.idSeance]
        }
    }

    /**
     * Enregistre ou met à jour une séance de cours pour une semaine donnée.
     *
     * @param idSeance Identifiant de la séance existante (si null, une nouvelle séance sera créée)
     * @param semaineId Identifiant de la semaine parente
     * @param nomMatiere Nom de la matière (ou "TPE")
     * @param classeId Classe concernée
     * @param dateJour Date du cours (format YYYY-MM-DD)
     * @param heureDebut Heure de début (format HH:MM:SS)
     * @param heureFin Heure de fin (format HH:MM:SS)
     * @param enseignantId Identifiant de l'enseignant (nullable)
     * @return L'identifiant de la séance (créée ou mise à jour)
     */
    fun enregistrerSeance(
        idSeance: Int?,
        semaineId: Int,
        nomMatiere: String,
        classeId: String,
        dateJour: String,
        heureDebut: String,
        heureFin: String,
        enseignantId: Int? = null
    ): Int {
        return transaction {
            if (idSeance != null) {
                SeanceTable.update({ SeanceTable.idSeance eq idSeance }) {
                    it[SeanceTable.nomMatiere] = nomMatiere
                    it[SeanceTable.heureDebut] = heureDebut
                    it[SeanceTable.heureFin] = heureFin
                    it[SeanceTable.enseignantId] = enseignantId
                }
                idSeance
            } else {
                val result = SeanceTable.insert {
                    it[SeanceTable.nomMatiere] = nomMatiere
                    it[SeanceTable.classeId] = classeId
                    it[SeanceTable.dateJour] = dateJour
                    it[SeanceTable.heureDebut] = heureDebut
                    it[SeanceTable.heureFin] = heureFin
                    it[SeanceTable.statutSeance] = "PLANIFIE"
                    it[SeanceTable.semaineId] = semaineId
                    it[SeanceTable.enseignantId] = enseignantId
                }
                result[SeanceTable.idSeance]
            }
        }
    }


    // ─── Enseignants ──────────────────────────────────────────────────────────

    /**
     * Crée un enseignant dans la base de données.
     */
    fun creerEnseignant(matricule: String, nom: String, prenom: String): Int? {
        return transaction {
            val existante = EnseignantTable
                .selectAll()
                .where { EnseignantTable.matriculeEnseignant eq matricule }
                .firstOrNull()

            if (existante != null) return@transaction null

            val result = EnseignantTable.insert {
                it[EnseignantTable.matriculeEnseignant] = matricule
                it[EnseignantTable.nom] = nom
                it[EnseignantTable.prenom] = prenom
            }
            result[EnseignantTable.idEnseignant]
        }
    }

    /**
     * Récupère tous les enseignants.
     */
    fun getTousLesEnseignants(): List<EnseignantRow> {
        return transaction {
            EnseignantTable
                .selectAll()
                .map {
                    EnseignantRow(
                        idEnseignant = it[EnseignantTable.idEnseignant],
                        matriculeEnseignant = it[EnseignantTable.matriculeEnseignant],
                        nom = it[EnseignantTable.nom],
                        prenom = it[EnseignantTable.prenom],
                        deviceUuid = it[EnseignantTable.deviceUuid]
                    )
                }
        }
    }

    // ─── Séances d'une semaine ────────────────────────────────────────────────

    /**
     * Récupère toutes les séances associées à une semaine.
     */
    fun getSeancesParSemaine(semaineId: Int): List<SeanceRow> {
        return transaction {
            (SeanceTable leftJoin EnseignantTable)
                .selectAll()
                .where { SeanceTable.semaineId eq semaineId }
                .map {
                    val profNom = it.getOrNull(EnseignantTable.nom)
                    val profPrenom = it.getOrNull(EnseignantTable.prenom)
                    val nomComplet = if (profNom != null && profPrenom != null) "$profPrenom $profNom" else null

                    SeanceRow(
                        idSeance = it[SeanceTable.idSeance],
                        nomMatiere = it[SeanceTable.nomMatiere],
                        classeId = it[SeanceTable.classeId],
                        dateJour = it[SeanceTable.dateJour],
                        heureDebut = it[SeanceTable.heureDebut],
                        heureFin = it[SeanceTable.heureFin],
                        statutSeance = it[SeanceTable.statutSeance],
                        enseignantId = it[SeanceTable.enseignantId],
                        enseignantNomComplet = nomComplet,
                        semaineId = it[SeanceTable.semaineId]
                    )
                }
        }
    }

    // ─── Récupération ─────────────────────────────────────────────────────────

    /**
     * Récupère toutes les semaines pour une classe donnée.
     */
    fun getSemainesParClasse(classeId: String): List<SeanceSemaineRow> {
        return transaction {
            SeanceSemaineTable
                .selectAll()
                .where { SeanceSemaineTable.classeId eq classeId }
                .map {
                    SeanceSemaineRow(
                        idSemaine = it[SeanceSemaineTable.idSemaine],
                        classeId = it[SeanceSemaineTable.classeId],
                        semaineIso = it[SeanceSemaineTable.semaineIso],
                        latReference = it[SeanceSemaineTable.latReference],
                        lonReference = it[SeanceSemaineTable.lonReference],
                        rayonMetres = it[SeanceSemaineTable.rayonMetres],
                        tokenSemaine = it[SeanceSemaineTable.tokenSemaine]
                    )
                }
        }
    }

    /**
     * Récupère une semaine par son identifiant.
     */
    fun getSemaineById(semaineId: Int): SeanceSemaineRow? {
        return transaction {
            SeanceSemaineTable
                .selectAll()
                .where { SeanceSemaineTable.idSemaine eq semaineId }
                .firstOrNull()
                ?.let {
                    SeanceSemaineRow(
                        idSemaine = it[SeanceSemaineTable.idSemaine],
                        classeId = it[SeanceSemaineTable.classeId],
                        semaineIso = it[SeanceSemaineTable.semaineIso],
                        latReference = it[SeanceSemaineTable.latReference],
                        lonReference = it[SeanceSemaineTable.lonReference],
                        rayonMetres = it[SeanceSemaineTable.rayonMetres],
                        tokenSemaine = it[SeanceSemaineTable.tokenSemaine]
                    )
                }
        }
    }

    // ─── Génération du token HMAC ─────────────────────────────────────────────

    /**
     * Génère un token HMAC-SHA256 signé à partir de l'identité de la semaine.
     * Ce token est opaque et ne révèle aucune information sur le planning.
     *
     * Format de la charge : "{classeId}:{semaineIso}"
     *
     * @return Token hexadécimal de 64 caractères
     */
    fun genererTokenSemaine(classeId: String, semaineIso: String): String {
        val payload = "$classeId:$semaineIso"
        val mac = Mac.getInstance("HmacSHA256")
        val keySpec = SecretKeySpec(HMAC_SECRET.toByteArray(Charsets.UTF_8), "HmacSHA256")
        mac.init(keySpec)
        val bytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

// ─── Data classes de retour ───────────────────────────────────────────────────

data class SeanceSemaineRow(
    val idSemaine: Int,
    val classeId: String,
    val semaineIso: String,
    val latReference: Double,
    val lonReference: Double,
    val rayonMetres: Int,
    val tokenSemaine: String
)

data class EnseignantRow(
    val idEnseignant: Int,
    val matriculeEnseignant: String,
    val nom: String,
    val prenom: String,
    val deviceUuid: String?
)

data class SeanceRow(
    val idSeance: Int,
    val nomMatiere: String,
    val classeId: String,
    val dateJour: String,
    val heureDebut: String,
    val heureFin: String,
    val statutSeance: String,
    val enseignantId: Int?,
    val enseignantNomComplet: String?,
    val semaineId: Int?
)
