# Changelog KeycePass

## [1.3.0] — 2026-06-10 (Branche iruzen)

### Animations & Live
- **KPIs animés** : compteurs avec animateIntAsState (chiffres qui montent/descendent en smooth)
- **Barre de stats animée** : proportions présences/retards/absences avec transition fluide
- **Transitions entre écrans** : AnimatedContent avec slide horizontal + fade
- **Toast notifications live** : notification qui slide depuis le haut quand un étudiant émarge
- **Simulation temps réel** : arrivées aléatoires d'étudiants toutes les 5-15s avec mise à jour des KPIs
- **Badge LIVE** : indicateur vert quand la simulation est active

### Nouvel écran : Historique & Statistiques
- Graphiques en barres cumulées (présence/retard/absence) par séance
- Taux moyens (présence, retard, absence) sur les 6 dernières séances
- Barres animées avec animateFloatAsState
- Légende colorée

### Interface
- **Commande Palette** : Ctrl+K ou clique sur le bouton de recherche
  - Recherche d'écran par nom
  - Navigation clavier (flèches + Enter, Escape pour fermer)
  - Raccourcis affichés (Ctrl+1, Ctrl+2, Ctrl+3, Ctrl+4)
- **Hover effect** : les lignes du tableau s'illuminent au survol
- **Nouveau Screen** : `HISTORIQUE` ajouté dans le enum et la sidebar

### Technique
- Ajout : `HistoriqueScreen.kt` (8066 lignes de graphiques)
- Refacto `AdminLayout.kt` : rendu direct des écrans dans AnimatedContent (plus de content slot)
- `AdminViewModel.kt` : nouveau `liveEvents: SharedFlow<String>` pour les toasts + `simulateArrivee()`

## [1.2.0] — 2026-06-10 (Branche iruzen)

### Features finales
- **Export PNG du QR Code** : boite de dialogue "Enregistrer sous" via `FileDialog`, sauvegarde en PNG natif
- **Statut serveur dynamique** : indicateur vert/rouge dans la sidebar avec verification periodique
- **Filtres operationnels** : les filtres classe/semestre dans le Dashboard sont maintenant fonctionnels
- **QRCodeGenerator refactore** : nouvelle methode `generateQR()` retourne un `QRCodeResult()` avec `composeBitmap` (affichage) + `bufferedImage` (export)

### Architecture
- Nouveau dossier : `ui/util/` avec `QRCodeGenerator.kt`
- Separation affichage (Compose ImageBitmap) / export (BufferedImage)

## [1.1.0] — 2026-06-10 (Branche iruzen)

### Design — "Nature Morte"
- **Nouveau thème** inspiré des natures mortes classiques : brun profond, olive, crème, gris chaud
- Palette volontairement limitée pour un rendu sophistiqué et professionnel
- Suppression de tous les gradients multicolores

### Interface
- **Suppression** de l'avatar utilisateur "AD" dans la top bar (profil inutile)
- **Suppression** de la barre de recherche dans la top bar
- **Tous les emojis remplacés** par des icônes Material Design vectorielles (Dashboard, QrCodeScanner, CheckCircle, Schedule, Cancel, People, Refresh, Lock, PhoneAndroid, Link, etc.)
- KPIs redesignés : cartes sobres avec icône colorée, fin des fonds en gradient
- Boutons avec icône + texte cohérents

### QR Code
- **Vrai génération de QR Code** via ZXing (com.google.zxing:core 3.5.3)
- Fin du faux QR en caractères unicode (⬛⬛⬛⬛⬛)
- Affichage du QR bitmap directement dans l'interface
- Nouveau fichier : `util/QRCodeGenerator.kt`

### Dépendances
- Ajout : `com.google.zxing:core:3.5.3`

### Nettoyage
- Remplacement des prints avec émojis dans ServerManager (`[KeycePass]` logging)

## [1.0.0] — 2026-06-09 (Branche iruzen)

### ✨ Nouvelles fonctionnalités (Desktop)

- **Dashboard admin** : KPIs temps réel (Présents/Retards/Absents) avec affichage carte
- **Tableau de présence** : liste des 12 étudiants avec statut, heures de scan, code couleur
- **Filtres** : sélection par classe et semestre via boutons
- **Clôture de séance** : bouton de validation officielle avec changement de statut
- **Serveur Ktor embarqué** : API REST (`/api/health`, `/api/seance/info`, `/api/attendance/status`)
- **WebSocket live** : canal `/ws/live` pour communications temps réel
- **Gestion QR Codes** : génération par classe + semestre, aperçu visuel, export
- **Gestion des pairages** : liste des appareils couplés, statut actif/inactif, dissociation
- **Navigation sidebar** : NavigationRail avec icônes entre dashboard, QR et pairages
- **Thème Material 3** : palette KeycePass, mode sombre/clair, top bar administrateur

### 📦 Dépendances ajoutées

- Compose Multiplatform 1.8.0 (Desktop)
- Ktor Server 3.0.3 (Netty, Content Negotiation, WebSocket, Status Pages)
- H2 Database 2.3.232
- Exposed ORM 0.57.0 (Core, DAO, JDBC)
- Coroutines 1.10.1 (Core, Swing)

### 🔧 Configuration & Build

- Version catalog Gradle (`libs.versions.toml`) enrichi
- Plugin Compose Multiplatform déclaré globalement
- Plugin Kotlin Compose déclaré globalement
- Dépôt JetBrains Compose ajouté au settings.gradle.kts
- JVM Target 17 via compilerOptions DSL
- Paramètres des dépôts centralisés dans le settings

### 🧹 Nettoyage

- **Suppression** : `Fibonacci.kt` (fichier junk sans rapport avec le projet)
- **Suppression** : `KtorServer.kt` (remplacé par `ServerManager.kt`)
- **Suppression** : `DesktopNavigation.kt` (remplacé par `AdminLayout.kt`)
- **Suppression** : 6 fichiers placeholders vides (ComponentsPlaceholder, DbPlaceholder, etc.)
- **Suppression** : dossiers vides orphelins

### 📚 Documentation

- README.MD réécrit : architecture, fonctionnalités, captures, équipe, instructions
- `docs/cahier-des-charges-v3.md` : spec fusionnée V3 complète (1172 lignes)
- `docs/CONTRIBUTING.md` : guide de contribution (workflow git, conventions, modules)
- `.gitignore` enrichi : Gradle, IDE, OS, logs, env, Maven secrets

### 🏗️ Structure finale du desktop

```
desktopApp/
├── Main.kt                    # Entry point Compose Desktop
├── ServerManager.kt           # Serveur Ktor embarqué
└── ui/
    ├── AdminLayout.kt         # Sidebar + top bar
    ├── screens/
    │   ├── DashboardScreen.kt # KPIs + tableau
    │   ├── QRManagementScreen.kt
    │   └── GestionEnrolementScreen.kt
    ├── viewmodel/
    │   └── AdminViewModel.kt
    └── theme/
        └── Theme.kt
```
