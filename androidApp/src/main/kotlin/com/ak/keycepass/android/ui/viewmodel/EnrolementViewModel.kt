package com.ak.keycepass.android.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ak.keycepass.android.data.local.SessionManager
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.android.data.repository.EnrolementResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel gérant le flux d'enrôlement initial (US_01).
 *
 * Exposé à la vue [LoginScreen] via des [StateFlow] observables.
 * Le collaborateur en charge de la vue n'a qu'à observer [enrolementState]
 * et appeler [enroler] lorsque le QR Code est scanné.
 */
class EnrolementViewModel(
    private val repository: AttendanceRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _enrolementState = MutableStateFlow<EnrolementUiState>(EnrolementUiState.Idle)
    val enrolementState: StateFlow<EnrolementUiState> = _enrolementState.asStateFlow()

    /**
     * Lance le processus d'enrôlement.
     * À appeler depuis la vue après que l'utilisateur a saisi son matricule
     * et scanné le QR Code d'enrôlement de sa classe.
     */
    fun enroler(
        context: Context,
        matricule: String,
        nom: String,
        prenom: String,
        contenuQr: String
    ) {
        if (matricule.isBlank()) {
            _enrolementState.value = EnrolementUiState.Erreur("Veuillez saisir votre matricule.")
            return
        }
        if (nom.isBlank() || prenom.isBlank()) {
            _enrolementState.value = EnrolementUiState.Erreur("Veuillez saisir votre nom et prenom.")
            return
        }

        _enrolementState.value = EnrolementUiState.Chargement

        viewModelScope.launch {
            val deviceUuid = SessionManager(context).getDeviceUuid(context)
            val result = repository.enroler(matricule, nom, prenom, deviceUuid, contenuQr)
            when (result) {
                is EnrolementResult.Succes -> _enrolementState.value = EnrolementUiState.Succes(result.role.name)
                is EnrolementResult.Erreur -> _enrolementState.value = EnrolementUiState.Erreur(result.message)
            }
        }
    }

    fun reinitialiser() {
        _enrolementState.value = EnrolementUiState.Idle
    }
}

// ─── États UI de l'enrôlement ─────────────────────────────────────────────────

sealed class EnrolementUiState {
    data object Idle : EnrolementUiState()
    data object Chargement : EnrolementUiState()
    data class Succes(val role: String) : EnrolementUiState()
    data class Erreur(val message: String) : EnrolementUiState()
}
