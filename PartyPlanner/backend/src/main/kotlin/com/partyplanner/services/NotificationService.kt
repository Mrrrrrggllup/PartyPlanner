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

    // userId -> last seen epoch millis (in-memory, no DB cost per request)
    private val activeUsers = ConcurrentHashMap<Int, Long>()

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

    /** Mark a user as currently active (called on WS connect). No DB cost. */
    fun touch(userId: Int) {
        activeUsers[userId] = System.currentTimeMillis()
    }

    /** Reset notification flag and mark active. Called when app comes to foreground. */
    suspend fun markActive(userId: Int) {
        touch(userId)
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
        notifyList(recipientIds)
    }

    /**
     * Notify a specific list of users (e.g. invitation to a new event).
     * Excludes actorUserId.
     */
    suspend fun notifyUsers(recipientIds: List<Int>, actorUserId: Int) {
        notifyList(recipientIds.filter { it != actorUserId }.distinct())
    }

    // ── internals ──────────────────────────────────────────────────────────────

    private fun isOnline(userId: Int): Boolean {
        val last = activeUsers[userId] ?: return false
        return (System.currentTimeMillis() - last) < 90_000L
    }

    private suspend fun notifyList(recipientIds: List<Int>) {
        if (recipientIds.isEmpty()) return

        // Atomically set notificationPending = true only if it was false.
        // Returns 1 if this is the first notification for this user (send FCM), 0 if already pending (skip).
        val usersToNotify = recipientIds.filter { !isOnline(it) }.mapNotNull { userId ->
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
            scope.launch { sendFcm(tokens) }
        }
    }

    private fun sendFcm(tokens: List<String>) {
        if (FirebaseApp.getApps().isEmpty()) return
        runCatching {
            val msg = MulticastMessage.builder()
                .setNotification(
                    Notification.builder()
                        .setTitle("PartyPlanner")
                        .setBody("Vous avez de nouvelles activités sur un événement")
                        .build()
                )
                .addAllTokens(tokens)
                .build()
            val result = FirebaseMessaging.getInstance().sendEachForMulticast(msg)
            logger.info("FCM sent: ${result.successCount} ok, ${result.failureCount} failed")
        }.onFailure { logger.error("FCM send error: $it") }
    }
}
