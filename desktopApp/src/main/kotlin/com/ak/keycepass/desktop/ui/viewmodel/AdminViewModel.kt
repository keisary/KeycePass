package com.ak.keycepass.desktop.ui.viewmodel

import com.ak.keycepass.desktop.data.database.DatabaseTables.EmargementTable
import com.ak.keycepass.desktop.data.database.DatabaseTables.EtudiantTable
import com.ak.keycepass.desktop.data.database.DatabaseTables.SeanceTable
import com.ak.keycepass.desktop.data.database.ImportService
import com.ak.keycepass.desktop.data.utils.QrCodeGenerator
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
            _qrCodeImage.value = QrCodeGenerator.genererQrEnrolement(classeId, token)
        }
    }

    /**
     * Génère le QR Code de présence pour une séance journalière.
     * Ce QR Code change à chaque séance (anti-fraude).
     */
    fun genererQrPresence(seanceId: Int) {
        scope.launch {
            val jeton = "${seanceId}_${System.currentTimeMillis()}"
            _qrCodeImage.value = QrCodeGenerator.genererQrPresence(seanceId, jeton)
        }
    }

    /**
     * Récupère les statistiques de présence pour une séance donnée.
     */
    fun chargerStatistiquesSeance(seanceId: Int) {
        scope.launch {
            val stats = transaction {
                val etudiants = EtudiantTable.selectAll()
                val emargements = EmargementTable
                    .selectAll()
                    .where { EmargementTable.seanceId eq seanceId }
                    .toList()

                val totalPresents = emargements.count { it[EmargementTable.statutFinal] == "PRESENT" }
                val totalRetards = emargements.count { it[EmargementTable.statutFinal] == "RETARD" }
                val totalAbsents = emargements.count { it[EmargementTable.statutFinal] == "ABSENT" }
                val statut = SeanceTable
                    .selectAll()
                    .where { SeanceTable.idSeance eq seanceId }
                    .firstOrNull()
                    ?.get(SeanceTable.statutSeance) ?: "PLANIFIE"

                SessionStatusDto(
                    seanceId = seanceId,
                    totalInscrits = etudiants.count(),
                    totalPresents = totalPresents,
                    totalRetards = totalRetards,
                    totalAbsents = totalAbsents,
                    cloture = statut == "CLOTURE_ENSEIGNANT"
                )
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
