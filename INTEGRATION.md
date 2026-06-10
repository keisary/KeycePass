# KeycePass — Note d'Intégration

> Fusion des branches `iruzen` (UI Compose Desktop) et `admin-desktop` (backend Ktor/SQLite)
> Branche de travail : `feature/integration`

---

## 1. Architecture générale

```
┌─────────────────────────────────────────────────────┐
│                   Fenêtre Compose                    │
│  ┌────────────────────────────────────────────────┐ │
│  │               AdminLayout                        │ │
│  │  ┌──────┐ ┌───────────┐ ┌──────────┐ ┌──────┐ │ │
│  │  │Dash. │ │ QR Codes  │ │Pairages  │ │Hist. │ │ │
│  │  └──────┘ └───────────┘ └──────────┘ └──────┘ │ │
│  │  │ mock)  │  (ZXing)   │(enrolemt) │(stats) │ │
│  └────────────────────────────────────────────────┘ │
│                         │                            │
│              ┌──────────┴──────────┐                │
│              │  AdminViewModel     │                │
│              │ (mock + backend)    │                │
│              └──────────┬──────────┘                │
└─────────────────────────┼──────────────────────────┘
                          │
    ┌─────────────────────┼─────────────────────┐
    │                     │                       │
    ▼                     ▼                       ▼
┌─────────────┐  ┌────────────────┐  ┌──────────────────┐
│ Ktor Server │  │  Exposed ORM   │  │  Fichiers        │
│ :8080       │  │  (SQLite)      │  │  (Excel, images) │
│ 7 endpoints │  │  5 tables      │  │                  │
└─────────────┘  └────────────────┘  └──────────────────┘
```

## 2. Modules

### `desktopApp/` — Application Desktop (Compose Desktop + Ktor)

| Package | Rôle |
|---|---|
| `ui/` | Composants Compose Desktop (AdminLayout, écrans, thème) |
| `ui/viewmodel/AdminViewModel.kt` | ViewModel fusionné (données mockées + méthodes backend) |
| `ui/screens/` | 4 écrans : Dashboard, QRManagement, Enrolement, Historique |
| `data/database/` | Exposed ORM : DatabaseManager, DatabaseTables (5 tables), ImportService |
| `data/server/KtorServer.kt` | Serveur REST Ktor (7 endpoints) |
| `data/service/SeanceSemaineService.kt` | Logique semaines d'enseignement + tokens HMAC |
| `data/utils/` | GeoUtils (Haversine), QrCodeGenerator (ZXing) |
| `Main.kt` | Point d'entrée : init DB → start Ktor → fenêtre |
| `Screen.kt` | Énumération des 4 écrans de navigation |

### `shared/` — Module partagé (KMP)

| Package | Rôle |
|---|---|
| `domain/model/` | Etudiant, Seance, Emargement, StatutFinal, StatutSeance |
| `network/` | DTOs sérialisables : ScanPayload, ScanResponse, SessionStatusDto, SeanceCouranteDto |

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
    │ scanDebut     │           │
    │ scanFin       │           │
    │ statutFinal   │           │
    │ latScan       │           │
    │ lonScan       │           │
    └───────────────┘           │
                         ┌──────▼──────┐
                         │ Enseignant  │
                         │─────────────│
                         │ id_ensign.  │
                         │ nom         │
                         │ prenom      │
                         │ email       │
                         └─────────────┘
```

## 4. Endpoints Ktor (serveur :8080)

| Méthode | Route | Description |
|---|---|---|
| POST | `/api/enrolement` | Enregistre l'UUID appareil d'un étudiant |
| POST | `/api/scan` | Soumet un scan (présence) avec coordonnées GPS |
| GET | `/api/semaine/{id}/seance-courante` | Récupère la séance active pour une semaine |
| GET | `/api/statistiques/seance/{id}` | Stats d'une séance (P/R/A) |
| GET | `/api/statistiques/etudiant/{matricule}` | Stats par étudiant |
| POST | `/api/cloture` | Clôture une séance (admin) |
| POST | `/api/sync` | Synchronisation différée (mobile → serveur) |

## 5. Workflow d'utilisation

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
Charger les classes disponibles
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
QR code (ZXing) affiché à l'écran
    │
    ▼
Étudiants scannent → Appareil enrôlé → Vérification géo (Haversine 200m)
```

### Scan étudiant (mobile, futur)
```
QR scanné → Requête GET /api/semaine/{id}/seance-courante
    │
    ▼
Seance trouvée → POST /api/scan (avec lat/lon GPS)
    │
    ▼
Vérification HMAC token + Haversine 200m
    │
    ▼
Statut calculé : PRESENT / RETARD / ABSENT / EN_ATTENTE
```

## 6. Anti-fraude

- **Token HMAC** : chaque semaine a un token signé HMAC-SHA256 opaque (64 hex)
- **Géolocalisation** : Haversine vérifie que le scan est dans le rayon (200m défaut)
- **UUID appareil** : chaque étudiant enrôle son téléphone (1 appareil par étudiant)
- **Double scan** : un étudiant ne peut scanner qu'une fois par séance

## 7. Statut du projet (v2.0.0)

- [x] **M1** — Dashboard avec liste des présences (mock + vrai)
- [x] **M2** — QR Codes (ZXing génération + serveur Ktor + vérification HMAC)  ← BACKEND OK
- [ ] **M3** — Enrolement des appareils (UI en cours)
- [ ] **M4** — Historique + Statistiques (backlog)
- [x] **M5** — Build distribution + lanceur desktop ✅
- [ ] **M6** — Synchronisation réseau (backlog)

Légende : ✅ fait sur `feature/integration` | 🟡 UI faite (mock) / 💡 backend prêt

## 8. Déploiement (local)

```bash
# Build distribution
./gradlew :desktopApp:installDist

# Lancer (via le bureau)
Double-clic sur KeycePass_Launch.bat

# OU via le projet
./gradlew :desktopApp:run   # attention : fenêtre invisible (Session 0)
```

Le lanceur `.bat` sur le bureau utilise `javaw` avec `-classpath "lib\*"` et
`-Dskiko.renderApi=SOFTWARE_FAST` (contourne le bug GPU AMD/Radeon).

---

*Documentation générée le 10/06/2026 — Branche `feature/integration`*
