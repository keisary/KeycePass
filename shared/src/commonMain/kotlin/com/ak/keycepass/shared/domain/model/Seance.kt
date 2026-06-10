package com.ak.keycepass.shared.domain.model

/**
 * Représente une séance de cours au sein du système KeycePass.
 * Basé sur la structure de la table 'Seance' du cahier des charges.
 */
data class Seance(
    val idSeance: Int,           // Clé primaire de la séance (ex: 101)
    val nomMatiere: String,      // Libellé du cours (ex: "Ingénierie Logicielle")
    val classeId: String,        // Indexation de la classe cible (ex: "B2_IT")
    val dateJour: String,        // Date de la séance au format AAAA-MM-JJ
    val heureDebut: String,      // Heure de début officielle au format HH:MM:SS
    val heureFin: String,        // Heure de fin officielle au format HH:MM:SS
    val statutSeance: StatutSeance // L'état actuel de la séance (voir l'enum ci-dessous)
)

/**
 * Les trois états possibles d'une séance selon les exigences du projet.
 */
enum class StatutSeance {
    PLANIFIE, 
    EN_COURS, 
    CLOTURE_ENSEIGNANT
}