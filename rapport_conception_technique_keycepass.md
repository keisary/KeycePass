# Rapport de Conception Technique — KeycePass

**Projet** : KeycePass — Système de Gestion des Présences par QR Code  
**Version** : 1.1.0  
**Date** : Juin 2026  
**Équipe** : Département Informatique — B2_IT  

---

## Table des Matières

1. [Vue d'ensemble du projet](#1-vue-densemble-du-projet)
2. [Architecture générale](#2-architecture-générale)
3. [Module partagé (:shared)](#3-module-partagé-shared)
4. [Application Desktop (Administration)](#4-application-desktop-administration)
5. [Application Mobile Android](#5-application-mobile-android)
6. [Protocole de Communication](#6-protocole-de-communication)
7. [Base de Données SQLite](#7-base-de-données-sqlite)
8. [Sécurité et Anti-Fraude](#8-sécurité-et-anti-fraude)
9. [Flux Utilisateurs (User Stories)](#9-flux-utilisateurs-user-stories)
10. [Stack Technique](#10-stack-technique)
11. [Structure du Dépôt Git](#11-structure-du-dépôt-git)
12. [Procédures de Déploiement](#12-procédures-de-déploiement)
13. [Corrections Appliquées & Problèmes Résolus](#13-corrections-appliquées--problèmes-résolus)
14. [Travaux des Collaborateurs](#14-travaux-des-collaborateurs)

---

## 1. Vue d'ensemble du projet

### 1.1 Contexte

KeycePass est un système de **gestion des présences dématérialisé** conçu pour les établissements d'enseignement supérieur. Il remplace la feuille de présence papier par un mécanisme sécurisé à double scan QR code, réduisant la fraude tout en automatisant la collecte des données d'assiduité.

### 1.2 Acteurs du système

| Acteur | Rôle | Application utilisée |
|--------|------|---------------------|
| **Administrateur** | Gère les étudiants, les semaines, les séances et les QR codes | Desktop (PC) |
| **Enseignant** | Sélectionne sa séance, clôture le cours et affiche le QR de fin | Mobile Android |
| **Étudiant** | S'enrôle une fois, puis scanne à l'arrivée et au départ de chaque cours | Mobile Android |
| **Délégué** | Consulte les statistiques de présence en temps réel | Mobile Android |

### 1.3 Principe de fonctionnement général

```
[Admin Desktop] ──génère──► QR Enrôlement ──scan──► [Étudiant Mobile]
                 ──génère──► QR Semaine   ──scan──► [Étudiant Mobile]
[Enseignant Mobile] ──clôture──► QR Fin de cours ──scan──► [Étudiant Mobile]

[Mobile] ──HTTP/REST──► [Serveur Ktor embarqué sur Desktop] ──SQL──► [SQLite]
```

---

## 2. Architecture Générale

### 2.1 Diagramme d'architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                         PROJET KEYCEPASS                         │
│                      (Gradle Multi-Module)                       │
├─────────────────┬────────────────────┬───────────────────────────┤
│  :desktopApp    │      :shared       │      :androidApp          │
│  (Kotlin/JVM)   │  (Kotlin Common)   │   (Kotlin/Android)        │
│                 │                    │                           │
│  Compose Desktop│  domain/model/     │  Jetpack Compose          │
│  Ktor Server    │  domain/utils/     │  Room Database            │
│  Exposed ORM    │  network/          │  Ktor Client (OkHttp)     │
│  SQLite         │                    │  Navigation Compose        │
└────────┬────────┴─────────┬──────────┴────────────┬──────────────┘
         │                  │                        │
         │    Réseau local Wi-Fi / Internet           │
         └──────────────────┴────────────────────────┘
                      HTTP REST (port 8080)
```

### 2.2 Pattern architectural

Chaque module suit le pattern **MVVM (Model-View-ViewModel)** :

- **Model** : Entités de domaine (`Seance`, `Emargement`, `Etudiant`) + couche données (DAO, API)
- **ViewModel** : `AdminViewModel` (Desktop), `ScanViewModel` / `EnrolementViewModel` (Android)
- **View** : Composables Jetpack Compose (Desktop & Android)

---

## 3. Module partagé (:shared)

Le module `:shared` contient tout le code **commun** aux deux plateformes.

### 3.1 Modèles de domaine

#### `Seance.kt`
```kotlin
data class Seance(
    val idSeance: Int,
    val nomMatiere: String,
    val classeId: String,
    val dateJour: String,       // Format YYYY-MM-DD
    val heureDebut: String,     // Format HH:MM:SS
    val heureFin: String,       // Format HH:MM:SS
    val statutSeance: StatutSeance
)

enum class StatutSeance { PLANIFIE, EN_COURS, CLOTURE_ENSEIGNANT }
```

#### `Emargement.kt`
```kotlin
data class Emargement(
    val idEmargement: Int = 0,
    val etudiantId: Int,
    val seanceId: Int,
    val horodatageScanDebut: String? = null,
    val horodatageScanFin: String? = null,
    val statutFinal: StatutFinal = StatutFinal.EN_ATTENTE
)

enum class StatutFinal { EN_ATTENTE, PRESENT, RETARD, ABSENT }
```

### 3.2 Utilitaires partagés

#### `StatutUtils.kt` — Algorithme de calcul de présence

L'algorithme central du projet détermine le statut final d'un étudiant selon les règles métier :

| Condition | Statut |
|-----------|--------|
| 1er scan manquant OU 2e scan absent | `ABSENT` |
| 1er scan arrivé ≤ 15 min après l'heure de début | `PRESENT` |
| 1er scan arrivé > 15 min après l'heure de début | `RETARD` |

```kotlin
fun determinerStatutFinal(
    heureDebutCoursStr: String,
    heurePremierScanStr: String?,
    secondScanValide: Boolean
): StatutFinal
```

Supporte plusieurs formats horodatage : `HH:MM:SS`, `YYYY-MM-DD HH:MM:SS`, `ISO-8601 (2026-06-09T08:12:34.567Z)`.

### 3.3 Modèles réseau partagés (`NetworkModels.kt`)

| Classe | Description |
|--------|-------------|
| `ScanPayload` | Payload envoyé lors d'un scan (matricule, deviceUuid, seanceId, timestamp, GPS optionnel) |
| `ScanResponse` | Réponse serveur post-scan (success, statutCalcule, localisationRefusee) |
| `SessionStatusDto` | Statistiques de présence d'une séance (pour le délégué) |
| `SeanceCouranteDto` | Séance active retournée après scan QR hebdomadaire |
| `SeanceDto` | Séance complète pour synchronisation mobile ↔ serveur |

---

## 4. Application Desktop (Administration)

### 4.1 Point d'entrée — `Main.kt`

Au démarrage :
1. Initialisation de la base de données SQLite (`DatabaseManager.init()`)
2. Démarrage du serveur HTTP Ktor sur le port **8080** (`KtorServer.start(port = 8080)`)
3. Lancement de l'interface graphique Compose Desktop
4. Arrêt propre du serveur à la fermeture (`KtorServer.stop()`)

### 4.2 Structure des écrans Desktop

```
AdminLayout.kt (navigation principale, barre latérale)
├── DashboardScreen.kt       — Tableau de bord présences en temps réel
├── EtudiantsScreen.kt       — Gestion des étudiants + import Excel + QR enrôlement
├── QRManagementScreen.kt    — Gestion des semaines, séances et QR codes de présence
├── GestionEnrolementScreen.kt — Suivi des enrôlements et statuts appareils
└── HistoriqueScreen.kt      — Historique complet des émargements avec export
```

#### Navigation latérale (`Screen.kt`)
```kotlin
sealed class Screen {
    object Dashboard, Etudiants, QRManagement,
           GestionEnrolement, Historique : Screen()
}
```

### 4.3 Couche de données Desktop

#### Base de données — Exposed ORM + SQLite

**`DatabaseManager.kt`**
- Connexion JDBC SQLite : `jdbc:sqlite:database/keycepass_central.db`
- Création automatique des tables au premier démarrage (`SchemaUtils.createMissingTablesAndColumns`)
- Le fichier `.db` est versionné dans Git (exception `.gitignore`)

**`DatabaseTables.kt`** — Définition des 5 tables :

| Table | Description |
|-------|-------------|
| `EtudiantTable` | Étudiants (matricule, nom, prénom, classe, deviceUuid) |
| `EnseignantTable` | Enseignants (matricule, nom, prénom, deviceUuid) |
| `SeanceSemaineTable` | Semaines de cours avec coordonnées GPS de référence et token HMAC |
| `SeanceTable` | Séances individuelles (matière, horaires, statut, FK semaine et enseignant) |
| `EmargementTable` | Enregistrements de présence (scan début, scan fin, statut, coordonnées GPS) |

#### Services

**`SeanceSemaineService.kt`** — Service métier principal Desktop :
- `creerSemaine()` : Crée une nouvelle semaine d'enseignement avec token HMAC
- `enregistrerSeance()` : Insère ou met à jour une séance journalière
- `genererQrCodeEnrolement()` : Génère le QR d'enrôlement de classe
- `genererQrCodePresence()` : Génère le QR hebdomadaire de présence
- `genererQrCodeEnseignant()` : Génère le QR d'enrôlement enseignant

**`ImportService.kt`** — Import Excel (`.xlsx`) :
- Lecture des fichiers Apache POI
- Insertion/mise à jour des étudiants en base
- Validation de format (matricule, nom, prénom, classeId)

#### Utilitaires

**`GeoUtils.kt`** — Géolocalisation anti-fraude :
- Implémentation de la **formule de Haversine** pour calculer des distances GPS précises sur de courtes portées
- `distanceMetres(lat1, lon1, lat2, lon2)` → Distance en mètres
- `localisationValide(latScan, lonScan, latRef, lonRef, rayonM)` → Bool

**`QrCodeGenerator.kt`** — Génération de QR codes :
- Utilise **ZXing** (Zebra Crossing)
- Génération en `BufferedImage` pour affichage dans Compose
- Export vers fichier `.png` pour impression

### 4.4 Serveur HTTP Ktor embarqué — `KtorServer.kt`

Le Desktop embarque un **serveur HTTP léger Ktor (engine CIO)** qui reçoit les requêtes des applications mobiles via le réseau local ou Internet.

#### Endpoints REST

| Méthode | Route | Description |
|---------|-------|-------------|
| `GET` | `/api/ping` | Health check du serveur |
| `POST` | `/api/enrolement` | Lie un deviceUuid à un matricule (US_01) |
| `POST` | `/api/scan` | Reçoit un scan de présence début ou fin (US_02/US_03) |
| `GET` | `/api/seance/{id}/statut` | Retourne le statut d'une séance |
| `POST` | `/api/seance/{id}/cloturer` | L'enseignant clôture sa séance (US_04) |
| `GET` | `/api/seance/{id}/stats` | Statistiques de présence pour le délégué (US_05) |
| `GET` | `/api/semaine/{id}/seance-courante` | Séance active à l'instant T pour une semaine |
| `GET` | `/api/seances` | Liste complète de toutes les séances (sync mobile) |
| `POST` | `/api/sync` | Réception de scans en mode hors-ligne (batch) |

#### Logique de scan

Lors d'un **POST /api/scan** avec `scanType = DEBUT` :
1. Vérification que l'étudiant existe et que son `deviceUuid` correspond
2. Vérification GPS si coordonnées fournies (formule Haversine, rayon 200 m par défaut)
3. Si hors périmètre → `localisationRefusee: true` dans la réponse
4. Sinon → insertion dans `EmargementTable` avec statut `EN_ATTENTE`

Lors d'un **POST /api/scan** avec `scanType = FIN` :
1. Récupération du scan de début dans `EmargementTable`
2. Calcul du statut final via `StatutUtils.determinerStatutFinal()`
3. Mise à jour de l'émargement avec `horodatageScanFin` et `statutFinal`

### 4.5 ViewModel Desktop — `AdminViewModel.kt`

Orchestre toutes les opérations de l'interface :
- **Chargement des données** : Étudiants par classe, séances, émargements
- **Filtrage dashboard** : Tous les étudiants de la classe sont affichés (enrôlés ou non), avec un badge **« Non Enrôlé »** pour ceux sans `deviceUuid`. Voir Section 14 pour les détails.
- **Génération QR** : Délègue à `SeanceSemaineService`
- **Export PDF/Excel** : Depuis l'historique
- **Gestion CRUD** des semaines et séances

---

## 5. Application Mobile Android

### 5.1 Point d'entrée — `MainActivity.kt`

```kotlin
val sessionManager = SessionManager(applicationContext)
val serverUrl = sessionManager.serverUrl ?: "http://localhost:8080"
val repository = AttendanceRepository(
    sessionManager = sessionManager,
    db = LocalDatabase.getDatabase(applicationContext),
    networkClient = NetworkClient(serverUrl)
)
KeycePassNavHost(repository = repository, sessionManager = sessionManager)
```

> **Point clé** : L'URL du serveur est restaurée depuis `SessionManager` (chiffrée dans `EncryptedSharedPreferences`) à chaque redémarrage de l'app.

### 5.2 Navigation — `Navigation.kt`

La destination de démarrage est déterminée dynamiquement :

```kotlin
val startDest = if (sessionManager.estEnrole) {
    if (sessionManager.role == UserRole.ENSEIGNANT) "teacher" else "scan"
} else {
    "enrolement"
}
```

| Route | Écran | Rôle |
|-------|-------|------|
| `enrolement` | `LoginScreen` | Tous (premier lancement) |
| `scan` | `ScanScreen` | Étudiant / Délégué |
| `teacher` | `TeacherScreen` | Enseignant |

### 5.3 Stockage local chiffré — `SessionManager.kt`

Utilise **EncryptedSharedPreferences** (clé AES256-GCM) pour stocker de façon sécurisée :

| Clé | Valeur stockée |
|-----|---------------|
| `matricule` | Matricule de l'utilisateur |
| `nom` / `prenom` | Identité |
| `device_uuid` | ANDROID_ID (identifiant unique de l'appareil) |
| `role` | `ETUDIANT`, `ENSEIGNANT`, ou `DELEGUE` |
| `server_url` | URL du serveur Desktop |
| `est_enrole` | Boolean d'état d'enrôlement |

### 5.4 Base de données locale Room — `LocalDatabase.kt`

Base SQLite locale pour la **persistance offline** et le cache :

#### Entités Room

| Entité | Champs clés |
|--------|-------------|
| `EtudiantLocal` | matricule, classeId, identifiantAppareil, role |
| `SeanceLocal` | idSeance, nomMatiere, classeId, dateJour, heureDebut, heureFin, statut |
| `EmargementLocal` | seanceId, heureScanDebut, statutProvisoire, envoiConfirme |

> **Version DB** : 1 — `fallbackToDestructiveMigration()` configuré pour les mises à jour de développement.

### 5.5 Client réseau — `NetworkClient.kt`

Client HTTP Ktor (engine **OkHttp**) avec `ContentNegotiation JSON` :

```kotlin
class NetworkClient(var serverBaseUrl: String) {
    suspend fun enroler(matricule: String, deviceUuid: String): Boolean
    suspend fun envoyerScan(payload: ScanPayload): ScanResponse
    suspend fun verifierCloture(seanceId: Int): Boolean
    suspend fun cloturerSeance(seanceId: Int): Boolean
    suspend fun getStatistiquesSeance(seanceId: Int): SessionStatusDto?
    suspend fun getSeanceCourante(semaineId: Int): SeanceCouranteDto?
    suspend fun getToutesLesSeances(): List<SeanceDto>
}
```

> L'attribut `serverBaseUrl` est **mutable** : il est mis à jour dynamiquement lorsque l'app lit l'URL depuis un QR code.

### 5.6 Dépôt central — `AttendanceRepository.kt`

Orchestre la logique métier mobile en coordonnant :
- `SessionManager` — identité locale chiffrée
- `LocalDatabase` (Room) — stockage offline
- `NetworkClient` — communication HTTP
- `StatutUtils` — algorithme de calcul du statut (module :shared)

#### Méthodes principales

| Méthode | Description |
|---------|-------------|
| `enroler()` | Parse le QR, appelle l'API, sauvegarde la session |
| `enregistrerPremierScan()` | Résout la séance, calcule le statut provisoire, envoie le scan DEBUT |
| `enregistrerSecondScan()` | Vérifie la clôture enseignant, calcule le statut final, envoie le scan FIN |
| `resolveSeanceCourante()` | Interroge le serveur pour la séance active d'une semaine |
| `synchroniserSeances()` | Télécharge toutes les séances du serveur et les persiste en Room |
| `verifierCloture()` | Interroge le statut d'une séance |
| `cloturerSeance()` | L'enseignant clôture sa séance |

#### Parsing QR Code

```kotlin
private fun parseQrParams(contenu: String): Map<String, String> {
    // Extrait les paramètres URL-décodés d'un URI scheme
    // Ex: "keycepass://enrolement?classeId=B2_IT&serverUrl=http%3A%2F%2F192.168.1.10%3A8080"
    // → { "classeId": "B2_IT", "serverUrl": "http://192.168.1.10:8080" }
}
```

### 5.7 ViewModels Android

#### `EnrolementViewModel.kt`

Gère le flux d'enrôlement (US_01) :

```kotlin
class EnrolementViewModel(repository, sessionManager) : ViewModel() {
    val enrolementState: StateFlow<EnrolementUiState>
    fun enroler(context, matricule, nom, prenom, contenuQr)
    fun reinitialiser()
}

sealed class EnrolementUiState {
    Idle, Chargement, Succes(role: String), Erreur(message: String)
}
```

#### `ScanViewModel.kt`

Gère les scans de présence (US_02/US_03/US_04) :

```kotlin
class ScanViewModel(repository, sessionManager) : ViewModel() {
    val scanState: StateFlow<ScanUiState>
    val seances: StateFlow<List<Seance>>
    val activeSeance: StateFlow<Seance?>

    fun chargerSeances()          // Sync serveur → Room → UI
    fun selectSeance(seance)      // Sélection sans clôture (enseignant)
    fun cloturerSeanceActive()    // Clôture serveur + génération QR de fin
    fun lancerScan(context)       // Lance le scanner ZXing
    fun traiterResultatScan(result) // Traite le contenu scanné
    fun reinitialiser()
}

sealed class ScanUiState {
    Pret, Traitement,
    AttenteClotureEnseignant(statutProvisoire),
    StatutFinal(statut),
    SeanceCloturee, Erreur(message)
}
```

> **Priorité de chargement des séances** : 1) Serveur (sync) → 2) Cache Room → 3) Liste vide.

### 5.8 Écrans Android

#### `LoginScreen.kt` — Écran d'enrôlement

- Saisie : matricule, nom, prénom, URL serveur, rôle (ETUDIANT / ENSEIGNANT)
- Construit le QR de simulation : `keycepass://enrolement?classeId=...&serverUrl=...&role=...`
- Navigation post-enrôlement : rôle `ENSEIGNANT` → `"teacher"`, sinon → `"scan"`
- Popups back stack : `popUpTo("enrolement") { inclusive = true }`

#### `ScanScreen.kt` — Écran étudiant

- Gestion des permissions : CAMERA + ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION
- État `Pret` → bouton "Scanner"
- État `AttenteClotureEnseignant` → affiche statut provisoire + bouton "Scanner le QR de fin"
- État `StatutFinal` → affiche le résultat final

#### `TeacherScreen.kt` — Écran enseignant

- Liste des séances synchronisées depuis le serveur
- Sélection d'une séance → détails (matière, horaires, classe)
- Bouton **"Clôturer la séance"** (explicite, séparé de la sélection)
- Après clôture → génération et affichage du **QR Code de fin** via ZXing
- Format du QR de clôture : `teacher-close-{seanceId}`

---

## 6. Protocole de Communication

### 6.1 Format des QR Codes

#### QR Code d'enrôlement (généré par le Desktop)

```
keycepass://enrolement?classeId=B2_IT&token=HMAC_SHA256&serverUrl=http://192.168.1.10:8080&role=ETUDIANT
```

| Paramètre | Description |
|-----------|-------------|
| `classeId` | Identifiant de la classe |
| `token` | Jeton HMAC-SHA256 d'authenticité |
| `serverUrl` | URL du serveur Ktor (IP locale ou adresse publique) |
| `role` | `ETUDIANT` ou `ENSEIGNANT` |

#### QR Code de présence hebdomadaire (généré par le Desktop)

```
keycepass://presence?semaineId=3&classeId=B2_IT&token=HMAC&serverUrl=http://192.168.1.10:8080
```

#### QR Code de clôture enseignant (généré par l'app Mobile)

```
teacher-close-{seanceId}
```

### 6.2 Flux de données réseau

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUX US_01 : ENRÔLEMENT                      │
├─────────────────────────────────────────────────────────────────┤
│ 1. Admin génère QR → Étudiant scanne                            │
│ 2. App parse l'URL → extrait serverUrl, classeId, role          │
│ 3. POST /api/enrolement {matricule, deviceUuid}                 │
│ 4. Serveur : vérifie matricule en DB, lie deviceUuid            │
│ 5. Réponse : {success: true}                                    │
│ 6. App : sauvegarde session chiffrée → navigue vers scan/teacher│
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│              FLUX US_02 : PREMIER SCAN (ARRIVÉE)                │
├─────────────────────────────────────────────────────────────────┤
│ 1. Étudiant scanne QR hebdomadaire                              │
│ 2. App parse → extrait semaineId                                │
│ 3. GET /api/semaine/{semaineId}/seance-courante                 │
│ 4. Serveur retourne la séance active (selon heure actuelle)     │
│ 5. POST /api/scan {matricule, deviceUuid, seanceId, DEBUT, GPS} │
│ 6. Serveur : vérifie UUID, GPS, insère émargement EN_ATTENTE    │
│ 7. App : persiste en Room, affiche statut provisoire            │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│         FLUX US_03/US_04 : CLÔTURE + SECOND SCAN (FIN)         │
├─────────────────────────────────────────────────────────────────┤
│ 1. Enseignant sélectionne sa séance dans l'app                  │
│ 2. Appuie sur "Clôturer la séance"                              │
│ 3. POST /api/seance/{id}/cloturer → statut = CLOTURE_ENSEIGNANT │
│ 4. QR de fin généré localement : "teacher-close-{seanceId}"     │
│ 5. Étudiant scanne ce QR                                        │
│ 6. GET /api/seance/{id}/statut → vérifie CLOTURE_ENSEIGNANT     │
│ 7. POST /api/scan {FIN, GPS} → serveur calcule statut final     │
│ 8. Réponse : {statutCalcule: "PRESENT" | "RETARD" | "ABSENT"}  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Base de Données SQLite

### 7.1 Schéma Entité-Relation

```
┌─────────────────────┐      ┌──────────────────────┐
│     Enseignant      │      │      Etudiant         │
├─────────────────────┤      ├──────────────────────┤
│ id_enseignant (PK)  │      │ id_etudiant (PK)     │
│ matricule_enseignant│      │ matricule (UNIQUE)   │
│ nom, prenom         │      │ nom, prenom           │
│ device_uuid (UNIQUE)│      │ classe_id             │
└────────┬────────────┘      │ device_uuid (UNIQUE) │
         │                   └──────────┬────────────┘
         │                              │
         ▼                              ▼
┌────────────────────┐       ┌──────────────────────┐
│   SeanceSemaine    │       │      Emargement       │
├────────────────────┤       ├──────────────────────┤
│ id_semaine (PK)    │       │ id_emargement (PK)   │
│ classe_id          │       │ etudiant_id (FK)      │
│ semaine_iso        │ ◄──┐  │ seance_id (FK)        │
│ lat_reference      │    │  │ horodatage_scan_debut │
│ lon_reference      │    │  │ horodatage_scan_fin   │
│ rayon_metres       │    │  │ statut_final          │
│ token_semaine      │    │  │ lat_scan, lon_scan    │
└────────────────────┘    │  │ localisation_valide   │
                           │  └──────────────────────┘
┌───────────────────────┐  │
│        Seance         │  │
├───────────────────────┤  │
│ id_seance (PK)        │──┘
│ nom_matiere           │
│ classe_id             │
│ date_jour             │
│ heure_debut, heure_fin│
│ statut_seance         │
│ enseignant_id (FK)    │
│ semaine_id (FK)       │
└───────────────────────┘
```

### 7.2 Description des tables

#### `Etudiant`
- `matricule` : Identifiant unique institutionnel (ex: `2024-B2-001`)
- `device_uuid` : `ANDROID_ID` de l'appareil, null tant que l'enrôlement n'est pas effectué
- `classe_id` : Identifiant de la classe (ex: `B2_IT`, `B2_MANAGEMENT`)

#### `SeanceSemaine`
- `semaine_iso` : Format ISO 8601 (`2026-W24`)
- `lat_reference / lon_reference` : Coordonnées GPS copiées depuis Google Maps
- `rayon_metres` : Rayon de tolérance de présence physique (défaut : **200 m**)
- `token_semaine` : Signature HMAC-SHA256 hexadécimale — empêche la falsification des QR codes

#### `Seance`
- `statut_seance` : `PLANIFIE` → `EN_COURS` → `CLOTURE_ENSEIGNANT`
- `semaine_id` : Lien optionnel avec la semaine parente (permet la résolution de séance courante)

#### `Emargement`
- `statut_final` : Valeur finale calculée par `StatutUtils` : `EN_ATTENTE` | `PRESENT` | `RETARD` | `ABSENT`
- `localisation_valide` : Résultat de la vérification GPS (audit anti-fraude)

### 7.3 Emplacement physique

```
KeycePass/
└── desktopApp/
    └── database/
        └── keycepass_central.db   ← Versionné dans Git (exception .gitignore)
```

---

## 8. Sécurité et Anti-Fraude

### 8.1 Liaison Appareil ↔ Identité (Anti-Usurpation)

L'enrôlement lie de manière **permanente** le `ANDROID_ID` de l'appareil (`device_uuid`) au matricule d'un étudiant en base de données.

Règles serveur :
1. Si `device_uuid` **null** → liaison acceptée (premier enrôlement)
2. Si `device_uuid` existant **= payload.deviceUuid** → réenrôlement du même appareil accepté
3. Si `device_uuid` existant **≠ payload.deviceUuid** → **REFUS** (appareil différent)

### 8.2 Géolocalisation (Anti-Présence Délocalisée)

```kotlin
object GeoUtils {
    // Formule de Haversine : précise pour distances < 10 km
    fun distanceMetres(lat1, lon1, lat2, lon2): Double

    // Validation : étudiant dans le rayon de la salle
    fun localisationValide(latScan, lonScan, latRef, lonRef, rayonM = 200): Boolean
}
```

- Les coordonnées GPS de chaque scan sont **archivées** dans `EmargementTable` pour audit
- En l'absence de coordonnées GPS (app non mise à jour), le scan est accepté par défaut

### 8.3 Authentification des QR Codes (Anti-Falsification)

Les QR codes de présence contiennent un **token HMAC-SHA256** calculé à partir des données de la semaine. Ce token empêche la création de QR codes frauduleux.

### 8.4 Session Chiffrée Mobile

Les données sensibles de l'étudiant (matricule, UUID, rôle, URL serveur) sont stockées via **AndroidX Security Crypto** :
- Schéma de chiffrement des clés : `AES256_SIV`
- Schéma de chiffrement des valeurs : `AES256_GCM`
- Clé maître : `MasterKey.KeyScheme.AES256_GCM`

---

## 9. Flux Utilisateurs (User Stories)

### US_01 — Enrôlement initial

**Acteur** : Étudiant (ou Enseignant)  
**Précondition** : L'admin a importé la liste Excel et généré le QR d'enrôlement de la classe  

1. L'admin affiche le QR d'enrôlement dans l'onglet **Étudiants** → **QR Code de la classe**
2. L'étudiant ouvre l'app → saisit matricule, nom, prénom, URL serveur, sélectionne le rôle
3. L'app envoie `POST /api/enrolement` avec son matricule et ANDROID_ID
4. Le serveur valide, lie l'UUID au matricule → `{success: true}`
5. La session est sauvegardée chiffrée. Redirection vers l'écran principal.

### US_02 — Premier scan d'arrivée

**Acteur** : Étudiant  
**Précondition** : Étudiant enrôlé, séance planifiée, QR hebdomadaire affiché en salle  

1. L'étudiant appuie sur "Scanner" dans l'app
2. Scanne le QR hebdomadaire de la salle
3. L'app résout la séance courante via l'API
4. GPS capturé, envoi `POST /api/scan {DEBUT}` avec coordonnées
5. Vérification géolocalisation côté serveur (< 200 m)
6. Statut provisoire affiché : **"À L'HEURE"** ou **"EN RETARD"**

### US_03 — Second scan de fin

**Acteur** : Étudiant  
**Précondition** : Séance clôturée par l'enseignant  

1. L'enseignant a clôturé la séance → affiche son QR de fin
2. L'étudiant appuie sur "Scanner le QR de fin de cours"
3. Scanne le QR `teacher-close-{seanceId}`
4. L'app vérifie la clôture via l'API
5. Envoi `POST /api/scan {FIN}` → statut final calculé côté serveur
6. Statut final affiché : **PRESENT**, **RETARD** ou **ABSENT**

### US_04 — Clôture de séance (Enseignant)

**Acteur** : Enseignant  
**Précondition** : Enseignant enrôlé avec rôle ENSEIGNANT  

1. L'enseignant ouvre l'app → écran **Espace Enseignant**
2. Sélectionne sa séance dans la liste synchronisée
3. Appuie sur **"Clôturer la séance"**
4. L'app envoie `POST /api/seance/{id}/cloturer`
5. Un QR Code `teacher-close-{id}` est généré et affiché à l'écran
6. Les étudiants peuvent alors effectuer leur scan de fin

### US_05 — Statistiques (Délégué)

**Acteur** : Délégué de classe  
**Précondition** : Séance en cours ou terminée  

1. Le délégué peut consulter les stats via `GET /api/seance/{id}/stats`
2. La réponse contient : totalInscrits, totalPresents, totalRetards, totalAbsents, cloture

---

## 10. Stack Technique

### 10.1 Technologies partagées

| Technologie | Rôle | Version |
|-------------|------|---------|
| **Kotlin** | Langage principal | 2.x |
| **KotlinX Serialization** | JSON (réseau) | 1.7+ |
| **Ktor** | Client HTTP (Android) + Serveur HTTP (Desktop) | 2.x |
| **Gradle (Kotlin DSL)** | Build system multi-module | 8.x |

### 10.2 Application Desktop

| Technologie | Rôle |
|-------------|------|
| **Compose Desktop** | Interface graphique JVM |
| **Exposed ORM** | Abstraction SQL (core + jdbc + dao) |
| **SQLite JDBC** | Driver base de données |
| **Apache POI** | Import fichiers Excel (.xlsx) |
| **ZXing (JavaSE)** | Génération de QR codes |
| **Ktor Server CIO** | Serveur HTTP embarqué |
| **jmDNS** | Découverte réseau mDNS (keycepass.local) |
| **Kotlin Coroutines (Swing)** | Threading UI desktop |
| **JVM 21** | Runtime |

### 10.3 Application Android

| Technologie | Rôle |
|-------------|------|
| **Jetpack Compose** | Interface graphique déclarative |
| **Navigation Compose** | Routage entre écrans |
| **Room (+ KSP)** | Base de données locale SQLite |
| **AndroidX Security Crypto** | EncryptedSharedPreferences (AES-256) |
| **Ktor Client (OkHttp)** | Client HTTP REST |
| **Google Play Services Location** | GPS (FusedLocationProviderClient) |
| **ZXing Android Embedded** | Scanner de QR codes (CaptureActivity) |
| **Accompanist Permissions** | Gestion des permissions Compose |
| **Lifecycle ViewModel Compose** | ViewModel dans Compose |
| **Android API 24+** | Compatibilité minimale |

### 10.4 Outils de développement

| Outil | Usage |
|-------|-------|
| **Git / GitHub** | Versioning, collaboration (`keisary/KeycePass`) |
| **Python 3** | Scripts utilitaires (`generate_etudiants.py`, `clean_db.py`) |
| **openpyxl** | Génération de fichiers Excel de test |
| **IntelliJ IDEA / Android Studio** | IDE de développement |

---

## 11. Structure du Dépôt Git

```
KeycePass/
├── .gitignore                          # Exclut build/, *.db sauf keycepass_central.db
├── README.MD                           # Guide d'installation et d'utilisation
├── CHANGELOG.md                        # Historique des versions
├── INTEGRATION.md                      # Guide d'intégration mobile ↔ serveur
├── CAHIER DES CHARGES V1.pdf           # Cahier des charges original
├── settings.gradle.kts                 # Modules : shared, desktopApp, androidApp
├── build.gradle.kts                    # Config root (versions plugins)
├── gradle/libs.versions.toml           # Catalog de dépendances (version catalog)
│
├── shared/                             # Module partagé Kotlin Multiplatform
│   └── src/commonMain/kotlin/
│       └── com/ak/keycepass/shared/
│           ├── domain/
│           │   ├── model/              # Emargement.kt, Etudiant.kt, Seance.kt
│           │   └── utils/             # StatutUtils.kt, AntiFraude.kt
│           └── network/               # NetworkModels.kt (DTOs réseau)
│
├── desktopApp/                         # Application Administrateur (JVM)
│   ├── build.gradle.kts
│   ├── database/
│   │   └── keycepass_central.db        # Base SQLite versionnée
│   └── src/main/kotlin/com/ak/keycepass/desktop/
│       ├── Main.kt
│       ├── Screen.kt
│       ├── data/
│       │   ├── database/              # DatabaseManager, DatabaseTables, ImportService
│       │   ├── server/                # KtorServer.kt (tous les endpoints REST)
│       │   ├── service/               # SeanceSemaineService.kt
│       │   └── utils/                 # GeoUtils.kt, QrCodeGenerator.kt
│       └── ui/
│           ├── AdminLayout.kt
│           ├── screens/               # DashboardScreen, EtudiantsScreen, QRManagementScreen...
│           ├── theme/                 # Couleurs, typographie Compose Desktop
│           └── viewmodel/             # AdminViewModel.kt
│
├── androidApp/                         # Application Mobile (Android)
│   ├── build.gradle.kts
│   └── src/main/kotlin/com/ak/keycepass/android/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── local/
│       │   │   ├── LocalDatabase.kt   # Room Database
│       │   │   ├── SessionManager.kt  # EncryptedSharedPreferences
│       │   │   ├── UserRole.kt
│       │   │   ├── dao/               # EmargementDao, EtudiantDao, SeanceDao
│       │   │   └── entities/          # EmargementLocal, EtudiantLocal, SeanceLocal
│       │   ├── network/
│       │   │   └── NetworkClient.kt   # Client HTTP Ktor
│       │   └── repository/
│       │       └── AttendanceRepository.kt
│       ├── navigation/
│       │   └── Navigation.kt          # NavHost, destinations dynamiques
│       └── ui/
│           ├── screens/               # LoginScreen, ScanScreen, TeacherScreen
│           ├── theme/                 # KeycePassTheme
│           └── viewmodel/             # EnrolementViewModel, ScanViewModel
│
├── generate_etudiants.py               # Génère des fichiers Excel de test
├── clean_db.py                         # Nettoie la base de données (dev)
├── etudiants_B2_IT.xlsx                # Données test B2_IT
├── etudiants_B2_MANAGEMENT.xlsx        # Données test B2_MANAGEMENT
├── launch.bat                          # Lance l'application Desktop
└── launch_console.bat                  # Lance avec console (debug)
```

---

## 12. Procédures de Déploiement

### 12.1 Prérequis

- **JDK 21** (pour le Desktop)
- **Android SDK 36** (pour l'app mobile)
- **Gradle 8.x** (wrapper inclus dans le projet)
- **Réseau** : Desktop et mobiles sur le même réseau Wi-Fi (ou VPN/Internet)

### 12.2 Lancement de l'application Desktop

```batch
# Option 1 : Via le batch
.\launch.bat

# Option 2 : Via Gradle
.\gradlew :desktopApp:run

# Option 3 : Via l'exécutable distribué
.\gradlew :desktopApp:createDistributable
```

L'application démarre automatiquement le **serveur HTTP sur le port 8080**.

### 12.3 Découverte de l'IP

L'interface Desktop affiche l'URL à communiquer aux étudiants :
- Exemple : `http://192.168.1.10:8080`
- Cette URL est encodée dans les QR codes d'enrôlement et de présence

### 12.4 Installation de l'application Android

```bash
# Build APK debug
.\gradlew :androidApp:assembleDebug

# Installation directe sur appareil connecté
.\gradlew :androidApp:installDebug
```

### 12.5 Workflow de mise en production

1. L'admin importe la liste des étudiants (`.xlsx`) via l'onglet **Étudiants**
2. L'admin crée les semaines avec coordonnées GPS dans l'onglet **Gestion QR**
3. Les QR codes d'enrôlement sont affichés/imprimés classe par classe
4. Les étudiants s'enrôlent une seule fois avec leur app
5. Chaque semaine, le QR hebdomadaire est affiché en salle
6. Chaque cours : l'enseignant clôture → les étudiants scannent leur sortie

### 12.6 Scripts utilitaires Python

```bash
# Générer des fichiers Excel de test (openpyxl)
python generate_etudiants.py

# Nettoyer la base de données (garder la structure, vider les données)
python clean_db.py
```

---

## 13. Corrections Appliquées & Problèmes Résolus

Cette section documente les bogues identifiés lors des tests d'intégration et les corrections apportées au code pour permettre le fonctionnement du système end-to-end.

### 13.1 Enrôlement Enseignant Non Supporté

**Problème** : Le point d'accès `POST /api/enrolement` ne cherchait que dans `EtudiantTable`. Les enseignants ne pouvaient pas s'enrôler et obtenaient un échec silencieux.

**Correction** (`KtorServer.kt`) :
```kotlin
// AVANT : recherche uniquement dans EtudiantTable
val etudiant = EtudiantTable.selectAll()
    .where { EtudiantTable.matricule eq payload.matricule }.firstOrNull()

// APRÈS : fallback vers EnseignantTable si étudiant introuvable
if (etudiant != null) {
    // ... lier l'UUID à l'étudiant
} else {
    val enseignant = EnseignantTable.selectAll()
        .where { EnseignantTable.matriculeEnseignant.lowerCase()
                 eq payload.matricule.trim().lowercase() }
        .firstOrNull()
    // ... lier l'UUID à l'enseignant
}
```

### 13.2 Comparaison Insensible à la Casse des Matricules

**Problème** : Les matricules saisis en minuscules ou avec des espaces ne correspondaient pas aux enregistrements en base (ex. `"2024-B2-001 "` vs `"2024-B2-001"`), causant des échecs de scan.

**Correction** :
- Côté Exposed (SQL) : `.lowerCase() eq payload.matricule.trim().lowercase()`
- Côté Kotlin : `.trim().lowercase()` systématique avant toute comparaison
- Appliqué dans : `POST /api/enrolement`, `POST /api/scan`, `POST /api/sync`

### 13.3 Sanitisation de l'URL du Serveur

**Problème** : L'URL saisie manuellement ou scannée depuis un QR pouvait contenir des espaces en fin de chaîne ou un slash terminal (ex. `"http://192.168.1.10:8080/ "`), causant des erreurs HTTP 404 sur toutes les requêtes.

**Correction** (`LoginScreen.kt`, `AttendanceRepository.kt`) :
```kotlin
val serverUrl = rawUrl.trim().trimEnd('/')
// Exemple : "http://192.168.1.10:8080/ " → "http://192.168.1.10:8080"
```

### 13.4 Parsing QR Code Robuste

**Problème** : La fonction `parseQrParams` dans `AttendanceRepository.kt` plantait avec une `IndexOutOfBoundsException` si un paramètre QR était mal formé (absent du caractère `=`).

**Correction** :
```kotlin
private fun parseQrParams(contenu: String): Map<String, String> {
    return queryString.split("&")
        .mapNotNull { param ->
            val idx = param.indexOf('=')
            if (idx < 0) null  // Ignorer les paramètres sans valeur
            else param.substring(0, idx) to URLDecoder.decode(param.substring(idx + 1), "UTF-8")
        }.toMap()
}
```

### 13.5 Intégration Scanner ZXing avec Jetpack Compose

**Problème** : L'ancien code utilisait `onActivityResult` (API obsolète, incompatible avec le contexte Compose) pour récupérer le résultat du scanner ZXing `CaptureActivity`. Le scan se lançait mais le résultat était perdu.

**Correction** (`ScanScreen.kt`) :
```kotlin
// AVANT (obsoète)
@Deprecated("Use ActivityResultLauncher instead")
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { ... }

// APRÈS (Compose-compatible)
val scanLauncher = rememberLauncherForActivityResult(
    contract = ScanContract()
) { result ->
    result.contents?.let { viewModel.traiterResultatScan(it) }
}
// ...
Button(onClick = { scanLauncher.launch(ScanOptions()) }) { ... }
```

### 13.6 Affichage URL Serveur au Démarrage

**Problème** : Les administrateurs ne savaient pas quelle adresse IP communiquer aux étudiants. L'URL du serveur Ktor n'était pas visible.

**Correction** (`Main.kt`, commit `ece3e9a`) :
```kotlin
// Le serveur affiche son URL exacte dans la console au démarrage :
println("[KeycePass] Serveur démarré : ${KtorServer.getServerUrl()}")
// Exemple : [KeycePass] Serveur démarré : http://192.168.1.50:8080
```

---

## 14. Travaux des Collaborateurs

Cette section documente les contributions apportées par les membres de l'équipe dans les derniers commits avant la livraison finale.

### 14.1 Commit `b88c5ef` — "final" (UnoriginalDude14, 11 Juin 2026)

#### 14.1.1 Affichage de tous les étudiants sur le Dashboard

**Avant** : Le Dashboard ne listait les étudiants que s'ils étaient enrôlés (`deviceUuid` non null) ET s'il existait une séance active. En l'absence de séance, la liste était vide.

**Après** : Le Dashboard affiche **toujours** tous les étudiants de la classe sélectionnée :
- **Sans séance active** : Tous les étudiants s'affichent avec le statut `ABSENT` par défaut et les horodatages `---`
- **Avec séance active** : Tous les étudiants s'affichent avec leur statut réel d'émargement

```kotlin
// AdminViewModel.kt — Branche sans séance active
if (seance == null) {
    val etudiants = EtudiantTable.selectAll()
        .where { EtudiantTable.classeId eq classe }
        .toList()
    val rows = etudiants.mapIndexed { index, etudiant ->
        val enrolled = !etudiant[EtudiantTable.deviceUuid].isNullOrEmpty()
        AttendanceRow(
            id = index + 1,
            nom = etudiant[EtudiantTable.nom],
            prenom = etudiant[EtudiantTable.prenom],
            matricule = etudiant[EtudiantTable.matricule],
            statut = StatutFinal.ABSENT,
            heureScanDebut = "---",
            heureScanFin = "---",
            classe = etudiant[EtudiantTable.classeId],
            isEnrolled = enrolled   // NOUVEAU champ
        )
    }
    Pair(null, rows)
}
```

#### 14.1.2 Badge « Non Enrôlé » dans le Dashboard

Ajout du champ `isEnrolled: Boolean` dans `AttendanceRow` (cheminé depuis `EtudiantTable.deviceUuid`). Le composable `StudentRow` adapt sa couleur et son libellé en conséquence :

| État | Couleur badge | Libellé |
|------|--------------|----------|
| `isEnrolled = false` | Gris `onSurfaceVariant` | « Non Enrôlé » |
| `isEnrolled = true` + PRESENT | Vert `StatusPresent` | « Present » |
| `isEnrolled = true` + RETARD | Orange `StatusLate` | « Retard » |
| `isEnrolled = true` + ABSENT | Rouge `StatusAbsent` | « Absent » |

#### 14.1.3 Classe de session sérializable et nouveau DTO `SeanceDto`

- `StatutSeance` annoté avec `@Serializable` dans `Seance.kt` pour permettre la sérialisation JSON.
- Nouveau `SeanceDto` ajouté dans `NetworkModels.kt` avec tous les champs de séance (id, matiere, classe, date, heures, statut), utilisé par `GET /api/seances`.

#### 14.1.4 Correction de l'état de séance dans le Dashboard

- **Avant** : `seanceStatut` était forcé à `EN_COURS` même si aucune séance n'était active
- **Après** : `seanceStatut = if (seanceRow != null) StatutSeance.EN_COURS else StatutSeance.PLANIFIE`
- La matière courante affichée passe à `"Aucune seance en cours"` si aucune séance n'est active

### 14.2 Commit `594ccf8` — ScanScreen UI Redesign (Juin 2026)

Refonte complète de l'écran étudiant mobile (`ScanScreen.kt`) :
- **Design glassmorphique** sombre avec palette de couleurs sur mesure (Indigo/Slate)
- **Carte profil étudiant** avec initiales générées, nom, matricule, classe
- **Bouton radar pulsant** : animation `InfiniteTransition` simulant une impulsion sonar
- **Liste des séances du jour** avec badges de statut (Planifié, En cours, Clôturé)
- **Transitions `AnimatedContent`** entre les états (Prêt, Traitement, Succès, Erreur)

### 14.3 Commit `ece3e9a` — Affichage URL dynamique au démarrage

Le serveur détectait déjà l'IP locale via `getLocalIpAddress()`. Ce commit :
- Affiche l'URL complète dans la console au démarrage : `[KeycePass] Serveur démarré : http://192.168.x.x:8080`
- Permet aux administrateurs de copier-coller l'URL exacte dans les QR codes et de la communiquer aux étudiants

---

## Annexe — Commits Git Significatifs

| Commit | Auteur | Description |
|--------|--------|-------------|
| `feat(desktop): add navigation and EtudiantsScreen tab` | Équipe | Ajout onglet Étudiants avec import Excel |
| `feat(desktop): add enregistrerSeance and filter dashboard` | Équipe | Séances par jours, filtrage dashboard |
| `chore: autoriser et ajouter la base de données centrale de test` | Équipe | DB versionnée |
| `fix(android): correction navigation enseignant, sync seances serveur, UX cloture explicite` | Équipe | Flux enseignant complet |
| `ddee97a` — `fix(android,desktop): resolve connection, case matching...` | Équipe | **Corrections enrôlement, casse, URL, QR** |
| `445027c` — `fix(login): Remove URLEncoder on serverUrl` | Équipe | URL serveur non double-encodée dans QR |
| `ece3e9a` — `fix(desktop): print dynamic KtorServer URL on startup` | Équipe | URL affichée au démarrage |
| `594ccf8` — `style(android): completely redesign ScanScreen` | Équipe | Refonte UI Scanner glassmorphique |
| `b88c5ef` — `final` | UnoriginalDude14 | Dashboard tous-étudiants + badge Non-Enrôlé |
