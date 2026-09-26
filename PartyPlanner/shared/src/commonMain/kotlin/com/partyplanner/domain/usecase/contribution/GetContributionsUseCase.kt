package com.partyplanner.domain.usecase.contribution

import com.partyplanner.domain.model.Contribution
import com.partyplanner.domain.repository.ContributionRepository

class GetContributionsUseCase(private val repository: ContributionRepository) {
    suspend operator fun invoke(eventId: Int): Result<List<Contribution>> =
        repository.getContributions(eventId)
}
