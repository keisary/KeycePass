package com.ak.keycepass.desktop.ui.viewmodel

import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// ── Modeles ──

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

data class DashboardState(
    val presents: Int = 0,
    val lates: Int = 0,
    val absents: Int = 0,
    val total: Int = 0,
    val rows: List<AttendanceRow> = emptyList(),
    val seanceStatut: StatutSeance = StatutSeance.PLANIFIE,
    val selectedClasse: String = "B2_IT",
    val selectedSemestre: String = "S2_2026",
    val classes: List<String> = listOf("B2_IT", "B1_DEV", "B3_DATA"),
    val semestres: List<String> = listOf("S1_2026", "S2_2026"),
    val matiere: String = "Ingenierie Logicielle"
)

data class HistoriqueEntry(
    val date: String,
    val label: String,
    val presents: Int,
    val retards: Int,
    val absents: Int,
    val total: Int
)

// ── ViewModel ──

class AdminViewModel {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _liveEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val liveEvents: SharedFlow<String> = _liveEvents.asSharedFlow()

    // Donnees brutes (non filtrees)
    private val allStudents = listOf(
        Triple("Diallo", "Amadou", "B2-001"),
        Triple("Kone", "Fatoumata", "B2-002"),
        Triple("Traore", "Moussa", "B2-003"),
        Triple("Camara", "Seydou", "B2-004"),
        Triple("Bamba", "Kadiatou", "B2-005"),
        Triple("Sissoko", "Ibrahim", "B2-006"),
        Triple("Diop", "Mariama", "B2-007"),
        Triple("Fofana", "Yacouba", "B2-008"),
        Triple("Ndiaye", "Aminata", "B2-009"),
        Triple("Toure", "Mamadou", "B2-010"),
        Triple("Keita", "Aicha", "B2-011"),
        Triple("Sow", "Ousmane", "B2-012")
    )

    private var historique = listOf(
        HistoriqueEntry("03/06", "Sem 1 - Lun", 9, 1, 2, 12),
        HistoriqueEntry("04/06", "Sem 1 - Mar", 7, 3, 2, 12),
        HistoriqueEntry("05/06", "Sem 1 - Mer", 10, 1, 1, 12),
        HistoriqueEntry("06/06", "Sem 1 - Jeu", 8, 2, 2, 12),
        HistoriqueEntry("07/06", "Sem 1 - Ven", 6, 2, 4, 12),
        HistoriqueEntry("10/06", "Sem 2 - Lun", 8, 2, 2, 12),
    )

    init { chargerDonnees() }

    fun getHistorique(): List<HistoriqueEntry> = historique

    private fun chargerDonnees() {
        chargerDonneesMockees()
    }

    private fun chargerDonneesMockees() {
        val mockRows = allStudents.mapIndexed { index, (nom, prenom, matricule) ->
            val statut = when (index) {
                1 -> StatutFinal.RETARD
                2 -> StatutFinal.ABSENT
                4 -> StatutFinal.ABSENT
                7 -> StatutFinal.RETARD
                11 -> StatutFinal.ABSENT
                else -> StatutFinal.PRESENT
            }
            val debut = when (statut) {
                StatutFinal.PRESENT -> listOf("07:55", "07:58", "08:00", "08:02", "08:05", "08:08", "08:12")
                    .getOrElse(index % 7) { "08:0${index % 9}" }
                StatutFinal.RETARD -> listOf("08:18", "08:22")[index % 2]
                else -> "---"
            }
            val fin = if (statut == StatutFinal.ABSENT) "---"
            else listOf("09:58", "10:00", "10:01", "10:02", "10:03", "10:04", "10:05", "10:06", "10:07")
                .getOrElse(index % 9) { "10:00" }

            AttendanceRow(
                id = index + 1,
                nom = nom, prenom = prenom, matricule = matricule,
                statut = statut, heureScanDebut = debut, heureScanFin = fin,
                classe = listOf("B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT", "B2_IT")[index],
                semestre = "S2_2026"
            )
        }

        appliquerFiltres(mockRows)
    }

    private fun appliquerFiltres(rows: List<AttendanceRow> = _state.value.rows) {
        val s = _state.value
        val filtered = if (s.selectedClasse == "Toutes") rows
        else rows.filter { it.classe == s.selectedClasse }
        val final = if (s.selectedSemestre == "Tous") filtered
        else filtered.filter { it.semestre == s.selectedSemestre }

        _state.value = s.copy(
            rows = final,
            presents = final.count { it.statut == StatutFinal.PRESENT },
            lates = final.count { it.statut == StatutFinal.RETARD },
            absents = final.count { it.statut == StatutFinal.ABSENT },
            total = final.size
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
        _state.value = _state.value.copy(selectedClasse = classe)
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

    fun onDestroy() { scope.cancel() }
}
