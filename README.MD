# KeycePass

Système de gestion de présence numérique pour établissements d'enseignement supérieur.

> **Ce README est destiné au collaborateur qui implémente les vues (UI).**
> Il documente l'architecture, les flux d'écrans, les nouvelles fonctionnalités de sécurité et les données exposées par les ViewModels.

---

## Table des matières

1. [Architecture générale](#architecture-générale)
2. [Rôles utilisateur](#rôles-utilisateur)
3. [Flux d'écrans](#flux-décrans)
4. [QR Code hebdomadaire](#qr-code-hebdomadaire)
5. [Vérification de géolocalisation](#vérification-de-géolocalisation)
6. [API du serveur Desktop](#api-du-serveur-desktop)
7. [ViewModels disponibles](#viewmodels-disponibles)
8. [Branches Git](#branches-git)
9. [Lancer le projet](#lancer-le-projet)

---

## Architecture générale

```
┌───────────────────────────────────────────────────────┐
│                   Module :shared                       │
│  DTOs communs (ScanPayload, ScanResponse, etc.)        │
│  Logique de calcul de statut (StatutUtils)             │
└───────────┬───────────────────────┬───────────────────┘
            │                       │
     ┌──────▼──────┐         ┌──────▼──────┐
     │  androidApp │         │ desktopApp  │
     │  (mobile)   │◄───────►│  (serveur)  │
     │  Room + Ktor│  Wi-Fi  │  SQLite     │
     │  Client     │  local  │  Ktor Server│
     └─────────────┘         └─────────────┘
```

- **desktopApp** : L'application Desktop (Windows/Linux/macOS) joue le rôle de **serveur**. Elle héberge un serveur HTTP Ktor sur le port `8080` accessible via le réseau Wi-Fi local. Il n'y a pas de cloud — tout communique en réseau local.
- **androidApp** : L'app mobile pour étudiants, délégués et enseignants. Elle découvre l'adresse IP du serveur via le QR Code.
- **shared** : Module Kotlin Multiplatform contenant les DTOs sérialisables et les utilitaires partagés.

---

## Rôles utilisateur

L'application mobile est **identique pour tous**. Le rôle est attribué **lors de l'enrôlement** et stocké localement de façon chiffrée.

| Rôle        | Accès                                                     |
|-------------|-----------------------------------------------------------|
| `ETUDIANT`  | Scanner le QR de présence (début + fin)                   |
| `DELEGUE`   | Scanner + consulter les statistiques de la séance         |
| `ENSEIGNANT`| Scanner + clôturer la séance (déverrouilage scan de fin)  |

---

## Flux d'écrans

### Écran 1 — Démarrage / Enrôlement

```
┌─────────────────────────────┐
│  Bienvenue dans KeycePass   │
│                             │
│  [Saisir mon matricule]     │
│  [Scanner le QR d'enrôlement]│
└─────────────────────────────┘
```

1. L'étudiant saisit son **matricule**.
2. Il scanne le **QR Code d'enrôlement** affiché par l'administration.
   - Format du QR : `keycepass://enrolement?classeId=B2_IT&token=XXX&serverUrl=http://192.168.x.x:8080`
3. L'app envoie `POST /api/enrolement` et sauvegarde la session localement (chiffré).
4. L'app redirige vers l'écran principal selon le rôle reçu.

**ViewModel** : `EnrolementViewModel`
- `uiState: StateFlow<EnrolementUiState>` — états : `Idle`, `Chargement`, `Succes(role)`, `Erreur`

---

### Écran 2 — Dashboard principal (post-enrôlement)

Affiché selon le rôle stocké :

| Rôle        | Widgets affichés                                          |
|-------------|-----------------------------------------------------------|
| ETUDIANT    | Bouton « Scanner le QR de présence »                     |
| DELEGUE     | Bouton « Scanner » + Section stats de la séance          |
| ENSEIGNANT  | Bouton « Scanner » + Bouton « Clôturer la séance »       |

---

### Écran 3 — Scanner QR de présence (NOUVEAU flux hebdomadaire)

> ⚠️ **Changement important** : Le QR Code de présence est maintenant **hebdomadaire**, pas par séance. L'UI doit gérer ce flux en deux étapes.

**Étape 1 — Scan du QR hebdomadaire**

L'étudiant scanne le QR affiché par l'administration.

Format du QR hebdomadaire :
```
keycepass://presence?semaineId=3&classeId=B2_IT&token=abc123...&serverUrl=http://192.168.x.x:8080
```

**Étape 2 — Résolution de la séance courante**

L'app appelle automatiquement `GET /api/semaine/{semaineId}/seance-courante` pour savoir quel cours est en cours à l'instant T.

- Si une séance est trouvée → afficher « Cours : Algorithmique — 08h00 à 10h00 » + bouton Confirmer
- Si aucune séance → message « Aucun cours en cours actuellement. »

**Étape 3 — Envoi du scan**

L'app envoie `POST /api/scan` avec les coordonnées GPS récupérées via le GPS du téléphone.

**ViewModel** : `ScanViewModel`
- `scanState: StateFlow<ScanUiState>` — états :
  - `Idle`
  - `ResolutionSeance` (appel API en cours)
  - `SeanceTrouvee(nomMatiere, heureDebut, heureFin)`
  - `ScanEnregistre(statutProvisoire)`
  - `RefusGeo` ← **NOUVEAU** — afficher « Vous êtes trop loin de la salle »
  - `Erreur(message)`

---

### Écran 4 — Résultat du scan

```
┌──────────────────────────┐     ┌──────────────────────────┐
│  ✅ Présence enregistrée │     │  📍 Localisation refusée │
│  Statut : À l'heure      │     │  Vous êtes à plus de     │
│  Cours : Algo avancée    │     │  200m de la salle        │
└──────────────────────────┘     └──────────────────────────┘
```

---

## QR Code hebdomadaire

### Pourquoi ?

Auparavant, un QR code était généré par séance (manuellement). Désormais, **un seul QR code couvre toute la semaine** pour une classe. L'administration le génère en début de semaine et l'affiche (à l'écran ou imprimé).

### Génération (côté Desktop - AdminViewModel)

```kotlin
// Créer une semaine avec les coordonnées GPS du lieu de cours
adminViewModel.creerSemaine(
    classeId = "B2_IT",
    semaineIso = "2026-W24",
    lat = 48.8566,   // Latitude saisie depuis Google Maps
    lon = 2.3522     // Longitude saisie depuis Google Maps
)

// Générer le QR Code à afficher
adminViewModel.genererQrPresenceSemaine(semaineId = 3)
```

### Sécurité du QR Code

Le token contenu dans le QR est un **HMAC-SHA256** signé côté serveur. Il est **opaque** (ne contient aucune information lisible) et empêche la falsification de QR codes maison.

---

## Vérification de géolocalisation

### Principe

Quand un étudiant scanne le QR de présence, l'app Android **envoie ses coordonnées GPS** avec le scan. Le serveur Desktop :

1. Récupère la semaine associée à la séance (qui contient la latitude/longitude de référence).
2. Calcule la distance avec la **formule de Haversine**.
3. Si la distance > **200 mètres** → le scan est refusé avec `localisationRefusee: true`.

### Côté UI Android

```kotlin
// L'UI doit récupérer la position GPS AVANT d'appeler le ViewModel
val location = fusedLocationClient.lastLocation.await()

scanViewModel.enregistrerPremierScan(
    contenuQr = contenuQrScanne,
    lat = location?.latitude,
    lon = location?.longitude
)
```

> **Permission Android requise** dans `AndroidManifest.xml` :
> ```xml
> <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
> <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
> ```

### Message d'erreur recommandé

```
📍 Présence refusée
Vous devez être dans la salle de cours pour enregistrer votre présence.
Distance maximale autorisée : 200 mètres.
```

---

## API du serveur Desktop

Base URL : `http://{IP_DU_PC}:8080`

| Méthode | Route | Description |
|---------|-------|-------------|
| `GET` | `/api/ping` | Santé du serveur |
| `POST` | `/api/enrolement` | Enrôler un étudiant (liaison UUID) |
| `POST` | `/api/scan` | Enregistrer un scan de présence (+ coords GPS) |
| `GET` | `/api/seance/{id}/statut` | Vérifier si la séance est clôturée |
| `POST` | `/api/seance/{id}/cloturer` | L'enseignant clôture la séance |
| `GET` | `/api/seance/{id}/stats` | Statistiques de présence (délégué) |
| `GET` | `/api/semaine/{id}/seance-courante` | **NOUVEAU** — Séance active pour une semaine |

### Détail : `POST /api/scan`

**Body** (JSON) :
```json
{
  "matricule": "2024001",
  "deviceUuid": "abc-def-123",
  "seanceId": 42,
  "timestamp": "2026-06-10T08:05:00",
  "scanType": "DEBUT",
  "lat": 48.8566,
  "lon": 2.3522
}
```

**Réponse** :
```json
{
  "success": true,
  "statutCalcule": "EN_ATTENTE",
  "message": "Scan de début enregistré.",
  "localisationRefusee": false
}
```

### Détail : `GET /api/semaine/{semaineId}/seance-courante`

**Réponse** (si une séance est en cours) :
```json
{
  "seanceId": 42,
  "nomMatiere": "Algorithmique avancée",
  "heureDebut": "08:00:00",
  "heureFin": "10:00:00"
}
```

**Réponse** (aucune séance) : `404 Not Found`

---

## ViewModels disponibles

### `EnrolementViewModel` (androidApp)

| StateFlow | Type | Description |
|-----------|------|-------------|
| `uiState` | `EnrolementUiState` | État du processus d'enrôlement |

| Méthode | Description |
|---------|-------------|
| `enroler(matricule, deviceUuid, contenuQr)` | Lance l'enrôlement |

---

### `ScanViewModel` (androidApp)

| StateFlow | Type | Description |
|-----------|------|-------------|
| `scanState` | `ScanUiState` | État du scan en cours |

| Méthode | Paramètres | Description |
|---------|-----------|-------------|
| `enregistrerPremierScan(qr, lat?, lon?)` | QR + GPS optionnel | Scan d'arrivée |
| `enregistrerSecondScan(qr, lat?, lon?)` | QR + GPS optionnel | Scan de fin |

---

### `DelegueViewModel` (androidApp)

| StateFlow | Type | Description |
|-----------|------|-------------|
| `statsSeance` | `SessionStatusDto?` | Statistiques en temps réel |

---

### `AdminViewModel` (desktopApp)

| StateFlow | Type | Description |
|-----------|------|-------------|
| `classes` | `List<String>` | Classes disponibles |
| `etudiants` | `List<Etudiant>` | Étudiants de la classe |
| `semaines` | `List<SeanceSemaineRow>` | Semaines créées |
| `qrCodeImage` | `BufferedImage?` | QR Code à afficher |
| `importState` | `ImportState` | État de l'import Excel |
| `creationSemaineState` | `CreationSemaineState` | État de création d'une semaine |

| Méthode | Description |
|---------|-------------|
| `importerExcel(fichier)` | Import liste étudiants depuis Excel |
| `genererQrEnrolement(classeId)` | QR Code d'enrôlement d'une classe |
| `creerSemaine(classeId, semaineIso, lat, lon)` | Créer une semaine avec géo |
| `chargerSemaines(classeId)` | Charger les semaines d'une classe |
| `genererQrPresenceSemaine(semaineId)` | QR Code de présence hebdomadaire |
| `chargerStatistiquesSeance(seanceId)` | Récupérer les stats de présence |

---

## Branches Git

| Branche | Contenu |
|---------|---------|
| `feature/admin-desktop` | Interface et serveur Desktop (à développer côté UI Desktop) |
| `feature/android-logic` | Logique métier Android — ViewModels, Repository, DAO |
| `main` | Branche stable (merge après validation) |

---

## Lancer le projet

### Desktop (serveur)
```bash
KeycePass_Launch.bat
```

> ⚠️ Ne pas utiliser `./gradlew :desktopApp:run` : le daemon Gradle tourne en Session 0,
> donc la fenêtre Compose démarre invisible pour l’utilisateur.
> Le launcher utilise `javaw` + `-classpath "lib/*"` + `-Dskiko.renderApi=SOFTWARE_FAST`.

### Android
Ouvrir dans Android Studio et lancer sur un émulateur ou appareil physique.

> ⚠️ Pour les scans depuis un émulateur Android, les coordonnées GPS seront nulles (pas de vrai GPS). Le serveur accepte les scans sans coordonnées (elles seront `null` en base).

---

## Format des QR Codes

| Type | Format |
|------|--------|
| Enrôlement | `keycepass://enrolement?classeId={id}&token={tok}&serverUrl={url}` |
| Présence hebdomadaire | `keycepass://presence?semaineId={id}&classeId={cId}&token={hmac}&serverUrl={url}` |

---

## Saisie des coordonnées GPS (admin)

Pour obtenir la latitude/longitude d'une salle :
1. Ouvrir [Google Maps](https://maps.google.com)
2. Faire un clic droit sur l'emplacement → « Plus d'infos sur cet endroit »
3. Copier les coordonnées affichées en haut (ex. `48.8566, 2.3522`)
4. Les saisir dans le formulaire de création de semaine

**Rayon de tolérance** : 200 mètres (global, non modifiable par l'UI pour l'instant).
