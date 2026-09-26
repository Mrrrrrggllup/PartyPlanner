package com.partyplanner.domain.repository

import com.partyplanner.domain.model.Contribution

interface ContributionRepository {
    suspend fun getContributions(eventId: Int): Result<List<Contribution>>
    suspend fun addContribution(eventId: Int, linkedUserId: Int, label: String, amount: Double): Result<Contribution>
    suspend fun deleteContribution(eventId: Int, contributionId: Int): Result<Unit>
}
