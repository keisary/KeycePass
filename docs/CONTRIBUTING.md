# 🤝 Contribuer à KeycePass

## 🌿 Workflow Git

1. **Chaque membre** travaille sur sa branche dédiée
2. Les branches partent de `main`
3. On merge vers `main` avant la deadline (jeudi 11 juin)

### Conventions de branches

```

feature/membre-1-shared      → M1 (Modèles partagés)
feature/membre-2-mobile      → M2 (UI Mobile)
feature/membre-3-camera      → M3 (CameraX + QR)
feature/membre-4-data        → M4 (Room + Sécurité)
iruzen                       → M5 (Desktop — livré sur main)
feature/membre-6-server      → M6 (API + DB)
```

### Conventions de commits

Format : `type(scope): description`

| Type | Quand l'utiliser |
|------|-----------------|
| `feat` | Nouvelle fonctionnalité |
| `fix` | Correction de bug |
| `docs` | Documentation |
| `chore` | Configuration, build, nettoyage |
| `refactor` | Réorganisation de code |
| `test` | Tests |

**Exemples :**
```
feat(shared): ajout du modèle Seance avec statuts
fix(desktop): correction du calcul des retards
docs(readme): mise à jour de l'installation
chore(build): configuration Gradle Compose Desktop
```

## ✅ Avant de commit

- [ ] Le code compile (vérifier avec `./gradlew :desktopApp:compileKotlin`)
- [ ] Les tests passent (vérifier avec `./gradlew :shared:check`)
- [ ] Pas de fichiers inutiles (Fibonacci.kt, fichiers vides...)

## ⚡ Push

```bash
git add -A
git commit -m "type(scope): description"
git push origin <ma-branche>
```

## 📦 Modules

| Module | Description | Responsable |
|--------|-------------|-------------|
| `shared/` | Modèles, DTOs, algorithme statuts | M1 |
| `androidApp/` | Application mobile (Jetpack Compose) | M2 + M3 + M4 |
| `desktopApp/` | Application desktop + serveur Ktor | M5 (fait) + M6 |

## 🔗 Liens utiles

- [Cahier des charges V3](./docs/cahier-des-charges-v3.md)
- [README principal](./README.MD)
