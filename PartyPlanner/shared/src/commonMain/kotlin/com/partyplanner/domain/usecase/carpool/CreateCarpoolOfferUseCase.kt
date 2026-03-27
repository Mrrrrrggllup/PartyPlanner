package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.repository.CarpoolRepository

class CreateCarpoolOfferUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(eventId: Int, seats: Int, departurePoint: String?, notes: String?): Result<CarpoolOffer> =
        repository.createOffer(eventId, seats, departurePoint, notes)
}
