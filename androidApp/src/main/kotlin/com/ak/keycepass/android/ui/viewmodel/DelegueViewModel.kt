package com.ak.keycepass.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.shared.network.SessionStatusDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel du délégué de classe (US_05).
 *
 * Exposé à la vue dédiée au délégué.
 * Gère :
 * - La récupération des statistiques de présence depuis le serveur Desktop.
 * - Le contenu du QR Code de présence à afficher aux étudiants (généré par le serveur).
 * - La validation et la transmission du rapport final.
 */
class DelegueViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _statsState = MutableStateFlow<StatsUiState>(StatsUiState.Idle)
    val statsState: StateFlow<StatsUiState> = _statsState.asStateFlow()

    /**
     * Récupère les statistiques de présence de la séance depuis le serveur Desktop.
     * À appeler à l'ouverture de l'écran de synthèse du délégué.
     *
     * @param seanceId L'identifiant de la séance courante.
     */
    fun chargerStatistiques(seanceId: Int) {
        _statsState.value = StatsUiState.Chargement
        viewModelScope.launch {
            val stats = repository.getStatistiquesSeance(seanceId)
            _statsState.value = if (stats != null) {
                StatsUiState.Succes(stats)
            } else {
                StatsUiState.Erreur("Impossible de récupérer les statistiques. Vérifiez la connexion.")
            }
        }
    }

    fun reinitialiser() {
        _statsState.value = StatsUiState.Idle
    }
}

// ─── États UI des statistiques du délégué ─────────────────────────────────────

sealed class StatsUiState {
    data object Idle : StatsUiState()
    data object Chargement : StatsUiState()
    data class Succes(val stats: SessionStatusDto) : StatsUiState()
    data class Erreur(val message: String) : StatsUiState()
}
