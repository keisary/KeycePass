# Changelog KeycePass

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
