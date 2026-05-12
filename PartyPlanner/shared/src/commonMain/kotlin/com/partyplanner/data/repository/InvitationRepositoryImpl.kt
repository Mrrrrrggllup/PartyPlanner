package com.partyplanner.data.repository

import com.partyplanner.data.remote.InvitationApi
import com.partyplanner.data.remote.dto.InvitationResponse
import com.partyplanner.data.remote.dto.InviteInfoResponse
import com.partyplanner.data.remote.dto.UserSuggestionResponse
import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.InviteInfo
import com.partyplanner.domain.model.UserSuggestion
import com.partyplanner.domain.repository.InvitationRepository

class InvitationRepositoryImpl(private val api: InvitationApi) : InvitationRepository {

    override suspend fun getInviteInfo(token: String): Result<InviteInfo> = runCatching {
        api.getInviteInfo(token).toDomain()
    }

    override suspend fun rsvp(token: String, status: InvitationStatus): Result<InviteInfo> = runCatching {
        api.rsvp(token, status.name).toDomain()
    }

    override suspend fun getEventInvitations(eventId: Int): Result<List<Invitation>> = runCatching {
        api.getEventInvitations(eventId).map { it.toDomain() }
    }

    override suspend fun inviteByEmail(eventId: Int, email: String): Result<Invitation> = runCatching {
        api.inviteByEmail(eventId, email).toDomain()
    }

    override suspend fun inviteByUserId(eventId: Int, userId: Int): Result<Invitation> = runCatching {
        api.inviteByUserId(eventId, userId).toDomain()
    }

    override suspend fun getInviteSuggestions(eventId: Int): Result<List<UserSuggestion>> = runCatching {
        api.getInviteSuggestions(eventId).map { it.toDomain() }
    }

    private fun UserSuggestionResponse.toDomain() = UserSuggestion(id = id, displayName = displayName)

    private fun InviteInfoResponse.toDomain() = InviteInfo(
        eventId       = eventId,
        title         = title,
        startDate     = startDate,
        endDate       = endDate,
        location      = location,
        organizerName = organizerName,
        isOwner       = isOwner,
        currentStatus = currentStatus?.let { runCatching { InvitationStatus.valueOf(it) }.getOrNull() },
    )

    private fun InvitationResponse.toDomain() = Invitation(
        id              = id,
        userId          = userId,
        userDisplayName = userDisplayName,
        status          = runCatching { InvitationStatus.valueOf(status) }.getOrElse { InvitationStatus.PENDING },
        respondedAt     = respondedAt,
    )
}
