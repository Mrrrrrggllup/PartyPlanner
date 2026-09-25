# Bloc métier — Covoiturage

## Rôle
Permet aux participants d'un événement de se coordonner pour le transport. Un participant peut proposer de conduire (driver) ou rejoindre une offre existante (passager).

## Périmètre fonctionnel
- Créer une offre de covoiturage (nombre de places, point de départ optionnel, notes)
- Modifier son offre (driver uniquement)
- Supprimer son offre (driver uniquement)
- Rejoindre une offre comme passager (avec point de prise en charge optionnel)
- Quitter une offre
- Affichage contextuel des boutons selon le rôle de l'utilisateur courant
- Badge onglet "Covoit" : nouvelles offres non vues
- Tracking "vu / non vu" : badge disparaît quand l'onglet est ouvert

## Fichiers clés

### Shared (domain)
- `shared/domain/model/Carpool.kt` — `CarpoolOffer`, `CarpoolPassenger`, `EventCarpool`
- `shared/domain/repository/CarpoolRepository.kt`
- `shared/domain/usecase/carpool/GetCarpoolOffersUseCase.kt`
- `shared/domain/usecase/carpool/CreateCarpoolOfferUseCase.kt`
- `shared/domain/usecase/carpool/UpdateCarpoolOfferUseCase.kt`
- `shared/domain/usecase/carpool/DeleteCarpoolOfferUseCase.kt`
- `shared/domain/usecase/carpool/JoinCarpoolUseCase.kt`
- `shared/domain/usecase/carpool/LeaveCarpoolUseCase.kt`
- `shared/domain/usecase/carpool/MarkCarpoolSeenUseCase.kt`

### Shared (data)
- `shared/data/remote/CarpoolApi.kt`
- `shared/data/remote/dto/CarpoolDtos.kt` — `CarpoolOffersResponse(offers, newCarpoolCount)`
- `shared/data/repository/CarpoolRepositoryImpl.kt`

### UI
- Onglet "Covoit" dans `composeApp/ui/event/EventDetailScreen.kt`
- `CreateCarpoolSheet` — création offre (places, départ, notes)
- `EditCarpoolSheet` — modification offre (version pré-remplie de CreateCarpoolSheet)
- `JoinCarpoolSheet` — rejoindre une offre (point de prise en charge)

### Backend
- `backend/routes/CarpoolRoutes.kt`
- `backend/services/CarpoolService.kt`
- `backend/db/tables/Carpool.kt`

## Schéma de données

```kotlin
object CarpoolOffers : IntIdTable("carpool_offers") {
    val eventId        = reference("event_id", Events)
    val driverId       = reference("driver_id", Users)
    val seatsAvailable = integer("seats_available")
    val departurePoint = varchar("departure_point", 300).nullable()
    val departureTime  = datetime("departure_time").nullable()
    val notes          = text("notes").nullable()
    val createdAt      = datetime("created_at")   // ajouté pour le tracking "non vu"
}

object CarpoolPassengers : IntIdTable("carpool_passengers") {
    val offerId     = reference("offer_id", CarpoolOffers)
    val passengerId = reference("passenger_id", Users)
    val status      = enumerationByName("status", 20, CarpoolRequestStatus::class)
    val pickupPoint = varchar("pickup_point", 300).nullable()
}

enum class CarpoolRequestStatus { OPEN, MATCHED, CANCELLED }

// Table de tracking "vu" (identique au système Items)
object EventCarpoolViews : Table("event_carpool_views") {
    val eventId   = reference("event_id", Events)
    val userId    = reference("user_id", Users)
    val lastSeenAt = datetime("last_seen_at")
}
```

## Routes API

| Méthode | Route | Description |
|---|---|---|
| GET | `/events/{id}/carpool` | Liste des offres (`CarpoolOffersResponse`) |
| POST | `/events/{id}/carpool` | Créer une offre |
| PUT | `/events/{id}/carpool/{offerId}` | Modifier son offre (driver only) |
| DELETE | `/events/{id}/carpool/{offerId}` | Supprimer son offre (driver only) |
| POST | `/events/{id}/carpool/{offerId}/join` | Rejoindre (upsert MATCHED) |
| POST | `/events/{id}/carpool/{offerId}/leave` | Quitter (set CANCELLED) |
| POST | `/events/{id}/carpool/seen` | Marquer les offres comme vues |

## Règles métier
- `seatsRemaining` est calculé dynamiquement : `seatsAvailable - count(MATCHED passengers)`
- Rejoindre une offre complète est bloqué côté backend
- Un passager quitte en passant son statut à CANCELLED (soft leave, pas de suppression)
- Tout participant ACCEPTED (+ organisateur) peut créer une offre
- Boutons contextuels selon le rôle :
  - Driver : pas de bouton d'action (bouton ✏️ pour modifier)
  - Passager actif (MATCHED) : "Je descends 🚪"
  - Non passager + places disponibles : "Je monte ! 🙋"
  - Non passager + complet : badge grisé, pas de bouton
- `newCarpoolCount` = nombre d'offres créées après le dernier `seen` de l'utilisateur
