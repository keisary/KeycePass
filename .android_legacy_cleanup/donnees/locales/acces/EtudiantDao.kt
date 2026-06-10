package com.ak.keycepass.android.donnees.locales.acces

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ak.keycepass.android.donnees.locales.entites.EtudiantLocal
import kotlinx.coroutines.flow.Flow

@Dao
interface EtudiantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserer(etudiant: EtudiantLocal)

    @Query("SELECT * FROM etudiant_local WHERE matricule = :matricule LIMIT 1")
    suspend fun trouverParMatricule(matricule: String): EtudiantLocal?
}
