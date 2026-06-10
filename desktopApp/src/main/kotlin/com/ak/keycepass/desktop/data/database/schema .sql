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
