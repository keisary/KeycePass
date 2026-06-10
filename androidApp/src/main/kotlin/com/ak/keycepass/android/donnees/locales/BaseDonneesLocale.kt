package com.ak.keycepass.android.donnees.locales

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ak.keycepass.android.donnees.locales.acces.AccesDonnees
import com.ak.keycepass.android.donnees.locales.entites.EmargementLocal
import com.ak.keycepass.android.donnees.locales.entites.SeanceLocal

@Database(
    entities = [SeanceLocal::class, EmargementLocal::class, com.ak.keycepass.android.donnees.locales.entites.EtudiantLocal::class],
    version = 1,
    exportSchema = false
)
abstract class BaseDonneesLocale : RoomDatabase() {
    abstract fun accesDonnees(): AccesDonnees

    companion object {
        @Volatile private var INSTANCE: BaseDonneesLocale? = null
        fun obtenirBase(context: Context): BaseDonneesLocale =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    BaseDonneesLocale::class.java,
                    "keycepass-base"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
