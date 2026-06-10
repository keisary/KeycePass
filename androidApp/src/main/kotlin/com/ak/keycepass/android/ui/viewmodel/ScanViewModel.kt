package com.ak.keycepass.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.android.data.repository.ScanResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ak.keycepass.shared.domain.model.Seance
import com.ak.keycepass.shared.domain.model.StatutSeance

class ScanViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Pret)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _seances = MutableStateFlow<List<Seance>>(
        listOf(
            Seance(101, "Algorithmique", "B2_IT", "2026-06-10", "08:00:00", "10:00:00", StatutSeance.EN_COURS),
            Seance(102, "Réseaux", "B2_IT", "2026-06-10", "10:15:00", "12:15:00", StatutSeance.PLANIFIE),
            Seance(103, "Base de données", "B2_IT", "2026-06-10", "13:30:00", "15:30:00", StatutSeance.PLANIFIE)
        )
    )
    val seances: StateFlow<List<Seance>> = _seances.asStateFlow()

    private val _activeSeance = MutableStateFlow<Seance?>(null)
    val activeSeance: StateFlow<Seance?> = _activeSeance.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun selectSeance(seance: Seance) {
        _activeSeance.value = seance
    }

    fun getSessionReport(seanceId: Int, callback: (com.ak.keycepass.shared.network.SessionStatusDto?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val stats = repository.getStatistiquesSeance(seanceId)
            val report = stats ?: com.ak.keycepass.shared.network.SessionStatusDto(
                seanceId = seanceId,
                totalInscrits = 25,
                totalPresents = 18,
                totalRetards = 3,
                totalAbsents = 4,
                cloture = true
            )
            callback(report)
            _isLoading.value = false
        }
    }

    fun exportDelegateReport(seanceId: Int, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            delay(1000)
            callback(true)
            _isLoading.value = false
        }
    }

    private var seanceIdCourant: Int? = null

    fun onQrCodeDetecte(contenuQr: String) {
        val etatActuel = _scanState.value
        if (etatActuel is ScanUiState.Traitement || etatActuel is ScanUiState.StatutFinal) return

        viewModelScope.launch {
            _scanState.value = ScanUiState.Traitement

            val seanceId = extraireSeanceId(contenuQr)

            when (etatActuel) {
                is ScanUiState.Pret -> {
                    seanceIdCourant = seanceId
                    when (val result = repository.enregistrerPremierScan(contenuQr)) {
                        is ScanResult.ScanDebutEnregistre -> {
                            _scanState.value = ScanUiState.AttenteClotureEnseignant(
                                statutProvisoire = result.statutProvisoire
                            )
                        }
                        is ScanResult.Erreur -> _scanState.value = ScanUiState.Erreur(result.message)
                        else -> Unit
                    }
                }
                else -> Unit
            }
        }
    }

    fun cloturerSeance() {
        val id = seanceIdCourant ?: return
        viewModelScope.launch {
            val succes = repository.cloturerSeance(id)
            if (succes) _scanState.value = ScanUiState.SeanceCloturee
            else _scanState.value = ScanUiState.Erreur("Impossible de clôturer la séance. Vérifiez la connexion.")
        }
    }

    fun reinitialiser() {
        seanceIdCourant = null
        _scanState.value = ScanUiState.Pret
    }

    private fun extraireSeanceId(contenuQr: String): Int? {
        return try {
            contenuQr.substringAfter("seanceId=").substringBefore("&").toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

sealed class ScanUiState {
    data object Pret : ScanUiState()
    data object Traitement : ScanUiState()
    data class AttenteClotureEnseignant(val statutProvisoire: String) : ScanUiState()
    data class StatutFinal(val statut: String) : ScanUiState()
    data object SeanceCloturee : ScanUiState()
    data class Erreur(val message: String) : ScanUiState()
}
