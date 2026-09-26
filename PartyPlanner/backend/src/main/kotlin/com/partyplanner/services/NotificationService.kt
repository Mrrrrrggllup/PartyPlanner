package com.partyplanner.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.partyplanner.db.tables.DeviceTokenEntity
import com.partyplanner.db.tables.DeviceTokens
import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.db.tables.Invitations
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.db.tables.Users
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class NotificationService(private val serviceAccountPath: String) {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)
    private val scope  = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // userId -> eventId they are currently viewing (in-memory, cleared on server restart)
    private val viewingEvent = ConcurrentHashMap<Int, Int>()

    init {
        if (serviceAccountPath.isNotBlank()) {
            runCatching {
                val stream  = File(serviceAccountPath).inputStream()
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .build()
                if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options)
                logger.info("Firebase Admin SDK initialized")
            }.onFailure { logger.warn("FCM disabled — could not initialize Firebase: ${it.message}") }
        } else {
            logger.info("FCM disabled — fcm.serviceAccountPath not configured")
        }
    }

    /** Signal that userId is currently viewing eventId. Called on event screen open / app foreground. */
    fun enterEvent(userId: Int, eventId: Int) {
        viewingEvent[userId] = eventId
    }

    /** Signal that userId left the event screen. Called on navigate away / app background. */
    fun leaveEvent(userId: Int) {
        viewingEvent.remove(userId)
    }

    /** Reset notification flag. Called when app comes to foreground. */
    suspend fun markActive(userId: Int) {
        withContext(Dispatchers.IO) {
            transaction {
                Users.update({ Users.id eq userId }) { it[Users.notificationPending] = false }
            }
        }
    }

    /** Register or update the FCM token for a device. */
    suspend fun registerToken(userId: Int, token: String, platform: String) = withContext(Dispatchers.IO) {
        transaction {
            val existing = DeviceTokenEntity.find { DeviceTokens.token eq token }.firstOrNull()
            val now      = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val user     = UserEntity.findById(userId)!!
            if (existing != null) {
                existing.user      = user
                existing.updatedAt = now
            } else {
                DeviceTokenEntity.new {
                    this.user      = user
                    this.token     = token
                    this.platform  = platform
                    this.updatedAt = now
                }
            }
        }
    }

    /**
     * Notify all ACCEPTED participants + owner of an event, excluding the actor.
     * Sends at most one push per user until they open the app.
     */
    suspend fun notifyParticipants(eventId: Int, actorUserId: Int) {
        val recipientIds = withContext(Dispatchers.IO) {
            transaction {
                val event    = EventEntity.findById(eventId) ?: return@transaction emptyList()
                val ownerId  = event.owner.id.value
                val guestIds = InvitationEntity.find {
                    (Invitations.eventId eq eventId) and (Invitations.status eq InvitationStatus.ACCEPTED)
                }.map { it.user.id.value }
                (listOf(ownerId) + guestIds).filter { it != actorUserId }.distinct()
            }
        }
        notifyList(recipientIds, eventId)
    }

    /**
     * Notify a specific list of users (e.g. invitation to a new event).
     * Excludes actorUserId.
     */
    suspend fun notifyUsers(recipientIds: List<Int>, actorUserId: Int, eventId: Int? = null) {
        notifyList(recipientIds.filter { it != actorUserId }.distinct(), eventId)
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private fun isViewingEvent(userId: Int, eventId: Int): Boolean =
        viewingEvent[userId] == eventId

    private suspend fun notifyList(recipientIds: List<Int>, eventId: Int? = null) {
        if (recipientIds.isEmpty()) return

        val eventTitle = eventId?.let {
            withContext(Dispatchers.IO) {
                transaction { EventEntity.findById(it)?.title }
            }
        }

        // Skip users currently viewing this specific event — they can see the update in real time.
        // Still send to users who are in the app but on a different screen.
        // Atomically set notificationPending = true only if it was false (prevents burst duplicates).
        val usersToNotify = recipientIds
            .filter { userId -> eventId == null || !isViewingEvent(userId, eventId) }
            .mapNotNull { userId ->
            val updated = withContext(Dispatchers.IO) {
                transaction {
                    Users.update({
                        (Users.id eq userId) and (Users.notificationPending eq false)
                    }) { it[Users.notificationPending] = true }
                }
            }
            if (updated > 0) userId else null
        }

        if (usersToNotify.isEmpty()) return

        val tokens = withContext(Dispatchers.IO) {
            transaction {
                DeviceTokenEntity.find {
                    DeviceTokens.userId inList usersToNotify.map { EntityID(it, Users) }
                }.map { it.token }
            }
        }

        if (tokens.isNotEmpty()) {
            scope.launch { sendFcm(tokens, eventId, eventTitle) }
        }
    }

    private fun sendFcm(tokens: List<String>, eventId: Int? = null, eventTitle: String? = null) {
        if (FirebaseApp.getApps().isEmpty()) return
        runCatching {
            val msgBuilder = MulticastMessage.builder()
                .setNotification(
                    Notification.builder()
                        .setTitle(eventTitle ?: "PartyPlanner")
                        .setBody("Vous avez de nouvelles activités")
                        .build()
                )
            if (eventId != null) msgBuilder.putData("eventId", eventId.toString())
            val msg = msgBuilder.addAllTokens(tokens).build()
            val result = FirebaseMessaging.getInstance().sendEachForMulticast(msg)
            logger.info("FCM sent: ${result.successCount} ok, ${result.failureCount} failed")
        }.onFailure { logger.error("FCM send error: $it") }
    }
}
