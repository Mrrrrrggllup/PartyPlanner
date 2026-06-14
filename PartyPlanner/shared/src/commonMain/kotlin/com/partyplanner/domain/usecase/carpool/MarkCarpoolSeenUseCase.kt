package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.repository.CarpoolRepository

class MarkCarpoolSeenUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(eventId: Int): Result<Unit> = repository.markCarpoolSeen(eventId)
}
