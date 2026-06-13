# KeycePass

> Système de gestion de présence numérique par QR Code pour établissements d'enseignement supérieur.  
> **Stack** : Kotlin Multiplatform · Compose Desktop · Jetpack Compose · Ktor HTTP · SQLite (Exposed)

---

## Table des matières

1. [Présentation](#présentation)
2. [Architecture](#architecture)
3. [Prérequis](#prérequis)
4. [Démarrage rapide](#démarrage-rapide)
   - [Desktop (Serveur + Admin)](#1-desktop-serveur--admin)
   - [Android (Mobile)](#2-android-mobile)
5. [Rôles utilisateur](#rôles-utilisateur)
6. [Workflow d'utilisation](#workflow-dutilisation)
7. [API du serveur](#api-du-serveur)
8. [Format des QR Codes](#format-des-qr-codes)
9. [Sécurité & Anti-Fraude](#sécurité--anti-fraude)
10. [Scripts utilitaires](#scripts-utilitaires)
11. [FAQ / Dépannage](#faq--dépannage)

---

## Présentation

KeycePass remplace la feuille de présence papier par un système **mobile + desktop** entièrement local (réseau Wi-Fi, aucun cloud requis) :

- 📱 **App Android** : L'étudiant scanne le QR Code de la salle → présence enregistrée automatiquement
- 💻 **App Desktop** : L'administrateur gère les étudiants, les séances, génère les QR Codes et consulte le tableau de bord
- 🔐 **Double scan** : entrée + sortie avec calcul automatique (Présent / Retard / Absent selon la règle des 15 min)
- 🛡️ **Anti-fraude** : liaison exclusive téléphone ↔ étudiant (ANDROID_ID) + vérification GPS optionnelle

---

## Architecture

```
┌────────────────────────────────────────────────────────┐
│                    MODULE :shared                        │
│  DTOs communs · Logique de calcul de statut             │
│  (ScanPayload, ScanResponse, StatutUtils, ...)          │
└──────────────┬─────────────────────┬────────────────────┘
               │                     │
        ┌──────▼──────┐       ┌──────▼──────┐
        │  androidApp  │       │ desktopApp  │
        │  (mobile)    │◄─────►│  (serveur)  │
        │  Room + Ktor │ Wi-Fi │  SQLite     │
        │  Client      │ :8080 │  Ktor Server│
        └─────────────┘       └─────────────┘
```

- **desktopApp** : joue le rôle de **serveur HTTP** (Ktor sur port 8080). Toute la communication passe par ce point central.
- **androidApp** : client mobile pour étudiants et enseignants. Découvre l'adresse IP du serveur via QR Code.
- **shared** : module Kotlin Multiplatform avec les DTOs sérialisables et les utilitaires partagés.

---

## Prérequis

| Composant | Version minimale |
|-----------|-----------------|
| **JDK** | 17+ (recommandé : JDK 21) |
| **Android SDK** | API 24 (Android 7.0) |
| **Gradle** | 8.x (wrapper inclus) |
| **Réseau** | Desktop et mobiles sur le **même réseau Wi-Fi** |
| **OS Desktop** | Windows 10+ / macOS / Linux |

---

## Démarrage rapide

### 1. Desktop (Serveur + Admin)

#### Option A — Batch (recommandé Windows)
```batch
.\launch.bat
```
ou avec console de debug :
```batch
.\launch_console.bat
```

#### Option B — Gradle
```batch
.\gradlew.bat :desktopApp:run
```

> ⚠️ Si la fenêtre n'apparaît pas avec Gradle, utiliser `launch.bat` qui lance via `javaw` directement.

**Au démarrage, la console affiche l'URL du serveur :**
```
[KeycePass] Serveur démarré : http://192.168.1.50:8080
```
→ **Notez cette URL**, elle est nécessaire pour les QR Codes d'enrôlement et pour la saisie manuelle sur mobile.

---

### 2. Android (Mobile)

#### Compiler et installer l'APK

```batch
# Build APK debug
.\gradlew.bat :androidApp:assembleDebug

# Installation directe (appareil connecté en USB)
.\gradlew.bat :androidApp:installDebug
```

Ou ouvrir le projet dans **Android Studio** et lancer sur un émulateur ou appareil physique.

> ℹ️ Sur émulateur, le GPS sera absent. Le serveur accepte les scans sans coordonnées GPS (champs `lat`/`lon` sont optionnels).

---

## Rôles utilisateur

L'application mobile est **identique pour tous**. Le rôle est attribué lors de l'enrôlement initial et stocké de façon chiffrée (EncryptedSharedPreferences, AES-256).

| Rôle | Écran affiché après enrôlement | Capacités |
|------|-------------------------------|-----------|
| `ETUDIANT` | Scan Screen | Scanner QR entrée + sortie |
| `ENSEIGNANT` | Teacher Screen | Sélectionner sa séance, clôturer, afficher QR de fin |
| `DELEGUE` | Scan Screen | Scanner + consultation stats via API (IHM V4) |

---

## Workflow d'utilisation

### Étape 1 — Import des étudiants (Admin Desktop)

1. Onglet **Étudiants** → **Importer Excel**
2. Sélectionner un fichier `.xlsx` (colonnes : matricule, nom, prénom, classeId)
3. Les étudiants apparaissent dans la liste (statut **Non Enrôlé** jusqu'au premier scan)

> Des fichiers Excel de test sont disponibles à la racine : `etudiants_B2_IT.xlsx`, `etudiants_B2_MANAGEMENT.xlsx`

---

### Étape 2 — Génération du QR d'enrôlement (Admin Desktop)

1. Onglet **Étudiants** → **QR Code de la classe**
2. Sélectionner la classe → **Générer**
3. Afficher ou imprimer le QR Code → les étudiants scannent ce code pour s'enrôler

**Format du QR d'enrôlement :**
```
keycepass://enrolement?classeId=B2_IT&token=HMAC_SHA256&serverUrl=http://192.168.1.50:8080&role=ETUDIANT
```

---

### Étape 3 — Enrôlement (Étudiant / Enseignant, première utilisation uniquement)

Sur l'app mobile :
1. Saisir le **matricule** (insensible à la casse, espaces ignorés automatiquement)
2. Saisir **nom** et **prénom**
3. Scanner le **QR Code d'enrôlement** affiché par l'admin  
   *ou* saisir manuellement l'URL du serveur (ex: `http://192.168.1.50:8080`)
4. Sélectionner le **rôle** (`ETUDIANT` ou `ENSEIGNANT`)
5. Appuyer sur **S'enrôler**

→ L'app se connecte au serveur, lie votre ANDROID_ID à votre matricule, et vous redirige vers votre écran principal.

> **Note** : La comparaison des matricules est insensible à la casse. `"2024-b2-001"`, `"2024-B2-001"` et `" 2024-B2-001 "` sont équivalents.

---

### Étape 4 — Création d'une semaine & QR de présence (Admin Desktop)

1. Onglet **QR Management** → **Nouvelle semaine**
2. Renseigner : classe, semaine ISO (ex: `2026-W25`), coordonnées GPS de la salle (depuis Google Maps)
3. Ajouter les séances jour par jour (matière, horaires, enseignant)
4. **Générer le QR de présence** → à afficher en salle chaque semaine

**Format du QR de présence hebdomadaire :**
```
keycepass://presence?semaineId=3&classeId=B2_IT&token=HMAC&serverUrl=http://192.168.1.50:8080
```

---

### Étape 5 — Scan d'arrivée (Étudiant)

1. Ouvrir l'app → Bouton **Scanner** (radar pulsant)
2. Scanner le QR Code affiché en salle
3. L'app résout automatiquement la séance en cours → affiche la matière et les horaires
4. Statut provisoire affiché : **À l'heure** (≤ 15 min) ou **En retard** (> 15 min)

---

### Étape 6 — Clôture de séance (Enseignant)

1. Écran **Espace Enseignant** → sélectionner la séance dans la liste
2. Appuyer sur **Clôturer la séance**
3. Un QR Code de fin est généré et affiché → les étudiants scannent ce code pour valider leur sortie

**Format du QR de clôture :**
```
teacher-close-{seanceId}
```

---

### Étape 7 — Scan de sortie (Étudiant)

1. Scanner le QR Code de fin affiché par l'enseignant
2. L'app calcule et affiche le **statut final** : ✅ Présent / ⚠️ Retard / ❌ Absent

---

### Étape 8 — Dashboard Admin (Desktop)

Le tableau de bord se rafraîchit automatiquement :
- **KPIs** : Présents / Retards / Absents / Total
- **Liste des étudiants** : tous affichés (enrôlés ou non), avec badge coloré
  - 🟢 **Présent** — 🟡 **Retard** — 🔴 **Absent** — ⚪ **Non Enrôlé**
- **Filtre par classe** : sélecteur dynamique chargé depuis la DB
- **Statut de séance** : PLANIFIÉ / EN_COURS / CLÔTURE_ENSEIGNANT

---

## API du serveur

Base URL : `http://{IP_DU_DESKTOP}:8080`

| Méthode | Route | Description |
|---------|-------|-------------|
| `GET` | `/api/ping` | Santé du serveur |
| `POST` | `/api/enrolement` | Enrôler un étudiant ou enseignant (liaison UUID) |
| `POST` | `/api/scan` | Enregistrer un scan de présence (début ou fin) |
| `GET` | `/api/seance/{id}/statut` | Vérifier si la séance est clôturée |
| `POST` | `/api/seance/{id}/cloturer` | Clôturer la séance (enseignant) |
| `GET` | `/api/seance/{id}/stats` | Statistiques de présence (délégué) |
| `GET` | `/api/semaine/{id}/seance-courante` | Séance active à l'instant T pour une semaine |
| `GET` | `/api/seances` | Liste complète de toutes les séances |
| `POST` | `/api/sync` | Réception batch de scans hors-ligne |

### Exemple — `POST /api/enrolement`

**Corps :**
```json
{
  "matricule": "2024-B2-001",
  "deviceUuid": "abc-def-123-456"
}
```

**Réponse :**
```json
{ "success": true }
```

### Exemple — `POST /api/scan`

**Corps :**
```json
{
  "matricule": "2024-B2-001",
  "deviceUuid": "abc-def-123-456",
  "seanceId": 42,
  "timestamp": "2026-06-13T08:05:00",
  "scanType": "DEBUT",
  "lat": 48.8566,
  "lon": 2.3522
}
```

**Réponse :**
```json
{
  "success": true,
  "statutCalcule": "EN_ATTENTE",
  "message": "Scan de début enregistré.",
  "localisationRefusee": false
}
```

---

## Format des QR Codes

| Type | Format |
|------|--------|
| **Enrôlement** | `keycepass://enrolement?classeId={id}&token={hmac}&serverUrl={url}&role={ETUDIANT\|ENSEIGNANT}` |
| **Présence hebdomadaire** | `keycepass://presence?semaineId={id}&classeId={cId}&token={hmac}&serverUrl={url}` |
| **Clôture enseignant** | `teacher-close-{seanceId}` |

> Le token HMAC-SHA256 intégré dans les QR codes d'enrôlement et de présence empêche la falsification. Il est généré côté serveur lors de la création de la semaine.

---

## Sécurité & Anti-Fraude

### Liaison appareil ↔ étudiant (Device Binding)

Lors de l'enrôlement, l'`ANDROID_ID` de l'appareil est lié de façon permanente au matricule en base :

| Situation | Comportement |
|-----------|-------------|
| Première liaison (`deviceUuid` null) | ✅ Accepté |
| Même appareil re-scanne | ✅ Accepté (re-enrôlement) |
| Appareil différent | ❌ Refusé (fraude détectée) |
| Admin réinitialise | ✅ Liaison effacée, re-enrôlement possible |

### Vérification GPS (optionnelle)

Si des coordonnées GPS sont fournies lors du scan et qu'une semaine avec coordonnées GPS de référence existe, le serveur calcule la distance (formule de Haversine). Si la distance > **200 mètres**, le scan est refusé avec `localisationRefusee: true`.

### Chiffrement mobile

Les données sensibles (matricule, UUID, rôle, URL serveur) sont stockées via **EncryptedSharedPreferences** (AES256-GCM, clé maître AES256).

---

## Scripts utilitaires

```bash
# Générer des fichiers Excel de test (nécessite openpyxl)
python generate_etudiants.py

# Nettoyer la base de données (conserve la structure, vide les données)
python clean_db.py
```

Pour obtenir les coordonnées GPS d'une salle :
1. Ouvrir [Google Maps](https://maps.google.com)
2. Clic droit sur l'emplacement → **Plus d'infos sur cet endroit**
3. Copier les coordonnées (ex. `48.8566, 2.3522`)

---

## FAQ / Dépannage

### ❓ Quelle adresse IP saisir sur le mobile ?

Regarder la **console de l'application Desktop** au démarrage :
```
[KeycePass] Serveur démarré : http://192.168.1.50:8080
```
Cette URL est également encodée dans les QR Codes d'enrôlement.

### ❓ L'enrôlement échoue ("Étudiant introuvable")

Vérifier que :
- Le matricule saisi correspond exactement à celui dans le fichier Excel importé (la casse est ignorée, mais les caractères doivent correspondre)
- L'étudiant a bien été importé dans la base via l'onglet **Étudiants** du Desktop
- L'URL du serveur est correcte et accessible (tester `http://{IP}:8080/api/ping` dans un navigateur)

### ❓ Le scan ne redirige pas vers l'écran de scan

Vérifier que :
- L'enrôlement a bien réussi (réponse `{"success": true}` attendue)
- Le `sessionManager.estEnrole` est `true` après l'enrôlement
- L'app n'est pas en cache — la relancer si nécessaire

### ❓ Le pare-feu Windows bloque les connexions

Autoriser les connexions entrantes sur le port **8080** pour Java/Kotlin :
```
Panneau de configuration → Pare-feu Windows Defender
→ Règles de trafic entrant → Nouvelle règle → Port → TCP 8080
```

### ❓ L'étudiant apparaît "Non Enrôlé" dans le dashboard

L'étudiant n'a pas encore effectué son enrôlement (son `deviceUuid` est null en base). Il doit scanner le QR d'enrôlement avec son téléphone.

---

> **Dépôt GitHub** : [keisary/KeycePass](https://github.com/keisary/KeycePass)  
> **Documentation technique** : [rapport_conception_technique_keycepass.md](./rapport_conception_technique_keycepass.md)  
> **Cahier des charges** : [docs/cahier-des-charges-v3.md](./docs/cahier-des-charges-v3.md)
