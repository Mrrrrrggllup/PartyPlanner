package com.partyplanner.data.repository

import com.partyplanner.data.remote.CarpoolApi
import com.partyplanner.data.remote.dto.CarpoolOfferResponse
import com.partyplanner.data.remote.dto.CarpoolPassengerResponse
import com.partyplanner.data.remote.dto.CreateCarpoolOfferDto
import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.model.CarpoolPassenger
import com.partyplanner.domain.repository.CarpoolRepository

class CarpoolRepositoryImpl(private val api: CarpoolApi) : CarpoolRepository {

    override suspend fun getOffers(eventId: Int): Result<List<CarpoolOffer>> = runCatching {
        api.getOffers(eventId).map { it.toDomain() }
    }

    override suspend fun createOffer(eventId: Int, seats: Int, departurePoint: String?, notes: String?): Result<CarpoolOffer> = runCatching {
        api.createOffer(eventId, CreateCarpoolOfferDto(seats, departurePoint, null, notes)).toDomain()
    }

    override suspend fun deleteOffer(eventId: Int, offerId: Int): Result<Unit> = runCatching {
        api.deleteOffer(eventId, offerId)
    }

    override suspend fun joinOffer(eventId: Int, offerId: Int, pickupPoint: String?): Result<CarpoolOffer> = runCatching {
        api.joinOffer(eventId, offerId, pickupPoint).toDomain()
    }

    override suspend fun leaveOffer(eventId: Int, offerId: Int): Result<CarpoolOffer> = runCatching {
        api.leaveOffer(eventId, offerId).toDomain()
    }

    private fun CarpoolPassengerResponse.toDomain() = CarpoolPassenger(id, passengerId, passengerName, pickupPoint)

    private fun CarpoolOfferResponse.toDomain() = CarpoolOffer(
        id             = id,
        driverId       = driverId,
        driverName     = driverName,
        seatsAvailable = seatsAvailable,
        seatsRemaining = seatsRemaining,
        departurePoint = departurePoint,
        departureTime  = departureTime,
        notes          = notes,
        passengers     = passengers.map { it.toDomain() },
    )
}
