package com.partyplanner.fake

import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.model.InviteInfo
import com.partyplanner.domain.model.UserSuggestion
import com.partyplanner.domain.repository.InvitationRepository

internal class FakeInvitationRepository(
    private val getInviteInfoResult: Result<InviteInfo> = Result.success(testInviteInfo),
    private val rsvpResult: Result<InviteInfo> = Result.success(testInviteInfo),
    private val getEventInvitationsResult: Result<List<Invitation>> = Result.success(emptyList()),
    private val inviteByEmailResult: Result<Invitation> = Result.success(testInvitation),
    private val inviteByUserIdResult: Result<Invitation> = Result.success(testInvitation),
    private val getSuggestionsResult: Result<List<UserSuggestion>> = Result.success(emptyList()),
) : InvitationRepository {

    var lastRsvpToken: String? = null
    var lastRsvpStatus: InvitationStatus? = null

    override suspend fun getInviteInfo(token: String): Result<InviteInfo> = getInviteInfoResult

    override suspend fun rsvp(token: String, status: InvitationStatus): Result<InviteInfo> {
        lastRsvpToken = token
        lastRsvpStatus = status
        return rsvpResult
    }

    override suspend fun getEventInvitations(eventId: Int): Result<List<Invitation>> = getEventInvitationsResult

    override suspend fun inviteByEmail(eventId: Int, email: String): Result<Invitation> = inviteByEmailResult

    override suspend fun inviteByUserId(eventId: Int, userId: Int): Result<Invitation> = inviteByUserIdResult

    override suspend fun getInviteSuggestions(eventId: Int): Result<List<UserSuggestion>> = getSuggestionsResult
}
