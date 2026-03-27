package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.repository.CarpoolRepository

class LeaveCarpoolUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(eventId: Int, offerId: Int): Result<CarpoolOffer> =
        repository.leaveOffer(eventId, offerId)
}
