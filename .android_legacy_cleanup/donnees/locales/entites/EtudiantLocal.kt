package com.ak.keycepass.android.donnees.locales.entites

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "etudiant_local")
data class EtudiantLocal(
    @PrimaryKey val matricule: String,
    val identifiantClasse: String,
    val identifiantAppareil: String,
    val role: String  // ETUDIANT / DELEGUE / ENSEIGNANT
)
