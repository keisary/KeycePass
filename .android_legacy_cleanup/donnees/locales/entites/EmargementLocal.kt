package com.ak.keycepass.android.donnees.locales.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "emargement_local")
data class EmargementLocal(
    @PrimaryKey(autoGenerate = true) val idEmargement: Long = 0,
    val idEtudiant: Int,
    val idSeance: Int,
    val horodatageScanDebut: String? = null,
    val horodatageScanFin: String? = null,
    val statutFinal: String = "EN_ATTENTE",
    val matricule: String,
    val identifiantClasse: String
)
