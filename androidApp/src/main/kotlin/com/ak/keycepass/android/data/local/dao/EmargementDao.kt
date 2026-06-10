package com.ak.keycepass.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ak.keycepass.android.data.local.entities.EmargementLocal

@Dao
interface EmargementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(emargement: EmargementLocal)

    @Query("SELECT * FROM emargement_local WHERE seanceId = :seanceId LIMIT 1")
    suspend fun findBySeanceId(seanceId: Int): EmargementLocal?

    @Query("UPDATE emargement_local SET envoiConfirme = 1 WHERE seanceId = :seanceId")
    suspend fun marquerEnvoiConfirme(seanceId: Int)

    @Query("DELETE FROM emargement_local WHERE seanceId = :seanceId")
    suspend fun deleteBySeanceId(seanceId: Int)
}
