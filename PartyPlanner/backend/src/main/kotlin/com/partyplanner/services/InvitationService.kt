package com.partyplanner.services

import com.partyplanner.db.tables.CarpoolOfferEntity
import com.partyplanner.db.tables.CarpoolOffers
import com.partyplanner.db.tables.CarpoolPassengerEntity
import com.partyplanner.db.tables.CarpoolPassengers
import com.partyplanner.db.tables.CarpoolPassengerStatus
import com.partyplanner.db.tables.Contributions
import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.Events
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.InvitationStatus
import com.partyplanner.db.tables.Invitations
import com.partyplanner.db.tables.ItemRequestEntity
import com.partyplanner.db.tables.ItemRequests
import com.partyplanner.db.tables.ItemsBrought
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.db.tables.Users
import com.partyplanner.dto.InvitationResponse
import com.partyplanner.dto.InviteInfoResponse
import com.partyplanner.dto.UserSuggestionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.transaction

class InvitationService(private val notificationService: NotificationService) {

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

        val (result, ownerId) = transaction {
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

            if (status == InvitationStatus.DECLINED) {
                ItemsBrought.deleteWhere {
                    (ItemsBrought.eventId eq event.id) and (ItemsBrought.userId eq user.id)
                }
                Contributions.deleteWhere {
                    (Contributions.eventId eq event.id) and
                    ((Contributions.addedById eq user.id) or (Contributions.linkedUserId eq user.id))
                }
            }

            Pair(
                InviteInfoResponse(
                    eventId       = event.id.value,
                    title         = event.title,
                    startDate     = event.startDate,
                    endDate       = event.endDate,
                    location      = event.location,
                    organizerName = event.owner.displayName,
                    isOwner       = false,
                    currentStatus = status.name,
                ),
                event.owner.id.value,
            )
        }

        // Notify the event owner when someone accepts or maybe-s
        if (status == InvitationStatus.ACCEPTED || status == InvitationStatus.MAYBE) {
            notificationService.notifyUsers(listOf(ownerId), userId, result.eventId)
        }
        result
    }

    suspend fun getEventInvitations(eventId: Int, userId: Int): List<InvitationResponse> = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
            val isOwner   = event.owner.id.value == userId
            val isInvited = InvitationEntity.find {
                (Invitations.eventId eq event.id) and (Invitations.userId eq userId)
            }.firstOrNull() != null
            require(isOwner || isInvited) { "Accès refusé" }
            InvitationEntity.find { Invitations.eventId eq event.id }
                .map { it.toResponse() }
        }
    }

    suspend fun getInviteSuggestions(eventId: Int, ownerId: Int): List<UserSuggestionResponse> = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
            require(event.owner.id.value == ownerId) { "Accès refusé" }

            val alreadyInvitedIds = InvitationEntity.find { Invitations.eventId eq event.id }
                .map { it.user.id.value }.toSet()

            val ownerEventIds = EventEntity.find { Events.ownerId eq ownerId }.map { it.id }

            InvitationEntity.find { Invitations.eventId inList ownerEventIds }
                .map { it.user }
                .filter { it.id.value !in alreadyInvitedIds && it.id.value != ownerId }
                .distinctBy { it.id.value }
                .sortedBy { it.displayName }
                .map { UserSuggestionResponse(it.id.value, it.displayName) }
        }
    }

    suspend fun inviteByUserId(eventId: Int, ownerId: Int, targetUserId: Int): InvitationResponse = withContext(Dispatchers.IO) {
        val result = transaction {
            val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
            require(event.owner.id.value == ownerId) { "Accès refusé" }
            require(targetUserId != ownerId) { "Vous ne pouvez pas vous inviter vous-même" }

            val invitedUser = UserEntity.findById(targetUserId) ?: error("Utilisateur introuvable")

            val existing = InvitationEntity.find {
                (Invitations.eventId eq event.id) and (Invitations.userId eq invitedUser.id)
            }.firstOrNull()
            require(existing == null) { "Utilisateur déjà invité" }

            InvitationEntity.new {
                this.event  = event
                this.user   = invitedUser
                this.status = InvitationStatus.PENDING
            }.toResponse()
        }
        notificationService.notifyUsers(listOf(result.userId), ownerId, eventId)
        result
    }

    suspend fun inviteByEmail(eventId: Int, ownerId: Int, email: String): InvitationResponse = withContext(Dispatchers.IO) {
        val result = transaction {
            val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
            require(event.owner.id.value == ownerId) { "Accès refusé" }

            val invitedUser = UserEntity.find { Users.email eq email }.firstOrNull()
                ?: error("Aucun utilisateur avec cet email")

            require(invitedUser.id.value != ownerId) { "Vous ne pouvez pas vous inviter vous-même" }

            val existing = InvitationEntity.find {
                (Invitations.eventId eq event.id) and (Invitations.userId eq invitedUser.id)
            }.firstOrNull()

            require(existing == null) { "Utilisateur déjà invité" }

            InvitationEntity.new {
                this.event  = event
                this.user   = invitedUser
                this.status = InvitationStatus.PENDING
            }.toResponse()
        }
        notificationService.notifyUsers(listOf(result.userId), ownerId, eventId)
        result
    }

    suspend fun removeGuest(eventId: Int, invitationId: Int, ownerId: Int): Unit = withContext(Dispatchers.IO) {
        transaction {
            val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
            require(event.owner.id.value == ownerId) { "Accès refusé" }

            val invitation = InvitationEntity.findById(invitationId) ?: error("Invitation introuvable")
            require(invitation.event.id.value == eventId) { "Invitation introuvable" }

            val userId = invitation.user.id.value

            // Remove all items brought by this user for this event
            ItemsBrought.deleteWhere {
                (ItemsBrought.eventId eq eventId) and (ItemsBrought.userId eq userId)
            }

            // Unmark item requests fulfilled by this user, putting them back as needed
            ItemRequestEntity.find { ItemRequests.eventId eq event.id }
                .filter { it.isFulfilled && it.assignedTo?.id?.value == userId }
                .forEach { req ->
                    req.isFulfilled = false
                    req.assignedTo  = null
                }

            // Cancel all carpool passenger entries for this user across this event's offers
            val offerIds = CarpoolOfferEntity.find { CarpoolOffers.eventId eq eventId }.map { it.id }
            if (offerIds.isNotEmpty()) {
                CarpoolPassengerEntity.find {
                    (CarpoolPassengers.offerId inList offerIds) and
                    (CarpoolPassengers.passengerId eq userId)
                }.forEach { it.status = CarpoolPassengerStatus.CANCELLED }
            }

            // Delete carpool offers where this user is the driver
            CarpoolOfferEntity.find {
                (CarpoolOffers.eventId eq eventId) and (CarpoolOffers.driverId eq userId)
            }.forEach { offer ->
                CarpoolPassengerEntity.find { CarpoolPassengers.offerId eq offer.id.value }.forEach { it.delete() }
                offer.delete()
            }

            // Delete all contributions added by or linked to this user for this event
            Contributions.deleteWhere {
                (Contributions.eventId eq eventId) and
                ((Contributions.addedById eq userId) or (Contributions.linkedUserId eq userId))
            }

            invitation.delete()
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
