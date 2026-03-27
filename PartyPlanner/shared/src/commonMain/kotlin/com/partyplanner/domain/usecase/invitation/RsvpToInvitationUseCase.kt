package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.repository.InvitationRepository

class RsvpToInvitationUseCase(private val repo: InvitationRepository) {
    suspend operator fun invoke(token: String, status: InvitationStatus) = repo.rsvp(token, status)
}
