package com.partyplanner.services

import com.partyplanner.db.tables.ChatMessageEntity
import com.partyplanner.db.tables.ChatMessages
import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.Invitations
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.dto.ChatMessageResponse
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

class ChatService {

    // eventId -> active WS sessions
    private val rooms = ConcurrentHashMap<Int, MutableSet<DefaultWebSocketSession>>()

    fun addSession(eventId: Int, session: DefaultWebSocketSession) {
        rooms.getOrPut(eventId) { Collections.synchronizedSet(mutableSetOf()) }.add(session)
    }

    fun removeSession(eventId: Int, session: DefaultWebSocketSession) {
        rooms[eventId]?.remove(session)
    }

    suspend fun broadcast(eventId: Int, message: ChatMessageResponse) {
        val frame = Frame.Text(Json.encodeToString(message))
        rooms[eventId]?.toList()?.forEach { session ->
            runCatching { session.send(frame) }
        }
    }

    suspend fun checkAccess(eventId: Int, userId: Int): Boolean = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(eventId) ?: return@transaction false
            if (event.ownerId.value == userId) return@transaction true
            InvitationEntity.find {
                (Invitations.eventId eq eventId) and
                (Invitations.invitedUserId eq userId) and
                (Invitations.status eq InvitationStatus.ACCEPTED)
            }.firstOrNull() != null
        }
    }

    suspend fun getHistory(eventId: Int, limit: Int = 50): List<ChatMessageResponse> =
        withContext(Dispatchers.IO) {
            transaction {
                ChatMessageEntity.find { ChatMessages.eventId eq eventId }
                    .orderBy(ChatMessages.createdAt to SortOrder.ASC)
                    .limit(limit)
                    .map { it.toResponse() }
            }
        }

    suspend fun saveMessage(eventId: Int, senderId: Int, content: String): ChatMessageResponse =
        withContext(Dispatchers.IO) {
            transaction {
                val event  = EventEntity.findById(eventId)  ?: error("Event not found")
                val sender = UserEntity.findById(senderId)   ?: error("User not found")
                ChatMessageEntity.new {
                    this.event    = event
                    this.sender   = sender
                    this.content  = content
                    this.createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                }.toResponse()
            }
        }

    private fun ChatMessageEntity.toResponse() = ChatMessageResponse(
        id         = id.value,
        senderId   = sender.id.value,
        senderName = sender.displayName,
        content    = content,
        createdAt  = createdAt,
    )
}
