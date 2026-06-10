package com.ak.keycepass.desktop.ui.viewmodel

import com.ak.keycepass.desktop.data.database.ImportService
import com.ak.keycepass.desktop.data.service.SeanceSemaineRow
import com.ak.keycepass.desktop.data.service.SeanceSemaineService
import com.ak.keycepass.desktop.data.utils.QrCodeGenerator
import com.ak.keycepass.desktop.data.server.KtorServer
import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import com.ak.keycepass.shared.domain.model.Etudiant
import com.ak.keycepass.shared.network.SessionStatusDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.image.BufferedImage
import java.io.File

// ── Profil Enseignant (mock) ──

data class TeacherProfile(
    val nom: String = "Iruzen",
    val prenom: String = "The Guy",
    val email: String = "iruz3n@keyce.ci",
    val matieres: List<String> = listOf("Ingenierie Logicielle", "Developpement Mobile"),
    val matiereCourante: String = "Ingenierie Logicielle"
)

// ── Ligne de presence (mock) ──

data class AttendanceRow(
    val id: Int,
    val nom: String,
    val prenom: String,
    val matricule: String,
    val statut: StatutFinal,
    val heureScanDebut: String,
    val heureScanFin: String,
    val classe: String = "B2_IT",
    val semestre: String = "S2_2026"
)

// ── Etat du Dashboard (mock) ──

data class DashboardState(
    val presents: Int = 0,
    val lates: Int = 0,
    val absents: Int = 0,
    val total: Int = 0,
    val rows: List<AttendanceRow> = emptyList(),
    val seanceStatut: StatutSeance = StatutSeance.PLANIFIE,
    val selectedClasse: String = "B2_IT",
    val selectedSemestre: String = "S2_2026",
    val classes: List<String> = listOf("Toutes", "B1_IT", "B1_MANAGEMENT", "B2_IT", "B2_MANAGEMENT", "B3_IT", "B3_MANAGEMENT"),
    val semestres: List<String> = listOf("Tous", "S1_2025", "S2_2025", "S1_2026", "S2_2026"),
    val enseignant: TeacherProfile = TeacherProfile(),
    val statutFilter: StatutFinal? = null
)

// ── Entree historique (mock) ──

data class HistoriqueEntry(
    val date: String,
    val label: String,
    val presents: Int,
    val retards: Int,
    val absents: Int,
    val total: Int
)

// ── ViewModel fusionne ──

class AdminViewModel {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ─── État Dashboard (mock) ────────────────────────────────────────
    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _liveEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val liveEvents: SharedFlow<String> = _liveEvents.asSharedFlow()

    // ─── État Backend (admin-desktop) ─────────────────────────────────
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _classes = MutableStateFlow<List<String>>(emptyList())
    val classes: StateFlow<List<String>> = _classes.asStateFlow()

    private val _etudiants = MutableStateFlow<List<Etudiant>>(emptyList())
    val etudiants: StateFlow<List<Etudiant>> = _etudiants.asStateFlow()

    private val _qrCodeImage = MutableStateFlow<BufferedImage?>(null)
    val qrCodeImage: StateFlow<BufferedImage?> = _qrCodeImage.asStateFlow()

    private val _statsSeance = MutableStateFlow<SessionStatusDto?>(null)
    val statsSeance: StateFlow<SessionStatusDto?> = _statsSeance.asStateFlow()

    private val _semaines = MutableStateFlow<List<SeanceSemaineRow>>(emptyList())
    val semaines: StateFlow<List<SeanceSemaineRow>> = _semaines.asStateFlow()

    private val _creationSemaineState = MutableStateFlow<CreationSemaineState>(CreationSemaineState.Idle)
    val creationSemaineState: StateFlow<CreationSemaineState> = _creationSemaineState.asStateFlow()

    // ─── Banque d'etudiants mock (par classe) ─────────────────────────
    private val classeData: Map<String, List<Triple<String, String, String>>> = mapOf(
        "B1_IT" to listOf(
            Triple("Konan", "Jean", "B1-001"), Triple("N'Guessan", "Marie", "B1-002"),
            Triple("Kouame", "Paul", "B1-003"), Triple("Yao", "Sarah", "B1-004"),
            Triple("Achi", "David", "B1-005"), Triple("Brou", "Esther", "B1-006"),
            Triple("Tano", "Franck", "B1-007"), Triple("Gore", "Nadia", "B1-008"),
        ),
        "B1_MANAGEMENT" to listOf(
            Triple("Koffi", "Alice", "B1M-001"), Triple("Zadi", "Brice", "B1M-002"),
            Triple("Dadie", "Celia", "B1M-003"), Triple("Sahi", "Daniel", "B1M-004"),
            Triple("Loba", "Elise", "B1M-005"), Triple("Goli", "Fidele", "B1M-006"),
        ),
        "B2_IT" to listOf(
            Triple("Diallo", "Amadou", "B2-001"), Triple("Kone", "Fatoumata", "B2-002"),
            Triple("Traore", "Moussa", "B2-003"), Triple("Camara", "Seydou", "B2-004"),
            Triple("Bamba", "Kadiatou", "B2-005"), Triple("Sissoko", "Ibrahim", "B2-006"),
            Triple("Diop", "Mariama", "B2-007"), Triple("Fofana", "Yacouba", "B2-008"),
            Triple("Ndiaye", "Aminata", "B2-009"), Triple("Toure", "Mamadou", "B2-010"),
            Triple("Keita", "Aicha", "B2-011"), Triple("Sow", "Ousmane", "B2-012"),
        ),
        "B2_MANAGEMENT" to listOf(
            Triple("Ouattara", "Grace", "B2M-001"), Triple("Soro", "Herve", "B2M-002"),
            Triple("Kouakou", "Irene", "B2M-003"), Triple("Ahou", "Jean-Marc", "B2M-004"),
            Triple("Kassi", "Ketty", "B2M-005"), Triple("M'Boh", "Leo", "B2M-006"),
            Triple("Adiko", "Mireille", "B2M-007"), Triple("Boni", "Noel", "B2M-008"),
        ),
        "B3_IT" to listOf(
            Triple("Allou", "Olive", "B3-001"), Triple("Beka", "Patrick", "B3-002"),
            Triple("Coulibaly", "Rachel", "B3-003"), Triple("Degny", "Serge", "B3-004"),
            Triple("Ekra", "Therese", "B3-005"), Triple("Gbongue", "Ulrich", "B3-006"),
        ),
        "B3_MANAGEMENT" to listOf(
            Triple("Hien", "Vanessa", "B3M-001"), Triple("Inza", "William", "B3M-002"),
            Triple("Jah", "Xavier", "B3M-003"), Triple("Koua", "Yvette", "B3M-004"),
        ),
    )

    private val historique = listOf(
        HistoriqueEntry("03/06", "Sem 1 - Lun", 9, 1, 2, 12),
        HistoriqueEntry("04/06", "Sem 1 - Mar", 7, 3, 2, 12),
        HistoriqueEntry("05/06", "Sem 1 - Mer", 10, 1, 1, 12),
        HistoriqueEntry("06/06", "Sem 1 - Jeu", 8, 2, 2, 12),
        HistoriqueEntry("07/06", "Sem 1 - Ven", 6, 2, 4, 12),
        HistoriqueEntry("10/06", "Sem 2 - Lun", 8, 2, 2, 12),
    )

    init { chargerDonnees() }

    // ═══════════════════════════════════════════════════════════════════
    // METHODES MOCK (Dashboard UI)
    // ═══════════════════════════════════════════════════════════════════

    fun getHistorique(): List<HistoriqueEntry> = historique

    private fun chargerDonnees() {
        chargerDonneesMockees(_state.value.selectedClasse)
    }

    private fun chargerDonneesMockees(classe: String) {
        val etudiants = classeData[classe] ?: classeData["B2_IT"]!!
        val mockRows = etudiants.mapIndexed { index, (nom, prenom, matricule) ->
            val statut = when {
                index % 7 == 3 -> StatutFinal.ABSENT
                index % 5 == 2 -> StatutFinal.RETARD
                index % 11 == 7 -> StatutFinal.ABSENT
                else -> StatutFinal.PRESENT
            }
            val debut = when (statut) {
                StatutFinal.PRESENT -> listOf("07:55","07:58","08:00","08:02","08:05","08:08","08:12","08:15")
                    .getOrElse(index % 8) { "08:0${index % 9}" }
                StatutFinal.RETARD -> listOf("08:18","08:22","08:35")[index % 3]
                else -> "---"
            }
            val fin = if (statut == StatutFinal.ABSENT) "---"
            else listOf("09:50","09:55","09:58","10:00","10:01","10:02","10:03","10:04","10:05","10:06","10:07","10:10")
                .getOrElse(index % 12) { "10:00" }

            AttendanceRow(
                id = index + 1, nom = nom, prenom = prenom, matricule = matricule,
                statut = statut, heureScanDebut = debut, heureScanFin = fin,
                classe = classe, semestre = _state.value.selectedSemestre
            )
        }
        appliquerFiltres(mockRows)
    }

    private fun appliquerFiltres(rows: List<AttendanceRow> = _state.value.rows) {
        val s = _state.value
        val filtered = if (s.selectedClasse == "Toutes") rows
        else rows.filter { it.classe == s.selectedClasse }
        val withSemestre = if (s.selectedSemestre == "Tous") filtered
        else filtered.filter { it.semestre == s.selectedSemestre }
        val withStatut = if (s.statutFilter == null) withSemestre
        else withSemestre.filter { it.statut == s.statutFilter }

        _state.value = s.copy(
            rows = withStatut,
            presents = withStatut.count { it.statut == StatutFinal.PRESENT },
            lates = withStatut.count { it.statut == StatutFinal.RETARD },
            absents = withStatut.count { it.statut == StatutFinal.ABSENT },
            total = withStatut.size
        )
    }

    fun simulateArrivee() {
        val current = _state.value
        if (current.seanceStatut != StatutSeance.EN_COURS) return

        val absentsOrLates = current.rows.filter {
            it.statut == StatutFinal.ABSENT || it.statut == StatutFinal.RETARD
        }
        if (absentsOrLates.isEmpty()) return

        val cible = absentsOrLates.random()
        val minuteAleatoire = (1..30).random()
        val heureArrivee = String.format("09:%02d", minuteAleatoire)

        val newRows = current.rows.map { row ->
            if (row.id == cible.id) {
                if (minuteAleatoire <= 15) row.copy(statut = StatutFinal.PRESENT, heureScanDebut = heureArrivee)
                else row.copy(statut = StatutFinal.RETARD, heureScanDebut = heureArrivee)
            } else row
        }

        _state.value = current.copy(
            presents = newRows.count { it.statut == StatutFinal.PRESENT },
            lates = newRows.count { it.statut == StatutFinal.RETARD },
            absents = newRows.count { it.statut == StatutFinal.ABSENT },
            rows = newRows
        )

        val statutText = if (minuteAleatoire <= 15) "present" else "en retard"
        _liveEvents.tryEmit("${cible.prenom} ${cible.nom} est $statutText")
    }

    fun filterByClasse(classe: String) {
        if (classe != _state.value.selectedClasse) {
            _state.value = _state.value.copy(selectedClasse = classe)
            chargerDonneesMockees(classe)
        }
    }

    fun filterByStatut(statut: StatutFinal?) {
        _state.value = _state.value.copy(statutFilter = statut)
        appliquerFiltres()
    }

    fun filterBySemestre(semestre: String) {
        _state.value = _state.value.copy(selectedSemestre = semestre)
        appliquerFiltres()
    }

    fun rafraichir() { chargerDonnees() }

    fun cloturerSeance() {
        _state.value = _state.value.copy(seanceStatut = StatutSeance.CLOTURE_ENSEIGNANT)
    }

    // ═══════════════════════════════════════════════════════════════════
    // METHODES BACKEND (admin-desktop)
    // ═══════════════════════════════════════════════════════════════════

    fun importerExcel(fichier: File) {
        _importState.value = ImportState.Chargement
        scope.launch(Dispatchers.IO) {
            val resultat = ImportService.importerDepuisExcel(fichier)
            _importState.value = if (resultat.erreurs.isEmpty()) {
                ImportState.Succes(resultat.lignesImportees, resultat.totalLignes)
            } else {
                ImportState.SuccesAvecAvertissements(
                    resultat.lignesImportees, resultat.totalLignes, resultat.erreurs
                )
            }
            chargerToutesLesClasses()
        }
    }

    fun chargerToutesLesClasses() {
        scope.launch(Dispatchers.IO) {
            _classes.value = ImportService.getAllClasses()
        }
    }

    fun selectionnerClasse(classeId: String) {
        scope.launch(Dispatchers.IO) {
            _etudiants.value = ImportService.getEtudiantsParClasse(classeId)
        }
    }

    fun genererQrEnrolement(classeId: String) {
        scope.launch(Dispatchers.IO) {
            val token = "${classeId}_${System.currentTimeMillis()}"
            val serverUrl = KtorServer.getServerUrl()
            _qrCodeImage.value = QrCodeGenerator.genererQrEnrolement(classeId, token, serverUrl)
        }
    }

    fun creerSemaine(classeId: String, semaineIso: String, lat: Double, lon: Double) {
        _creationSemaineState.value = CreationSemaineState.EnCours
        scope.launch(Dispatchers.IO) {
            val id = SeanceSemaineService.creerSemaine(classeId, semaineIso, lat, lon, rayonM = 200)
            _creationSemaineState.value = if (id != null) {
                chargerSemaines(classeId)
                CreationSemaineState.Succes(id)
            } else {
                CreationSemaineState.Erreur("Une semaine existe déjà pour $classeId / $semaineIso")
            }
        }
    }

    fun chargerSemaines(classeId: String) {
        scope.launch(Dispatchers.IO) {
            _semaines.value = SeanceSemaineService.getSemainesParClasse(classeId)
        }
    }

    fun genererQrPresenceSemaine(semaineId: Int) {
        scope.launch(Dispatchers.IO) {
            val semaine = SeanceSemaineService.getSemaineById(semaineId)
            if (semaine == null) return@launch
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

    fun chargerStatistiquesSeance(seanceId: Int) {
        scope.launch(Dispatchers.IO) {
            _statsSeance.value = null
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
    }

    fun onDestroy() { scope.cancel() }
}

// ─── États pour l'UI d'import Excel ───────────────────────────────────
sealed class ImportState {
    data object Idle : ImportState()
    data object Chargement : ImportState()
    data class Succes(val importees: Int, val total: Int) : ImportState()
    data class SuccesAvecAvertissements(
        val importees: Int, val total: Int, val avertissements: List<String>
    ) : ImportState()
    data class Erreur(val message: String) : ImportState()
}

// ─── États pour la création d'une semaine ─────────────────────────────
sealed class CreationSemaineState {
    data object Idle : CreationSemaineState()
    data object EnCours : CreationSemaineState()
    data class Succes(val semaineId: Int) : CreationSemaineState()
    data class Erreur(val message: String) : CreationSemaineState()
}
