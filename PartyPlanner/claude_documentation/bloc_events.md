# Bloc métier — Événements

## Rôle
Entité centrale de l'application. Un événement regroupe tous les autres blocs (invitations, courses, covoiturage, chat). Tout utilisateur connecté peut créer un événement et en devient l'organisateur.

## Périmètre fonctionnel
- Création d'un événement (titre, description, lieu, coordonnées GPS, date début/fin)
- Lecture de la liste des événements de l'utilisateur (créés + acceptés)
- Vue détaillée d'un événement avec hero gradient et navigation par onglets
- Modification d'un événement (organisateur uniquement)
- Suppression d'un événement (organisateur uniquement)
- Calendrier sur l'écran d'accueil (90 jours, J-14 → J+75)
- Calendrier mensuel complet (BottomSheet)

## Fichiers clés

### Shared (domain)
- `shared/domain/model/Event.kt` — entité Event
- `shared/domain/repository/EventRepository.kt` — interface
- `shared/domain/usecase/event/CreateEventUseCase.kt`
- `shared/domain/usecase/event/GetEventsUseCase.kt`
- `shared/domain/usecase/event/GetEventUseCase.kt`
- `shared/domain/usecase/event/UpdateEventUseCase.kt`
- `shared/domain/usecase/event/DeleteEventUseCase.kt`

### Shared (data)
- `shared/data/remote/EventApi.kt`
- `shared/data/remote/dto/EventDtos.kt`
- `shared/data/repository/EventRepositoryImpl.kt`

### Shared (présentation)
- `shared/presentation/home/HomeComponent.kt` + `HomeState.kt` + `DefaultHomeComponent.kt`
- `shared/presentation/event/CreateEventComponent.kt` + `CreateEventState.kt` + `DefaultCreateEventComponent.kt`
- `shared/presentation/event/EditEventComponent.kt` + `DefaultEditEventComponent.kt`
- `shared/presentation/event/EventDetailComponent.kt` + `EventDetailState.kt` + `DefaultEventDetailComponent.kt`

### UI
- `composeApp/ui/home/HomeScreen.kt` — calendrier strip + liste événements
- `composeApp/ui/event/CreateEventScreen.kt`
- `composeApp/ui/event/EditEventScreen.kt`
- `composeApp/ui/event/EventDetailScreen.kt` — hero + onglets (Invités, Courses, Covoit, Chat)

### Backend
- `backend/routes/EventRoutes.kt` — GET/POST /events, GET/PUT/DELETE /events/{id}
- `backend/services/EventService.kt`
- `backend/db/tables/Events.kt`

## Schéma de données

```kotlin
object Events : IntIdTable("events") {
    val title       = varchar("title", 200)
    val description = text("description").nullable()
    val location    = varchar("location", 500).nullable()
    val latitude    = double("latitude").nullable()
    val longitude   = double("longitude").nullable()
    val startDate   = datetime("start_date")
    val endDate     = datetime("end_date").nullable()
    val ownerId     = reference("owner_id", Users)
    val createdAt   = datetime("created_at")
}
```

## Routes API

| Méthode | Route | Description |
|---|---|---|
| GET | `/events` | Liste des événements de l'utilisateur courant |
| POST | `/events` | Créer un événement |
| GET | `/events/{id}` | Détail d'un événement |
| PUT | `/events/{id}` | Modifier un événement (owner only) |
| DELETE | `/events/{id}` | Supprimer un événement (owner only) |

## Règles métier
- Seul l'organisateur (ownerId) peut modifier ou supprimer un événement
- La liste `/events` retourne les événements créés + ceux où l'utilisateur a une invitation ACCEPTED
- Le hero de l'EventDetail affiche la pill "👥 X confirmés" visible par tous
- Le bouton ✏️ d'édition n'est visible que pour l'organisateur
- Pull-to-refresh recharge invitations + items + covoits en parallèle

## Structure de l'EventDetailScreen

```
EventDetailScreen
├── Hero (140dp / 164dp si organisateur affiché)
│   ├── Gradient titre + dates
│   ├── Pill confirmés
│   └── Bouton ✏️ (owner only)
├── StatsRow (4 tuiles cliquables : Confirmés, Courses, Chat, Covoit)
└── TabRow (Invités | Courses | Covoit | Chat)
    ├── Onglet Invités → InvitationsTab
    ├── Onglet Courses → ItemsTab
    ├── Onglet Covoit → CarpoolTab
    └── Onglet Chat → ChatTabLayout (layout dédié, hors LazyColumn)
```
