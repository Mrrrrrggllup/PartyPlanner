# Bloc métier — Infrastructure transversale

## Rôle
Socle technique commun à tous les blocs métier. Comprend la navigation, l'injection de dépendances, le stockage local, la communication réseau et la configuration plateforme.

---

## Navigation — Decompose

Decompose permet une navigation déclarative fonctionnant en `commonMain` (KMM compatible).

### Structure de navigation

```
RootComponent (Auth | Main | ForgotPassword | ResetPassword)
├── AuthComponent → AuthScreen (login / register)
├── ForgotPasswordComponent → ForgotPasswordScreen
├── ResetPasswordComponent → ResetPasswordScreen
└── MainComponent → MainScreen
    ├── HomeComponent → HomeScreen
    ├── CreateEventComponent → CreateEventScreen
    ├── EditEventComponent → EditEventScreen
    ├── EventDetailComponent → EventDetailScreen
    ├── InvitationComponent → InvitationScreen (deep link)
    └── ProfileComponent → ProfileScreen
```

### Fichiers clés
- `shared/presentation/root/RootComponent.kt` + `DefaultRootComponent.kt`
- `shared/presentation/main/MainComponent.kt` + `DefaultMainComponent.kt`
- `composeApp/RootContent.kt` — rendu Compose de la stack Decompose
- `composeApp/src/androidMain/MainActivity.kt` — `retainedComponent()` + deep links

### Deep links gérés (MainActivity)
| Deep link | Destination |
|---|---|
| `partyplanner://invite/{token}` | InvitationScreen |
| `partyplanner://reset-password?token=...` | ResetPasswordScreen |

---

## Injection de dépendances — Koin

Koin est utilisé à la place de Hilt car Hilt est Android uniquement (incompatible KMM).

### Modules
- `shared/di/SharedModule.kt` — use cases, repositories, API clients, HTTPClient
- `shared/src/androidMain/di/AndroidModule.kt` — dépendances Android (Context, DriverFactory)
- `shared/src/iosMain/di/IosModule.kt` — dépendances iOS
- `backend/di/BackendModule.kt` — services, repositories backend

### Initialisation
- Android : `PartyPlannerApp.kt` → `startKoin { modules(...) }`
- iOS : `KoinInitializer.kt` → appelé depuis Swift

### Pattern factory pour ChatRepository
`ChatRepository` est enregistré comme factory (pas singleton) pour créer une instance fraîche par session `EventDetail`, évitant les conflits de flux entre événements.

---

## Stockage local — SQLDelight

SQLDelight génère du code Kotlin type-safe à partir de schémas `.sq`.

### Fichiers
- `shared/data/local/SessionStorage.kt` — stockage JWT token + userId
- `shared/data/local/DriverFactory.kt` — déclaration `expect`
- `shared/src/androidMain/data/local/DriverFactory.android.kt` — `AndroidSqliteDriver`
- `shared/src/iosMain/data/local/DriverFactory.ios.kt` — `NativeSqliteDriver`

### Données stockées
| Clé | Type | Description |
|---|---|---|
| `auth_token` | String | JWT token de session |
| `user_id` | Int | ID utilisateur courant |

---

## Client réseau — Ktor Client

Un seul `HttpClient` partagé configuré dans `SharedModule`.

### Configuration
```kotlin
HttpClient {
    install(ContentNegotiation) { json() }
    install(Auth) { bearer { /* token depuis SessionStorage */ } }
    install(WebSockets)
}
```

### Points d'attention
- `ktor-client-okhttp` est utilisé côté Android (et non `ktor-client-android`) car ce dernier ne supporte pas les WebSockets
- L'URL de base est configurée via `AppConfig` / `PlatformConfig` (expect/actual)

---

## Bus d'événements auth — AuthEventBus

`AuthEventBus` est un `SharedFlow` global qui signale une déconnexion forcée (ex : 401 token expiré). Le `RootComponent` l'écoute pour rediriger vers l'écran d'authentification.

- `shared/util/AuthEventBus.kt`

---

## Configuration plateforme

`PlatformConfig` est une déclaration `expect/actual` qui fournit les valeurs spécifiques à chaque plateforme (ex : URL du serveur, flags debug).

- `shared/util/PlatformConfig.kt` — déclaration expect
- `shared/src/androidMain/util/PlatformConfig.android.kt`
- `shared/src/iosMain/util/PlatformConfig.ios.kt`

---

## Thème — Compose Multiplatform

- `composeApp/ui/theme/AppTheme.kt` — thème principal (Material 3)
- `composeApp/ui/theme/AppColors.kt` — palette de couleurs
- `composeApp/ui/theme/AppTypography.kt` — typographie
- `composeApp/ui/theme/AppShapes.kt` — formes

---

## Backend — Plugins Ktor Server

| Plugin | Fichier | Rôle |
|---|---|---|
| Routing | `plugins/Routing.kt` | Enregistrement de toutes les routes |
| Security | `plugins/Security.kt` | JWT auth |
| Serialization | `plugins/Serialization.kt` | JSON content negotiation |
| Databases | `plugins/Databases.kt` | Init PostgreSQL + création tables |
| Logging | `plugins/Logging.kt` | Logs requêtes/réponses |
| Sockets | `plugins/Sockets.kt` | WebSocket (ping 30s, timeout 60s) |

## Backend — Docker

```yaml
# backend/docker-compose.yml
services:
  db: postgres:16 (port 5432)
  adminer: port 8080
```

## Commandes utiles

```bash
# Lancer le backend
./gradlew :backend:run

# Lancer la base de données
docker compose -f backend/docker-compose.yml up -d

# Build complet
./gradlew build

# Tests
./gradlew test
```
