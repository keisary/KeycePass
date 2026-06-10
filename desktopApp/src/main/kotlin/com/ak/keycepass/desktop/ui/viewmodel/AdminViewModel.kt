package com.ak.keycepass.desktop.ui.viewmodel

import com.ak.keycepass.shared.domain.model.StatutFinal
import com.ak.keycepass.shared.domain.model.StatutSeance
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Données mockées pour un étudiant dans le tableau
data class AttendanceRow(
    val id: Int,
    val nom: String,
    val prenom: String,
    val matricule: String,
    val classe: String = "B2_IT",
    val semestre: String = "S2_2026",
    val statut: StatutFinal,
    val heureScanDebut: String,
    val heureScanFin: String
)

// État complet du dashboard
data class DashboardState(
    val presents: Int = 0,
    val lates: Int = 0,
    val absents: Int = 0,
    val total: Int = 0,
    val rows: List<AttendanceRow> = emptyList(),
    val seanceStatut: StatutSeance = StatutSeance.PLANIFIE,
    val selectedClasse: String = "B2_IT",
    val selectedSemestre: String = "S2_2026",
    val matiere: String = "Informatique"
)

class AdminViewModel {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow(DashboardState())
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    // Données brutes (toutes classes confondues)
    private val allRows = listOf(
        AttendanceRow(1, "Diallo", "Amadou", "B2-001", "B2_IT", "S2_2026", StatutFinal.PRESENT, "08:12", "10:05"),
        AttendanceRow(2, "Koné", "Fatoumata", "B2-002", "B2_IT", "S2_2026", StatutFinal.RETARD, "08:22", "10:03"),
        AttendanceRow(3, "Traoré", "Moussa", "B2-003", "B2_IT", "S2_2026", StatutFinal.ABSENT, "---", "---"),
        AttendanceRow(4, "Camara", "Seydou", "B2-004", "B2_IT", "S2_2026", StatutFinal.PRESENT, "07:58", "10:01"),
        AttendanceRow(5, "Bamba", "Kadiatou", "B2-005", "B2_IT", "S2_2026", StatutFinal.ABSENT, "08:10", "---"),
        AttendanceRow(6, "Sissoko", "Ibrahim", "B2-006", "B2_IT", "S2_2026", StatutFinal.PRESENT, "08:05", "10:02"),
        AttendanceRow(7, "Diop", "Mariama", "B2-007", "B2_IT", "S2_2026", StatutFinal.PRESENT, "07:55", "10:00"),
        AttendanceRow(8, "Fofana", "Yacouba", "B2-008", "B2_IT", "S2_2026", StatutFinal.RETARD, "08:18", "10:06"),
        AttendanceRow(9, "Ndiaye", "Aminata", "B2-009", "B2_IT", "S2_2026", StatutFinal.PRESENT, "08:02", "10:04"),
        AttendanceRow(10, "Touré", "Mamadou", "B2-010", "B2_IT", "S2_2026", StatutFinal.PRESENT, "08:00", "09:58"),
        AttendanceRow(11, "Keita", "Aïcha", "B2-011", "B2_IT", "S2_2026", StatutFinal.PRESENT, "08:08", "10:07"),
        AttendanceRow(12, "Sow", "Ousmane", "B2-012", "B2_IT", "S2_2026", StatutFinal.ABSENT, "---", "---"),
    )

    init {
        chargerDonneesMockees()
    }

    private fun chargerDonneesMockees() {
        appliquerFiltres()
    }

    private fun appliquerFiltres() {
        val currentState = _state.value
        val filtered = allRows.filter { row ->
            row.classe == currentState.selectedClasse &&
            row.semestre == currentState.selectedSemestre
        }

        val presents = filtered.count { it.statut == StatutFinal.PRESENT }
        val lates = filtered.count { it.statut == StatutFinal.RETARD }
        val absents = filtered.count { it.statut == StatutFinal.ABSENT }

        _state.value = currentState.copy(
            presents = presents,
            lates = lates,
            absents = absents,
            total = filtered.size,
            rows = filtered,
            seanceStatut = StatutSeance.EN_COURS
        )
    }

    fun filterByClasse(classe: String) {
        _state.value = _state.value.copy(selectedClasse = classe)
        appliquerFiltres()
    }

    fun filterBySemestre(semestre: String) {
        _state.value = _state.value.copy(selectedSemestre = semestre)
        appliquerFiltres()
    }

    fun rafraichir() {
        appliquerFiltres()
    }

    fun cloturerSeance() {
        _state.value = _state.value.copy(seanceStatut = StatutSeance.CLOTURE_ENSEIGNANT)
    }

    fun onDestroy() {
        scope.cancel()
    }
}
