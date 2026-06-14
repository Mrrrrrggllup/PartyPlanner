package com.partyplanner.domain.repository

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.model.EventCarpool

interface CarpoolRepository {
    suspend fun getOffers(eventId: Int): Result<EventCarpool>
    suspend fun createOffer(eventId: Int, seats: Int, departurePoint: String?, notes: String?): Result<CarpoolOffer>
    suspend fun updateOffer(eventId: Int, offerId: Int, seats: Int, departurePoint: String?, notes: String?): Result<CarpoolOffer>
    suspend fun deleteOffer(eventId: Int, offerId: Int): Result<Unit>
    suspend fun joinOffer(eventId: Int, offerId: Int, pickupPoint: String?): Result<CarpoolOffer>
    suspend fun leaveOffer(eventId: Int, offerId: Int): Result<CarpoolOffer>
    suspend fun markCarpoolSeen(eventId: Int): Result<Unit>
}
