package com.ak.keycepass.desktop.ui.viewmodel

import com.ak.keycepass.desktop.data.database.EmargementTable
import com.ak.keycepass.desktop.data.database.EtudiantTable
import com.ak.keycepass.desktop.data.database.SeanceTable
import com.ak.keycepass.desktop.data.database.ImportService
import com.ak.keycepass.desktop.data.service.SeanceSemaineRow
import com.ak.keycepass.desktop.data.service.SeanceSemaineService
import com.ak.keycepass.desktop.data.utils.QrCodeGenerator
import com.ak.keycepass.desktop.data.server.KtorServer
import com.ak.keycepass.shared.domain.model.Etudiant
import com.ak.keycepass.shared.network.SessionStatusDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.File

/**
 * ViewModel du module d'administration.
 * Expose des StateFlow observables pour la vue Compose Desktop.
 */
class AdminViewModel {

    private val scope = CoroutineScope(Dispatchers.IO)

    // ─── État : Import Excel ───────────────────────────────────────────────
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // ─── État : Liste des classes disponibles ──────────────────────────────
    private val _classes = MutableStateFlow<List<String>>(emptyList())
    val classes: StateFlow<List<String>> = _classes.asStateFlow()

    // ─── État : Étudiants de la classe sélectionnée ────────────────────────
    private val _etudiants = MutableStateFlow<List<Etudiant>>(emptyList())
    val etudiants: StateFlow<List<Etudiant>> = _etudiants.asStateFlow()

    // ─── État : QR Code affiché ─────────────────────────────────────────────
    private val _qrCodeImage = MutableStateFlow<BufferedImage?>(null)
    val qrCodeImage: StateFlow<BufferedImage?> = _qrCodeImage.asStateFlow()

    // ─── État : Statistiques de présence ────────────────────────────────────
    private val _statsSeance = MutableStateFlow<SessionStatusDto?>(null)
    val statsSeance: StateFlow<SessionStatusDto?> = _statsSeance.asStateFlow()

    // ─── État : Semaines de la classe sélectionnée ──────────────────────────
    private val _semaines = MutableStateFlow<List<SeanceSemaineRow>>(emptyList())
    val semaines: StateFlow<List<SeanceSemaineRow>> = _semaines.asStateFlow()

    // ─── État : Résultat de la création de semaine ───────────────────────────
    private val _creationSemaineState = MutableStateFlow<CreationSemaineState>(CreationSemaineState.Idle)
    val creationSemaineState: StateFlow<CreationSemaineState> = _creationSemaineState.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lance l'import des étudiants depuis un fichier Excel.
     */
    fun importerExcel(fichier: File) {
        _importState.value = ImportState.Chargement
        scope.launch {
            val resultat = ImportService.importerDepuisExcel(fichier)
            _importState.value = if (resultat.erreurs.isEmpty()) {
                ImportState.Succes(resultat.lignesImportees, resultat.totalLignes)
            } else {
                ImportState.SuccesAvecAvertissements(
                    resultat.lignesImportees,
                    resultat.totalLignes,
                    resultat.erreurs
                )
            }
            chargerToutesLesClasses()
        }
    }

    /**
     * Charge la liste de toutes les classes présentes dans la base.
     */
    fun chargerToutesLesClasses() {
        scope.launch {
            _classes.value = ImportService.getAllClasses()
        }
    }

    /**
     * Sélectionne une classe et charge les étudiants associés.
     */
    fun selectionnerClasse(classeId: String) {
        scope.launch {
            _etudiants.value = ImportService.getEtudiantsParClasse(classeId)
        }
    }

    /**
     * Génère le QR Code d'enrôlement pour une classe (liaison UUID appareil).
     * Ce QR Code est affiché à l'écran et scanné une seule fois par chaque étudiant.
     */
    fun genererQrEnrolement(classeId: String) {
        scope.launch {
            // Token basé sur l'ID de classe + timestamp
            val token = "${classeId}_${System.currentTimeMillis()}"
            val serverUrl = KtorServer.getServerUrl()
            _qrCodeImage.value = QrCodeGenerator.genererQrEnrolement(classeId, token, serverUrl)
        }
    }

    /**
     * Crée une nouvelle semaine d'enseignement pour une classe.
     * L'admin saisit les coordonnées GPS du lieu de cours.
     *
     * @param classeId La classe concernée
     * @param semaineIso La semaine au format ISO (ex. "2026-W24")
     * @param lat Latitude GPS du lieu de cours (saisie depuis Maps par l'admin)
     * @param lon Longitude GPS du lieu de cours
     */
    fun creerSemaine(classeId: String, semaineIso: String, lat: Double, lon: Double) {
        _creationSemaineState.value = CreationSemaineState.EnCours
        scope.launch {
            val id = SeanceSemaineService.creerSemaine(classeId, semaineIso, lat, lon, rayonM = 200)
            _creationSemaineState.value = if (id != null) {
                chargerSemaines(classeId)
                CreationSemaineState.Succes(id)
            } else {
                CreationSemaineState.Erreur("Une semaine existe déjà pour $classeId / $semaineIso")
            }
        }
    }

    /**
     * Charge les semaines disponibles pour la classe sélectionnée.
     */
    fun chargerSemaines(classeId: String) {
        scope.launch {
            _semaines.value = SeanceSemaineService.getSemainesParClasse(classeId)
        }
    }

    /**
     * Génère le QR Code de présence hebdomadaire pour une semaine.
     * Ce QR Code est affiché par l'administration et scanné une fois par semaine
     * par les étudiants pour enregistrer leurs présences.
     *
     * @param semaineId L'identifiant de la semaine dans la base
     */
    fun genererQrPresenceSemaine(semaineId: Int) {
        scope.launch {
            val semaine = SeanceSemaineService.getSemaineById(semaineId)
            if (semaine == null) {
                return@launch
            }
            val serverUrl = KtorServer.getServerUrl()
            _qrCodeImage.value = QrCodeGenerator.genererQrPresenceSemaine(
                semaineId = semaine.idSemaine,
                classeId = semaine.classeId,
                tokenSemaine = semaine.tokenSemaine,
                serverUrl = serverUrl
            )
        }
    }

    fun resetCreationSemaineState() {
        _creationSemaineState.value = CreationSemaineState.Idle
    }

    /**
     * Récupère les statistiques de présence pour une séance donnée.
     */
    fun chargerStatistiquesSeance(seanceId: Int) {
        scope.launch {
            val stats = transaction {
                val seance = SeanceTable
                    .selectAll()
                    .where { SeanceTable.idSeance eq seanceId }
                    .firstOrNull()

                if (seance != null) {
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
                } else {
                    null
                }
            }
            _statsSeance.value = stats
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }
}

// ─── États possibles pour l'interface d'import ────────────────────────────────
sealed class ImportState {
    data object Idle : ImportState()
    data object Chargement : ImportState()
    data class Succes(val importees: Int, val total: Int) : ImportState()
    data class SuccesAvecAvertissements(
        val importees: Int,
        val total: Int,
        val avertissements: List<String>
    ) : ImportState()
    data class Erreur(val message: String) : ImportState()
}

// ─── États possibles pour la création d'une semaine ───────────────────────────
sealed class CreationSemaineState {
    data object Idle : CreationSemaineState()
    data object EnCours : CreationSemaineState()
    data class Succes(val semaineId: Int) : CreationSemaineState()
    data class Erreur(val message: String) : CreationSemaineState()
}
