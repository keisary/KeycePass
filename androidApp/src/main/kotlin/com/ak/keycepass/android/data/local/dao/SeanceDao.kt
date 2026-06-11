package com.ak.keycepass.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ak.keycepass.android.data.local.entities.SeanceLocal

@Dao
interface SeanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(seance: SeanceLocal)

    @Query("SELECT * FROM seance_locale WHERE idSeance = :idSeance LIMIT 1")
    suspend fun findById(idSeance: Int): SeanceLocal?

    @Query("SELECT * FROM seance_locale")
    suspend fun obtenirToutesLesSeances(): List<SeanceLocal>

    @Query("UPDATE seance_locale SET statut = :statut WHERE idSeance = :idSeance")
    suspend fun mettreAJourStatut(idSeance: Int, statut: String)
}
