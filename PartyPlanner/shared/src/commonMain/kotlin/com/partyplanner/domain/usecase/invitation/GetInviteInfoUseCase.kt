package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.repository.InvitationRepository

class GetInviteInfoUseCase(private val repo: InvitationRepository) {
    suspend operator fun invoke(token: String) = repo.getInviteInfo(token)
}
