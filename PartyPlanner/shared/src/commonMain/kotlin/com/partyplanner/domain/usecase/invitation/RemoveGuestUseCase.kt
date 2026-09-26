package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.repository.InvitationRepository

class RemoveGuestUseCase(private val repository: InvitationRepository) {
    suspend operator fun invoke(eventId: Int, invitationId: Int): Result<Unit> =
        repository.removeGuest(eventId, invitationId)
}
