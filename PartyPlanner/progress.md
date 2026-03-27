# PartyPlanner — Suivi d'avancement

_Dernière mise à jour : 2026-03-28_

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

## Phase 4 — Restant

### Push notifications FCM ⬜

- Intégration Firebase Cloud Messaging
- Notification sur : nouveau message, RSVP reçu, item ajouté, passager qui rejoint
- Nécessite : `google-services.json`, service account côté backend, `FirebaseMessaging` SDK

---

## Phase 5 — Store ⬜

- Google Play (Android) : signing, release track
- TestFlight (iOS) : build Codemagic → App Store Connect
- RGPD : politique de confidentialité, suppression de compte
