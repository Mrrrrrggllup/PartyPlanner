package com.partyplanner.domain.usecase.invitation

import com.partyplanner.domain.model.UserSuggestion
import com.partyplanner.domain.repository.InvitationRepository

class GetInviteSuggestionsUseCase(private val repository: InvitationRepository) {
    suspend operator fun invoke(eventId: Int): Result<List<UserSuggestion>> =
        repository.getInviteSuggestions(eventId)
}
