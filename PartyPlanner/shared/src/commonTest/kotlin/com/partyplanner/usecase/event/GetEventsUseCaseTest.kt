package com.partyplanner.usecase.event

import com.partyplanner.domain.usecase.event.GetEventsUseCase
import com.partyplanner.fake.FakeEventRepository
import com.partyplanner.fake.testEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetEventsUseCaseTest {

    @Test
    fun `returns event list on success`() = runTest {
        val result = GetEventsUseCase(FakeEventRepository(getEventsResult = Result.success(listOf(testEvent))))()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
        assertEquals(testEvent, result.getOrNull()?.first())
    }

    @Test
    fun `returns empty list when no events`() = runTest {
        val result = GetEventsUseCase(FakeEventRepository(getEventsResult = Result.success(emptyList())))()
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!.isEmpty())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val result = GetEventsUseCase(
            FakeEventRepository(getEventsResult = Result.failure(Exception("Network error")))
        )()
        assertTrue(result.isFailure)
        assertEquals("Network error", result.exceptionOrNull()?.message)
    }
}
