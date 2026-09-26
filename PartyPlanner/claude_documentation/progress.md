# PartyPlanner — Suivi d'avancement

_Dernière mise à jour : 2026-09-26 (v1.7)_

---

## Phase 1 — Fondations ✅

- Setup KMM + Compose Multiplatform
- Auth JWT (register / login)
- SQLDelight — `SessionStorage` (token + userId)
- Navigation Decompose : Auth → Main
- Backend Ktor + PostgreSQL (Docker)
- Structure DI Koin (shared + android + ios)

---

## Phase 2 — Core événements ✅

- CRUD Events (create, read, update, delete)
- Liste des événements sur l'écran d'accueil
- `HomeScreen` avec bande calendrier (CalendarStrip)
  - 90 jours affichés (J-14 → J+75), scroll initial sur aujourd'hui
  - Filtre des événements par date sélectionnée
  - Bouton "Aujourd'hui" pour recentrer + sélectionner
  - Bouton "⊞ Mois" → ModalBottomSheet avec calendrier mensuel complet
- `EventDetailScreen` avec hero gradient + onglets

---

## Phase 3 — Invitations ✅

- Table `Invitations` (userId nullable + email/phone pour contacts externes)
- Token d'invitation unique par événement
- Lien d'invitation copiable depuis l'app
- RSVP : accepted / declined / maybe
- Écran d'accueil RSVP pour invité (lecture du token)
- Liste des invités dans l'onglet "Invités" de l'EventDetail
- Stats : nombre d'invités + confirmés

---

## Phase 3.5 — Calendrier amélioré ✅

- CalendarStrip fiable : `rememberLazyListState(initialFirstVisibleItemIndex)` au lieu de LaunchedEffect
- DayPill : 3 états visuels (sélectionné / aujourd'hui / normal)
- Bouton "Aujourd'hui" : scroll + sélection de la date
- MonthCalendarSheet : navigation mois précédent/suivant, grille lundi-first
- Filtrage des événements par date, "Tout afficher" pour revenir à la liste complète

---

## Phase 4 — Fonctionnalités événement

### Items ✅

**Backend**
- Tables : `ItemCategories` (arbre auto-référent max 2 niveaux), `ItemRequests`, `ItemsBrought`
- Colonne `price` en base (réservé pot commun, pas exposé en API)
- 5 catégories seedées au démarrage : 🍕 Nourriture, 🥤 Boissons, 🍰 Desserts, 🛠️ Matériel, 📦 Autre
- `GET /items/categories` (global authentifié)
- `GET/POST /events/{id}/items`
- `POST/DELETE /events/{id}/items/requests/{rid}` + `/fulfill`
- `POST/DELETE /events/{id}/items/brought/{bid}`
- Accès : tous les participants peuvent ajouter (owner + invités ACCEPTED)
- Tri : `ORDER BY categoryId ASC NULLS LAST, id ASC`

**Shared + UI**
- Modèles `ItemRequest`, `ItemBrought`, `EventItems`, `ItemCategory`
- 6 use cases + API + repository
- Onglet Items : header "+ Besoin" / "J'apporte…"
- Groupement par catégorie avec en-têtes dans la liste
- `AddItemSheet` avec sélecteur de catégorie (chips) et compteur de quantité
- Placeholder "chips, jus d'orange…" (sans alcool)

### Covoiturage ✅

**Backend**
- Tables : `CarpoolOffers`, `CarpoolPassengers` (statut MATCHED/CANCELLED, soft leave)
- `seatsRemaining` calculé dynamiquement (seats_available - count MATCHED)
- `GET/POST /events/{id}/carpool`
- `DELETE /events/{id}/carpool/{offerId}`
- `POST /events/{id}/carpool/{offerId}/join` (upsert MATCHED)
- `POST /events/{id}/carpool/{offerId}/leave` (set CANCELLED)
- Accès : tous les participants peuvent créer une offre

**Shared + UI**
- Modèles `CarpoolOffer`, `CarpoolPassenger`
- 5 use cases + API + repository
- Onglet Covoit : liste des offres avec boutons contextuels
  - Driver : pas de bouton
  - Passager actif : "Je descends 🚪"
  - Peut monter (places dispo) : "Je monte ! 🙋"
  - Complet : badge grisé
- `CreateCarpoolSheet` : nombre de places, départ optionnel, notes optionnelles
- `JoinCarpoolSheet` : point de prise en charge optionnel
- Stat "Covoits" dans la StatsRow reflète le nombre réel d'offres

### Chat WebSocket ✅ (testé et fonctionnel)

**Backend**
- Table `ChatMessages` (eventId, senderId, content, createdAt)
- `ChatService` : gestionnaire de salles par eventId (ConcurrentHashMap thread-safe), broadcast
- `AuthService.verifyToken()` : vérification JWT pour auth WS via query param
- `ws("/events/{id}/chat?token=...")` :
  - Authentification + vérification d'accès
  - Envoi de l'historique (50 derniers messages) à la connexion
  - Réception + persistance + broadcast à tous les connectés
- Plugin WebSockets installé (ping 30s, timeout 60s)

**Shared + UI**
- `ktor-client-okhttp` requis côté Android (`ktor-client-android` ne supporte pas les WS)
- `ktor-client-websockets` ajouté aux dépendances KMM
- `install(WebSockets)` sur le HttpClient partagé
- `ChatApi` : connexion WS persistante, `send()` via session stockée
- `ChatRepository` : `SharedFlow<ChatMessage>`, factory Koin (instance fraîche par EventDetail)
- `DefaultEventDetailComponent` : collecte le flow + maintient la connexion WS en arrière-plan
- Onglet Chat : layout dédié (pas dans le LazyColumn principal)
  - `ChatBubble` : bulles arrondies, gradient pour moi / surfaceVariant pour les autres
  - Auto-scroll sur nouveau message
  - Input bar fixe au-dessus de la tab bar
  - Heure formatée (HH:mm)

---

## Phase 4 — Polissage UX ✅

### Fixes EventDetailScreen
- Hero réduit à 140dp (au lieu de 200dp)
- Padding bas du contenu à 104dp pour ne pas déborder sous la tab bar
- Pill "👥 X confirmés" dans le hero (visible par tous)
- Bouton ✏️ en haut à droite du hero pour l'organisateur → navigue vers EditEvent
- Pull-to-refresh (`PullToRefreshBox`) : recharge invitations + items + covoits en parallèle
- Reconnexion WS automatique avec backoff exponentiel (2s → 4s → … → 30s max)
- Badge sur les onglets : messages non lus, items non remplis, invitations en attente (owner), places covoit dispo
- Badge chat reset à 0 quand l'onglet Chat est ouvert (`onChatRead` / `onChatLeft`)
- Renommage onglet "Items" → "Courses" (strings FR + EN)

### Édition d'événement ✅
- `EditEventScreen` / `EditEventComponent` / `DefaultEditEventComponent`
- Formulaire pré-rempli avec les données existantes via `LaunchedEffect`
- `UpdateEventUseCase` + route `PUT /events/{id}` (déjà existante)
- Navigation `Config.EditEvent(eventId)` dans le root stack main

### Édition d'offre de covoiturage ✅
- `EditCarpoolSheet` (version pré-remplie de `CreateCarpoolSheet`)
- Bouton "✏️ Modifier mon offre" visible uniquement pour le driver
- `UpdateCarpoolOfferUseCase` + `PUT /events/{id}/carpool/{offerId}` côté backend
- DTO `UpdateCarpoolOfferDto` (seats, departurePoint, notes)

### Nettoyage RSVP DECLINED ✅
- Quand un invité passe en DECLINED : suppression automatique de ses `ItemsBrought` et de ses `Contributions` pour l'événement

### Keyboard dismiss ✅
- `CreateEventScreen` : tap en dehors des champs ferme le clavier (`LocalFocusManager.clearFocus`)

### Tuiles de navigation ✅
- `StatsRow` : 5 tuiles cliquables (👥 Confirmés, 🛒 Courses, € Notes, 💬 Chat, 🚗 Covoit)
- Chaque tuile navigue directement vers son onglet (ripple natif via `OutlinedCard(onClick)`)
- Chaque tuile affiche un total informatif (confirmés, courses, contributions, messages, offres covoit)

### Deep link invitations ✅
- Lien partagé : `http://[serveur]/i/{token}` au lieu de `partyplanner://invite/{token}` (non cliquable)
- Backend : route publique `GET /i/{token}` → page HTML avec redirect JS vers `partyplanner://invite/{token}`
- Si l'app est installée : ouvre directement l'app via le deep link existant
- Si l'app n'est pas installée : bouton "Ouvrir dans PartyPlanner" sur la page web

### Corrections diverses (2026-06-14) ✅

- Hero : hauteur conditionnelle (140dp / 164dp si "Organisé par …" affiché) pour éviter le chevauchement du bouton retour avec le titre
- `StatsRow` sort du `LazyColumn` et reste affichée même sur l'onglet Chat
- Tuile Chat de la `StatsRow` affiche le nombre total de messages (au lieu des non-lus)
- Covoiturage : nouveau système de suivi "non lu" identique à Courses
  - Backend : `createdAt` sur `CarpoolOffers`, table `EventCarpoolViews`, `newCarpoolCount` dans `CarpoolOffersResponse`, route `POST /events/{id}/carpool/seen`
  - Shared : modèle `EventCarpool(offers, newCarpoolCount)`, `MarkCarpoolSeenUseCase`
  - UI : badge bas Covoit = `newCarpoolCount` (reset via `onCarpoolRead()` à l'ouverture de l'onglet)
- Suppression de la `QuickRsvpBar` sous le hero, redondante avec la `RsvpBanner` de l'onglet Invités
- Bump version Android : `versionCode = 5` / `versionName = "1.4"` — déployé + validé sur Firebase App Distribution

---

## Phase 4 — Restant

### Réinitialisation de mot de passe ✅

**Backend**
- Table `password_reset_tokens` (userId FK, token UNIQUE, expiresAt, usedAt nullable)
- `PasswordResetService` : `requestReset(email)` (silencieux si email inconnu), `resetPassword(token, newPassword)`
- `POST /auth/forgot-password` (toujours 200), `POST /auth/reset-password`
- Envoi de mail via API Resend (`java.net.http.HttpClient` JVM 17, pas de dépendance supplémentaire)
- Config `resend { apiKey, fromEmail, appBaseUrl }` dans `application.conf` + variables d'env

**Shared + UI**
- `ForgotPasswordUseCase`, `ResetPasswordUseCase`
- `ForgotPasswordComponent` / `DefaultForgotPasswordComponent`
- `ResetPasswordComponent` / `DefaultResetPasswordComponent`
- Navigation dans le root stack (avant login, accessible sans auth)
- `ForgotPasswordScreen` : saisie email, état succès avec message informatif
- `ResetPasswordScreen` : saisie + confirmation MDP, validation longueur + correspondance
- Lien "Mot de passe oublié ?" dans le LoginForm
- Deep link `partyplanner://reset-password?token=...` géré dans `MainActivity`

### Push notifications FCM ✅

**Backend**
- Table `DeviceTokens` : stocke les tokens FCM par device (unique index sur token)
- `Users.notificationPending` bool : atomic update, évite les doublons par burst
- `NotificationService` : in-memory `viewingEvent: ConcurrentHashMap<Int, Int>` userId→eventId
  - `enterEvent(userId, eventId)` / `leaveEvent(userId)` — détection de présence par événement
  - `isViewingEvent(userId, eventId)` — suppression de notif si l'utilisateur regarde l'event
  - Fallback `touch(userId)` / `isOnline()` (90s) pour compatibilité
- `POST /users/me/device-token` — enregistrement token FCM
- `DELETE /users/me/notification` — reset flag notif
- `PUT /users/me/event-presence/{eventId}` / `DELETE /users/me/event-presence` — présence par événement
- Services notifiant avec titre de l'événement : `ChatService`, `ItemService`, `CarpoolService`, `InvitationService`
- `serviceAccountKey.json` monté en volume read-only sur le serveur (`/opt/partyplanner/serviceAccountKey.json`)
- Graceful degradation si clé absente

**Shared + UI**
- `UserApi.registerDeviceToken()`, `UserApi.markActive()`, `UserApi.enterEvent()`, `UserApi.leaveEvent()`
- `DefaultEventDetailComponent` : `lifecycle.doOnStart` → `enterEvent`, `lifecycle.doOnStop` → `leaveEvent` (scope IO indépendant pour éviter l'annulation)
- `PartyPlannerMessagingService` : `onNewToken` → register, `onMessageReceived` → heads-up notif
- Icône notif : `ic_notification.xml` (monochromatic 24dp "P" blanc)
- `POST_NOTIFICATIONS` runtime permission demandée dans `MainActivity.onCreate` (Android 13+)
- FCM default notification icon déclaré dans `AndroidManifest.xml`

### Corrections et robustesse (2026-09-26) ✅

- **Icône app adaptive** : vecteurs `ic_launcher_background.xml` (fond navy `#1A1A2E`) et `ic_launcher_foreground.xml` (monogramme "P" doré + confettis) pour Android 8+ via `mipmap-anydpi-v26`
- **Icône notification** : `ic_notification.xml` (24dp monochromatic "P" blanc), référencée dans le service FCM et le manifest
- **WebSocket fiabilité** :
  - `pingIntervalMillis = 30_000L` sur le plugin WebSockets (détection connexion morte)
  - Reset du backoff à 2s après une connexion réussie (évite la dégradation permanente)
- **doOnStop scope** : `leaveEvent()` lancé dans un `CoroutineScope(Dispatchers.IO)` indépendant — le scope du composant est annulé synchroniquement après `doOnStop`
- **Chat keyboard** : `imePadding()` sur la Column de `ChatTabLayout` uniquement (pas sur la Column externe de `EventDetailScreen`), tab bar masquée quand le clavier est ouvert sur l'onglet Chat
- Bump version : `versionCode = 7` / `versionName = "1.6"`

---

## Nouvelles features (2026-09-26) ✅

### Suppression d'un invité par l'organisateur ✅

**Backend**
- `InvitationService.removeGuest(eventId, invitationId, ownerId)` : suppression en cascade
  - `ItemsBrought` supprimés (userId = invité)
  - `ItemRequests` fulfillés remis en "besoin" (assignedTo = null, isFulfilled = false)
  - Passager covoiturage : statut CANCELLED
  - Offre covoiturage si driver : passagers supprimés puis offre supprimée
  - Contributions supprimées (addedById = invité **ou** linkedUserId = invité)
- `DELETE /events/{id}/invitations/{invitationId}` (owner only)
- Même cascade contributions sur `rsvp()` DECLINED : ItemsBrought + Contributions supprimés

**Shared + UI**
- `RemoveGuestUseCase`, `InvitationRepository.removeGuest()`, `InvitationApi.removeGuest()`
- Bouton 🗑 dans `GuestRow` visible uniquement pour l'organisateur
- Dialog de confirmation : "X ne pourra plus accéder à la soirée. Ses courses et covoiturages seront supprimés."
- Après suppression : reload items + carpool + contributions (compteur tuile mis à jour)
- Le guest peut être ré-invité après suppression

**Tests** — 5 cas (non-owner refusé, clean + ré-invite, driver, passager, items + request reset) + 1 cas contributions cascade

### Contributions / Notes (€) ✅

**Backend**
- Table `Contributions` : `eventId`, `addedById`, `linkedUserId`, `label`, `amount DECIMAL(10,2)`, `createdAt`
- `ContributionService` : get (tri desc createdAt), add (validation montant > 0, label non vide, linkedUser participant), delete (uniquement l'ajouteur)
- `GET/POST/DELETE /events/{id}/contributions`
- Migration auto via `SchemaUtils.createMissingTablesAndColumns`

**Shared + UI**
- Modèle `Contribution`, `ContributionRepository`, `ContributionApi`, 3 use cases
- 5e tuile "€" dans la `StatsRow` entre Courses et Chat (compteur contributions)
- Onglet **Notes €** dans la barre du bas
  - Bouton "💶 Ajouter une dépense"
  - Toggle **Par date** (chronologique ↓) / **Par invité** (somme par guest, ordre alphabétique)
  - Bouton ✕ de suppression visible uniquement pour l'ajouteur
  - `AddContributionSheet` : chips guest picker (propriétaire + tous invités), libellé, montant décimal

**Tests** — 13 cas : accès (owner, invité, stranger), add (valid, montant 0, label vide, guest non-participant, adder stranger), delete (adder ok, non-adder refusé), tri desc, cascade removeGuest, cascade DECLINED

- Bump version : `versionCode = 8` / `versionName = "1.7"`

---

## Phase 5 — Store ⬜

- Google Play (Android) : signing, release track
- TestFlight (iOS) : build Codemagic → App Store Connect
- RGPD : politique de confidentialité, suppression de compte
