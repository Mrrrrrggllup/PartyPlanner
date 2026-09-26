package com.partyplanner.domain.usecase.contribution

import com.partyplanner.domain.model.Contribution
import com.partyplanner.domain.repository.ContributionRepository

class AddContributionUseCase(private val repository: ContributionRepository) {
    suspend operator fun invoke(
        eventId: Int,
        linkedUserId: Int,
        label: String,
        amount: Double,
    ): Result<Contribution> = repository.addContribution(eventId, linkedUserId, label, amount)
}
