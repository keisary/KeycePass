# 📋 Cahier des Charges V3
## KeycePass — Contrôle de Gestion des Fiches de Présence des Étudiants

> **Équipe Projet** — 6 membres  
> **Version** : 3.0 — Juin 2026  
> **Code UE** : B2COM0602 — Projet Tutoré N°2 & Ingénierie des Exigences  
> **Niveau** : Bachelor 2 (B2) IT — Collège de Paris Supérieur / Keyce Informatique  
> **Nom de l'Application** : KeycePass  
> **Stack** : Kotlin Multiplatform / Compose Desktop / Ktor / Jetpack Compose / Material Design 3  

---

## Table des Matières
1. [Présentation & Contexte](#1-présentation--contexte)
2. [Objectifs du Projet](#2-objectifs-du-projet)
3. [Périmètre Fonctionnel](#3-périmètre-fonctionnel)
4. [Acteurs du Système](#4-acteurs-du-système)
5. [Exigences Fonctionnelles (EF)](#5-exigences-fonctionnelles-ef)
6. [Exigences Non Fonctionnelles (ENF)](#6-exigences-non-fonctionnelles-enf)
7. [User Stories](#7-user-stories)
8. [Cycle de Vie d'une Séance](#8-cycle-de-vie-dune-séance)
9. [Architecture Globale](#9-architecture-globale)
10. [Spécifications Fonctionnelles Détaillées](#10-spécifications-fonctionnelles-détaillées)
11. [API REST & WebSocket](#11-api-rest--websocket)
12. [Modèle de Données](#12-modèle-de-données)
13. [Contraintes Techniques](#13-contraintes-techniques)
14. [Stack Technologique](#14-stack-technologique)
15. [Sécurité & Anti-Fraude](#15-sécurité--anti-fraude)
16. [Planning & Répartition](#16-planning--répartition)
17. [Risques & Mitigations](#17-risques--mitigations)
18. [Annexes : Guides Techniques par Rôle](#18-annexes--guides-techniques-par-rôle)

---

## 1. Présentation & Contexte

### 1.1 Constat Actuel et Problématique
Le système actuel de suivi des présences au sein de l'université repose exclusivement sur un support physique traditionnel (fiche d'émargement papier nominative). Ce mode de fonctionnement manuel engendre des failles et des vulnérabilités critiques :

- **Fraude par complaisance (Tricherie)** : L'absence de mécanisme d'authentification permet à des étudiants présents de signer en lieu et place d'élèves absents, ou de s'en aller immédiatement après avoir signé.
- **Altération et perte de données** : Risque élevé de détérioration physique, perte de fiches lors des transferts, falsification a posteriori.
- **Inefficacité** : Perte de temps en début/fin de séance, saisie manuelle chronophage.
- **Non centralisées** : Pas de vue d'ensemble en temps réel pour l'administration.
- **Inflexibilité administrative** : Charge de travail importante pour le service du contrôle de gestion (saisie manuelle des absences), délais importants pour le traitement des quotas d'absences requis pour la validation des examens.

### 1.2 La Solution : KeycePass
Un système connecté **mobile + desktop** qui révolutionne le contrôle de présence :

- 📱 **Application Android** : scan de QR Code par les étudiants → émargement automatique
- 💻 **Application Desktop** : administration, tableau de bord temps réel, génération QR
- 📡 **Communication WinFi** : réseau local (zéro dépendance Internet)
- 🔐 **Double scan** : entrée + sortie avec calcul automatique du statut
- 👨‍🏫 **Clôture Enseignant** : validation officielle de fin de séance anti-fraude

### 1.3 Public Cible
- Établissements d'enseignement supérieur (universités, grandes écoles, instituts)
- Classes de 20 à 200 étudiants
- Sessions quotidiennes ou hebdomadaires
- Administration et service de contrôle de gestion

---

## 2. Objectifs du Projet

### 2.1 Objectifs Fonctionnels
| # | Objectif | Priorité |
|---|----------|----------|
| **F1** | Scanner un QR Code unique pour pointer sa présence | 🔴 Critique |
| **F2** | Calcul automatique : Présent / Retard / Absent | 🔴 Critique |
| **F3** | Dashboard admin temps réel avec statuts | 🔴 Critique |
| **F4** | Génération de QR Codes statiques par classe/semestre | 🟡 Important |
| **F5** | Communication Android ↔ Desktop via WinFi | 🟡 Important |
| **F6** | Couplage sécurisé des appareils (UUID + Hardware ID) | 🟡 Important |
| **F7** | Scan enseignant de clôture de séance | 🔴 Critique |
| **F8** | Envoi du rapport consolidé par le Délégué | 🟡 Important |
| **F9** | Persistance locale (Room) avec sync réseau | 🟢 Souhaitable |
| **F10** | Réinitialisation sécurisée du couplage | 🟢 Souhaitable |
| **F11** | Horodatage automatique des scans | 🔴 Critique |
| **F12** | Export des rapports de présence | 🟢 Souhaitable |

### 2.2 Objectifs Techniques
- **Portabilité** : Kotlin Multiplatform pour partager la logique métier
- **Performance** : Application desktop légère, démarrage < 3s, scan < 1.5s
- **Sécurité** : Chiffrement des données sensibles, anti-falsification, device binding
- **Fiabilité** : Fonctionnement hors-ligne, puis sync quand réseau disponible
- **UX** : Interface Material Design 3, adaptative Android 7.0+ (API 24)
- **Architecture** : Clean Architecture MVVM multi-modules

---

## 3. Périmètre Fonctionnel

### 3.1 Dans le Périmètre ✅
- Application mobile Android (scanner QR, affichage statut)
- Application desktop Windows/Mac (administration, dashboard)
- Module shared (logique métier, modèles, réseau)
- Communication WinFi (réseau local sans Internet)
- Génération de QR Codes statiques par classe/semestre
- Gestion des séances (création, scan enseignant, clôture)
- Tableau de bord en temps réel
- Paire appareil-session sécurisée (Device Binding)
- Rôle délégué : validation et envoi du rapport
- Export des rapports de présence

### 3.2 Hors Périmètre ❌
- Application iOS / Web
- Authentification OAuth / SSO / biométrique
- Cloud / serveur distant (tout est local)
- Notification push
- Reconnaissance faciale
- Intégration API externe (Moodle, ENT, etc.)

---

## 4. Acteurs du Système

Le système KeycePass identifie **quatre acteurs** distincts :

| Acteur | Rôle | Interface | Actions |
|--------|------|-----------|---------|
| **👨‍🎓 Étudiant** | Présence en cours | Mobile Android | Scanner QR entrée/sortie, consulter son statut |
| **👨‍🏫 Enseignant** | Tenue et clôture de séance | Mobile Android | Scanner code de clôture, valider juridiquement la séance |
| **👥 Délégué de classe** | Consolidation et transmission | Mobile Android | Consulter le résumé, envoyer rapport à l'administration |
| **🏢 Administration** | Gestion et contrôle | Desktop (PC) | Dashboard, QR codes, gestion classes/étudiants, export |

---

## 5. Exigences Fonctionnelles (EF)

Chaque spécification fonctionnelle est unique, traçable, mesurable et orientée système.

| ID | Catégorie | Description de l'Exigence Fonctionnelle |
|----|-----------|------------------------------------------|
| **EF_01** | Sécurité / Enrôlement | Le système doit capturer l'identifiant unique du terminal (UUID/Hardware ID) lors du premier scan et bloquer toute tentative de modification sans validation physique préalable de l'administration. |
| **EF_02** | Gestion des QR Codes | L'interface administrative doit permettre la génération d'un QR Code statique unique indexé par classe (filière/niveau) et par semestre. |
| **EF_03** | Traitement du Scan | L'application mobile doit décoder le QR code de la classe et comparer l'horodatage système local lors du scan de début ET du scan de fin de cours pour attribuer le statut final de l'étudiant. |
| **EF_04** | Règles des Statuts | Le système doit croiser obligatoirement les données des deux scans selon les règles : Présent [0-15 min] + second scan validé ; Retard [>15 min] + second scan validé ; Absent si scan manquant. |
| **EF_05** | Clôture Enseignant | L'application doit exiger le scan d'un code de validation par l'enseignant en fin de séance pour valider juridiquement la tenue de la séance et déclencher l'autorisation du scan de fin pour les étudiants. |
| **EF_06** | Rapport Délégué | L'interface du délégué doit compiler les données locales de la séance et exporter un rapport de présence structuré vers l'administration. |
| **EF_07** | Dashboard Temps Réel | L'interface administrative desktop doit afficher en temps réel les statistiques de présence (Présents, Retards, Absents) avec filtres par classe, semestre et date. |
| **EF_08** | Couplage Appareil | L'administration doit pouvoir visualiser, dissocier et réinitialiser le couplage des appareils via une interface dédiée. |
| **EF_09** | Gestion des Classes | L'administration doit pouvoir créer, modifier et supprimer des classes, ainsi que gérer la liste des étudiants. |

---

## 6. Exigences Non Fonctionnelles (ENF)

| ID | Critère | Description de l'Exigence Non Fonctionnelle |
|----|---------|----------------------------------------------|
| **ENF_01** | Robustesse Anti-Fraude | Le système doit invalider le scan de présence si le jeton d'authentification ou l'identifiant matériel de l'appareil ne correspond pas au registre unique lié à l'étudiant en base de données (Device Binding). |
| **ENF_02** | Performance / UI | Le décodage du QR Code via CameraX et l'affichage visuel du statut de l'étudiant doivent s'exécuter en moins de 1,5 seconde. |
| **ENF_03** | Sécurité des Données | Les identifiants uniques et jetons de session stockés sur le smartphone doivent être chiffrés localement via EncryptedSharedPreferences d'Android. |
| **ENF_04** | Portabilité / Cible | L'application mobile doit être développée en natif Android avec Jetpack Compose (Material Design 3), compatible à partir d'Android 7.0 (API 24). |
| **ENF_05** | Fiabilité Réseau | Le système doit continuer à fonctionner en mode hors-ligne : les scans sont stockés localement (Room) et synchronisés dès que le réseau WinFi est disponible. |
| **ENF_06** | Performance Desktop | L'application desktop doit démarrer en moins de 3 secondes et le dashboard doit se rafraîchir en temps réel avec une latence < 1 seconde. |

---

## 7. User Stories

### US_01 : Enrôlement Initial du Téléphone (Anti-Fraude)
- **En tant qu'** Étudiant,
- **je veux** que mon application transmette l'identifiant unique de mon téléphone à l'administration lors de mon tout premier scan,
- **afin de** lier de manière exclusive mon appareil à mon identité pour le reste du semestre.
- **Critères d'acceptation :**
  - L'application bloque tout changement d'appareil.
  - L'administration dispose d'un bouton de réinitialisation sécurisé en base de données en cas de force majeure.

### US_02 : Premier Scan (Arrivée en Cours)
- **En tant qu'** Étudiant,
- **je veux** scanner le QR code de ma classe dès mon arrivée au début du cours,
- **afin d'** enregistrer mon heure d'arrivée dans le système.
- **Critères d'acceptation :**
  - Si heure du scan ≤ 15 min après début → pré-enregistre "À l'heure".
  - Si heure du scan > 15 min → pré-enregistre "En retard".

### US_03 : Deuxième Scan (Validation de Sortie et Statut Final)
- **En tant qu'** Étudiant,
- **je veux** scanner le QR code une seconde fois à la fin de la séance,
- **afin de** valider définitivement ma présence au cours.
- **Critères d'acceptation :**
  - 1er scan "À l'heure" + scan de fin validé → **Présent** ✅
  - 1er scan "En retard" + scan de fin validé → **Retard** ⚠️
  - Scan de début OU scan de fin manquant → **Absent** ❌

### US_04 : Clôture de Session Enseignant
- **En tant qu'** Enseignant,
- **je veux** scanner le code de confirmation en fin de cours,
- **afin d'** officialiser la tenue de la séance, de figer la liste et de permettre aux étudiants de faire leur second scan.
- **Critères d'acceptation :**
  - Le scan de l'enseignant clôture la session.
  - Empêche toute modification frauduleuse ultérieure des données.

### US_05 : Envoi du Rapport de Présence par le Délégué
- **En tant qu'** Délégué de classe,
- **je veux** générer et envoyer le rapport consolidé basé sur le double scan à l'administration,
- **afin de** clore administrativement le cours.
- **Critères d'acceptation :**
  - Affiche un résumé clair (Présents, Retardataires, Absents).
  - Bouton d'envoi réseau direct vers le système central de l'administration.

---

## 8. Cycle de Vie d'une Séance

Le déroulement nominal d'une séance de cours suit un cycle strict découpé en **5 phases** :

```
┌─────────────────────────────────────────────────────────────────┐
│                    CYCLE DE VIE D'UNE SÉANCE                     │
│                                                                  │
│  1. INITIALISATION (Début Semestre)                              │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ Admin génère QR Code classe/semestre                   │      │
│  │ Premier scan : UUID extrait → admin valide physiquement│      │
│  │ [Étudiant ⇔ Téléphone] enregistré dans DB centrale     │      │
│  └────────────────────────────────────────────────────────┘      │
│                           │                                       │
│                           ▼                                       │
│  2. PREMIER SCAN (Début Cours)                                   │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ Étudiants scannent le QR dans la salle                 │      │
│  │ [0-15min] → "À l'heure"  [>15min] → "En retard"       │      │
│  │ Données stockées localement (Room)                     │      │
│  └────────────────────────────────────────────────────────┘      │
│                           │                                       │
│                           ▼                                       │
│  3. CONSOLIDATION (Pendant le Cours)                              │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ Données en attente dans Room Database                  │      │
│  │ Étudiant sans premier scan → temporairement "Absent"   │      │
│  │ Aucune donnée transmise au réseau à ce stade            │      │
│  └────────────────────────────────────────────────────────┘      │
│                           │                                       │
│                           ▼                                       │
│  4. DEUXIÈME SCAN & CLÔTURE ENSEIGNANT (Fin Cours)               │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ 👨‍🏫 Enseignant scanne code de clôture → verrouille     │      │
│  │ 📱 Étudiants peuvent alors faire le second scan         │      │
│  │ 🔢 Système calcule le statut final                     │      │
│  └────────────────────────────────────────────────────────┘      │
│                           │                                       │
│                           ▼                                       │
│  5. EXPORTATION & ARCHIVAGE                                       │
│  ┌────────────────────────────────────────────────────────┐      │
│  │ 👥 Délégué valide + envoie rapport consolidé            │      │
│  │ 💻 Desktop intègre automatiquement → Dashboard          │      │
│  │ 📊 Tableau de bord du contrôle de gestion mis à jour    │      │
│  └────────────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. Architecture Globale

### 9.1 Diagramme d'Architecture

```
┌───────────────────────────────────────────────────────────────────────┐
│                       KEYCEPASS — SYSTÈME COMPLET                     │
│                                                                       │
│  ┌──────────────────────────────┐    ┌──────────────────────────────┐ │
│  │    APPLICATION MOBILE         │    │    APPLICATION MOBILE        │ │
│  │    (Étudiant)                 │    │    (Enseignant/Délégué)      │ │
│  │                               │    │                              │ │
│  │ ┌───────────┐ ┌────────────┐ │    │ ┌───────────┐ ┌───────────┐ │ │
│  │ │ CameraX   │ │ Room DB    │ │    │ │ CameraX   │ │ Room DB   │ │ │
│  │ │ QR Scan   │ │ (Local)    │ │    │ │ QR Scan   │ │ (Local)   │ │ │
│  │ └─────┬─────┘ └─────┬──────┘ │    │ └─────┬─────┘ └─────┬─────┘ │ │
│  │ ┌─────┴─────────────┴──────┐ │    │ ┌─────┴─────────────┴─────┐ │ │
│  │ │  EncryptedSharedPrefs    │ │    │ │  ViewModel + StateFlow  │ │ │
│  │ │  (Device Binding)        │ │    │ │  (Gestion état)         │ │ │
│  │ └──────────────────────────┘ │    │ └─────────────────────────┘ │ │
│  └──────────────┬───────────────┘    └──────────────┬──────────────┘ │
│                 │                                    │               │
│                 └────────────┬───────────────────────┘               │
│                              │                                       │
│                     ┌────────▼────────┐                              │
│                     │   WinFi LOCAL    │                              │
│                     │  (HTTP + WS)     │                              │
│                     │  Port 8080      │                              │
│                     └────────┬────────┘                              │
│                              │                                       │
│              ┌───────────────▼──────────────────────────┐            │
│              │     APPLICATION DESKTOP (ADMIN)           │            │
│              │     Compose Multiplatform                  │            │
│              │                                           │            │
│  ┌───────────┼─────────────────────────────────────┐     │            │
│  │           │             │                       │     │            │
│  │  ┌────────▼──────┐ ┌────▼───────────┐  ┌───────▼───┐ │            │
│  │  │ Ktor Server   │ │ Dashboard      │  │ QR Code   │ │            │
│  │  │ (Embedded)    │ │ Admin          │  │ Generator │ │            │
│  │  │ ─ REST API   │ │ ─ KPIs temps   │  │ ─ qrose   │ │            │
│  │  │ ─ WebSocket  │ │   réel         │  │ ─ Export  │ │            │
│  │  └──────┬───────┘ │ ─ Tableaux     │  │   PNG     │ │            │
│  │         │         └────────────────┘  └───────────┘ │            │
│  │  ┌──────▼───────┐                                   │            │
│  │  │ H2 Database   │                                   │            │
│  │  │ (Centrale)    │                                   │            │
│  │  └──────────────┘                                   │            │
│  └─────────────────────────────────────────────────────┘            │
└───────────────────────────────────────────────────────────────────────┘
```

### 9.2 Modules du Projet (Arborescence MVVM Multi-Modules)

```
KeycePass/
├── shared/                                    # MODULE COMMUN : LOGIQUE MÉTIER PARTAGÉE
│   └── src/commonMain/kotlin/com/ak/keycepass/shared/
│       ├── domain/                            # Noyau métier pur, indépendant des plateformes
│       │   ├── model/                         # Data classes
│       │   │   ├── Etudiant.kt
│       │   │   ├── Seance.kt
│       │   │   └── Emargement.kt
│       │   ├── enums/
│       │   │   ├── StatutPresence.kt          # PRESENT, RETARD, ABSENT
│       │   │   └── StatutSeance.kt            # PLANIFIE, EN_COURS, CLOTURE_ENSEIGNANT
│       │   └── utils/
│       │       └── CalculStatut.kt            # ♥️ Algorithme central (règle des 15 min)
│       └── network/                           # DTOs JSON pour échanges Wi-Fi
│           ├── RapportPresenceDto.kt
│           └── CouplageDto.kt
│
├── androidApp/                                # MODULE MOBILE CLIENT (APK)
│   └── src/main/kotlin/com/ak/keycepass/android/
│       ├── data/                              # Couche d'accès aux données
│       │   ├── local/                         # Room Database (DAO, Entities)
│       │   │   ├── AppDatabase.kt
│       │   │   ├── dao/
│       │   │   └── entities/
│       │   └── repository/                    # Implémentations concrètes
│       ├── hardware/                          # Fonctionnalités matérielles
│       │   ├── camera/
│       │   │   └── QrScanner.kt              # CameraX + MLKit
│       │   └── security/
│       │       ├── DeviceCouplage.kt          # UUID + Hardware ID
│       │       └── CryptoManager.kt           # EncryptedSharedPreferences
│       ├── ui/                                # IHM Jetpack Compose
│       │   ├── screens/
│       │   │   ├── LoginScreen.kt
│       │   │   ├── ScanScreen.kt
│       │   │   ├── DelegueScreen.kt
│       │   │   └── EnseignantScreen.kt
│       │   ├── components/                    # Éléments réutilisables
│       │   ├── viewmodel/                     # ViewModels Android réactifs
│       │   └── theme/                         # Material Design 3 (Color.kt, Theme.kt)
│       └── navigation/                        # Routage Jetpack Navigation Core
│
└── desktopApp/                                # MODULE ADMINISTRATION DESKTOP (EXE)
    └── src/main/kotlin/com/ak/keycepass/desktop/
        ├── data/
        │   ├── server/                        # Ktor embedded (réception flux JSON)
        │   │   ├── AttendanceServer.kt
        │   │   ├── routes/
        │   │   └── websocket/
        │   ├── database/                      # Base centrale H2
        │   │   └── CentralDatabase.kt
        │   └── repository/
        ├── ui/                                # IHM Compose for Desktop
        │   ├── screens/
        │   │   ├── DashboardScreen.kt
        │   │   ├── QRManagementScreen.kt
        │   │   ├── PairManagementScreen.kt
        │   │   └── ClassesScreen.kt
        │   ├── components/
        │   │   ├── KPICard.kt
        │   │   ├── AttendanceTable.kt
        │   │   └── StatutBadge.kt
        │   ├── viewmodel/                     # ViewModels Desktop (StateFlow)
        │   └── theme/                         # Thème Material 3 Desktop
        └── navigation/                        # Navigation Desktop
```

### 9.3 Flux de Données (Séance Complète)

```
1. AVANT LA SÉANCE (Desktop - Admin)
   ─────────────────────────────────
   Admin → Génère QR Code classe X / semestre Y → Affiche/Sauvegarde PNG
   Admin → Ouvre la séance sur le dashboard → Statut = EN_COURS
         → Ktor server écoute sur port 8080

2. ENRÔLEMENT (Première utilisation uniquement)
   ─────────────────────────────────
   Étudiant → Installe app → Premier scan du QR
            → App extrait UUID matériel
            → Admin valide PHYSIQUEMENT le couplage [Matricule ⇔ UUID]
            → Stocké dans DB centrale

3. PREMIER SCAN (Début Cours)
   ─────────────────────────────────
   Étudiant → Ouvre app → Scan QR → CameraX détecte
           → [0-15min depuis début ? → "À l'heure" : "En retard"]
           → Stockage Room local + attente réseau

4. CONSOLIDATION (Pendant le Cours)
   ─────────────────────────────────
   Données en attente en local
   Étudiant sans scan → marqué temporairement absent potentiel
   Aucune transmission réseau avant la clôture

5. CLÔTURE ENSEIGNANT (Fin de Cours)
   ─────────────────────────────────
   👨‍🏫 Enseignant → Ouvre app → Scan code clôture
            → Statut séance = CLOTURE_ENSEIGNANT
            → Verrouille les données
            → Débloque le second scan pour tous les étudiants

6. SECOND SCAN (Validation Sortie)
   ─────────────────────────────────
   Étudiant → Scan QR → Horodatage de sortie
           → Calcul statut final (Présent/Retard/Absent)
           → Transmission WinFi → Ktor server

7. RAPPORT DÉLÉGUÉ
   ─────────────────────────────────
   👥 Délégué → Ouvre app → Voir résumé consolidé
             → Bouton "Envoyer rapport"
             → POST /api/attendance/report

8. ADMINISTRATION (Live)
   ─────────────────────────────────
   Dashboard affiche en temps réel :
   🟢 Présents | 🟡 Retards | 🔴 Absents
   WebSocket pousse les mises à jour automatiquement
   Admin peut clôturer définitivement après vérification
```

---

## 10. Spécifications Fonctionnelles Détaillées

### 10.1 RÈGLE MÉTIER CARDINALE — Algorithme de Calcul du Statut

```
┌─────────────────────────────────────────────────────────────────┐
│                    ALGORITHME DE CALCUL DU STATUT                │
│                                                                  │
│  Premier Scan (entrée)     Second Scan (sortie)      RÉSULTAT   │
│  ─────────────────────     ─────────────────────      ────────   │
│  Dans les 15 min           Validé (après clôture)     PRÉSENT ✅ │
│  Au-delà de 15 min        Validé (après clôture)     RETARD ⚠️  │
│  Quelconque                Manquant                   ABSENT ❌  │
│  Manquant                  Quelconque                 ABSENT ❌  │
│  Manquant                  Manquant                   ABSENT ❌  │
└─────────────────────────────────────────────────────────────────┘
```

**Rappel :** Le second scan n'est possible qu'**après la clôture enseignant**.  
Si l'enseignant n'a pas scanné son code de clôture → aucun étudiant ne peut valider sa sortie → statut final = EN_ATTENTE.

### 10.2 Application Desktop (Membre 5 — Ton Domaine)

#### 10.2.1 Dashboard Administrateur 🖥️

| Élément | Description | Technologie |
|---------|-------------|-------------|
| **KPIs en temps réel** | 3 cartes : Présents / Retards / Absents avec compteurs et % | Compose Card + StateFlow |
| **Tableau des présences** | Liste avec nom, statut (couleur), 1er scan, 2nd scan | LazyColumn + Row |
| **Filtres** | Par classe, semestre, date, statut | Dropdown + TextField |
| **WebSocket live** | Mise à jour automatique sans clic | Ktor WS + SharedFlow |
| **Clôture de séance** | Arrête les scans, fige les résultats | Dialog confirmation |
| **Statut de la séance** | Badge PLANIFIE / EN_COURS / CLOTURE_ENSEIGNANT | Chip Material 3 |

**Wireframe :**
```
┌──────────────────────────────────────────────────────────────┐
│ 🏫 KeycePass    Dashboard   Classes   QR Codes   Pairages ⚙️│
├──────────────────────────────────────────────────────────────┤
│ [Statut séance : EN_COURS 🟢]                                │
│                                                              │
│ ┌──────────┐  ┌──────────┐  ┌──────────┐                    │
│ │ PRÉSENTS │  │ RETARDS  │  │ ABSENTS  │                    │
│ │    42    │  │    5     │  │    3     │                    │
│ │  🟢 84%  │  │  🟡 10%  │  │  🔴 6%   │                    │
│ │         │  │         │  │         │                    │
│ │ [+2]    │  │ [0]     │  │ [-1]    │ ← variation live     │
│ └──────────┘  └──────────┘  └──────────┘                    │
│                                                              │
│ [Classe: B2_IT ▼]  [Semestre: S2 ▼]  [Date: 09/06/2026]     │
│                                                              │
│ ┌───────┬────────────┬────────┬──────────┬──────────┐       │
│ │  📸   │  Nom        │ Statut │ 1er Scan │ 2nd Scan │       │
│ ├───────┼────────────┼────────┼──────────┼──────────┤       │
│ │  🟢   │  Diallo A. │ Présent│ 08:12    │ 10:05    │       │
│ │  🟡   │  Koné F.   │ Retard  │ 08:22    │ 10:03    │       │
│ │  🔴   │  Traoré M. │ Absent  │ ---      │ ---      │       │
│ │  🟢   │  Camara S. │ Présent│ 07:58    │ 10:01    │       │
│ │  🔴   │  Bamba K.  │ Absent  │ 08:10    │ ---      │       │
│ └───────┴────────────┴────────┴──────────┴──────────┘       │
│                                              [🔄 Rafraîchir]│
│                                              [📋 Clôturer]  │
└──────────────────────────────────────────────────────────────┘
```

#### 10.2.2 Gestion des QR Codes 📱
- **Création** : Sélection classe + semestre → Génération QR Code statique
- **Aperçu** : Affichage du QR Code dans l'interface avec taille ajustable
- **Export** : Sauvegarde en PNG (300x300px minimum)
- **Impression** : Format A4 prêt avec nom de classe et semestre
- **Historique** : Liste des QR Codes générés par date

**Données encodées dans le QR Code :**
```
QR Data Format: "class:<UUID_CLASSE>:sem:<SEMESTRE_ID>"
Exemple: "class:a1b2c3d4:sem:S2_2026"
```

#### 10.2.3 Gestion des Pairages 🔐
- **Liste des appareils couplés** : Nom, étudiant lié, date de couplage, dernier accès
- **Réinitialisation** : Bouton "Dissocier" par appareil (avec confirmation)
- **Regénération** : Nouveau token de couplage après réinitialisation
- **Logs d'activité** : Historique des accès et tentatives

#### 10.2.4 Écran des Classes 🏫
- **Liste des classes** : code (ex: B2_IT), nom, année, effectif
- **CRUD** : Création, modification, suppression
- **Gestion des étudiants** : Ajout/suppression par classe, import CSV

### 10.3 Application Mobile Android

#### 10.3.1 Écran de Connexion (Étudiant)
- Formulaire : Identifiant étudiant (matricule) + Nom
- Affichage des informations de la séance en cours
- Pas de mot de passe complexe (fluidité en amphi)

#### 10.3.2 Écran de Scan (CameraX)
- Détection automatique et continue du QR Code (pas de bouton)
- ✅ Feedback visuel : cadre vert = scan réussi, rouge = erreur
- 🔔 Feedback sonore optionnel
- Affichage du statut immédiatement après le scan
- Gestion des erreurs : QR invalide, réseau indisponible, séance inexistante

#### 10.3.3 Écran Enseignant (Clôture)
- Scan du code de clôture (QR spécial, généré par l'admin)
- Confirmation : "Voulez-vous clôturer la séance ?"
- Affiche le résumé avant validation : N étudiants en cours
- Une fois confirmé → Statut séance = CLOTURE_ENSEIGNANT

#### 10.3.4 Écran Délégué
- Résumé consolidé : Présents / Retards / Absents
- Tableau récapitulatif des statuts
- Bouton "Envoyer le rapport" → POST vers Ktor
- Confirmation d'envoi

---

## 11. API REST & WebSocket

### 11.1 Endpoints REST

| Méthode | Endpoint | Description | Corps (JSON) |
|---------|----------|-------------|-------------|
| **POST** | `/api/couplage/init` | Initier couplage appareil | `{"etudiantId", "deviceId", "deviceName"}` |
| **POST** | `/api/couplage/verify` | Vérifier couplage existant | `{"deviceId", "token"}` |
| **POST** | `/api/couplage/reset` | Réinitialiser couplage (admin) | `{"etudiantId"}` |
| **POST** | `/api/attendance/scan` | Enregistrer un scan | `{"etudiantId", "seanceId", "scanType": "DEBUT|FIN", "timestamp"}` |
| **POST** | `/api/attendance/report` | Rapport complet (délégué) | `RapportPresenceDto` |
| **POST** | `/api/seance/cloture` | Clôture enseignant | `{"enseignantId", "seanceId"}` |
| **GET** | `/api/attendance/status` | Statuts en cours pour une séance | - |
| **GET** | `/api/seance/info` | Infos séance active | - |
| **GET** | `/api/classes` | Liste des classes | - |
| **GET** | `/api/etudiants?classeId=X` | Liste des étudiants par classe | - |

### 11.2 WebSocket

| Endpoint | Direction | Description |
|----------|-----------|-------------|
| `ws://<ip>:8080/ws/live` | Server → Desktop | Push temps réel des nouvelles entrées |
| `ws://<ip>:8080/ws/sync` | Bidirectionnel | Sync des périphériques mobiles |

**Payload WebSocket (live) :**
```json
{
  "type": "ATTENDANCE_UPDATE",
  "etudiantId": "uuid",
  "statut": "PRESENT",
  "timestamp": "2026-06-09T10:05:00"
}
```

---

## 12. Modèle de Données

### 12.1 Module Shared (Data Classes Kotlin)

```kotlin
// === Etudiant.kt ===
data class Etudiant(
    val id: String,              // UUID
    val matricule: String,       // Identifiant académique officiel
    val nom: String,
    val prenom: String,
    val email: String?,
    val classeId: String,
    val deviceUuid: String?      // UUID matériel (null si pas encore couplé)
)

// === Seance.kt ===
data class Seance(
    val id: String,
    val classeId: String,
    val semestre: String,
    val matiere: String,
    val date: LocalDate,
    val heureDebut: LocalTime,
    val heureFin: LocalTime,
    val qrCodeData: String,       // "class:<UUID>:sem:<SEMESTRE>"
    val statut: StatutSeance      // PLANIFIE, EN_COURS, CLOTURE_ENSEIGNANT
)

// === Emargement.kt ===
data class Emargement(
    val id: String,
    val etudiantId: String,
    val seanceId: String,
    val horodatageScanDebut: Instant?,
    val horodatageScanFin: Instant?,
    val statutFinal: StatutPresence,  // EN_ATTENTE, PRESENT, RETARD, ABSENT
    val deviceId: String
)

// === StatutPresence.kt ===
enum class StatutPresence {
    EN_ATTENTE,
    PRESENT,
    RETARD,
    ABSENT
}

// === StatutSeance.kt ===
enum class StatutSeance {
    PLANIFIE,
    EN_COURS,
    CLOTURE_ENSEIGNANT
}
```

### 12.2 Algorithme Central (Membre 1)

```kotlin
object CalculStatut {
    
    /**
     * Calcule le statut de présence final d'un étudiant
     * basé sur les deux scans et les règles métier.
     *
     * RÈGLE :
     * - Premier scan ≤ 15min + second scan OK → PRÉSENT
     * - Premier scan > 15min + second scan OK → RETARD
     * - Scan(s) manquant(s) → ABSENT
     *
     * @param premierScan  Timestamp du premier scan (nullable)
     * @param secondScan   Timestamp du second scan (nullable)
     * @param heureDebut   Heure de début officielle du cours
     * @return StatutPresence calculé
     */
    fun calculer(
        premierScan: Instant?,
        secondScan: Instant?,
        heureDebut: LocalTime
    ): StatutPresence {
        // Si un des deux scans manque → ABSENT
        if (premierScan == null || secondScan == null) {
            return StatutPresence.ABSENT
        }
        
        // Calcul du delta entre premier scan et début du cours
        val scanTime = premierScan.atZone(ZoneId.systemDefault()).toLocalTime()
        val deltaMinutes = ChronoUnit.MINUTES.between(heureDebut, scanTime)
        
        return if (deltaMinutes <= 15) {
            StatutPresence.PRESENT
        } else {
            StatutPresence.RETARD
        }
    }
}
```

### 12.3 Schéma de la Base de Données Centrale (Desktop - H2)

```sql
-- Table des étudiants
CREATE TABLE etudiant (
    id_etudiant INT AUTO_INCREMENT PRIMARY KEY,
    matricule VARCHAR(20) UNIQUE NOT NULL,
    nom VARCHAR(50) NOT NULL,
    prenom VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    classe_id VARCHAR(20) NOT NULL,
    device_uuid VARCHAR(100)     -- NULL si pas encore couplé
);

-- Table des séances
CREATE TABLE seance (
    id_seance INT AUTO_INCREMENT PRIMARY KEY,
    nom_matiere VARCHAR(100) NOT NULL,
    classe_id VARCHAR(20) NOT NULL,
    semestre VARCHAR(20) NOT NULL,
    date_jour DATE NOT NULL,
    heure_debut TIME NOT NULL,
    heure_fin TIME NOT NULL,
    statut_seance VARCHAR(20) NOT NULL DEFAULT 'PLANIFIE',
    -- États : PLANIFIE, EN_COURS, CLOTURE_ENSEIGNANT
    qr_data VARCHAR(255)
);

-- Table des émargements
CREATE TABLE emargement (
    id_emargement INT AUTO_INCREMENT PRIMARY KEY,
    etudiant_id INT NOT NULL,
    seance_id INT NOT NULL,
    horodatage_scan_debut DATETIME,
    horodatage_scan_fin DATETIME,
    statut_final VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
    device_id VARCHAR(100),
    -- États : EN_ATTENTE, PRESENT, RETARD, ABSENT
    FOREIGN KEY (etudiant_id) REFERENCES etudiant(id_etudiant),
    FOREIGN KEY (seance_id) REFERENCES seance(id_seance)
);

-- Table des couplages
CREATE TABLE couplage (
    id_couplage INT AUTO_INCREMENT PRIMARY KEY,
    device_id VARCHAR(100) NOT NULL,
    device_nom VARCHAR(100),
    etudiant_id INT NOT NULL,
    token_chiffre VARCHAR(500) NOT NULL,
    est_actif BOOLEAN DEFAULT TRUE,
    couple_le TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (etudiant_id) REFERENCES etudiant(id_etudiant)
);
```

---

## 13. Contraintes Techniques

### 13.1 Contraintes Matérielles
- **Desktop** : Windows 10+, 4GB RAM, écran 1366x768 minimum
- **Mobile** : Android 7.0+ (API 24), appareil photo avec autofocus, 2GB RAM

### 13.2 Contraintes Réseau (WinFi)
- **WinFi** = réseau local uniquement (pas de connexion Internet requise)
- Découverte automatique du serveur (broadcast UDP ou adresse IP connue)
- Débit minimum : 1 Mbps (des données JSON légères seulement)
- Délai réseau max : 500ms pour un scan
- Gestion de la latence : cache local (Room) + sync différée si réseau indisponible

### 13.3 Contraintes de Sécurité
- Chiffrement des tokens de couplage (AES-256 via EncryptedSharedPreferences)
- Hardware ID anti-clonage (UUID + validation admin)
- QR Code statique = pas de données personnelles (que des identifiants)
- Requêtes paramétrées (pas d'injection SQL)
- Clôture enseignant : une fois BLOCKED, plus aucune modification possible

### 13.4 Contraintes de Performance
- Démarrage desktop < 3 secondes
- Scan mobile < 1.5 seconde (détection QR → affichage statut)
- Dashboard temps réel : latence < 1 seconde
- Base de données : jusqu'à 1000 étudiants, 100 séances par semestre

---

## 14. Stack Technologique

| Technologie | Version | Utilisation | Membres |
|-------------|---------|-------------|---------|
| **Kotlin** | 2.1.20 | Langage principal | Tous |
| **Compose Multiplatform** | 1.8.0 | UI Desktop (Compose for Desktop) | M5 |
| **Jetpack Compose** | - | UI Mobile (Material Design 3) | M2 |
| **Compose Navigation** | - | Navigation Mobile | M2 |
| **Voyager** | 1.1.0-beta03 | Navigation Desktop | M5 |
| **CameraX** | - | Appareil photo | M3 |
| **ML Kit Barcode** | - | Détection QR Code | M3 |
| **Room Database** | - | Persistance locale Android | M4 |
| **EncryptedSharedPreferences** | - | Stockage sécurisé tokens | M3 |
| **Ktor Server** | 3.0.3 | Serveur embed desktop (Netty) | M6 |
| **Ktor Client** | 3.0.3 | Client HTTP mobile | M3, M6 |
| **Ktor WebSocket** | 3.0.3 | Push temps réel | M5, M6 |
| **kotlinx-serialization** | 1.7.3 | JSON DTOs | M1 |
| **qrose** | 1.0.1 | QR Code génération (pure Kotlin) | M5 |
| **H2 Database** | - | DB centrale desktop | M6 |
| **Exposed** (Kotlin ORM) | - | ORM desktop | M6 |
| **Gradle** | 8.10+ | Build system | Tous |
| **kotlinx-coroutines** | - | Programmation asynchrone | Tous |
| **kotlinx-datetime** | - | Gestion dates/heures | M1 |

---

## 15. Sécurité & Anti-Fraude

### 15.1 Device Binding (Anti-Fraude #1)
Le mécanisme central de sécurisation :

```
1. Premier scan → App extrait UUID matériel du téléphone
2. Admin valide PHYSIQUEMENT le couple [Étudiant ↔ Téléphone]
3. Couplage enregistré : token chiffré sur mobile + dans DB centrale
4. À chaque scan → Vérification : l'UUID correspond au matricule ?
5. Si mismatch → Scan REFUSÉ, log de sécurité
6. Si changement d'appareil → Admin doit réinitialiser manuellement
```

### 15.2 Clôture Enseignant (Anti-Fraude #2)
```
- Seul l'enseignant peut clôturer la séance (QR code spécial)
- Une fois clôturée : VERROUILLÉE
  → Plus d'ajouts, plus de modifications, plus de suppressions
  → Les données sont figées juridiquement
- Le second scan étudiant n'est possible QU'APRÈS cette clôture
```

### 15.3 Chiffrement des Données

| Donnée | Méthode | Où |
|--------|---------|----|
| Token de couplage | AES-256 (EncryptedSharedPreferences) | Mobile Android |
| Token de couplage | DPAPI (Windows) ou AES-256 | Desktop |
| Données en transit | JSON simple (pas de TLS, réseau local) | WinFi |
| QR Code | Données non sensibles (que des ID) | Papier/écran |

### 15.4 Réinitialisation Sécurisée
- Interface de gestion des appareils couplés
- Suppression des tokens + invalidation côté serveur
- Régénération possible après validation admin
- Journalisation de tous les événements de sécurité

---

## 16. Planning & Répartition

### 16.1 Planning Prévisionnel (4 semaines)

```
Semaine 1 : Fondations 🏗️
├── M1 : Module shared — modèles, enums, DTOs
├── M2 : Structure navigation mobile, écrans vides
├── M3 : Setup CameraX + QR scan basique + DeviceCouplage
├── M4 : Setup Room Database + DAOs + Repository
├── M5 : Setup projet Desktop + structure UI + navigation
└── M6 : Setup Ktor embedded + routes de base + H2

Semaine 2 : Cœur Métier 🔧
├── M1 : Algo CalculStatut + tests unitaires complets
├── M2 : LoginScreen + ScanScreen UI (hors caméra)
├── M3 : Détection QR temps réel + EncryptedSharedPrefs
├── M4 : DAOs + Entities + relations entre tables
├── M5 : Dashboard + KPI cards + AttendanceTable (données mockées)
└── M6 : API POST /attendance/scan + réception + stockage H2

Semaine 3 : Intégration 🔌
├── M1 : Finalisation shared + debug cross-module
├── M2 : DelegueScreen + EnseignantScreen (scan clôture)
├── M3 : Intégration réseau WinFi + envoi scans
├── M4 : Sync Room → WinFi payload + gestion offline
├── M5 : QR management + Pair management (UI complète)
└── M6 : WebSocket live push + endpoint clôture enseignant

Semaine 4 : Finalisation & Présentation 🚀
├── Tous : Tests d'intégration complets (mobile → desktop)
├── Tous : Bug fixing + polishing UI
├── M2 + M5 : Animations Material + transitions
├── M6 : Export rapports + gestion des erreurs
└── Tous : Préparation de la démo + rapport final
```

### 16.2 Matrice des Dépendances

```
M5 (Toi) ← M1 (Modèles shared) ← Semaine 1 — STRATÉGIE : Données mockées en attendant
M5 (Toi) ← M6 (API + WebSocket) ← Semaines 2-3

M6 ← M1 (DTOs réseau) ← Semaine 1
M2 ← M1 (Modèles de données)
M3 ← M1 (DTOs couplage) + M1 (Enums statuts)
M4 ← M1 (Entités partagées)

CHEMIN CRITIQUE : M1 → M3 → (M5 et M6)
                 M1 → M6 → M5 (sans ça, pas de live dashboard)
```

### 16.3 Répartition des Tâches

| Membre | Rôle | Module | Technos | Dépend de |
|--------|------|--------|---------|-----------|
| **M1** | Architecte logiciel & logique métier | `shared/` | kotlinx-serialization, datetime | - |
| **M2** | Mobile UI/UX | `androidApp/ui/` | Jetpack Compose, Material 3 | M1 |
| **M3** | Mobile Features & Sécurité | `androidApp/hardware/` | CameraX, MLKit, EncryptedPrefs | M1 |
| **M4** | Mobile Persistance | `androidApp/data/` | Room Database, DAOs | M1 |
| **M5 (Toi)** | **Desktop UI/UX** 🎯 | `desktopApp/ui/` | **Compose Multiplatform, qrose** | **M1, M6** |
| **M6** | Desktop Serveur & DB | `desktopApp/data/` | Ktor, H2, Exposed, WebSocket | M1 |

---

## 17. Risques & Mitigations

| Risque | Probabilité | Impact | Mitigation |
|--------|------------|--------|------------|
| Communication WinFi instable (pare-feu) | Élevée | Critique | Règle firewall dans l'installateur, guide utilisateur, fallback local |
| Délais de couplage (admin pas dispo) | Élevée | Moyen | File d'attente de validation, notification admin |
| QR Code difficile à scanner | Moyenne | Élevé | Taille généreuse, test multi-appareils, contraste élevé |
| Perte de connexion WinFi pendant scan | Moyenne | Élevé | Cache Room local, sync automatique au retour réseau |
| Conflit de version Kotlin multiplatform | Faible | Critique | Fichier `gradle.properties` versionné, CI de build |
| Changement d'appareil étudiant | Moyenne | Moyen | Interface de réinitialisation admin, token d'autorisation |

---

## 18. Annexes : Guides Techniques par Rôle

### 18.1 Guide Membre 5 — Toi (Desktop UI/UX) 🚀

#### Stack Spécifique (build.gradle.kts)
```kotlin
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose") version "1.8.0"
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.desktop.currentOs)
                
                // Navigation Desktop
                implementation("cafe.adriel.voyager:voyager-core:1.1.0-beta03")
                
                // QR Code Generation
                implementation("io.github.alexzhirkevich:qrose:1.0.1")
                
                // Project Module
                implementation(project(":shared"))
            }
        }
    }
}
```

#### Point d'Entrée Desktop (Main.kt)
```kotlin
fun main() = application {
    val appScope = rememberCoroutineScope()
    
    Window(
        onCloseRequest = {
            appScope.launch { 
                AttendanceServer.stop()  // Arrêt propre du serveur Ktor
            }
            exitApplication()
        },
        title = "KeycePass — Administration",
        state = rememberWindowState(width = 1200.dp, height = 800.dp)
    ) {
        KeycePassTheme {
            App()
        }
    }
}
```

#### Navigation Desktop (App.kt)
```kotlin
enum class Screen(val label: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Default.Dashboard),
    Classes("Classes", Icons.Default.School),
    QRManagement("QR Codes", Icons.Default.QrCode),
    Pairs("Pairages", Icons.Default.Security)
}

@Composable
fun App() {
    var screen by remember { mutableStateOf(Screen.Dashboard) }
    
    Row(Modifier.fillMaxSize()) {
        // Sidebar Navigation
        NavigationRail {
            Screen.entries.forEach { s ->
                NavigationRailItem(
                    icon = { Icon(s.icon, null) },
                    label = { Text(s.label) },
                    selected = screen == s,
                    onClick = { screen = s }
                )
            }
        }
        VerticalDivider()
        
        // Content
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            when (screen) {
                Screen.Dashboard -> DashboardScreen()
                Screen.Classes -> ClassesScreen()
                Screen.QRManagement -> QRManagementScreen()
                Screen.Pairs -> PairManagementScreen()
            }
        }
    }
}
```

#### Dashboard (DashboardScreen.kt)
```kotlin
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = remember { DashboardViewModel() }) {
    val state by viewModel.attendanceState.collectAsState()
    
    // Ligne des KPIs
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        KPICard("Présents", state.presents, Color(0xFF4CAF50))
        KPICard("Retards", state.lates, Color(0xFFFFC107))
        KPICard("Absents", state.absents, Color(0xFFF44336))
    }
    
    Spacer(Modifier.height(16.dp))
    
    // Filtres
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ClasseDropdown(state.classes, state.selectedClasse, onSelect = viewModel::filterByClasse)
        SemestreDropdown(state.semestres, state.selectedSemestre, onSelect = viewModel::filterBySemestre)
        DatePicker(state.date, onSelect = viewModel::filterByDate)
        Spacer(Modifier.weight(1f))
        Text("Statut séance : ${state.seanceStatut.nom}",
             color = state.seanceStatut.couleur,
             fontWeight = FontWeight.Bold)
    }
    
    Spacer(Modifier.height(16.dp))
    
    // Tableau des présences (live)
    Card(modifier = Modifier.fillMaxSize()) {
        AttendanceTable(
            rows = state.rows,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        )
    }
}
```

#### KPI Card (KPICard.kt)
```kotlin
@Composable
fun KPICard(
    title: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(width = 200.dp, height = 120.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(
                "$value",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
```

#### 🔥 Tips pour Assurer en Démo

1. **Animations Material** : `animateContentSize()` sur les KPIs, `AnimatedVisibility` sur les changements de statut
2. **WebSocket temps réel** : Ktor WebSocket + `StateFlow` = dashboard qui bouge TOUT SEUL (impressionnant en démo)
3. **Raccourcis clavier** :
   - `Ctrl+R` → Rafraîchir
   - `Ctrl+Q` → Générer QR
   - `Ctrl+E` → Exporter rapport
   - `Ctrl+C` → Clôturer séance
4. **Mode sombre** : Toggle Material 3, le support est natif
5. **Export PDF** : Bouton "Exporter" → Génère un rapport avec les statistiques (bonus pour la note)
6. **Données mockées** : Commence TOUT DE SUITE avec des fausses données pour coder le dashboard sans attendre M1

### 18.2 Guide Membres 1 & 6 (Tes Dépendances)

- **Membre 1** doit te fournir le module `shared/` avec les data classes en **Semaine 1**
- **Membre 6** doit te fournir l'API + WebSocket en **Semaines 2-3**

**Stratégie** : Dès la Semaine 1, code le dashboard avec des **données mockées** pour ne pas être bloqué. Tu peux même montrer une maquette fonctionnelle avant que M1 ait fini.

### 18.3 Guide Membre 6 (Serveur Ktor Embedded)

```kotlin
// AttendanceServer.kt — Serveur Ktor embarqué dans le desktop
fun startServer() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        install(ContentNegotiation) { json() }
        install(WebSockets)
        install(StatusPages)
        
        routing {
            post("/api/attendance/scan") {
                val scan = call.receive<ScanDto>()
                // Traitement et stockage
                _updatesFlow.tryEmit(scan)
                call.respond(HttpStatusCode.OK)
            }
            
            post("/api/seance/cloture") {
                val cloture = call.receive<ClotureDto>()
                // Vérification et clôture
                _seanceStatut.value = StatutSeance.CLOTURE_ENSEIGNANT
                call.respond(HttpStatusCode.OK)
            }
            
            webSocket("/ws/live") {
                _updatesFlow.collect { update ->
                    send(Frame.Text(json.encodeToString(update)))
                }
            }
        }
    }.start(wait = false)  // Non bloquant
}
```

---

## Glossaire

| Terme | Définition |
|-------|------------|
| **KeycePass** | Nom du projet — système de contrôle de présence |
| **WinFi** | Communication réseau local sans accès Internet |
| **Device Binding** | Association sécurisée et vérifiée [Étudiant ⇔ Téléphone] |
| **QR Code** | Code-barres 2D contenant les identifiants de séance |
| **DTO** | Data Transfer Object — format JSON pour les échanges réseau |
| **Room** | ORM Android pour persistance locale SQLite |
| **Ktor** | Framework HTTP asynchrone Kotlin (client + serveur) |
| **MVVM** | Model-View-ViewModel — Pattern d'architecture |
| **Clôture Enseignant** | Action de verrouillage de séance par scan enseignant |
| **CameraX** | API Android Jetpack pour l'appareil photo |

---

## Documents Livrables

- [x] **Cahier des charges V3** (ce document) — fusionné des V1 et V2
- [ ] Diagramme d'architecture (UML)
- [ ] Diagramme de classes
- [ ] Diagramme de séquence (flux de scan + clôture)
- [ ] Wireframes UI (Desktop + Mobile)
- [ ] Code source complet (3 modules)
- [ ] Rapport de projet
- [ ] Manuel d'utilisation
- [ ] Présentation soutenance

---

> **Document rédigé par Hermes Agent** 🧠 — Juin 2026  
> *KeycePass — Contrôle de gestion de la fiche de présence des étudiants*  
> *Version 3.0 fusionnant les V1 et V2 officielles*  
> *Pour toute question : @THE GUY sur Discord*
