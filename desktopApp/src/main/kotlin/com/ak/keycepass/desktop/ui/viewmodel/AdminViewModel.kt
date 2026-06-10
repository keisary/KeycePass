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

// Donnees mockees pour un etudiant dans le tableau
data class AttendanceRow(
    val id: Int,
    val nom: String,
    val prenom: String,
    val matricule: String,
    val statut: StatutFinal,
    val heureScanDebut: String,
    val heureScanFin: String
)

// Etat complet du dashboard
data class DashboardState(
    val presents: Int = 0,
    val lates: Int = 0,
    val absents: Int = 0,
    val total: Int = 0,
    val rows: List<AttendanceRow> = emptyList(),
    val seanceStatut: StatutSeance = StatutSeance.PLANIFIE,
    val selectedClasse: String = "B2_IT",
    val selectedSemestre: String = "S2_2026",
    val matiere: String = "Ingenierie Logicielle"
)

class AdminViewModel {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Evenements en direct pour les toasts
    private val _liveEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val liveEvents: SharedFlow<String> = _liveEvents.asSharedFlow()

    init {
        chargerDonneesMockees()
    }

    private val mockStudents = listOf(
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

    private fun chargerDonneesMockees() {
        val mockRows = listOf(
            AttendanceRow(1, "Diallo", "Amadou", "B2-001", StatutFinal.PRESENT, "08:12", "10:05"),
            AttendanceRow(2, "Kone", "Fatoumata", "B2-002", StatutFinal.RETARD, "08:22", "10:03"),
            AttendanceRow(3, "Traore", "Moussa", "B2-003", StatutFinal.ABSENT, "---", "---"),
            AttendanceRow(4, "Camara", "Seydou", "B2-004", StatutFinal.PRESENT, "07:58", "10:01"),
            AttendanceRow(5, "Bamba", "Kadiatou", "B2-005", StatutFinal.ABSENT, "08:10", "---"),
            AttendanceRow(6, "Sissoko", "Ibrahim", "B2-006", StatutFinal.PRESENT, "08:05", "10:02"),
            AttendanceRow(7, "Diop", "Mariama", "B2-007", StatutFinal.PRESENT, "07:55", "10:00"),
            AttendanceRow(8, "Fofana", "Yacouba", "B2-008", StatutFinal.RETARD, "08:18", "10:06"),
            AttendanceRow(9, "Ndiaye", "Aminata", "B2-009", StatutFinal.PRESENT, "08:02", "10:04"),
            AttendanceRow(10, "Toure", "Mamadou", "B2-010", StatutFinal.PRESENT, "08:00", "09:58"),
            AttendanceRow(11, "Keita", "Aicha", "B2-011", StatutFinal.PRESENT, "08:08", "10:07"),
            AttendanceRow(12, "Sow", "Ousmane", "B2-012", StatutFinal.ABSENT, "---", "---"),
        )

        val presents = mockRows.count { it.statut == StatutFinal.PRESENT }
        val lates = mockRows.count { it.statut == StatutFinal.RETARD }
        val absents = mockRows.count { it.statut == StatutFinal.ABSENT }

        _state.value = DashboardState(
            presents = presents,
            lates = lates,
            absents = absents,
            total = mockRows.size,
            rows = mockRows,
            seanceStatut = StatutSeance.EN_COURS
        )
    }

    /**
     * Simule l'arrivee live d'un etudiant absent ou en retard.
     * Utilisee par le bouton "Simulation live" du Dashboard.
     */
    fun simulateArrivee() {
        val current = _state.value
        if (current.seanceStatut != StatutSeance.EN_COURS) return

        // Choisir un etudiant absent ou en retard au hasard
        val absentsOrLates = current.rows.filter {
            it.statut == StatutFinal.ABSENT || it.statut == StatutFinal.RETARD
        }
        if (absentsOrLates.isEmpty()) return

        val cible = absentsOrLates.random()
        val minuteAleatoire = (1..30).random()
        val heureArrivee = String.format("09:%02d", minuteAleatoire)

        val newRows = current.rows.map { row ->
            if (row.id == cible.id) {
                if (minuteAleatoire <= 15) {
                    row.copy(statut = StatutFinal.PRESENT, heureScanDebut = heureArrivee)
                } else {
                    row.copy(statut = StatutFinal.RETARD, heureScanDebut = heureArrivee)
                }
            } else row
        }

        val newPresents = newRows.count { it.statut == StatutFinal.PRESENT }
        val newLates = newRows.count { it.statut == StatutFinal.RETARD }
        val newAbsents = newRows.count { it.statut == StatutFinal.ABSENT }

        _state.value = current.copy(
            presents = newPresents,
            lates = newLates,
            absents = newAbsents,
            rows = newRows
        )

        val statutText = if (minuteAleatoire <= 15) "present" else "en retard"
        _liveEvents.tryEmit("${cible.prenom} ${cible.nom} est $statutText")
    }

    fun filterByClasse(classe: String) {
        _state.value = _state.value.copy(selectedClasse = classe)
    }

    fun filterBySemestre(semestre: String) {
        _state.value = _state.value.copy(selectedSemestre = semestre)
    }

    fun rafraichir() {
        chargerDonneesMockees()
    }

    fun cloturerSeance() {
        _state.value = _state.value.copy(seanceStatut = StatutSeance.CLOTURE_ENSEIGNANT)
    }

    fun onDestroy() {
        scope.cancel()
    }
}
