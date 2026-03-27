package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.repository.CarpoolRepository

class GetCarpoolOffersUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(eventId: Int): Result<List<CarpoolOffer>> = repository.getOffers(eventId)
}
