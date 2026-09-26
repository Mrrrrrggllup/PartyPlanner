package com.partyplanner.domain.usecase.contribution

import com.partyplanner.domain.repository.ContributionRepository

class DeleteContributionUseCase(private val repository: ContributionRepository) {
    suspend operator fun invoke(eventId: Int, contributionId: Int): Result<Unit> =
        repository.deleteContribution(eventId, contributionId)
}
