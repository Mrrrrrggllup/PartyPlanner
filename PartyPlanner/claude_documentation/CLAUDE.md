# PartyPlanner — Contexte projet pour Claude Code

## Profil développeur
- Lead tech Java/Kotlin/Angular
- Pas de Mac disponible → iOS compilé via Mac cloud (Codemagic) dans un second temps
- Développement principal sur Windows avec Android Studio

## Description de l'application
Application mobile d'organisation d'événements sociaux (BBQ, soirées, sorties...).

## Stack technique

### Mobile
- **Framework** : Kotlin Multiplatform (KMM) + Compose Multiplatform
- **Navigation** : Decompose
- **DI** : Koin
- **Réseau** : Ktor Client
- **BDD locale** : SQLDelight
- **Sérialisation** : kotlinx.serialization
- **Async** : Coroutines + Flow

### Backend
- **Framework** : Ktor Server
- **ORM** : Exposed (mode DAO)
- **BDD** : PostgreSQL (Docker)
- **Auth** : JWT
- **Temps réel** : WebSockets Ktor (pour le chat)

### Services tiers (à venir)
- Invitations SMS : Twilio
- Mail : Resend ou Mailgun
- Push notifications : Firebase Cloud Messaging
- iOS CI/CD : Codemagic

## Structure du projet
```
PartyPlanner/
├── composeApp/          ← UI Android + iOS (Compose Multiplatform)
├── shared/              ← Logique partagée KMM
│   └── src/
│       ├── commonMain/kotlin/com/partyplanner/
│       │   ├── domain/
│       │   │   ├── model/        ← entités métier
│       │   │   └── usecase/
│       │   ├── data/
│       │   │   ├── repository/
│       │   │   ├── remote/       ← Ktor client, DTOs
│       │   │   └── local/        ← SQLDelight
│       │   └── presentation/
│       │       └── viewmodel/
│       ├── androidMain/kotlin/com/partyplanner/actual/
│       └── iosMain/kotlin/com/partyplanner/actual/
├── backend/             ← Ktor Server
│   ├── src/main/kotlin/com/partyplanner/
│   │   ├── routes/
│   │   ├── services/
│   │   ├── db/
│   │   └── Application.kt
│   └── docker-compose.yml
├── iosApp/              ← Entry point iOS (Xcode)
└── CLAUDE.md
```

## Modèle de données complet

### Users
```kotlin
object Users : IntIdTable("users") {
    val email        = varchar("email", 255).uniqueIndex()
    val phone        = varchar("phone", 20).nullable()
    val displayName  = varchar("display_name", 100)
    val passwordHash = varchar("password_hash", 255)
    val createdAt    = datetime("created_at")
}
```

### Events
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

### Invitations
```kotlin
// Gère les users inscrits ET les contacts externes (email/phone)
// Logique de réconciliation quand un invité s'inscrit
object Invitations : IntIdTable("invitations") {
    val eventId       = reference("event_id", Events)
    val invitedUserId = reference("invited_user_id", Users).nullable()
    val invitedEmail  = varchar("invited_email", 255).nullable()
    val invitedPhone  = varchar("invited_phone", 20).nullable()
    val status        = enumerationByName("status", 20, InvitationStatus::class)
    val token         = varchar("token", 100).uniqueIndex()
    val sentAt        = datetime("sent_at")
    val respondedAt   = datetime("responded_at").nullable()
}

enum class InvitationStatus { PENDING, ACCEPTED, DECLINED, MAYBE }
```

### Item Categories (arbre auto-référent, max 2 niveaux)
```kotlin
// Exemples : Nourriture > Végétarien, Boisson > Alcool, Matériel > Jeux
object ItemCategories : IntIdTable("item_categories") {
    val label    = varchar("label", 100)
    val parentId = reference("parent_id", ItemCategories).nullable()
    val icon     = varchar("icon", 50).nullable()
}
```

### Items (deux tables séparées — sémantique différente)
```kotlin
// Demandé par l'organisateur
object ItemRequests : IntIdTable("item_requests") {
    val eventId      = reference("event_id", Events)
    val label        = varchar("label", 200)
    val quantity     = integer("quantity").default(1)
    val categoryId   = reference("category_id", ItemCategories).nullable()
    val assignedToId = reference("assigned_to", Users).nullable()
    val isFulfilled  = bool("is_fulfilled").default(false)
}

// Déclaré spontanément par un invité
object ItemsBrought : IntIdTable("items_brought") {
    val eventId    = reference("event_id", Events)
    val userId     = reference("user_id", Users)
    val label      = varchar("label", 200)
    val quantity   = integer("quantity").default(1)
    val categoryId = reference("category_id", ItemCategories).nullable()
}
```

### Covoiturage
```kotlin
object CarpoolOffers : IntIdTable("carpool_offers") {
    val eventId        = reference("event_id", Events)
    val driverId       = reference("driver_id", Users)
    val seatsAvailable = integer("seats_available")
    val departurePoint = varchar("departure_point", 300).nullable()
    val departureTime  = datetime("departure_time").nullable()
    val notes          = text("notes").nullable()
}

// Plusieurs passagers par offre
object CarpoolPassengers : IntIdTable("carpool_passengers") {
    val offerId     = reference("offer_id", CarpoolOffers)
    val passengerId = reference("passenger_id", Users)
    val status      = enumerationByName("status", 20, CarpoolRequestStatus::class)
    val pickupPoint = varchar("pickup_point", 300).nullable()
}

enum class CarpoolRequestStatus { OPEN, MATCHED, CANCELLED }
```

### Chat (temps réel via WebSockets)
```kotlin
// Accès : organisateur + invités ACCEPTED uniquement
// Chat de groupe unique par événement
object ChatMessages : IntIdTable("chat_messages") {
    val eventId   = reference("event_id", Events)
    val senderId  = reference("sender_id", Users)
    val content   = text("content")
    val replyToId = reference("reply_to_id", ChatMessages).nullable()
    val editedAt  = datetime("edited_at").nullable()
    val deletedAt = datetime("deleted_at").nullable() // soft delete
    val createdAt = datetime("created_at")
}

object ChatReactions : IntIdTable("chat_reactions") {
    val messageId = reference("message_id", ChatMessages)
    val userId    = reference("user_id", Users)
    val emoji     = varchar("emoji", 10)
    init { uniqueIndex(messageId, userId, emoji) }
}
```

### Pot commun (prévu pour phase future)
```kotlin
object CommonFunds : IntIdTable("common_funds") {
    val eventId      = reference("event_id", Events)
    val targetAmount = decimal("target_amount", 10, 2).nullable()
    val description  = text("description").nullable()
}

object CommonFundContributions : IntIdTable("common_fund_contributions") {
    val fundId = reference("fund_id", CommonFunds)
    val userId = reference("user_id", Users)
    val amount = decimal("amount", 10, 2)
    val paidAt = datetime("paid_at").nullable()
    val notes  = text("notes").nullable()
}
```

### Playlist (prévu pour phase future)
```kotlin
object Playlists : IntIdTable("playlists") {
    val eventId = reference("event_id", Events)
    val name    = varchar("name", 200).default("Playlist")
}

object PlaylistTracks : IntIdTable("playlist_tracks") {
    val playlistId  = reference("playlist_id", Playlists)
    val addedById   = reference("added_by", Users)
    val title       = varchar("title", 200)
    val artist      = varchar("artist", 200).nullable()
    val spotifyUri  = varchar("spotify_uri", 200).nullable()
    val addedAt     = datetime("added_at")
    val votes       = integer("votes").default(0)
}
```

### Index importants
```sql
CREATE INDEX idx_chat_messages_event_created ON chat_messages(event_id, created_at);
CREATE INDEX idx_chat_reactions_message ON chat_reactions(message_id);
```

## Docker Compose (backend/)
```yaml
services:
  db:
    image: postgres:16
    container_name: partyplanner-db
    environment:
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev
      POSTGRES_DB: partyplanner
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev -d partyplanner"]
      interval: 5s
      timeout: 5s
      retries: 5
  adminer:
    image: adminer
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy
volumes:
  postgres_data:
```

## Variables d'environnement backend (.env — ne pas commiter)
```
DATABASE_URL=jdbc:postgresql://localhost:5432/partyplanner
DATABASE_USER=dev
DATABASE_PASSWORD=dev
JWT_SECRET=changeme
```

## Roadmap par phases
1. **Phase 1** — Fondations : setup KMM, auth JWT, login/register, SQLDelight schéma
2. **Phase 2** — Core événements : CRUD events, calendrier page d'accueil, intégration agenda natif
3. **Phase 3** — Invitations : lecture contacts, SMS (Twilio), mail, statuts
4. **Phase 4** — Fonctionnalités event : items, covoiturage, chat WebSocket, push notifications
5. **Phase 5** — Store : Google Play + TestFlight via Codemagic, RGPD

## Décisions techniques actées
- Exposed en mode DAO (pas DSL) — plus proche de JPA que le dev connaît
- Soft delete sur ChatMessages (deletedAt) pour cohérence des threads
- ItemRequest et ItemBrought séparés (sémantique différente, sealed class côté domain)
- CarpoolPassengers en table de liaison (plusieurs passagers par offre)
- ItemCategories auto-référent limité à 2 niveaux max
- WebSocket Ktor natif pour le chat (pas de lib tierce)
- Koin pour DI (Hilt = Android only, incompatible KMM)
- Decompose pour la navigation (fonctionne en commonMain)

## Commandes utiles
```bash
# Build
./gradlew build

# Run backend
./gradlew :backend:run

# Docker
docker compose -f backend/docker-compose.yml up -d

# Tests
./gradlew test
```
