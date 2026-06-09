package com.ak.keycepass.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Représente une séance de cours dans le système KeycePass.
 * Conforme à la table Seance de la spécification de données (section 6.2).
 */
@Serializable
data class Seance(
    val idSeance: Int? = null,
    val nomMatiere: String,
    val classeId: String,
    val dateJour: String, // Format AAAA-MM-JJ
    val heureDebut: String, // Format HH:MM:SS
    val heureFin: String, // Format HH:MM:SS
    val statutSeance: StatutSeance
)

@Serializable
enum class StatutSeance {
    PLANIFIE,
    EN_COURS,
    CLOTURE_ENSEIGNANT
}
