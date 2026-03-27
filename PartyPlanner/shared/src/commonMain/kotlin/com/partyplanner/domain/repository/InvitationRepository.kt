package com.partyplanner.domain.repository

import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.InviteInfo

interface InvitationRepository {
    suspend fun getInviteInfo(token: String): Result<InviteInfo>
    suspend fun rsvp(token: String, status: InvitationStatus): Result<InviteInfo>
    suspend fun getEventInvitations(eventId: Int): Result<List<Invitation>>
}
