package com.ak.keycepass.android.ui.viewmodel

import android.content.Context
import com.ak.keycepass.android.data.local.SessionManager
import com.ak.keycepass.android.data.repository.AttendanceRepository
import com.ak.keycepass.android.data.repository.ScanResult
import com.google.android.gms.location.LocationServices
import com.journeyapps.barcodescanner.ScanOptions
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ak.keycepass.shared.domain.model.Seance
import com.ak.keycepass.shared.domain.model.StatutSeance
import com.ak.keycepass.android.data.local.entities.SeanceLocal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority

class ScanViewModel(
    private val repository: AttendanceRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Pret)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _seances = MutableStateFlow<List<Seance>>(emptyList())
    val seances: StateFlow<List<Seance>> = _seances.asStateFlow()

    private val _activeSeance = MutableStateFlow<Seance?>(null)
    val activeSeance: StateFlow<Seance?> = _activeSeance.asStateFlow()

    private var seanceIdCourant: Int? = null

    private val _etudiantClasse = MutableStateFlow("")
    val etudiantClasse: StateFlow<String> = _etudiantClasse.asStateFlow()

    val etudiantNom: String get() = "${sessionManager.prenom.orEmpty()} ${sessionManager.nom.orEmpty()}".trim().ifBlank { "Étudiant" }
    val etudiantMatricule: String get() = sessionManager.matricule.orEmpty()

    init {
        chargerSeances()
        chargerInfoEtudiant()
    }

    private fun chargerInfoEtudiant() {
        viewModelScope.launch {
            val matricule = sessionManager.matricule
            if (matricule != null) {
                val etudiant = repository.obtenirEtudiantLocal(matricule)
                if (etudiant != null) {
                    _etudiantClasse.value = etudiant.identifiantClasse
                }
            }
        }
    }

    fun chargerSeances() {
        viewModelScope.launch {
            // 1. Tenter d'abord une synchronisation avec le serveur
            val serverList = try { repository.synchroniserSeances() } catch (e: Exception) { emptyList() }

            if (serverList.isNotEmpty()) {
                _seances.value = serverList.map { it.toSeance() }
                return@launch
            }

            // 2. Charger depuis la DB locale si déjà remplie
            val listLocal = repository.obtenirToutesLesSeances()
            if (listLocal.isNotEmpty()) {
                _seances.value = listLocal.map { it.toSeance() }
                return@launch
            }

            // 3. Si rien n'est disponible, afficher une liste vide
            // (on n'insère plus de données dummy pour éviter de polluer la DB)
            _seances.value = emptyList()
        }
    }

    fun selectSeance(seance: Seance) {
        _activeSeance.value = seance
    }

    fun cloturerSeanceActive() {
        val seance = _activeSeance.value ?: return
        viewModelScope.launch {
            val success = repository.cloturerSeance(seance.idSeance)
            if (success) {
                repository.mettreAJourStatutSeance(seance.idSeance, StatutSeance.CLOTURE_ENSEIGNANT.name)
                chargerSeances()
                _activeSeance.value = _activeSeance.value?.copy(statutSeance = StatutSeance.CLOTURE_ENSEIGNANT)
            }
        }
    }

    private fun SeanceLocal.toSeance(): Seance {
        return Seance(
            idSeance = idSeance,
            nomMatiere = nomMatiere,
            classeId = classeId,
            dateJour = dateJour,
            heureDebut = heureDebut,
            heureFin = heureFin,
            statutSeance = when (statut) {
                "CLOTURE_ENSEIGNANT" -> StatutSeance.CLOTURE_ENSEIGNANT
                "EN_COURS" -> StatutSeance.EN_COURS
                else -> StatutSeance.PLANIFIE
            }
        )
    }

    fun lancerScan(context: Context) {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Placez le QR Code dans le cadre")
            setBeepEnabled(true)
            setOrientationLocked(false)
            captureActivity = com.journeyapps.barcodescanner.CaptureActivity::class.java
        }

        val scanIntent = options.createScanIntent(context)

        try {
            val activity = context as? androidx.activity.ComponentActivity
                ?: throw IllegalStateException("Lancement de scan impossible : activité introuvable.")
            activity.startActivityForResult(scanIntent, RC_SCAN)
        } catch (e: Exception) {
            _scanState.value = ScanUiState.Erreur("Lancement du scan impossible : ${e.message}")
        }
    }

    fun traiterResultatScan(result: com.journeyapps.barcodescanner.ScanIntentResult?) {
        val contenu = result?.contents.orEmpty().ifBlank {
            _scanState.value = ScanUiState.Erreur("Aucun QR Code détecté.")
            return
        }

        val currentState = _scanState.value
        viewModelScope.launch {
            _scanState.value = ScanUiState.Traitement

            val isTeacherClose = contenu.startsWith("teacher-close-")
            var seanceId = if (isTeacherClose) {
                contenu.substringAfter("teacher-close-").toIntOrNull()
            } else {
                extraireSeanceId(contenu)
            }

            if (seanceId == null && !isTeacherClose) {
                val resolved = repository.resolveSeanceCourante(contenu)
                if (resolved != null) {
                    seanceId = resolved.seanceId
                }
            }

            if (seanceId == null) {
                _scanState.value = ScanUiState.Erreur("QR Code ou séance invalide.")
                return@launch
            }
            seanceIdCourant = seanceId

            val position = runCatching { obtenirDernierePosition() }.getOrNull()
            val scanResult = if (currentState is ScanUiState.AttenteClotureEnseignant || isTeacherClose) {
                repository.enregistrerSecondScan(
                    contenuQr = contenu,
                    lat = position?.first,
                    lon = position?.second
                )
            } else {
                repository.enregistrerPremierScan(
                    contenuQr = contenu,
                    lat = position?.first,
                    lon = position?.second
                )
            }

            when (scanResult) {
                is ScanResult.ScanDebutEnregistre -> _scanState.value =
                    ScanUiState.AttenteClotureEnseignant(scanResult.statutProvisoire)
                is ScanResult.StatutFinalObtenu -> _scanState.value =
                    ScanUiState.StatutFinal(scanResult.statut)
                is ScanResult.RefusGeo -> _scanState.value =
                    ScanUiState.Erreur("Localisation refusée : vous êtes hors du périmètre autorisé.")
                is ScanResult.Erreur -> _scanState.value = ScanUiState.Erreur(scanResult.message)
                else -> Unit
            }
        }
    }

    fun reinitialiser() {
        seanceIdCourant = null
        _scanState.value = ScanUiState.Pret
    }

    private fun extraireSeanceId(contenu: String): Int? {
        return try {
            contenu.substringAfter("seanceId=").substringBefore("&").toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun obtenirDernierePosition(): Pair<Double, Double>? = suspendCoroutine { continuation ->
        try {
            val fused: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(sessionManager.context)

            fused.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location.latitude to location.longitude)
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }

    companion object {
        const val RC_SCAN = 1001
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
