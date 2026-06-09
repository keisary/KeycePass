package com.ak.keycepass.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Représente un émargement d'un étudiant pour une séance donnée.
 * Conforme à la table Emargement de la spécification de données (section 6.2).
 */
@Serializable
data class Emargement(
    val idEmargement: Int? = null,
    val etudiantId: Int,
    val seanceId: Int,
    val horodatageScanDebut: String?, // Format ISO-8601 ou HH:MM:SS
    val horodatageScanFin: String?,   // Format ISO-8601 ou HH:MM:SS
    val statutFinal: StatutFinal = StatutFinal.EN_ATTENTE
)

@Serializable
enum class StatutFinal {
    EN_ATTENTE,
    PRESENT,
    RETARD,
    ABSENT
}
