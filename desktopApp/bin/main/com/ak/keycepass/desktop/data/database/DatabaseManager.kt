package com.ak.keycepass.desktop.data.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

/**
 * Gestionnaire de la connexion à la base de données SQLite centrale.
 * Crée les tables si elles n'existent pas encore (première utilisation).
 */
object DatabaseManager {

    private const val DB_FILENAME = "keycepass_central.db"

    /**
     * Chemin physique du fichier de base de données.
     * Stocké dans le dossier "database" à la racine du projet.
     */
    val dbPath: String
        get() {
            val dir = File("database")
            dir.mkdirs()
            return File(dir, DB_FILENAME).absolutePath
        }

    /**
     * Initialise la connexion et crée les tables si elles n'existent pas.
     * À appeler une seule fois au démarrage de l'application.
     */
    fun init() {
        Database.connect(
            url = "jdbc:sqlite:$dbPath",
            driver = "org.sqlite.JDBC"
        )
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                EnseignantTable,
                EtudiantTable,
                SeanceSemaineTable,
                SeanceTable,
                EmargementTable
            )
        }
        println("Base de données initialisée : $dbPath")
    }
}
