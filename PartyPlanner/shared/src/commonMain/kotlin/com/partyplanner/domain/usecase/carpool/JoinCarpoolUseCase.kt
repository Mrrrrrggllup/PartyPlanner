package com.partyplanner.domain.usecase.carpool

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.repository.CarpoolRepository

class JoinCarpoolUseCase(private val repository: CarpoolRepository) {
    suspend operator fun invoke(eventId: Int, offerId: Int, pickupPoint: String?): Result<CarpoolOffer> =
        repository.joinOffer(eventId, offerId, pickupPoint)
}
