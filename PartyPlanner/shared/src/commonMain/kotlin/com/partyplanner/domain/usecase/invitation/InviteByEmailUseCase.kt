package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.repository.InvitationRepository

class InviteByEmailUseCase(private val repository: InvitationRepository) {
    suspend operator fun invoke(eventId: Int, email: String): Result<Invitation> =
        repository.inviteByEmail(eventId, email)
}
