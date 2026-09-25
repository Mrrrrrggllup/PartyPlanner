package com.partyplanner.usecase.carpool

import com.partyplanner.domain.model.EventCarpool
import com.partyplanner.domain.usecase.carpool.DeleteCarpoolOfferUseCase
import com.partyplanner.domain.usecase.carpool.GetCarpoolOffersUseCase
import com.partyplanner.domain.usecase.carpool.JoinCarpoolUseCase
import com.partyplanner.domain.usecase.carpool.LeaveCarpoolUseCase
import com.partyplanner.domain.usecase.carpool.MarkCarpoolSeenUseCase
import com.partyplanner.domain.usecase.carpool.UpdateCarpoolOfferUseCase
import com.partyplanner.fake.FakeCarpoolRepository
import com.partyplanner.fake.testCarpoolOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetCarpoolOffersUseCaseTest {

    @Test
    fun `returns offers on success`() = runTest {
        val carpool = EventCarpool(listOf(testCarpoolOffer), newCarpoolCount = 1)
        val result = GetCarpoolOffersUseCase(
            FakeCarpoolRepository(getOffersResult = Result.success(carpool))
        )(eventId = 1)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.offers?.size)
        assertEquals(1, result.getOrNull()?.newCarpoolCount)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = GetCarpoolOffersUseCase(
            FakeCarpoolRepository(getOffersResult = Result.failure(Exception("Access denied")))
        )(1)
        assertTrue(result.isFailure)
    }
}

class UpdateCarpoolOfferUseCaseTest {

    @Test
    fun `returns updated offer on success`() = runTest {
        val updated = testCarpoolOffer.copy(seatsAvailable = 4)
        val result = UpdateCarpoolOfferUseCase(
            FakeCarpoolRepository(updateOfferResult = Result.success(updated))
        )(eventId = 1, offerId = 1, seats = 4, departurePoint = null, notes = null)
        assertTrue(result.isSuccess)
        assertEquals(4, result.getOrNull()?.seatsAvailable)
    }

    @Test
    fun `propagates access denied`() = runTest {
        val result = UpdateCarpoolOfferUseCase(
            FakeCarpoolRepository(updateOfferResult = Result.failure(Exception("Seul le conducteur peut modifier son offre")))
        )(1, 1, 4, null, null)
        assertTrue(result.isFailure)
    }
}

class DeleteCarpoolOfferUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = DeleteCarpoolOfferUseCase(FakeCarpoolRepository())(eventId = 1, offerId = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates access denied`() = runTest {
        val result = DeleteCarpoolOfferUseCase(
            FakeCarpoolRepository().let {
                object : com.partyplanner.domain.repository.CarpoolRepository by it {
                    override suspend fun deleteOffer(eventId: Int, offerId: Int) =
                        Result.failure<Unit>(Exception("Access denied"))
                }
            }
        )(1, 1)
        assertTrue(result.isFailure)
    }
}

class JoinCarpoolUseCaseTest {

    @Test
    fun `returns updated offer on success`() = runTest {
        val joined = testCarpoolOffer.copy(seatsRemaining = 2)
        val result = JoinCarpoolUseCase(
            FakeCarpoolRepository(joinOfferResult = Result.success(joined))
        )(eventId = 1, offerId = 1, pickupPoint = "Gare du Nord")
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()?.seatsRemaining)
    }

    @Test
    fun `propagates full offer failure`() = runTest {
        val result = JoinCarpoolUseCase(
            FakeCarpoolRepository(joinOfferResult = Result.failure(Exception("Plus de places disponibles")))
        )(1, 1, null)
        assertTrue(result.isFailure)
        assertEquals("Plus de places disponibles", result.exceptionOrNull()?.message)
    }
}

class LeaveCarpoolUseCaseTest {

    @Test
    fun `returns updated offer on success`() = runTest {
        val result = LeaveCarpoolUseCase(
            FakeCarpoolRepository(leaveOfferResult = Result.success(testCarpoolOffer))
        )(eventId = 1, offerId = 1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates not-a-passenger failure`() = runTest {
        val result = LeaveCarpoolUseCase(
            FakeCarpoolRepository(leaveOfferResult = Result.failure(Exception("Tu n'es pas passager de cette offre")))
        )(1, 1)
        assertTrue(result.isFailure)
    }
}

class MarkCarpoolSeenUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = MarkCarpoolSeenUseCase(FakeCarpoolRepository())(eventId = 1)
        assertTrue(result.isSuccess)
    }
}
