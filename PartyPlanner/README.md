# PartyPlanner

Application mobile d'organisation d'événements sociaux (BBQ, soirées, sorties...).
Stack : **Kotlin Multiplatform + Compose Multiplatform** côté mobile, **Ktor Server** côté backend.

---

## Structure du projet

```
PartyPlanner/
├── shared/          ← Logique métier partagée Android + iOS
├── composeApp/      ← Interface utilisateur (Compose Multiplatform)
├── backend/         ← Serveur API REST (Ktor)
├── iosApp/          ← Point d'entrée iOS (Xcode)
└── progress.md      ← Suivi d'avancement
```

---

## `shared/` — Logique partagée (KMM)

Contient tout ce qui est commun aux deux plateformes (Android et iOS) : règles métier, appels réseau, stockage local, navigation. Aucun code UI ici.

### `domain/`
Le cœur de l'application, sans dépendance externe.

| Dossier | Contenu |
|---|---|
| `model/` | Entités métier (`User`, etc.) |
| `repository/` | Interfaces des dépôts de données (`AuthRepository`) |
| `usecase/` | Cas d'usage (`LoginUseCase`, `RegisterUseCase`) — orchestrent les repositories |

### `data/`
Implémentation concrète de l'accès aux données.

| Dossier | Contenu |
|---|---|
| `remote/` | Client HTTP Ktor (`AuthApi`), DTOs de l'API |
| `local/` | SQLDelight : `SessionStorage` (token JWT local), `DriverFactory` |
| `repository/` | `AuthRepositoryImpl` — relie l'API et le stockage local |

### `presentation/`
Gestion de l'état et navigation, partagées entre les plateformes.

| Dossier | Contenu |
|---|---|
| `auth/` | `AuthComponent` (interface), `DefaultAuthComponent` (état login/register via Decompose) |
| `main/` | `MainComponent` (placeholder Phase 2) |
| `root/` | `RootComponent` + `DefaultRootComponent` — navigation principale (Auth → Main) |

> **Pourquoi Decompose ?** Contrairement à `ViewModel` (Android only), Decompose fonctionne en `commonMain` et gère le cycle de vie sur les deux plateformes.

### `di/`
Configuration Koin (injection de dépendances).

| Fichier | Contenu |
|---|---|
| `SharedModule.kt` | Bindings communs : HttpClient, AuthApi, SessionStorage, AuthRepository, UseCases |
| `AndroidModule.kt` | DriverFactory avec `Context` Android |
| `IosModule.kt` | DriverFactory sans contexte (iOS natif) |

### `actual/` (androidMain / iosMain)
Implémentations spécifiques à chaque plateforme pour les classes `expect` :

| Classe | Android | iOS |
|---|---|---|
| `DriverFactory` | `AndroidSqliteDriver` | `NativeSqliteDriver` |
| `BASE_URL` | `10.0.2.2:8080` (émulateur) | `localhost:8080` |

---

## `composeApp/` — Interface utilisateur

UI commune Android + iOS en **Compose Multiplatform**. Reçoit les composants Decompose du module `shared` et les affiche.

### `commonMain/`

| Fichier / Dossier | Rôle |
|---|---|
| `App.kt` | Point d'entrée Compose — applique le `MaterialTheme` et lance `RootContent` |
| `RootContent.kt` | Lit le `ChildStack` du `RootComponent` et affiche le bon écran avec une animation slide |
| `ui/auth/AuthScreen.kt` | Écran login / register (à implémenter) |
| `ui/main/MainScreen.kt` | Écran principal post-auth (à implémenter) |

### `androidMain/`

| Fichier | Rôle |
|---|---|
| `PartyPlannerApp.kt` | `Application` Android — démarre Koin au lancement |
| `MainActivity.kt` | Crée le `DefaultRootComponent` via `retainedComponent` (survit aux rotations) et appelle `App()` |

### `iosMain/`

| Fichier | Rôle |
|---|---|
| `MainViewController.kt` | Crée le `DefaultRootComponent` avec `ApplicationLifecycle` et expose un `UIViewController` à Swift |

---

## `backend/` — API REST (Ktor Server)

Serveur JVM qui expose l'API consommée par l'application mobile.

### Structure

```
backend/src/main/kotlin/com/partyplanner/
├── Application.kt       ← Point d'entrée Ktor, installe les plugins
├── plugins/
│   ├── Routing.kt       ← Déclare les routes (health, auth...)
│   ├── Security.kt      ← Configuration JWT (vérification des tokens)
│   ├── Serialization.kt ← Content negotiation JSON
│   ├── Databases.kt     ← Initialisation de la BDD
│   └── Logging.kt       ← Logs des requêtes HTTP
├── routes/
│   └── AuthRoutes.kt    ← POST /auth/register, POST /auth/login
├── services/
│   └── AuthService.kt   ← Logique : hash BCrypt, vérification, génération JWT
├── db/
│   ├── DatabaseFactory.kt  ← Connexion PostgreSQL, création des tables
│   └── tables/Users.kt     ← Table Exposed DAO
├── dto/
│   └── AuthDtos.kt      ← RegisterRequest, LoginRequest, AuthResponse
└── di/
    └── BackendModule.kt ← Koin : fournit AuthService avec config JWT
```

### Endpoints disponibles

| Méthode | Route | Description | Codes |
|---|---|---|---|
| GET | `/health` | Vérification que le serveur tourne | 200 |
| POST | `/auth/register` | Création de compte | 201 / 409 |
| POST | `/auth/login` | Authentification | 200 / 401 |

### Configuration (`application.conf`)

```
ktor.deployment.port = 8080
database.url = jdbc:postgresql://localhost:5432/partyplanner
jwt.secret = changeme   ← à surcharger via variable d'environnement
```

---

## `iosApp/` — Point d'entrée iOS

Projet Xcode minimal. Appelle `KoinInitializer.initKoin()` pour démarrer Koin, puis `MainViewController()` pour afficher l'UI Compose.
La compilation iOS se fait via **Codemagic** (Mac cloud) — pas de Mac nécessaire en développement.

---

## Lancer le projet

### Backend

```bash
# Démarrer PostgreSQL
docker compose -f backend/docker-compose.yml up -d

# Lancer le serveur
./gradlew :backend:run
```

### Android

Ouvrir dans Android Studio et lancer sur émulateur ou device.

### iOS

Pousser sur la branche configurée dans Codemagic pour déclencher le build cloud.

---

## Stack technique

| Couche | Technologie |
|---|---|
| UI | Compose Multiplatform |
| Navigation | Decompose |
| DI | Koin |
| Réseau | Ktor Client |
| BDD locale | SQLDelight |
| Sérialisation | kotlinx.serialization |
| Async | Coroutines + Flow |
| Backend | Ktor Server + Exposed DAO |
| BDD serveur | PostgreSQL |
| Auth | JWT (7 jours) + BCrypt |