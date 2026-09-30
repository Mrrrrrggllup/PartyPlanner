# Procédure de déploiement — PartyPlanner

## 1 — Bump de version

Dans `composeApp/build.gradle.kts` :

```kotlin
versionCode = X      // incrémenter de 1
versionName = "Y.Z"  // incrémenter le numéro mineur
```

Mettre à jour `composeApp/release_notes.txt` avec les nouveautés visibles par les testeurs :

```
v1.X — Nouveautés

- ...
- ...
```

---

## 2 — Commit + push (déclenche le pipeline backend)

```bash
git add <fichiers>
git commit -m "vX.Y — Description courte"
git push origin main
```

Le push déclenche automatiquement `.github/workflows/deploy.yml` si des fichiers dans `backend/` ou `shared/` ont changé :
- Build + push image Docker sur `ghcr.io`
- Déploiement sur le VPS Hetzner (docker compose pull + up)
- Exécution des migrations SQL pendantes dans `scripts/*.sql` (marquées `.done` après passage)

---

## 3 — Déploiement Firebase App Distribution (APK release)

**Toujours utiliser `--rerun-tasks` pour forcer un build propre** (sans ça le cache Gradle peut livrer un APK obsolète) :

```bash
cd PartyPlanner/PartyPlanner
./gradlew :composeApp:assembleRelease appDistributionUploadRelease --rerun-tasks
```

L'APK est envoyé automatiquement au testeur `antoine.zudas@gmail.com` via Firebase.

> **Prérequis locaux** : `keystore.properties` présent à la racine, `google-services.json` dans `composeApp/`.

---

## Récapitulatif rapide

| Étape | Action |
|---|---|
| 1 | Bump `versionCode` + `versionName` dans `build.gradle.kts` |
| 2 | Mettre à jour `release_notes.txt` |
| 3 | `git commit` + `git push origin main` |
| 4 | `./gradlew :composeApp:assembleRelease appDistributionUploadRelease --rerun-tasks` |
