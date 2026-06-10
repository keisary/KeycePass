# KeycePass — Note d'Intégration

> Branche `iruzen` — toutes les fonctionnalités fusionnées et fonctionnelles.
> Dernière mise à jour : 10/06/2026

---

## 1. Architecture générale

```
┌─────────────────────────────────────────────────────────┐
│                  Fenêtre Compose Desktop                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │                AdminLayout                          │ │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐  │ │
│  │  │Dashboard │ │QR Codes  │ │Pairages  │ │Hist. │  │ │
│  │  │(DB+mock) │ │(ZXing)   │ │(DB)      │ │(DB)  │  │ │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────┘  │ │
│  └───────────────────┬────────────────────────────────┘ │
│                      │                                  │
│              ┌───────┴────────┐                         │
│              │AdminViewModel  │                         │
│              │(backend réel)  │                         │
│              └───────┬────────┘                         │
│                      │                                  │
│    ┌─────────────────┼─────────────────────┐            │
│    │                 │                     │            │
│    ▼                 ▼                     ▼            │
│┌──────────┐   ┌────────────┐   ┌────────────────────┐  │
││ Ktor API │   │ Exposed    │   │ MdnsService        │  │
││ :8080    │   │ (SQLite)   │   │ keycepass.local    │  │
││ 8 pts    │   │ 5 tables   │   │ (JmDNS)            │  │
│└──────────┘   └────────────┘   └────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## 2. Modules

### `desktopApp/` — Application Desktop (Compose Desktop + Ktor + mDNS)

| Package | Rôle |
|---|---|
| `ui/` | Composants Compose Desktop (AdminLayout, 4 écrans, thème monochrome) |
| `ui/viewmodel/AdminViewModel.kt` | ViewModel fusionné (méthodes backend réelles + fallback mock) |
| `ui/screens/DashboardScreen.kt` | KPIs, filtre promo/classe/semestre, recherche directe, table paginée, export CSV |
| `ui/screens/QRManagementScreen.kt` | QR ZXing, import Excel, création semaines GPS, sauvegarde PNG |
| `ui/screens/GestionEnrolementScreen.kt` | Liste appareils enrôlés (deviceUuid non nul) + dissociation |
| `ui/screens/HistoriqueScreen.kt` | Stats DB, sélecteur période (semaine/mois/tout), export CSV |
| `data/database/` | Exposed ORM : DatabaseManager, 5 tables, ImportService |
| `data/server/KtorServer.kt` | Serveur REST Ktor (8 endpoints) |
| `data/server/MdnsService.kt` | Annonce mDNS `keycepass.local` (JmDNS) |
| `data/service/SeanceSemaineService.kt` | Logique semaines + tokens HMAC-SHA256 |
| `data/utils/` | GeoUtils (Haversine), QrCodeGenerator (ZXing) |
| `Main.kt` | Point d'entrée : init DB → start Ktor → mDNS → fenêtre |
| `Screen.kt` | Énumération : DASHBOARD, QR_MANAGEMENT, ENROLEMENT, HISTORIQUE |
| `resources/icons/keycepass_logo.svg` | Logo nœud métallique (utilisateur) |

### `shared/` — Module partagé (KMP)

| Package | Rôle |
|---|---|
| `domain/model/` | Etudiant, Seance, Emargement, StatutFinal, StatutSeance |
| `domain/utils/StatutUtils.kt` | Calcul statut (PRESENT/RETARD/ABSENT) selon horaires |
| `network/` | DTOs : ScanPayload, ScanResponse, SessionStatusDto, SeanceCouranteDto |

## 3. Schéma Base de Données (Exposed / SQLite)

```
┌──────────────┐     ┌──────────────────┐
│   Etudiant   │     │  SeanceSemaine   │
│──────────────│     │──────────────────│
│ id_etudiant  │◄───►│ classeId         │
│ matricule    │     │ semaineIso       │
│ nom          │     │ latReference     │
│ prenom       │     │ lonReference     │
│ classeId     │     │ rayonMetres      │
│ email        │     │ tokenSemaine     │
│ motDePasse   │     └────────┬─────────┘
│ deviceUuid   │              │
└──────────────┘              │
         │                    │
         │              ┌─────▼──────────┐
         │              │    Seance      │
         ├──────────────►────────────────┤
         │              │ id_seance      │
         │              │ nomMatiere     │
         │              │ dateJour       │
         │              │ heureDebut     │
         │              │ heureFin       │
         │              │ statutSeance   │
         │              │ semaineId ─────┤
         │              └────────┬───────┘
         │                       │
    ┌────▼──────────┐           │
    │  Emargement   │           │
    │───────────────│           │
    │ id_emargement │           │
    │ etudiantId ───┤           │
    │ seanceId ─────┼───────────┤
    │ horodatageScanDebut │     │
    │ horodatageScanFin   │     │
    │ statutFinal   │           │
    │ latScan       │           │
    │ lonScan       │           │
    └───────────────┘           │
                         ┌──────▼──────┐
                         │ Token       │
                         │─────────────│
                         │ id_token    │
                         │ valeur      │
                         │ dateExpir.  │
                         └─────────────┘
```

**5 tables** : Etudiant, Seance, SeanceSemaine, Emargement, Token

## 4. Endpoints Ktor (serveur :8080)

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/enrolement` | Enregistre l'UUID appareil d'un étudiant (pairage) |
| POST | `/api/scan` | Soumet un scan (présence) avec coordonnées GPS |
| GET | `/api/semaine/{id}/seance-courante` | Récupère la séance active pour une semaine |
| GET | `/api/statistiques/seance/{id}` | Stats d'une séance (P/R/A) |
| GET | `/api/statistiques/etudiant/{matricule}` | Stats par étudiant |
| POST | `/api/cloture` | Clôture une séance (admin) |
| POST | `/api/sync` | Synchronisation différée (mobile → serveur) |
| POST | `/api/etudiants` | Liste des étudiants (filtrable par classe) |

## 5. mDNS — Découverte réseau

Le serveur Ktor annonce automatiquement sa présence via **JmDNS** :

```
[mDNS] keycepass.local annoncé sur le réseau (port 8080)
```

- Le nom **`keycepass.local`** est résolvable par tout appareil sur le réseau local
- Les QR codes intègrent `http://keycepass.local:8080` au lieu d'une IP fixe
- Plus besoin de regénérer les QR si l'IP change
- L'arrêt de l'application désenregistre le service proprement

Prérequis pour le mobile :
- Android résout nativement `.local` via mDNS
- iOS nécessite Bonjour (actif par défaut)

## 6. Workflow d'utilisation

### Import des étudiants (admin)
```
Fichier Excel (.xlsx)
    │
    ▼
ImportService.importerDepuisExcel()
    │
    ▼
Table Etudiant (SQLite)
    │
    ▼
Charger les classes disponibles dans les filtres
```

### Création d'une semaine (admin)
```
Admin → Créer semaine → Saisit coordonnées GPS (via Google Maps)
    │
    ▼
SeanceSemaineService.creerSemaine()
    │
    ▼
Génération token HMAC
    │
    ▼
QR code (ZXing) affiché à l'écran (contient keycepass.local:8080)
    │
    ▼
Étudiants scannent → Appareil enrôlé → Vérification géo (Haversine 200m)
```

### Scan étudiant (mobile)
```
QR scanné → requête GET /api/semaine/{id}/seance-courante
    │
    ▼
Séance trouvée → POST /api/scan (avec lat/lon GPS + UUID appareil)
    │
    ▼
Vérification HMAC + Haversine + UUID appareil connu
    │
    ▼
Statut : PRESENT / RETARD / ABSENT / EN_ATTENTE
```

### Dashboard (admin)
```
Ouverture → charge les stats depuis la DB (ou mock si vide)
    │
    ▼
Filtres par promo / classe / statut
    │
    ▼
Recherche en direct (nom / prénom / matricule)
    │
    ▼
Export CSV avec JFileChooser
```

### Pairage appareil
```
Admin génère QR → l'étudiant scanne avec l'app mobile
    │
    ▼
POST /api/enrolement { matricule, nom, prenom, deviceUuid, deviceName }
    │
    ▼
UPDATE Etudiant SET deviceUuid = ?
    │
    ▼
Visible dans l'écran Pairages (avec possibilité de dissociation)
```

## 7. Anti-fraude

- **Token HMAC** : chaque semaine a un token signé HMAC-SHA256 opaque (64 hex)
- **Géolocalisation** : Haversine vérifie que le scan est dans le rayon (200m défaut)
- **UUID appareil** : chaque étudiant enrôle son téléphone (1 appareil par étudiant)
- **Double scan** : un étudiant ne peut scanner qu'une fois par séance

## 8. Statut du projet (v2.1.0)

| Module | Statut |
|---|---|
| M1 — Dashboard (stats DB + recherche + export CSV) | ✅ |
| M2 — QR Codes ZXing + HMAC | ✅ |
| M3 — Enrolement appareils (DB + UI) | ✅ |
| M4 — Historique + Statistiques + période | ✅ |
| M5 — Build distribution + lanceur .bat | ✅ |
| M6 — Serveur Ktor + API REST | ✅ |
| M7 — Sync différé mobile (POST /api/sync) | ✅ |
| M8 — mDNS (keycepass.local) | ✅ |
| Logo — SVG utilisateur intégré | ✅ |

Tout est fonctionnel et compilé (`installDist` BUILD SUCCESSFUL).

## 9. Déploiement (local)

```bash
# Pull la dernière version
git pull origin iruzen

# Build distribution
./gradlew :desktopApp:installDist

# Lancer
KeycePass_Launch.bat    # (sur le bureau)
```

Le lanceur utilise `javaw` avec :
- `-classpath "lib\*"` (wildcard Java — contourne limite 8191 chars de cmd.exe)
- `-Dskiko.renderApi=SOFTWARE_FAST` (contourne le bug GPU AMD/Radeon)

> ⚠️ Ne PAS utiliser `./gradlew run` : le daemon Gradle tourne en Session 0 (services),
> ce qui rend les fenêtres invisibles pour l'utilisateur.

### Vérifier le bon fonctionnement

1. Lancer `KeycePass_Launch.bat`
2. Vérifier dans le terminal :
   ```
   [KeycePass] Base de données initialisée
   [KeycePass] Serveur démarré sur http://192.168.x.x:8080
   [mDNS] keycepass.local annoncé sur le réseau (port 8080)
   ```
3. Depuis un navigateur (même PC ou mobile) :
   ```
   http://keycepass.local:8080/api/etudiants
   ```
4. Si ça répond → tout fonctionne

---

*Documentation générée le 10/06/2026 — Branche `iruzen`*
