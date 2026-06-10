package com.ak.keycepass.shared.domain.model

/**
 * Représente un étudiant au sein du système KeycePass.
 * Basé sur la structure de la table 'Etudiant' du cahier des charges.
 */
data class Etudiant(
    val idEtudiant: Int = 0,        // Clé primaire (générée automatiquement par le système)
    val matricule: String,         // Identifiant académique unique (ex: "MAT-12345")
    val nom: String,               // Nom de famille
    val prenom: String,            // Prénom
    val classeId: String,          // Clé étrangère logique (ex: "B2_IT")
    
    // Le deviceUuid est "String?" (avec un point d'interrogation) car il peut être 
    // nul au début, tant que l'étudiant n'a pas fait son premier scan d'enrôlement.
    val deviceUuid: String? = null 
)