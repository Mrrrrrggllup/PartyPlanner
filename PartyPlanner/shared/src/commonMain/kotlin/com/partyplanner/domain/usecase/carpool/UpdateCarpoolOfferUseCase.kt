package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.repository.CarpoolRepository

class UpdateCarpoolOfferUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(
        eventId: Int,
        offerId: Int,
        seats: Int,
        departurePoint: String?,
        notes: String?,
    ): Result<CarpoolOffer> = repository.updateOffer(eventId, offerId, seats, departurePoint, notes)
}
