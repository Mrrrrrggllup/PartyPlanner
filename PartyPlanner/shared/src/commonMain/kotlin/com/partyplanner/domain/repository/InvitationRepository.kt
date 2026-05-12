package com.partyplanner.domain.repository

import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.InviteInfo
import com.partyplanner.domain.model.UserSuggestion

interface InvitationRepository {
    suspend fun getInviteInfo(token: String): Result<InviteInfo>
    suspend fun rsvp(token: String, status: InvitationStatus): Result<InviteInfo>
    suspend fun getEventInvitations(eventId: Int): Result<List<Invitation>>
    suspend fun inviteByEmail(eventId: Int, email: String): Result<Invitation>
    suspend fun inviteByUserId(eventId: Int, userId: Int): Result<Invitation>
    suspend fun getInviteSuggestions(eventId: Int): Result<List<UserSuggestion>>
}
