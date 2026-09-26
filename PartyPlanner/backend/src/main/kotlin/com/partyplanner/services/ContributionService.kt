package com.partyplanner.services

import com.partyplanner.db.tables.ContributionEntity
import com.partyplanner.db.tables.Contributions
import com.partyplanner.db.tables.EventEntity
import com.partyplanner.db.tables.InvitationEntity
import com.partyplanner.db.tables.Invitations
import com.partyplanner.db.tables.UserEntity
import com.partyplanner.dto.AddContributionDto
import com.partyplanner.dto.ContributionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal

class ContributionService {

    private fun checkAccess(eventId: Int, userId: Int) {
        val event = EventEntity.findById(eventId) ?: error("Événement introuvable")
        val isOwner   = event.owner.id.value == userId
        val isInvited = InvitationEntity.find {
            (Invitations.eventId eq eventId) and (Invitations.userId eq userId)
        }.firstOrNull() != null
        require(isOwner || isInvited) { "Accès refusé" }
    }

    suspend fun getContributions(eventId: Int, userId: Int): List<ContributionResponse> =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                ContributionEntity.find { Contributions.eventId eq eventId }
                    .sortedByDescending { it.createdAt }
                    .map { it.toResponse() }
            }
        }

    suspend fun addContribution(eventId: Int, userId: Int, dto: AddContributionDto): ContributionResponse =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                require(dto.amount > 0) { "Le montant doit être supérieur à 0" }
                require(dto.label.isNotBlank()) { "Le libellé ne peut pas être vide" }

                val event      = EventEntity.findById(eventId)!!
                val addedBy    = UserEntity.findById(userId) ?: error("Utilisateur introuvable")
                val linkedUser = UserEntity.findById(dto.linkedUserId) ?: error("Invité introuvable")

                // Verify linked user is a participant of this event
                val isLinkedOwner   = event.owner.id.value == dto.linkedUserId
                val isLinkedInvited = InvitationEntity.find {
                    (Invitations.eventId eq eventId) and (Invitations.userId eq dto.linkedUserId)
                }.firstOrNull() != null
                require(isLinkedOwner || isLinkedInvited) { "L'invité lié ne participe pas à cet événement" }

                ContributionEntity.new {
                    this.event      = event
                    this.addedBy    = addedBy
                    this.linkedUser = linkedUser
                    this.label      = dto.label.trim()
                    this.amount     = BigDecimal(dto.amount)
                    this.createdAt  = Clock.System.now().toLocalDateTime(TimeZone.UTC)
                }.toResponse()
            }
        }

    suspend fun deleteContribution(eventId: Int, contributionId: Int, userId: Int): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                val contribution = ContributionEntity.findById(contributionId)
                    ?: error("Contribution introuvable")
                require(contribution.event.id.value == eventId) { "Contribution introuvable" }
                require(contribution.addedBy.id.value == userId) { "Seul la personne qui a ajouté cette contribution peut la supprimer" }
                contribution.delete()
            }
        }

    private fun ContributionEntity.toResponse() = ContributionResponse(
        id             = id.value,
        addedById      = addedBy.id.value,
        linkedUserId   = linkedUser.id.value,
        linkedUserName = linkedUser.displayName,
        label          = label,
        amount         = amount.toDouble(),
        createdAt      = createdAt,
    )
}
