package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.repository.InvitationRepository

class GetEventInvitationsUseCase(private val repo: InvitationRepository) {
    suspend operator fun invoke(eventId: Int) = repo.getEventInvitations(eventId)
}
