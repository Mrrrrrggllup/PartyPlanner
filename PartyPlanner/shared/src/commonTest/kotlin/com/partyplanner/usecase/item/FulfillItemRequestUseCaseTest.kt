package com.partyplanner.usecase.item

import com.partyplanner.domain.usecase.item.FulfillItemRequestUseCase
import com.partyplanner.fake.FakeItemRepository
import com.partyplanner.fake.testItemRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FulfillItemRequestUseCaseTest {

    @Test
    fun `delegates eventId and requestId to repository`() = runTest {
        val fake = FakeItemRepository()
        FulfillItemRequestUseCase(fake)(eventId = 1, requestId = 42)
        assertEquals(Pair(1, 42), fake.fulfillCalledWith)
    }

    @Test
    fun `returns fulfilled item on success`() = runTest {
        val fulfilled = testItemRequest.copy(isFulfilled = true, assignedToName = "Alice")
        val result = FulfillItemRequestUseCase(
            FakeItemRepository(fulfillResult = Result.success(fulfilled))
        )(eventId = 1, requestId = 1)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isFulfilled)
    }

    @Test
    fun `propagates access denied failure`() = runTest {
        val result = FulfillItemRequestUseCase(
            FakeItemRepository(fulfillResult = Result.failure(Exception("Seul la personne assignée ou l'organisateur peut retirer son engagement")))
        )(eventId = 1, requestId = 1)
        assertTrue(result.isFailure)
    }
}
