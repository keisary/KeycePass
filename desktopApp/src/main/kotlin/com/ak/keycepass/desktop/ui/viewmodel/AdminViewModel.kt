package com.ak.keycepass.desktop.ui.viewmodel

import com.ak.keycepass.desktop.data.database.EmargementTable
import com.ak.keycepass.desktop.data.database.EtudiantTable
import com.ak.keycepass.desktop.data.database.ImportService
import com.ak.keycepass.desktop.data.database.SeanceTable
import com.ak.keycepass.desktop.data.service.SeanceSemaineRow
import com.ak.keycepass.desktop.data.service.SeanceSemaineService
import com.ak.keycepass.desktop.data.service.EnseignantRow
import com.ak.keycepass.desktop.data.service.SeanceRow
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
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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
    val semestre: String = "S2_2026",
    val isEnrolled: Boolean = true
)

// ── Etat du Dashboard (mock) ──

data class DashboardState(
    val presents: Int = 0,
    val lates: Int = 0,
    val absents: Int = 0,
    val total: Int = 0,
    val rows: List<AttendanceRow> = emptyList(),
    val seanceStatut: StatutSeance = StatutSeance.PLANIFIE,
    val selectedClasse: String = "Toutes",
    val selectedSemestre: String = "S2_2026",
    // Classes chargees depuis la BDD uniquement — pas de valeur hardcodee
    val classes: List<String> = listOf("Toutes"),
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

// ── Appareil enrole (backend) ──

data class PairedDevice(
    val id: Int,
    val deviceName: String,
    val deviceId: String,
    val etudiant: String,
    val matricule: String,
    val pairedAt: String,
    val isActive: Boolean
)

// ── Periode pour historique ──

enum class PeriodeStats { SEMAINE, MOIS, TOUT }

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

    private val _historiqueBackend = MutableStateFlow<List<HistoriqueEntry>>(emptyList())
    val historiqueBackend: StateFlow<List<HistoriqueEntry>> = _historiqueBackend.asStateFlow()

    private val _enseignants = MutableStateFlow<List<EnseignantRow>>(emptyList())
    val enseignants: StateFlow<List<EnseignantRow>> = _enseignants.asStateFlow()

    private val _seancesSemaine = MutableStateFlow<List<SeanceRow>>(emptyList())
    val seancesSemaine: StateFlow<List<SeanceRow>> = _seancesSemaine.asStateFlow()

    private val _periodeStats = MutableStateFlow(PeriodeStats.SEMAINE)
    val periodeStats: StateFlow<PeriodeStats> = _periodeStats.asStateFlow()

    // ─── État Appareils enroles ──────────────────────────────────
    private val _pairedDevices = MutableStateFlow<List<PairedDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDevice>> = _pairedDevices.asStateFlow()

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

    init {
        chargerToutesLesClasses()
        chargerDonneesDepuisDB()
        chargerEnseignants()
    }

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
            chargerDonneesDepuisDB()
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

    fun rafraichir() {
        chargerDonneesDepuisDB()
    }

    fun cloturerSeance() {
        _state.value = _state.value.copy(seanceStatut = StatutSeance.CLOTURE_ENSEIGNANT)
    }

    // ═══════════════════════════════════════════════════════════════════
    // DASHBOARD : vraies donnees DB
    // ═══════════════════════════════════════════════════════════════════

    fun chargerDonneesDepuisDB() {
        scope.launch(Dispatchers.IO) {
            try {
                val classe = _state.value.selectedClasse
                val aujourdhui = java.time.LocalDate.now().toString()
                val maintenant = java.time.LocalTime.now().toString().substring(0, 8)

                val result = transaction {
                    // Chercher seance en cours aujourd'hui
                    val seance = SeanceTable
                        .selectAll()
                        .where {
                            (SeanceTable.dateJour eq aujourdhui) and
                            (SeanceTable.heureDebut lessEq maintenant) and
                            (SeanceTable.heureFin greaterEq maintenant)
                        }
                        .orderBy(SeanceTable.idSeance to org.jetbrains.exposed.sql.SortOrder.DESC)
                        .firstOrNull()

                    if (seance == null) return@transaction null

                    val seanceId = seance[SeanceTable.idSeance]
                    val classeId = seance[SeanceTable.classeId]

                    // Tous les etudiants de la classe
                    val etudiants = EtudiantTable
                        .selectAll()
                        .where { EtudiantTable.classeId eq classeId }
                        .toList()

                    // Emargements pour cette seance
                    val emargements = EmargementTable
                        .selectAll()
                        .where { EmargementTable.seanceId eq seanceId }
                        .associateBy { it[EmargementTable.etudiantId] }

                    val rows = etudiants.mapIndexed { index, etudiant ->
                        val eid = etudiant[EtudiantTable.idEtudiant]
                        val emarg = emargements[eid]
                        val enrolled = !etudiant[EtudiantTable.deviceUuid].isNullOrEmpty()

                        val statut = when (emarg?.get(EmargementTable.statutFinal)) {
                            "PRESENT" -> StatutFinal.PRESENT
                            "RETARD" -> StatutFinal.RETARD
                            "ABSENT" -> StatutFinal.ABSENT
                            "EN_ATTENTE" -> StatutFinal.EN_ATTENTE
                            else -> StatutFinal.ABSENT
                        }
                        val debut = emarg?.get(EmargementTable.horodatageScanDebut) ?: "---"
                        val fin = emarg?.get(EmargementTable.horodatageScanFin) ?: "---"
                        val debutAbrege = if (debut.length >= 5) debut.takeLast(5) else debut
                        val finAbrege = if (fin.length >= 5) fin.takeLast(5) else fin

                        AttendanceRow(
                            id = index + 1,
                            nom = etudiant[EtudiantTable.nom],
                            prenom = etudiant[EtudiantTable.prenom],
                            matricule = etudiant[EtudiantTable.matricule],
                            statut = statut,
                            heureScanDebut = debutAbrege,
                            heureScanFin = finAbrege,
                            classe = classeId,
                            isEnrolled = enrolled
                        )
                    }
                    Pair(seance, rows)
                }

                if (result != null) {
                    val (seanceRow, rows) = result
                    val presents = rows.count { it.statut == StatutFinal.PRESENT }
                    val retards = rows.count { it.statut == StatutFinal.RETARD }
                    val absents = rows.count { it.statut == StatutFinal.ABSENT }

                    _state.value = _state.value.copy(
                        presents = presents,
                        lates = retards,
                        absents = absents,
                        total = rows.size,
                        rows = rows,
                        seanceStatut = StatutSeance.EN_COURS,
                        enseignant = _state.value.enseignant.copy(
                            matiereCourante = seanceRow[SeanceTable.nomMatiere] ?: "Cours"
                        )
                    )
                } else {
                    val dbClasses = ImportService.getAllClasses()
                    _state.value = _state.value.copy(
                        rows = emptyList(),
                        presents = 0,
                        lates = 0,
                        absents = 0,
                        total = 0,
                        seanceStatut = StatutSeance.PLANIFIE,
                        classes = listOf("Toutes") + dbClasses
                    )
                }
            } catch (e: Exception) {
                println("[KeycePass] Erreur chargement DB: ${e.message}")
                val dbClasses = try { ImportService.getAllClasses() } catch (ex: Exception) { emptyList() }
                _state.value = _state.value.copy(
                    rows = emptyList(),
                    presents = 0,
                    lates = 0,
                    absents = 0,
                    total = 0,
                    classes = listOf("Toutes") + dbClasses
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // METHODES BACKEND (admin-desktop)
    // ═══════════════════════════════════════════════════════════════════

    fun importerExcel(fichier: File) {
        _importState.value = ImportState.Chargement
        scope.launch(Dispatchers.IO) {
            try {
                val resultat = ImportService.importerDepuisExcel(fichier)
                _importState.value = if (resultat.erreurs.isEmpty()) {
                    ImportState.Succes(resultat.lignesImportees, resultat.totalLignes)
                } else {
                    ImportState.SuccesAvecAvertissements(
                        resultat.lignesImportees, resultat.totalLignes, resultat.erreurs
                    )
                }
                chargerToutesLesClasses()
            } catch (e: Exception) {
                _importState.value = ImportState.Erreur(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun chargerToutesLesClasses() {
        scope.launch(Dispatchers.IO) {
            try {
                val dbClasses = ImportService.getAllClasses()
                _classes.value = dbClasses
                _state.value = _state.value.copy(
                    classes = listOf("Toutes") + dbClasses
                )
            } catch (e: Exception) {
                println("[KeycePass] Erreur chargement classes: ${e.message}")
            }
        }
    }

    fun selectionnerClasse(classeId: String) {
        scope.launch(Dispatchers.IO) {
            try {
                _etudiants.value = ImportService.getEtudiantsParClasse(classeId)
            } catch (e: Exception) {
                println("[KeycePass] Erreur selection classe: ${e.message}")
            }
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

    fun chargerHistorique(periode: PeriodeStats = _periodeStats.value) {
        scope.launch(Dispatchers.IO) {
            try {
                val maintenant = java.time.LocalDate.now()
                val dateDebut = when (periode) {
                    PeriodeStats.SEMAINE -> maintenant.minusDays(7).toString()
                    PeriodeStats.MOIS -> maintenant.minusDays(30).toString()
                    PeriodeStats.TOUT -> "2000-01-01"
                }

                val entries = transaction {
                    val seances = SeanceTable
                        .selectAll()
                        .where { SeanceTable.dateJour greaterEq dateDebut }
                        .orderBy(SeanceTable.dateJour to org.jetbrains.exposed.sql.SortOrder.DESC)
                        .limit(10)
                        .map { seance ->
                            val seanceId = seance[SeanceTable.idSeance]
                            val emargements = EmargementTable
                                .selectAll()
                                .where { EmargementTable.seanceId eq seanceId }

                            val presents = emargements.count { it[EmargementTable.statutFinal] == "PRESENT" }
                            val retards = emargements.count { it[EmargementTable.statutFinal] == "RETARD" }
                            val absents = emargements.count { it[EmargementTable.statutFinal] == "ABSENT" }
                            val total = presents + retards + absents
                            val date = seance[SeanceTable.dateJour] ?: ""
                            val label = seance[SeanceTable.nomMatiere] ?: "Seance"

                            HistoriqueEntry(
                                date = date.takeLast(5),
                                label = label,
                                presents = presents,
                                retards = retards,
                                absents = absents,
                                total = total
                            )
                        }
                    seances.ifEmpty {
                        // Fallback vers donnees mockees si aucune seance en DB
                        historique
                    }
                }
                _historiqueBackend.value = entries
            } catch (e: Exception) {
                println("[KeycePass] Erreur historique: ${e.message}")
                _historiqueBackend.value = historique
            }
        }
    }

    fun changerPeriode(periode: PeriodeStats) {
        _periodeStats.value = periode
        chargerHistorique(periode)
    }

    fun chargerAppareilsEnroles() {
        scope.launch(Dispatchers.IO) {
            try {
                val devices = transaction {
                    EtudiantTable
                        .selectAll()
                        .where { EtudiantTable.deviceUuid.isNotNull() }
                        .mapIndexed { index, row ->
                            val uuid = row[EtudiantTable.deviceUuid] ?: ""
                            PairedDevice(
                                id = index + 1,
                                deviceName = "Appareil ${uuid.take(8)}...",
                                deviceId = uuid,
                                etudiant = "${row[EtudiantTable.prenom]} ${row[EtudiantTable.nom]}",
                                matricule = row[EtudiantTable.matricule],
                                pairedAt = "Enrole",
                                isActive = uuid.isNotEmpty()
                            )
                        }
                }
                _pairedDevices.value = devices.ifEmpty {
                    listOf(PairedDevice(0, "Aucun appareil", "", "", "", "", false))
                }
            } catch (e: Exception) {
                println("[KeycePass] Erreur chargement appareils: ${e.message}")
            }
        }
    }

    fun dissocierAppareil(matricule: String) {
        scope.launch(Dispatchers.IO) {
            try {
                transaction {
                    EtudiantTable.update({ EtudiantTable.matricule eq matricule }) {
                        it[deviceUuid] = null
                    }
                }
                chargerAppareilsEnroles()
            } catch (e: Exception) {
                println("[KeycePass] Erreur dissociation: ${e.message}")
            }
        }
    }

    fun onDestroy() { scope.cancel() }

    // ─── Enseignants ───────────────────────────────────────────────

    fun chargerEnseignants() {
        scope.launch(Dispatchers.IO) {
            try {
                _enseignants.value = SeanceSemaineService.getTousLesEnseignants()
            } catch (e: Exception) {
                println("[KeycePass] Erreur chargement enseignants: ${e.message}")
            }
        }
    }

    fun creerEnseignant(matricule: String, nom: String, prenom: String) {
        scope.launch(Dispatchers.IO) {
            try {
                SeanceSemaineService.creerEnseignant(matricule, nom, prenom)
                chargerEnseignants()
            } catch (e: Exception) {
                println("[KeycePass] Erreur creation enseignant: ${e.message}")
            }
        }
    }

    // ─── Séances d'une semaine ──────────────────────────────────────

    fun chargerSeancesDeLaSemaine(semaineId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                _seancesSemaine.value = SeanceSemaineService.getSeancesParSemaine(semaineId)
            } catch (e: Exception) {
                println("[KeycePass] Erreur chargement seances: ${e.message}")
            }
        }
    }

    fun ajouterSeanceALaSemaine(
        semaineId: Int,
        nomMatiere: String,
        classeId: String,
        dateJour: String,
        heureDebut: String,
        heureFin: String,
        enseignantId: Int?
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                SeanceSemaineService.ajouterSeanceALaSemaine(
                    semaineId = semaineId,
                    nomMatiere = nomMatiere,
                    classeId = classeId,
                    dateJour = dateJour,
                    heureDebut = heureDebut,
                    heureFin = heureFin,
                    enseignantId = enseignantId
                )
                chargerSeancesDeLaSemaine(semaineId)
                chargerDonneesDepuisDB()
            } catch (e: Exception) {
                println("[KeycePass] Erreur ajout seance: ${e.message}")
            }
        }
    }

    fun enregistrerSeance(
        idSeance: Int?,
        semaineId: Int,
        nomMatiere: String,
        classeId: String,
        dateJour: String,
        heureDebut: String,
        heureFin: String,
        enseignantId: Int?
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                SeanceSemaineService.enregistrerSeance(
                    idSeance = idSeance,
                    semaineId = semaineId,
                    nomMatiere = nomMatiere,
                    classeId = classeId,
                    dateJour = dateJour,
                    heureDebut = heureDebut,
                    heureFin = heureFin,
                    enseignantId = enseignantId
                )
                chargerSeancesDeLaSemaine(semaineId)
                chargerDonneesDepuisDB()
            } catch (e: Exception) {
                println("[KeycePass] Erreur enregistrement seance: ${e.message}")
            }
        }
    }

    fun genererFichierTestExcel(destination: File) {
        scope.launch(Dispatchers.IO) {
            try {
                ImportService.genererFichierTestExcel(destination)
            } catch (e: Exception) {
                println("[KeycePass] Erreur generation Excel test: ${e.message}")
            }
        }
    }
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
