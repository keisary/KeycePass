package com.ak.keycepass.desktop.data.database

import org.jetbrains.exposed.sql.Table

/**
 * Définitions des tables SQL avec l'ORM Exposed.
 * Ces objets correspondent exactement au schéma du fichier schema.sql.
 */

object EtudiantTable : Table("Etudiant") {
    val idEtudiant = integer("id_etudiant").autoIncrement()
    val matricule = varchar("matricule", 50).uniqueIndex()
    val nom = varchar("nom", 100)
    val prenom = varchar("prenom", 100)
    val classeId = varchar("classe_id", 50)
    val deviceUuid = varchar("device_uuid", 255).nullable().uniqueIndex()

    override val primaryKey = PrimaryKey(idEtudiant)
}

object SeanceTable : Table("Seance") {
    val idSeance = integer("id_seance").autoIncrement()
    val nomMatiere = varchar("nom_matiere", 100)
    val classeId = varchar("classe_id", 50)
    val dateJour = varchar("date_jour", 10)       // Format YYYY-MM-DD
    val heureDebut = varchar("heure_debut", 8)     // Format HH:MM:SS
    val heureFin = varchar("heure_fin", 8)         // Format HH:MM:SS
    val statutSeance = varchar("statut_seance", 30)

    override val primaryKey = PrimaryKey(idSeance)
}

object EmargementTable : Table("Emargement") {
    val idEmargement = integer("id_emargement").autoIncrement()
    val etudiantId = integer("etudiant_id").references(EtudiantTable.idEtudiant)
    val seanceId = integer("seance_id").references(SeanceTable.idSeance)
    val horodatageScanDebut = varchar("horodatage_scan_debut", 30).nullable()
    val horodatageScanFin = varchar("horodatage_scan_fin", 30).nullable()
    val statutFinal = varchar("statut_final", 20)

    override val primaryKey = PrimaryKey(idEmargement)
}
