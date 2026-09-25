package com.partyplanner.usecase.carpool

import com.partyplanner.domain.usecase.carpool.CreateCarpoolOfferUseCase
import com.partyplanner.fake.FakeCarpoolRepository
import com.partyplanner.fake.testCarpoolOffer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateCarpoolOfferUseCaseTest {

    @Test
    fun `delegates parameters to repository`() = runTest {
        val fake = FakeCarpoolRepository()
        CreateCarpoolOfferUseCase(fake)(eventId = 1, seats = 3, departurePoint = "Gare du Nord", notes = null)
        assertEquals(Triple(1, 3, "Gare du Nord"), fake.createCalledWith)
    }

    @Test
    fun `returns created offer on success`() = runTest {
        val result = CreateCarpoolOfferUseCase(
            FakeCarpoolRepository(createOfferResult = Result.success(testCarpoolOffer))
        )(eventId = 1, seats = 3, departurePoint = null, notes = null)
        assertTrue(result.isSuccess)
        assertEquals(testCarpoolOffer, result.getOrNull())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val result = CreateCarpoolOfferUseCase(
            FakeCarpoolRepository(createOfferResult = Result.failure(Exception("Access denied")))
        )(eventId = 1, seats = 2, departurePoint = null, notes = null)
        assertTrue(result.isFailure)
        assertEquals("Access denied", result.exceptionOrNull()?.message)
    }
}
