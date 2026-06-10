package com.ak.keycepass.android.donnees.locales.acces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ak.keycepass.android.donnees.locales.entites.EmargementLocal
import com.ak.keycepass.android.donnees.locales.entites.SeanceLocal
import kotlinx.coroutines.flow.Flow

interface AccesDonnees {
    fun observerSeancesParClasse(identifiantClasse: String): Flow<List<SeanceLocal>>
    suspend fun insererSeance(seance: SeanceLocal)
    suspend fun mettreAJourStatutSeance(idLocal: Long, statut: String)
    suspend fun trouverSeanceParIdServeur(idSeance: Int): SeanceLocal?

    fun observerEmargementsParSeance(idSeance: Int): Flow<List<EmargementLocal>>
    suspend fun trouverEmargementParMatriculeEtSeance(matricule: String, idSeance: Int): EmargementLocal?
    suspend fun insererEmargement(emargement: EmargementLocal)
}
