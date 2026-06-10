# Planning Équipe — Semaine 1 (10-11 Juin)

**Deadline : Jeudi 11/06 — chaque membre doit commit sur sa branche.**

---

## 🌿 Branches créées

| Membre | Branche | À faire |
|--------|---------|---------|
| **M2** | `feature/membre-2-mobile` | UI Mobile (Jetpack Compose) |
| **M3** | `feature/membre-3-camera` | CameraX + QR + Sécurité |
| **M4** | `feature/membre-4-data` | Room Database + DAOs |
| **M6** | `feature/membre-6-server` | H2 + Exposed + API |
| **M1** | *(à créer)* | Shared module (si pas fait) |

---

## 📋 Tâches par membre

### M2 — Mobile UI/UX
**Branche :** `feature/membre-2-mobile`
- [ ] Setup Compose Navigation (LoginScreen → ScanScreen → EnseignantScreen)
- [ ] LoginScreen UI (email + password + bouton connexion)
- [ ] ScanScreen UI (hors caméra — afficher placeholder "Caméra")
- [ ] EnseignantScreen UI (delegue/enseignant, avec bouton clôture)
- [ ] Thème Material 3 (couleurs, typo) — copier depuis `shared/` si dispo
- [ ] Écrans responsifs, état chargement/erreur

### M3 — Camera & QR + Sécurité
**Branche :** `feature/membre-3-camera`
- [ ] Setup CameraX (PreviewView + analyseur image)
- [ ] Intégration ML Kit Barcode Scanning (détection QR)
- [ ] EncryptedSharedPreferences (stockage token session)
- [ ] DeviceCouplage (afficher QR de couplage avec code session)
- [ ] Gestion permissions caméra (Manifest + Runtime)

### M4 — Persistance Mobile
**Branche :** `feature/membre-4-data`
- [ ] Setup Room Database (entities, database class)
- [ ] Créer entities : EtudiantEntity, ScanEntity, SeanceEntity, CouplageEntity
- [ ] Créer DAOs (CRUD pour chaque entité)
- [ ] Room TypeConverters (Enum, Date, etc.)
- [ ] Repository pattern (couche data)
- [ ] Sync offline (file d'attente scans non envoyés)

### M6 — Serveur & Base de données
**Branche :** `feature/membre-6-server`
- [ ] Schema H2 + Exposed (tables : etudiants, seances, scans, couplages)
- [ ] Endpoint POST /api/attendance/scan (réception scan mobile)
- [ ] Endpoint GET /api/attendance/status (statut temps réel)
- [ ] Endpoint POST /api/seance/start (démarrer séance depuis desktop)
- [ ] Endpoint POST /api/seance/end (clôturer séance)
- [ ] WebSocket live push (notifier dashboard desktop)
- [ ] Exposed DAOs + Repository
- *Note : le serveur Ktor embarqué est déjà setup dans `desktopApp/` par M5*

### M1 — Shared Module
- [ ] Finaliser les data classes partagées en `shared/`
- [ ] Vérifier le bon fonctionnement des `StatutUtils` et tests
- [ ] Fournir les DTOs réseau aux autres membres

---

## 🔗 Dépendances entre membres

```
M1 (modèles) ──→ M2 (UI)
              ──→ M3 (QR)
              ──→ M4 (Room)
              ──→ M6 (API)
              
M3 (QR scan) ──→ M4 (stockage scan)
M6 (API)     ──→ M5 (dashboard desktop) ← NOUS
```

## ⏰ Timeline

| Quand | Action |
|-------|--------|
| **Mercredi 10/06** | Chacun sur sa branche, commit avant 18h |
| **Jeudi 11/06 avant midi** | Merge PR → `main` |
| **Jeudi 11/06 après-midi** | Tests cross-modules + recette finale |
| **Deadline** | **Jeudi 11/06 23h59** |

---

**Lien GitHub :** https://github.com/keisary/KeycePass
