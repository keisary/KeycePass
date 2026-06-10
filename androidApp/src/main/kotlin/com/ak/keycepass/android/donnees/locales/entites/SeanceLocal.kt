package com.ak.keycepass.android.donnees.locales.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "seance_local")
data class SeanceLocal(
    @PrimaryKey(autoGenerate = true) val idSeanceLocal: Long = 0,
    val idSeance: Int?,
    val identifiantClasse: String,
    val nomMatiere: String,
    val dateJour: String,
    val heureDebut: String,
    val heureFin: String,
    val statutSeance: String
)
