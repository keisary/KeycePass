package com.ak.keycepass.android.reseau

import kotlinx.serialization.Serializable

@Serializable
data class DonneesScan(
    val matricule: String,
    val identifiantAppareil: String,
    val idSeance: Int,
    val horodatage: String,
    val typeScan: TypeScan,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
enum class TypeScan {
    DEBUT,
    FIN
}

@Serializable
data class ReponseScan(
    val succes: Boolean,
    val statutCalcule: String,
    val message: String? = null,
    val localisationRefusee: Boolean = false
)

@Serializable
data class SyntheseSession(
    val idSeance: Int,
    val totalInscrits: Int,
    val totalPresents: Int,
    val totalRetards: Int,
    val totalAbsents: Int,
    val cloture: Boolean
)

@Serializable
data class SeanceCouranteDto(
    val idSeance: Int,
    val nomMatiere: String,
    val heureDebut: String,
    val heureFin: String
)
