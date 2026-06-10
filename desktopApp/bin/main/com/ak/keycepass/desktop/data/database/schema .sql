PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS "Enseignant" (
    "id_enseignant" INTEGER PRIMARY KEY AUTOINCREMENT,
    "matricule_enseignant" VARCHAR(50) NOT NULL UNIQUE,
    "nom" VARCHAR(100) NOT NULL,
    "prenom" VARCHAR(100) NOT NULL,
    "device_uuid" VARCHAR(255) UNIQUE NULL
);

CREATE TABLE IF NOT EXISTS "Etudiant" (
    "id_etudiant" INTEGER PRIMARY KEY AUTOINCREMENT,
    "matricule" VARCHAR(50) NOT NULL UNIQUE,
    "nom" VARCHAR(100) NOT NULL,
    "prenom" VARCHAR(100) NOT NULL,
    "classe_id" VARCHAR(50) NOT NULL,
    "device_uuid" VARCHAR(255) UNIQUE NULL
);

CREATE TABLE IF NOT EXISTS "SeanceSemaine" (
    "id_semaine" INTEGER PRIMARY KEY AUTOINCREMENT,
    "classe_id" VARCHAR(50) NOT NULL,
    "semaine_iso" VARCHAR(10) NOT NULL,
    "lat_reference" DOUBLE NOT NULL,
    "lon_reference" DOUBLE NOT NULL,
    "rayon_metres" INTEGER NOT NULL DEFAULT 200,
    "token_semaine" VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS "seancesemaine_classe_id_semaine_iso_unique"
ON "SeanceSemaine" ("classe_id", "semaine_iso");
 
CREATE TABLE IF NOT EXISTS "Seance" (
    "id_seance" INTEGER PRIMARY KEY AUTOINCREMENT,
    "nom_matiere" VARCHAR(100) NOT NULL,
    "classe_id" VARCHAR(50) NOT NULL,
    "date_jour" VARCHAR(10) NOT NULL,
    "heure_debut" VARCHAR(8) NOT NULL,
    "heure_fin" VARCHAR(8) NOT NULL,
    "statut_seance" VARCHAR(30) NOT NULL,
    "enseignant_id" INTEGER NULL,
    "semaine_id" INTEGER NULL,
    FOREIGN KEY ("enseignant_id") REFERENCES "Enseignant" ("id_enseignant") ON DELETE SET NULL,
    FOREIGN KEY ("semaine_id") REFERENCES "SeanceSemaine" ("id_semaine") ON DELETE SET NULL
);
 
CREATE TABLE IF NOT EXISTS "Emargement" (
    "id_emargement" INTEGER PRIMARY KEY AUTOINCREMENT,
    "etudiant_id" INTEGER NOT NULL,
    "seance_id" INTEGER NOT NULL,
    "horodatage_scan_debut" VARCHAR(30) NULL,
    "horodatage_scan_fin" VARCHAR(30) NULL,
    "statut_final" VARCHAR(20) NOT NULL,
    "lat_scan" DOUBLE NULL,
    "lon_scan" DOUBLE NULL,
    "localisation_valide" BOOLEAN NULL,
    FOREIGN KEY ("etudiant_id") REFERENCES "Etudiant" ("id_etudiant") ON DELETE CASCADE,
    FOREIGN KEY ("seance_id") REFERENCES "Seance" ("id_seance") ON DELETE CASCADE
);
