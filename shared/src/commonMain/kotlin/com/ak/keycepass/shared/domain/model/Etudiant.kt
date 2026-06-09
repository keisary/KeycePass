package com.ak.keycepass.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Représente un étudiant dans le système KeycePass.
 * Conforme à la table Etudiant de la spécification de données (section 6.2).
 */
@Serializable
data class Etudiant(
    val idEtudiant: Int? = null,
    val matricule: String,
    val nom: String,
    val prenom: String,
    val classeId: String,
    val deviceUuid: String? = null
)
