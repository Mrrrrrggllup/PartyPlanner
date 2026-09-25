package com.partyplanner.usecase.event

import com.partyplanner.domain.usecase.event.DeleteEventUseCase
import com.partyplanner.domain.usecase.event.GetEventUseCase
import com.partyplanner.domain.usecase.event.UpdateEventUseCase
import com.partyplanner.fake.FakeEventRepository
import com.partyplanner.fake.testEvent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetEventUseCaseTest {

    @Test
    fun `returns event on success`() = runTest {
        val result = GetEventUseCase(FakeEventRepository())(id = 1)
        assertTrue(result.isSuccess)
        assertEquals(testEvent, result.getOrNull())
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = GetEventUseCase(
            FakeEventRepository(getEventResult = Result.failure(Exception("Not found")))
        )(1)
        assertTrue(result.isFailure)
        assertEquals("Not found", result.exceptionOrNull()?.message)
    }
}

class DeleteEventUseCaseTest {

    @Test
    fun `delegates eventId to repository`() = runTest {
        val fake = FakeEventRepository()
        DeleteEventUseCase(fake)(42)
        assertEquals(42, fake.deleteCalledWithId)
    }

    @Test
    fun `returns success from repository`() = runTest {
        val result = DeleteEventUseCase(FakeEventRepository())(1)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates access denied failure`() = runTest {
        val result = DeleteEventUseCase(
            FakeEventRepository(deleteEventResult = Result.failure(Exception("Access denied")))
        )(1)
        assertTrue(result.isFailure)
        assertEquals("Access denied", result.exceptionOrNull()?.message)
    }
}

class UpdateEventUseCaseTest {

    @Test
    fun `returns updated event on success`() = runTest {
        val result = UpdateEventUseCase(FakeEventRepository())(1, "New Title", null, null, null, null)
        assertTrue(result.isSuccess)
        assertEquals(testEvent, result.getOrNull())
    }

    @Test
    fun `propagates access denied failure`() = runTest {
        val result = UpdateEventUseCase(
            FakeEventRepository(updateEventResult = Result.failure(Exception("Access denied")))
        )(1, null, null, null, null, null)
        assertTrue(result.isFailure)
    }
}
