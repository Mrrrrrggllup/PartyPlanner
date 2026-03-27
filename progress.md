# PartyPlanner — Progress

## Phase 1 — Fondations

### Backend ✅
- [x] Structure Ktor (Application.kt, plugins séparés)
- [x] PostgreSQL via Exposed DAO, `DatabaseFactory`, table `Users`
- [x] `AuthService` : register (BCrypt hash, unicité email), login (vérif BCrypt), génération JWT 7j
- [x] Routes : `POST /auth/register` (201/409), `POST /auth/login` (200/401)
- [x] Configuration `application.conf` (port, db, jwt)
- [x] Koin backend (`BackendModule`)
- [x] DTOs backend : `RegisterRequest`, `LoginRequest`, `AuthResponse`

### Shared — Domain ✅
- [x] `User` model
- [x] `AuthRepository` interface
- [x] `LoginUseCase`, `RegisterUseCase`

### Shared — Data ✅
- [x] `AuthApi` (Ktor client HTTP)
- [x] DTOs shared : `RegisterRequest`, `LoginRequest`, `AuthResponse`
- [x] `AuthRepositoryImpl` (appel API + sauvegarde session)
- [x] `SessionStorage` (SQLDelight : selectSession / insertSession / clearSession)
- [x] `DriverFactory` expect/actual (Android : AndroidSqliteDriver, iOS : NativeSqliteDriver)
- [x] SQLDelight schema `Session.sq`
- [x] `PlatformConfig` expect/actual (BASE_URL emulateur / iOS)

### Shared — DI ✅
- [x] `SharedModule` (HttpClient, AuthApi, SessionStorage, AuthRepository, UseCases, ViewModel)
- [x] `AndroidModule` (DriverFactory avec Context)
- [x] `IosModule` (DriverFactory sans Context)
- [x] `KoinInitializer` (appelé depuis Swift)

### Shared — Presentation ✅
- [x] `AuthState` (Idle, Loading, Success, Error)
- [x] `AuthComponent` (interface KMM)
- [x] `DefaultAuthComponent` (Decompose `ComponentContext`, `coroutineScope` lié au lifecycle)

### Navigation Decompose ✅
- [x] `RootComponent` interface + `DefaultRootComponent` (ChildStack, Config sérialisé)
- [x] `MainComponent` placeholder
- [x] `DefaultAuthComponent` — callback `onAuthSuccess` → navigate to Main
- [x] `RootContent.kt` (Children + slide animation)
- [x] `PartyPlannerApp` (Application, startKoin Android)
- [x] `MainActivity` — `retainedComponent` survit aux rotations
- [x] `MainViewController` iOS — `ApplicationLifecycle`
- [x] `App.kt` — MaterialTheme wrapper

### UI (composeApp) ✅ Auth
- [x] `AuthScreen` — onglets Login / Inscription, champs validés, loading, erreur inline
- [x] Restauration de session au lancement — `DefaultRootComponent.init` vérifie `getStoredSession()`, navigue vers Main si session existante

### DA / Thème ✅
- [x] `AppConfig` — constante `APP_NAME` dans `shared/core`
- [x] `AppColors` — tokens light/dark + `lightColorScheme` / `darkColorScheme` Material3 + `AppExtraColors` (gradients)
- [x] `AppTypography` — scale typographique centralisée (Syne + Plus Jakarta Sans, fonts système en attendant)
- [x] `AppShapes` — tokens sémantiques (Avatar 12dp, Card 20dp, Pill 100dp...)
- [x] `AppTheme` — entry point, `MaterialTheme.appColors` extension, switch dark/light OS automatique
- [x] `AuthScreen` refaite dans la DA (hero gradient, pill tabs, gradient button, form card)

---

> **Phase 1 terminée et testée** — inscription, login, erreurs, restauration de session, DA appliquée.

---

## Backlog UI / polish (hors phases)
- [ ] Validation mot de passe : longueur min (8 car.) + complexité (maj, chiffre) côté client
- [ ] Switch dark/light manuel depuis l'écran Profil (`ThemeMode` SYSTEM/LIGHT/DARK dans les préférences)
- [ ] Validation des dates côté client : début pas dans le passé, fin postérieure au début
- [x] Jours du calendrier strip cliquables → filtrer les événements par date sélectionnée
- [ ] Décision métier : deux événements du même organisateur peuvent-ils se chevaucher ?
- [ ] Vraies polices Syne + Plus Jakarta Sans — ajouter les `.ttf` dans `composeApp/src/commonMain/composeResources/font/`

---

## Phase 2 — Core événements ✅
### Backend ✅
- [x] Table `Events` (Exposed DAO) + `EventEntity`
- [x] `EventService` : CRUD complet avec contrôle d'accès (owner uniquement)
- [x] Routes `GET/POST /events`, `GET/PUT/DELETE /events/{id}` sous `authenticate("auth-jwt")`
- [x] DTOs backend : `CreateEventRequest`, `UpdateEventRequest`, `EventResponse`

### Shared — Domain ✅
- [x] `Event` model (domain)
- [x] `EventRepository` interface
- [x] `GetEventsUseCase`, `GetEventUseCase`, `CreateEventUseCase`, `UpdateEventUseCase`, `DeleteEventUseCase`

### Shared — Data ✅
- [x] DTOs shared : `CreateEventRequest`, `EventResponse`
- [x] `EventApi` (Ktor client, Authorization Bearer via `SessionStorage`)
- [x] `EventRepositoryImpl` + mapper `EventResponse.toDomain()`
- [x] `SharedModule` mis à jour (EventApi, EventRepository, 5 use cases)

### Shared — Presentation ✅
- [x] `HomeComponent` + `HomeState` (Loading / Success / Error)
- [x] `DefaultHomeComponent` — `lifecycle.doOnResume` pour refresh au retour
- [x] `EventDetailComponent` + `EventDetailState` + `DefaultEventDetailComponent`
- [x] `CreateEventComponent` + `CreateEventState` + `DefaultCreateEventComponent`
- [x] `MainComponent` reécrit avec `ChildStack` (Home / EventDetail / CreateEvent)
- [x] `DefaultMainComponent` — `KoinComponent` + `by inject()` pour éviter cascade constructeur

### UI (composeApp) ✅
- [x] `MainScreen` — `Children` avec `slide()` animation, délègue aux 3 écrans
- [x] `HomeScreen` — hero header, calendar strip, liste événements (`EventCard`), FAB gradient, état vide
- [x] `EventDetailScreen` — hero gradient, back glassmorphism, info cards, suppression avec confirmation
- [x] `CreateEventScreen` — `DatePickerDialog` + `TimePickerDialog`, champs validés, gradient submit

### Corrections DA ✅
- [x] `AppShapes.TextField = 8dp` — champs de texte moins ronds (label non coupé)
- [x] `AppMaterialShapes.extraLarge = 28dp` (pas Pill) — `DatePickerDialog` non coupé

## Phase 3 — Invitations ✅ (partiel)

### Backend ✅
- [x] Table `Invitations` (eventId, userId, status, respondedAt) + contrainte unique (eventId, userId)
- [x] `inviteToken` UUID ajouté à `Events`, généré à la création
- [x] `InvitationService` : `getInviteInfo`, `rsvp` (upsert), `getEventInvitations` (owner only)
- [x] Routes : `GET/POST /invite/{token}/rsvp`, `GET /events/{id}/invitations`
- [x] `getEventsForUser` retourne events owned + events accepted/maybe
- [x] `getEvent` accessible aux invités (pas seulement à l'owner)
- [x] `SchemaUtils.createMissingTablesAndColumns` — migration sans perte de données

### Shared ✅
- [x] `InviteInfo` + `Invitation` + `InvitationStatus` domain models
- [x] `InvitationRepository` + `InvitationRepositoryImpl`
- [x] `GetInviteInfoUseCase`, `RsvpToInvitationUseCase`, `GetEventInvitationsUseCase`
- [x] `InvitationApi` (Ktor client)

### Navigation ✅
- [x] `MainComponent.InvitationChild` + `Config.Invitation(token)` dans le stack
- [x] `DefaultMainComponent(initialInviteToken)` — push Invitation screen si token présent au démarrage
- [x] `DefaultRootComponent(initialInviteToken)` — stocke token pendant auth, le passe à Main après login/register
- [x] Réconciliation : user non connecté → auth → InvitationScreen automatiquement

### Android ✅
- [x] `AndroidManifest.xml` — intent-filter `partyplanner://invite/*`
- [x] `MainActivity` — extrait le token du deep link, passe à `DefaultRootComponent`

### UI ✅
- [x] `InvitationScreen` — hero gradient, infos événement, RSVP buttons (✅ / 🤔 / ❌), status banner
- [x] `EventDetailScreen` — bouton "Copier le lien" (clipboard + feedback 2s) si owner, liste invités avec statut emoji
- [x] Bouton supprimer caché pour les invités (owner only)
- [x] Badge "Organisateur" / "Invité" sur les cards d'événements (HomeScreen)

### Backlog Phase 3
- [ ] Push notification FCM quand un invité répond (Phase 4)
- [ ] Section "Événements où je suis invité" séparée dans HomeScreen
- [ ] `onNewIntent` dans MainActivity pour deep link si app déjà ouverte

> SMS (Twilio) et mail (Resend/Mailgun) écartés — deep link + push FCM (Phase 4) couvre le besoin.

---

## Phase 3.5 — UI événement + Profil + Calendrier

### Profil & Déconnexion ✅
- [x] `ThemeManager` singleton — `StateFlow<ThemeMode>` (SYSTEM / LIGHT / DARK)
- [x] `LogoutUseCase` — `clearSession()` + callback → `DefaultRootComponent` navigue vers Auth
- [x] `ProfileComponent` + `ProfileState` + `DefaultProfileComponent` (KoinComponent)
- [x] `ProfileScreen` — hero gradient, grand avatar initiales, nom, theme switcher pills, déconnexion avec confirmation
- [x] `App.kt` — collecte `ThemeManager.themeMode`, switch dark/light instantané
- [x] Avatar HomeScreen cliquable → Profil, initiale dynamique depuis la session
- [x] `ProfileChild` dans `MainComponent` + `DefaultMainComponent`
- [x] `onLogout` propagé Root → Main → Profile

### Écran détail événement (refonte DA) ✅
- [x] Hero 200dp (gradient gradA, 🎊🎶 décoratifs, back button glassmorphism, titre + sous-titre date·lieu)
- [x] Stats row 3 tuiles : Invités total (gradA), Confirmés (gradC), Covoits placeholder 0 (gradB)
- [x] Barre de tabs basse : Invités | Items | Chat | Covoit (switch de contenu)
- [x] Tab Invités : bouton copier lien (owner), liste invités avec emoji statut, bouton supprimer (owner)
- [x] Tabs Items / Chat / Covoit : placeholders "Phase 4"

### Calendrier ✅
- [x] Jours du strip cliquables — filtrer les événements par date sélectionnée
- [x] Navigation du strip : scroll libre + bouton "aujourd'hui" pour recentrer
- [x] Vue calendrier mensuel complète (bottom sheet) accessible depuis le strip

## Phase 4 — Items, covoiturage, chat, push

### Items ✅
- [x] Tables `ItemRequests` + `ItemsBrought` (backend Exposed DAO)
- [x] `ItemService` : getItems, addItemRequest, fulfillRequest (toggle), deleteItemRequest, addItemBrought, deleteItemBrought
- [x] Routes : `GET/POST /events/{id}/items/requests`, `POST .../requests/{rid}/fulfill`, `DELETE .../requests/{rid}`, `POST/DELETE /events/{id}/items/brought/{bid}`
- [x] Shared : `ItemRequest` + `ItemBrought` + `EventItems` domain models, `ItemApi`, `ItemRepository`, 6 use cases
- [x] `EventDetailState.Success` + `items: EventItems` + `currentUserId`
- [x] `EventDetailComponent` : 5 nouvelles actions items
- [x] Tab Items : section "Ce qu'il manque" (ItemRequests, toggle fulfill, delete owner) + "Ce qu'on apporte" (ItemBrought, delete owner/auteur) + dialogs ajout (bottom sheet label + quantité)

### Covoiturage ❌
- [ ] `CarpoolOffers` + `CarpoolPassengers` (backend + UI)

### Chat ❌
- [ ] Chat WebSocket (Ktor natif)

### Push ❌
- [ ] Firebase Cloud Messaging (push notifications invitations + chat)

## Phase 5 — Store ❌
- [ ] Google Play
- [ ] TestFlight (Codemagic)
- [ ] RGPD