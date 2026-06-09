# 🎓 KeycePass — Contrôle de Présence

> **Application de gestion des fiches de présence** — Projet Tutoré B2COM0602  
> Bachelor 2 IT — Collège de Paris Supérieur / Keyce Informatique  
> Juin 2026

---

## 📋 Présentation

KeycePass est un système connecté **mobile + desktop** qui révolutionne le contrôle de présence des étudiants :

- 📱 **Application Android** : scan de QR Code → émargement automatique
- 💻 **Application Desktop** : administration, dashboard temps réel, QR Codes
- 📡 **WinFi** : communication réseau local (zéro dépendance Internet)
- 🔐 **Double scan** : entrée + sortie avec calcul automatique du statut
- 👨‍🏫 **Clôture Enseignant** : validation officielle anti-fraude

## 🏗️ Architecture

```
KeycePass/
├── shared/           # Logique métier partagée (modèles, algo, DTOs)
├── androidApp/       # Application mobile Android
└── desktopApp/       # Application desktop (administration)
```

## 🖥️ Application Desktop (branche iruzen)

L'application desktop est développée avec **Compose Multiplatform** et embarque un **serveur Ktor** intégré.

### Fonctionnalités

| Écran | Description | Statut |
|-------|-------------|--------|
| **📊 Dashboard** | KPIs temps réel (Présents/Retards/Absents) + tableau des étudiants + filtres | ✅ Livré |
| **📱 QR Codes** | Génération de QR Code par classe et semestre + export PNG | ✅ Livré |
| **🔐 Pairages** | Gestion des appareils couplés, dissociation, recouplage | ✅ Livré |
| **🌐 Serveur Ktor** | API REST embarquée (port 8080) + WebSocket live | ✅ Livré |

### Stack technique

| Technologie | Version |
|-------------|---------|
| Kotlin | 2.4.0 |
| Compose Multiplatform | 1.8.0 |
| Ktor (Server + WebSocket) | 3.0.3 |
| H2 Database | 2.3.232 |
| Exposed ORM | 0.57.0 |

### Captures d'écran (rendu attendu)

```
┌──────────────────────────────────────────────────────────────┐
│ 🔐 KeycePass    📊 Dashboard   📱 QR Codes   🔐 Pairages    │
├──────────────────────────────────────────────────────────────┤
│ [Statut: 🟢 EN COURS]                                        │
│ ┌────────┐  ┌────────┐  ┌────────┐  ┌────────┐             │
│ │PRÉSENTS│  │RETARDS │  │ABSENTS │  │ TOTAL  │             │
│ │   8    │  │   2    │  │   2    │  │   12   │             │
│ └────────┘  └────────┘  └────────┘  └────────┘             │
│ [B2_IT]  [S2]          [🔄 Rafraîchir]  [🔒 Clôturer]     │
│ ┌───────┬────────────┬────────┬──────────┬──────────┐       │
│ │  📸   │  Nom        │ Statut │ 1er Scan │ 2nd Scan │       │
│ ├───────┼────────────┼────────┼──────────┼──────────┤       │
│ │  🟢   │  Diallo A. │ Présent│ 08:12    │ 10:05    │       │
│ │  🟡   │  Koné F.   │ Retard  │ 08:22    │ 10:03    │       │
│ │  🔴   │  Traoré M. │ Absent  │ ---      │ ---      │       │
│ └───────┴────────────┴────────┴──────────┴──────────┘       │
└──────────────────────────────────────────────────────────────┘
```

## 🔧 Installation & Exécution

### Prérequis
- JDK 17+
- Gradle 8.10+ (wrapper inclus)

### Lancer l'application desktop
```bash
./gradlew :desktopApp:run
```
Le serveur Ktor démarre automatiquement sur le port 8080.

### API REST disponibles
| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/seance/info` | Infos séance active |
| GET | `/api/attendance/status` | Statistiques présences |
| WS | `/ws/live` | WebSocket temps réel |

## 👥 Équipe

| Rôle | Membre | Module |
|------|--------|--------|
| **👑 Tech Lead / Architecte** | Membre 1 | `shared/` |
| **📱 Mobile UI/UX** | Membre 2 | `androidApp/ui/` |
| **📷 Hardware & Réseau** | Membre 3 | `androidApp/hardware/` |
| **💾 Persistance & Sécurité** | Membre 4 | `androidApp/data/` |
| **🖥️ Desktop UI/UX** | **THE GUY** 🎯 | `desktopApp/ui/` |
| **🌐 Serveur & DB** | Membre 6 | `desktopApp/data/` |

## 📄 Documents

- [`CAHIER_DES_CHARGES_V3.md`](./caHIER_DES_CHARGES_V3.md) — Cahier des charges fusionné V3
- [`CAHIER_DES_CHARGES_V1.pdf`](./CAHIER%20DES%20CHARGES%20V1.pdf) — Version initiale

## 🌿 Branches

| Branche | Description |
|---------|-------------|
| `main` | Version initiale (squelette par keisary) |
| `iruzen` | **Application desktop complète** + nettoyage |

---

*KeycePass — Contrôle de gestion de la fiche de présence des étudiants*
