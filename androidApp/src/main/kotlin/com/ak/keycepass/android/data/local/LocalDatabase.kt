package com.ak.keycepass.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ak.keycepass.android.data.local.dao.EmargementDao
import com.ak.keycepass.android.data.local.dao.SeanceDao
import com.ak.keycepass.android.data.local.entities.EmargementLocal
import com.ak.keycepass.android.data.local.entities.SeanceLocal

/**
 * Base de données Room locale du smartphone.
 * Stocke temporairement les scans en attente de validation (Phase 3 → Phase 4)
 * et les informations de la séance courante pour un accès hors connexion.
 */
@Database(
    entities = [EmargementLocal::class, SeanceLocal::class],
    version = 1,
    exportSchema = false
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun emargementDao(): EmargementDao
    abstract fun seanceDao(): SeanceDao

    companion object {
        @Volatile
        private var INSTANCE: LocalDatabase? = null

        fun getDatabase(context: Context): LocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocalDatabase::class.java,
                    "keycepass_local_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
