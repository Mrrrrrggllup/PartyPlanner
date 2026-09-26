# Bloc métier — Chat

## Rôle
Messagerie de groupe en temps réel par événement. Chaque événement dispose d'un salon de chat unique accessible uniquement aux participants confirmés (ACCEPTED) et à l'organisateur.

## Périmètre fonctionnel
- Connexion WebSocket persistante (par session EventDetail)
- Envoi et réception de messages en temps réel
- Historique des 50 derniers messages chargé à la connexion
- Auto-scroll sur nouveau message
- Bulles de chat différenciées (moi vs autres)
- Badge onglet "Chat" : messages non lus (reset à l'ouverture de l'onglet)
- Reconnexion automatique avec backoff exponentiel (2s → 4s → … → 30s max, reset à 2s après connexion réussie)
- Ping WebSocket toutes les 30s (`pingIntervalMillis`) pour détecter les connexions mortes
- Suivi de présence : `enterEvent` / `leaveEvent` déclenché par les hooks `doOnStart` / `doOnStop` du composant

## Fichiers clés

### Shared (domain)
- `shared/domain/model/ChatMessage.kt`
- `shared/domain/repository/ChatRepository.kt` — interface avec `SharedFlow<ChatMessage>`

### Shared (data)
- `shared/data/remote/ChatApi.kt` — connexion WS, `send()` via session stockée
- `shared/data/remote/dto/ChatDtos.kt`
- `shared/data/repository/ChatRepositoryImpl.kt` — `SharedFlow`, factory Koin (instance fraîche par EventDetail)

### Shared (présentation)
- `shared/presentation/event/DefaultEventDetailComponent.kt` — collecte le flow WS + maintient la connexion en arrière-plan

### UI
- `ChatTabLayout` dans `composeApp/ui/event/EventDetailScreen.kt` — layout dédié (hors du LazyColumn principal)
- `ChatBubble` — bulles arrondies, gradient pour moi / surfaceVariant pour les autres
- Input bar fixe au-dessus de la tab bar

### Backend
- `backend/routes/ChatRoutes.kt` — `ws("/events/{id}/chat?token=...")`
- `backend/services/ChatService.kt` — gestionnaire de salles par eventId (ConcurrentHashMap thread-safe), broadcast
- `backend/db/tables/Chat.kt` — `chat_messages` + `chat_reactions`
- `backend/plugins/Sockets.kt` — configuration WebSocket Ktor (ping 30s, timeout 60s)

## Schéma de données

```kotlin
object ChatMessages : IntIdTable("chat_messages") {
    val eventId   = reference("event_id", Events)
    val senderId  = reference("sender_id", Users)
    val content   = text("content")
    val replyToId = reference("reply_to_id", ChatMessages).nullable()
    val editedAt  = datetime("edited_at").nullable()
    val deletedAt = datetime("deleted_at").nullable()   // soft delete
    val createdAt = datetime("created_at")
}

object ChatReactions : IntIdTable("chat_reactions") {
    val messageId = reference("message_id", ChatMessages)
    val userId    = reference("user_id", Users)
    val emoji     = varchar("emoji", 10)
    init { uniqueIndex(messageId, userId, emoji) }
}

// Index important pour les performances
// CREATE INDEX idx_chat_messages_event_created ON chat_messages(event_id, created_at)
```

## Protocole WebSocket

```
Connexion : ws://[serveur]/events/{id}/chat?token={jwt}
  → Authentification JWT
  → Vérification accès (ACCEPTED ou owner)
  → Envoi historique (50 derniers messages)
  → Boucle lecture/broadcast
  → Ping 30s / timeout 60s
```

## Règles métier
- Seuls les participants ACCEPTED et l'organisateur peuvent accéder au chat
- L'authentification WS se fait via query param `?token=...` (pas de header, limitation WebSocket)
- Le client Android utilise `ktor-client-okhttp` (pas `ktor-client-android` qui ne supporte pas les WS)
- `ChatRepository` est créé comme instance fraîche par EventDetail (via factory Koin) pour éviter les conflits entre événements
- Soft delete sur `ChatMessages` (`deletedAt`) pour préserver la cohérence des threads de réponse
- Le badge chat affiche le nombre de messages **totaux** (pas uniquement les non lus) dans la StatsRow
- Le badge de l'onglet affiche les messages **non lus** et se remet à 0 à l'ouverture
- `imePadding()` est sur la Column de `ChatTabLayout` uniquement — pas sur la Column externe de `EventDetailScreen` (sinon les insets IME sont consommés et `imeVisible` retourne toujours false dans les descendants)
- `doOnStop` utilise un scope IO indépendant (`CoroutineScope(Dispatchers.IO)`) pour `leaveEvent()` — le scope du composant est annulé synchroniquement juste après `doOnStop`

## Dépendances importantes
```kotlin
// build.gradle.kts (shared)
implementation("io.ktor:ktor-client-websockets:$ktor_version")

// build.gradle.kts (composeApp - androidMain)
implementation("io.ktor:ktor-client-okhttp:$ktor_version")  // requis pour WS Android
```
