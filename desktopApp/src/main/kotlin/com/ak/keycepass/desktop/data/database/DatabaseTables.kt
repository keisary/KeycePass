

package com.ak.keycepass.desktop.data.database

import org.jetbrains.exposed.sql.Table

/**
 * Définitions des tables SQL avec l'ORM Exposed.
 * Schéma complet de la base de données KeycePass Desktop.
 */

// ─── Étudiants ───────────────────────────────────────────────────────────────

object EtudiantTable : Table("Etudiant") {
    val idEtudiant = integer("id_etudiant").autoIncrement()
    val matricule = varchar("matricule", 50).uniqueIndex()
    val nom = varchar("nom", 100)
    val prenom = varchar("prenom", 100)
    val classeId = varchar("classe_id", 50)
    val deviceUuid = varchar("device_uuid", 255).nullable().uniqueIndex()

    override val primaryKey = PrimaryKey(idEtudiant)
}

// ─── Enseignants ─────────────────────────────────────────────────────────────

object EnseignantTable : Table("Enseignant") {
    val idEnseignant = integer("id_enseignant").autoIncrement()
    val matriculeEnseignant = varchar("matricule_enseignant", 50).uniqueIndex()
    val nom = varchar("nom", 100)
    val prenom = varchar("prenom", 100)
    val deviceUuid = varchar("device_uuid", 255).nullable().uniqueIndex()

    override val primaryKey = PrimaryKey(idEnseignant)
}

// ─── Semaines (QR Code hebdomadaire) ─────────────────────────────────────────

/**
 * Représente une semaine d'enseignement pour une classe.
 *
 * L'administration saisit la localisation GPS de référence (copie depuis Maps)
 * et le rayon de tolérance (défaut global : 200 mètres).
 *
 * Le [tokenSemaine] est un HMAC-SHA256 utilisé comme charge utile du QR code
 * hebdomadaire pour éviter la falsification.
 */
object SeanceSemaineTable : Table("SeanceSemaine") {
    val idSemaine = integer("id_semaine").autoIncrement()
    val classeId = varchar("classe_id", 50)
    val semaineIso = varchar("semaine_iso", 10)         // ex. "2026-W24" (ISO 8601)
    val latReference = double("lat_reference")           // Latitude GPS de référence
    val lonReference = double("lon_reference")           // Longitude GPS de référence
    val rayonMetres = integer("rayon_metres").default(200) // Rayon de tolérance (200 m par défaut)
    val tokenSemaine = varchar("token_semaine", 64)      // HMAC-SHA256 hex (anti-fraude)

    override val primaryKey = PrimaryKey(idSemaine)

    init {
        uniqueIndex(classeId, semaineIso) // Un seul QR par classe par semaine
    }
}

// ─── Séances ─────────────────────────────────────────────────────────────────

object SeanceTable : Table("Seance") {
    val idSeance = integer("id_seance").autoIncrement()
    val nomMatiere = varchar("nom_matiere", 100)
    val classeId = varchar("classe_id", 50)
    val dateJour = varchar("date_jour", 10)               // Format YYYY-MM-DD
    val heureDebut = varchar("heure_debut", 8)             // Format HH:MM:SS
    val heureFin = varchar("heure_fin", 8)                 // Format HH:MM:SS
    val statutSeance = varchar("statut_seance", 30)        // PLANIFIE, EN_COURS, CLOTURE_ENSEIGNANT
    val enseignantId = integer("enseignant_id").references(EnseignantTable.idEnseignant).nullable()
    val semaineId = integer("semaine_id").references(SeanceSemaineTable.idSemaine).nullable()

    override val primaryKey = PrimaryKey(idSeance)
}

// ─── Émargements ─────────────────────────────────────────────────────────────

object EmargementTable : Table("Emargement") {
    val idEmargement = integer("id_emargement").autoIncrement()
    val etudiantId = integer("etudiant_id").references(EtudiantTable.idEtudiant)
    val seanceId = integer("seance_id").references(SeanceTable.idSeance)
    val horodatageScanDebut = varchar("horodatage_scan_debut", 30).nullable()
    val horodatageScanFin = varchar("horodatage_scan_fin", 30).nullable()
    val statutFinal = varchar("statut_final", 20)
    // ── Données de géolocalisation (audit anti-fraude) ──
    val latScan = double("lat_scan").nullable()           // Latitude reçue lors du scan
    val lonScan = double("lon_scan").nullable()           // Longitude reçue lors du scan
    val localisationValide = bool("localisation_valide").nullable() // true = dans le rayon

    override val primaryKey = PrimaryKey(idEmargement)
}

