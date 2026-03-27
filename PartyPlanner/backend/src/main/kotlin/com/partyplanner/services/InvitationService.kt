package com.partyplanner.services

import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.Events
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.db.tables.Invitations
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.dto.InvitationResponse
import com.partyplanner.dto.InviteInfoResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class InvitationService {

    suspend fun getInviteInfo(token: String, userId: Int): InviteInfoResponse = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.find { Events.inviteToken eq token }.firstOrNull()
                ?: error("Invitation introuvable")
            val isOwner = event.owner.id.value == userId
            val existing = InvitationEntity.find {
                (Invitations.eventId eq event.id) and (Invitations.userId eq userId)
            }.firstOrNull()
            InviteInfoResponse(
                eventId       = event.id.value,
                title         = event.title,
                startDate     = event.startDate,
                endDate       = event.endDate,
                location      = event.location,
                organizerName = event.owner.displayName,
                isOwner       = isOwner,
                currentStatus = existing?.status?.name,
            )
        }
    }

    suspend fun rsvp(token: String, userId: Int, statusStr: String): InviteInfoResponse = withContext(Dispatchers.IO) {
        val status = runCatching { InvitationStatus.valueOf(statusStr) }.getOrNull()
            ?: error("Statut invalide : $statusStr")

        transaction {
            val event = EventEntity.find { Events.inviteToken eq token }.firstOrNull()
                ?: error("Invitation introuvable")
            require(event.owner.id.value != userId) { "L'organisateur ne peut pas s'inviter" }
            val user = UserEntity.findById(userId) ?: error("Utilisateur introuvable")
            val now  = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            val existing = InvitationEntity.find {
                (Invitations.eventId eq event.id) and (Invitations.userId eq userId)
            }.firstOrNull()

            if (existing != null) {
                existing.status      = status
                existing.respondedAt = now
            } else {
                InvitationEntity.new {
                    this.event       = event
                    this.user        = user
                    this.status      = status
                    this.respondedAt = now
                }
            }

            InviteInfoResponse(
                eventId       = event.id.value,
                title         = event.title,
                startDate     = event.startDate,
                endDate       = event.endDate,
                location      = event.location,
                organizerName = event.owner.displayName,
                isOwner       = false,
                currentStatus = status.name,
            )
        }
    }

    suspend fun getEventInvitations(eventId: Int, userId: Int): List<InvitationResponse> = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
            require(event.owner.id.value == userId) { "Accès refusé" }
            InvitationEntity.find { Invitations.eventId eq event.id }
                .map { it.toResponse() }
        }
    }

    private fun InvitationEntity.toResponse() = InvitationResponse(
        id              = id.value,
        userId          = user.id.value,
        userDisplayName = user.displayName,
        status          = status.name,
        respondedAt     = respondedAt,
    )
}
