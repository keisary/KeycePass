# KeycePass — Contrôle de Présence

> **Application de gestion des fiches de présence** — Projet Tutoré B2COM0602  
> Bachelor 2 IT — Collège de Paris Supérieur / Keyce Informatique  
> Juin 2026

---

## Presentation

KeycePass est un système connecté **mobile + desktop** qui révolutionne le contrôle de présence des étudiants :

- **Application Android** : scan de QR Code → émargement automatique
- **Application Desktop** : administration, dashboard temps réel, QR Codes
- **WinFi** : communication réseau local (zéro dépendance Internet)
- **Double scan** : entrée + sortie avec calcul automatique du statut
- **Clôture Enseignant** : validation officielle anti-fraude

## Architecture

```
KeycePass/
├── shared/           # Logique métier partagée (modèles, algo, DTOs)
├── androidApp/       # Application mobile Android
└── desktopApp/       # Application desktop (administration)
```

## Application Desktop (branche iruzen)

L'application desktop est développée avec **Compose Multiplatform** et embarque un **serveur Ktor** intégré.

### Design Philosophy — "Monochrome"

L'interface s'inspire des sites modernes ultra-sophistiqués (Linear, Stripe, Vercel) :
- Palette **noir & blanc pur** : fond `#000000`, surfaces `#0A0A0A`, texte `#F5F5F5`
- Zéro couleur inutile — seuls les statuts ont une micro-couleur subtile
- Zéro gradient, zéro emoji, zéro effet flashy
- Typographie sobre, espacement généreux, poids de police pour la hiérarchie visuelle
- Icônes vectorielles Material Design (zéro emoji)
- Dark mode par défaut (comme les apps pro)
- Professionnel, austère, premium

### Animations & Interations
- **Compteurs animés** : les KPIs augmentent/diminuent en smooth (animateIntAsState)
- **Transitions** : slide horizontal + fade entre chaque écran
- **Notifications live** : toasts qui glissent depuis le haut quand un étudiant émarge
- **Survol** : les lignes du tableau s'illuminent au passage de la souris

### Raccourcis
| Touche | Action |
|--------|--------|
| Ctrl+K / Cmd+K | Commande Palette (recherche d'écran, navigation) |
| Escape | Fermer la palette |
| Ctrl+1..4 | Navigation directe vers un écran |

### Fonctionnalités

| Ecran | Description | Statut |
|-------|-------------|--------|
| **Dashboard** | KPIs temps reel (Presence/Retards/Absents) + tableau des etudiants + filtres | Livre |
| **QR Codes** | Generation de vrai QR Code (ZXing) par classe et semestre + **export PNG** | Livre |
| **Pairages** | Gestion des appareils couples, dissociation, recouplage | Livre |
| **Historique** | Graphiques barres cumulees, taux presence/retard/absence, evolution sur 6 seances | Livre |
| **Serveur Ktor** | API REST embarquee (port 8080) + WebSocket live + statut dynamique | Livre |

### Stack technique

| Technologie | Version |
|-------------|---------|
| Kotlin | 2.4.0 |
| Compose Multiplatform | 1.8.0 |
| Ktor (Server + WebSocket) | 3.0.3 |
| H2 Database | 2.3.232 |
| Exposed ORM | 0.57.0 |
| ZXing (QR Code) | 3.5.3 |
| Coroutines | 1.10.1 |

### Palette — Monochrome

```css
Fond              #000000   — Pure black (dark mode)
Surface           #0A0A0A   — Almost black
Surface 2         #141414   — Subtle elevation
Texte principal   #F5F5F5   — Off-white
Texte secondaire  #737373   — Gris silencieux
Bordure           #1A1A1A   — Ligne ultra-subtile

--- Status (micro-couleurs uniquement) ---
Present           #22C55E   — Vert (seul vrai point couleur)
Retard            #A3A3A3   — Gris (presque invisible)
Absent            #6B3A3A   — Rouge foncé discret
```

## Installation & Execution

### Prérequis
- JDK 17+
- Gradle 8.10+ (wrapper inclus)

### Lancer l'application desktop
```bash
./gradlew :desktopApp:run
```
Le serveur Ktor démarre automatiquement sur le port 8080.

### API REST disponibles
| Methode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/seance/info` | Infos séance active |
| GET | `/api/attendance/status` | Statistiques présences |
| WS | `/ws/live` | WebSocket temps réel |

## Equipe

| Role | Membre | Module |
|------|--------|--------|
| **Tech Lead / Architecte** | Membre 1 | `shared/` |
| **Mobile UI/UX** | Membre 2 | `androidApp/ui/` |
| **Hardware & Réseau** | Membre 3 | `androidApp/hardware/` |
| **Persistance & Sécurité** | Membre 4 | `androidApp/data/` |
| **Desktop UI/UX** | **THE GUY** | `desktopApp/ui/` |
| **Serveur & DB** | Membre 6 | `desktopApp/data/` |

## Documents

- [`CAHIER_DES_CHARGES_V3.md`](./caHIER_DES_CHARGES_V3.md) — Cahier des charges fusionné V3

## Branches

| Branche | Description |
|---------|-------------|
| `main` | Version initiale (squelette par keisary) |
| `iruzen` | **Application desktop complète** + redesign Nature Morte |

---

*KeycePass — Contrôle de gestion de la fiche de présence des étudiants*
