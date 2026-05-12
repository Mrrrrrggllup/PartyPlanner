package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.model.Invitation
import com.partyplanner.domain.repository.InvitationRepository

class InviteByUserIdUseCase(private val repository: InvitationRepository) {
    suspend operator fun invoke(eventId: Int, userId: Int): Result<Invitation> =
        repository.inviteByUserId(eventId, userId)
}
