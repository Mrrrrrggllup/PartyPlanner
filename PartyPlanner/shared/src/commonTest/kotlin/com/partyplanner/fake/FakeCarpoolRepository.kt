package com.partyplanner.fake

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.model.EventCarpool
import com.partyplanner.domain.repository.CarpoolRepository

internal class FakeCarpoolRepository(
    private val getOffersResult: Result<EventCarpool> = Result.success(EventCarpool(emptyList())),
    private val createOfferResult: Result<CarpoolOffer> = Result.success(testCarpoolOffer),
    private val updateOfferResult: Result<CarpoolOffer> = Result.success(testCarpoolOffer),
    private val joinOfferResult: Result<CarpoolOffer> = Result.success(testCarpoolOffer),
    private val leaveOfferResult: Result<CarpoolOffer> = Result.success(testCarpoolOffer),
) : CarpoolRepository {

    var createCalledWith: Triple<Int, Int, String?>? = null

    override suspend fun getOffers(eventId: Int): Result<EventCarpool> = getOffersResult

    override suspend fun createOffer(
        eventId: Int,
        seats: Int,
        departurePoint: String?,
        notes: String?,
    ): Result<CarpoolOffer> {
        createCalledWith = Triple(eventId, seats, departurePoint)
        return createOfferResult
    }

    override suspend fun updateOffer(
        eventId: Int,
        offerId: Int,
        seats: Int,
        departurePoint: String?,
        notes: String?,
    ): Result<CarpoolOffer> = updateOfferResult

    override suspend fun deleteOffer(eventId: Int, offerId: Int): Result<Unit> = Result.success(Unit)

    override suspend fun joinOffer(eventId: Int, offerId: Int, pickupPoint: String?): Result<CarpoolOffer> =
        joinOfferResult

    override suspend fun leaveOffer(eventId: Int, offerId: Int): Result<CarpoolOffer> = leaveOfferResult

    override suspend fun markCarpoolSeen(eventId: Int): Result<Unit> = Result.success(Unit)
}
