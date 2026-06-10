package com.ak.keycepass.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ak.keycepass.android.data.local.entities.EtudiantLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface EtudiantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(etudiant: EtudiantLocal)

    @Query("SELECT * FROM etudiant_local WHERE matricule = :matricule LIMIT 1")
    suspend fun findByMatricule(matricule: String): EtudiantLocal?
}
