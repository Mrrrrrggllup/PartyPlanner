package com.partyplanner.services

import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.Events
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.dto.CreateEventRequest
import com.partyplanner.dto.EventResponse
import com.partyplanner.dto.UpdateEventRequest
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.Invitations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class EventService {
    private val logger = LoggerFactory.getLogger(EventService::class.java)

    suspend fun getEventsForUser(userId: Int): List<EventResponse> = withContext(Dispatchers.IO) {
        transaction {
            val ownedEvents = EventEntity.find { Events.ownerId eq userId }.toList()

            val invitedEventIds = InvitationEntity
                .find {
                    (Invitations.userId eq userId) and
                    (Invitations.status inList listOf(InvitationStatus.ACCEPTED, InvitationStatus.MAYBE))
                }
                .map { it.event.id.value }

            val invitedEvents = invitedEventIds.mapNotNull { EventEntity.findById(it) }

            (ownedEvents + invitedEvents)
                .distinctBy { it.id.value }
                .sortedBy { it.startDate }
                .map { it.toResponse() }
        }
    }

    suspend fun getEvent(id: Int, userId: Int): EventResponse = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(id) ?: error("Event not found")
            val isOwner = event.owner.id.value == userId
            val isInvited = InvitationEntity.find {
                (Invitations.eventId eq id) and (Invitations.userId eq userId)
            }.firstOrNull() != null
            require(isOwner || isInvited) { "Access denied" }
            event.toResponse()
        }
    }

    suspend fun createEvent(userId: Int, request: CreateEventRequest): EventResponse = withContext(Dispatchers.IO) {
        logger.info("Creating event for userId=$userId title=${request.title}")
        transaction {
            val owner = UserEntity.findById(userId) ?: error("User not found")
            EventEntity.new {
                title       = request.title
                description = request.description
                location    = request.location
                startDate   = request.startDate
                endDate     = request.endDate
                this.owner  = owner
                inviteToken = java.util.UUID.randomUUID().toString()
                createdAt   = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }.toResponse()
        }
    }

    suspend fun updateEvent(id: Int, userId: Int, request: UpdateEventRequest): EventResponse = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(id) ?: error("Event not found")
            require(event.owner.id.value == userId) { "Access denied" }
            request.title?.let       { event.title = it }
            request.description?.let { event.description = it }
            request.location?.let    { event.location = it }
            request.startDate?.let   { event.startDate = it }
            request.endDate?.let     { event.endDate = it }
            event.toResponse()
        }
    }

    suspend fun deleteEvent(id: Int, userId: Int): Unit = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(id) ?: error("Event not found")
            require(event.owner.id.value == userId) { "Access denied" }
            event.delete()
        }
    }

    private fun EventEntity.toResponse() = EventResponse(
        id          = id.value,
        title       = title,
        description = description,
        location    = location,
        startDate   = startDate,
        endDate     = endDate,
        ownerId     = owner.id.value,
        inviteToken = inviteToken,
        createdAt   = createdAt,
    )
}
