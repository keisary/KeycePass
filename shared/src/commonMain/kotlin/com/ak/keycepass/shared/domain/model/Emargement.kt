package com.ak.keycepass.shared.domain.model

/**
 * Représente la fiche de présence (l'émargement) d'un étudiant pour une séance donnée.
 * Basé sur la structure de la table 'Emargement' du cahier des charges.
 */
data class Emargement(
    val idEmargement: Int = 0,         // Clé primaire de l'émargement [cite: 87]
    val etudiantId: Int,              // Clé étrangère reliée à l'étudiant [cite: 87]
    val seanceId: Int,                // Clé étrangère reliée à la séance [cite: 87]
    
    // Ces deux variables sont "String?" (nullable) car au début du cours,
    // l'étudiant n'a pas encore scanné. Les valeurs s'ajouteront au fur et à mesure.
    val horodatageScanDebut: String? = null, // Heure précise du 1er scan (Ex: "2026-06-10 08:05:23") [cite: 87]
    val horodatageScanFin: String? = null,   // Heure précise du 2e scan (Ex: "2026-06-10 09:55:12") 
    
    // Par défaut, le statut est "EN_ATTENTE" tant que le cours n'est pas fini[cite: 88].
    val statutFinal: StatutFinal = StatutFinal.EN_ATTENTE
)

/**
 * Les différents résultats possibles pour la présence d'un étudiant[cite: 88].
 */
enum class StatutFinal {
    EN_ATTENTE, 
    PRESENT, 
    RETARD, 
    ABSENT
}