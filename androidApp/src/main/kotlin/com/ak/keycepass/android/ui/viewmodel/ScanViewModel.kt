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

/**
 * ViewModel gérant le cycle complet de scan de présence (US_02, US_03, US_04).
 *
 * Exposé à la vue [ScanScreen] via des [StateFlow] observables.
 * Le collaborateur en charge de la vue appelle [onQrCodeDetecte] à chaque
 * nouveau contenu QR détecté par CameraX.
 */
class ScanViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Pret)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    // Identifiant de la séance en cours (mis à jour lors du premier scan)
    private var seanceIdCourant: Int? = null

    /**
     * Point d'entrée unique depuis la vue.
     * La logique interne détermine automatiquement si c'est un scan de début,
     * si la séance est déverrouillée ou s'il faut déclencher le scan de fin.
     *
     * @param contenuQr Contenu brut du QR Code détecté par CameraX.
     */
    fun onQrCodeDetecte(contenuQr: String) {
        // Ignorer les scans si on est déjà en cours de traitement
        val etatActuel = _scanState.value
        if (etatActuel is ScanUiState.Traitement || etatActuel is ScanUiState.StatutFinal) return

        viewModelScope.launch {
            _scanState.value = ScanUiState.Traitement

            // Extraire le seanceId du QR pour le suivre localement
            val seanceId = extraireSeanceId(contenuQr)

            when (etatActuel) {
                // Premier scan : l'étudiant arrive en cours
                is ScanUiState.Pret -> {
                    seanceIdCourant = seanceId
                    when (val result = repository.enregistrerPremierScan(contenuQr)) {
                        is ScanResult.ScanDebutEnregistre -> {
                            _scanState.value = ScanUiState.AttenteClotureEnseignant(
                                statutProvisoire = result.statutProvisoire
                            )
                        }
                        is ScanResult.Erreur -> {
                            _scanState.value = ScanUiState.Erreur(result.message)
                        }
                        else -> Unit
                    }
                }

                // En attente de clôture : vérifier si l'enseignant a déverrouillé
                is ScanUiState.AttenteClotureEnseignant -> {
                    val id = seanceIdCourant ?: seanceId ?: run {
                        _scanState.value = ScanUiState.Erreur("Séance non identifiée.")
                        return@launch
                    }
                    val cloture = repository.verifierCloture(id)
                    if (cloture) {
                        // Déclencher immédiatement le scan de fin
                        when (val result = repository.enregistrerSecondScan(contenuQr)) {
                            is ScanResult.StatutFinalObtenu -> {
                                _scanState.value = ScanUiState.StatutFinal(result.statut)
                            }
                            is ScanResult.Erreur -> {
                                _scanState.value = ScanUiState.Erreur(result.message)
                            }
                            else -> Unit
                        }
                    } else {
                        _scanState.value = ScanUiState.AttenteClotureEnseignant(etatActuel.statutProvisoire)
                    }
                }

                else -> Unit
            }
        }
    }

    /**
     * Permet à l'enseignant de clôturer la séance directement depuis son écran.
     */
    fun cloturerSeance() {
        val id = seanceIdCourant ?: return
        viewModelScope.launch {
            val succes = repository.cloturerSeance(id)
            if (succes) {
                _scanState.value = ScanUiState.SeanceCloturee
            } else {
                _scanState.value = ScanUiState.Erreur("Impossible de clôturer la séance. Vérifiez la connexion.")
            }
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

// ─── États UI du cycle de scan ────────────────────────────────────────────────

sealed class ScanUiState {
    /** Prêt à scanner — en attente du premier QR Code de présence */
    data object Pret : ScanUiState()

    /** Traitement en cours — spinner à afficher */
    data object Traitement : ScanUiState()

    /**
     * Premier scan enregistré — en attente que l'enseignant clôture le cours.
     * @param statutProvisoire "A_L_HEURE" ou "EN_RETARD"
     */
    data class AttenteClotureEnseignant(val statutProvisoire: String) : ScanUiState()

    /**
     * Statut final obtenu après le second scan.
     * @param statut "PRESENT" ou "RETARD"
     */
    data class StatutFinal(val statut: String) : ScanUiState()

    /** La séance a été clôturée par l'enseignant (état affiché côté enseignant) */
    data object SeanceCloturee : ScanUiState()

    /** Erreur survenue à n'importe quelle étape */
    data class Erreur(val message: String) : ScanUiState()
}
