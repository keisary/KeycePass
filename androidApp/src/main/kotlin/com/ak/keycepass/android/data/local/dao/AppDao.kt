package com.ak.keycepass.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ak.keycepass.android.data.local.entities.EmargementLocal
import com.ak.keycepass.android.data.local.entities.SeanceLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface AccesDonnees {
    // Séances
    @Query("SELECT * FROM seance_local WHERE identifiantClasse = :identifiantClasse")
    fun observerSeancesParClasse(identifiantClasse: String): Flow<List<SeanceLocal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererOuMettreAJourSeance(seance: SeanceLocal)

    @Query("UPDATE seance_local SET statutSeance = :statut WHERE idSeanceLocal = :id")
    suspend fun mettreAJourStatutSeance(id: Long, statut: String)

    @Query("SELECT * FROM seance_local WHERE idSeance = :idSeance LIMIT 1")
    suspend fun trouverSeanceParIdServeur(idSeance: Int): SeanceLocal?

    // Émargements
    @Query("SELECT * FROM emargement_local WHERE idSeance = :idSeance")
    fun observerEmargementsParSeance(idSeance: Int): Flow<List<EmargementLocal>>

    @Query("SELECT * FROM emargement_local WHERE matricule = :matricule AND idSeance = :idSeance LIMIT 1")
    suspend fun trouverEmargementParMatriculeEtSeance(matricule: String, idSeance: Int): EmargementLocal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insererOuMettreAJourEmargement(emargement: EmargementLocal)
}
