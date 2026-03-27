package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.repository.CarpoolRepository

class DeleteCarpoolOfferUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(eventId: Int, offerId: Int): Result<Unit> = repository.deleteOffer(eventId, offerId)
}
