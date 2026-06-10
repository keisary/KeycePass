package com.ak.keycepass.android.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room mettant en cache les informations de la séance courante.
 * Permet d'afficher le nom de la matière et les horaires sans connexion réseau.
 */
@Entity(tableName = "seance_locale")
data class SeanceLocal(
    @PrimaryKey val idSeance: Int,
    val nomMatiere: String,
    val classeId: String,
    val dateJour: String,      // Format YYYY-MM-DD
    val heureDebut: String,    // Format HH:MM:SS
    val heureFin: String,      // Format HH:MM:SS
    val statut: String         // PLANIFIE / EN_COURS / CLOTURE_ENSEIGNANT
)
