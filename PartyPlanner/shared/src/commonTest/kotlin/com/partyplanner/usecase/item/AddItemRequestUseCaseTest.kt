package com.partyplanner.usecase.item

import com.partyplanner.domain.usecase.item.AddItemRequestUseCase
import com.partyplanner.fake.FakeItemRepository
import com.partyplanner.fake.testItemRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddItemRequestUseCaseTest {

    @Test
    fun `delegates parameters to repository`() = runTest {
        val fake = FakeItemRepository()
        AddItemRequestUseCase(fake)(eventId = 1, label = "Chips", quantity = 3)
        assertEquals(Triple(1, "Chips", 3), fake.addRequestCalledWith)
    }

    @Test
    fun `returns created item on success`() = runTest {
        val result = AddItemRequestUseCase(FakeItemRepository(addRequestResult = Result.success(testItemRequest)))(
            eventId = 1, label = "Chips", quantity = 2
        )
        assertTrue(result.isSuccess)
        assertEquals(testItemRequest, result.getOrNull())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val result = AddItemRequestUseCase(
            FakeItemRepository(addRequestResult = Result.failure(Exception("Access denied")))
        )(eventId = 1, label = "Chips", quantity = 1)
        assertTrue(result.isFailure)
        assertEquals("Access denied", result.exceptionOrNull()?.message)
    }
}
