package com.partyplanner.usecase.event

import com.partyplanner.domain.usecase.event.CreateEventUseCase
import com.partyplanner.fake.FakeEventRepository
import com.partyplanner.fake.testEvent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateEventUseCaseTest {

    private val startDate = LocalDateTime(2026, 10, 1, 18, 0)

    @Test
    fun `delegates title to repository`() = runTest {
        val fake = FakeEventRepository()
        CreateEventUseCase(fake)("BBQ d'été", null, null, startDate, null)
        assertEquals("BBQ d'été", fake.lastCreatedTitle)
    }

    @Test
    fun `returns created event on success`() = runTest {
        val result = CreateEventUseCase(FakeEventRepository(createEventResult = Result.success(testEvent)))(
            "BBQ d'été", null, null, startDate, null
        )
        assertTrue(result.isSuccess)
        assertEquals(testEvent, result.getOrNull())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val result = CreateEventUseCase(
            FakeEventRepository(createEventResult = Result.failure(Exception("Unauthorized")))
        )("BBQ d'été", null, null, startDate, null)
        assertTrue(result.isFailure)
        assertEquals("Unauthorized", result.exceptionOrNull()?.message)
    }
}
