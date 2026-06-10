package com.ak.keycepass.android.metier.vue

import com.ak.keycepass.android.RendezVous

data class EtatScan(
    val etape: EtapeScan = EtapeScan.Inactif,
    val message: String? = null,
    val localisationRefusee: Boolean = false,
    val seance: SeanceCourante? = null
) {
    val enChargement: Boolean get() = etape == EtapeScan.ResolutionSeance
    val estTermine: Boolean get() = etape == EtapeScan.Depart
    val erreur: String? get() = if (etape is EtapeScan.Erreur && message != null) message else null
}

sealed interface EtapeScan {
    object Inactif : EtapeScan
    object ResolutionSeance : EtapeScan
    object AttenteConfirmation : EtapeScan
    data class SeanceTrouvee(val nomMatiere: String, val heureDebut: String, val heureFin: String) : EtapeScan
    object ScanEnregistre : EtapeScan
    object RefusGeo : EtapeScan
    object Depart : EtapeScan
    data class Erreur(val cause: Throwable? = null) : EtapeScan
}

data class SeanceCourante(
    val idSeance: Long,
    val nomMatiere: String,
    val heureDebut: String,
    val heureFin: String
)
