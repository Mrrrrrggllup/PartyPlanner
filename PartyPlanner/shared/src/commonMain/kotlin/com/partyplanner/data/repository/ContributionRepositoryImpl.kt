package com.partyplanner.data.repository

import com.partyplanner.data.remote.ContributionApi
import com.partyplanner.data.remote.dto.ContributionResponse
import com.partyplanner.domain.model.Contribution
import com.partyplanner.domain.repository.ContributionRepository

class ContributionRepositoryImpl(private val api: ContributionApi) : ContributionRepository {

    override suspend fun getContributions(eventId: Int): Result<List<Contribution>> = runCatching {
        api.getContributions(eventId).map { it.toDomain() }
    }

    override suspend fun addContribution(
        eventId: Int, linkedUserId: Int, label: String, amount: Double
    ): Result<Contribution> = runCatching {
        api.addContribution(eventId, linkedUserId, label, amount).toDomain()
    }

    override suspend fun deleteContribution(eventId: Int, contributionId: Int): Result<Unit> = runCatching {
        api.deleteContribution(eventId, contributionId)
    }

    private fun ContributionResponse.toDomain() = Contribution(
        id             = id,
        addedById      = addedById,
        linkedUserId   = linkedUserId,
        linkedUserName = linkedUserName,
        label          = label,
        amount         = amount,
        createdAt      = createdAt,
    )
}
