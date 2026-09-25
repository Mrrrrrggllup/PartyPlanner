package com.partyplanner.presentation.event

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.partyplanner.domain.usecase.event.GetEventUseCase
import com.partyplanner.domain.usecase.event.UpdateEventUseCase
import com.partyplanner.fake.FakeEventRepository
import com.partyplanner.fake.testEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEditEventComponentTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createComponent(
        getEventResult: Result<com.partyplanner.domain.model.Event> = Result.success(testEvent),
        updateEventResult: Result<com.partyplanner.domain.model.Event> = Result.success(testEvent),
        onBack: () -> Unit = {},
        onSaved: () -> Unit = {},
    ): DefaultEditEventComponent {
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle = lifecycle)
        val repo = FakeEventRepository(
            getEventResult = getEventResult,
            updateEventResult = updateEventResult,
        )
        return DefaultEditEventComponent(
            componentContext = context,
            eventId = 1,
            getEventUseCase = GetEventUseCase(repo),
            updateEventUseCase = UpdateEventUseCase(repo),
            onBack = onBack,
            onSaved = onSaved,
        )
    }

    @Test
    fun `initial state is Loading then Loaded after init`() = runTest {
        val component = createComponent()
        val state = component.state.value
        assertTrue(state is EditEventState.Loaded)
        assertEquals(testEvent, state.event)
    }

    @Test
    fun `init failure transitions to Error`() = runTest {
        val component = createComponent(
            getEventResult = Result.failure(Exception("Not found"))
        )
        val state = component.state.value
        assertTrue(state is EditEventState.Error)
        assertEquals("Not found", state.message)
    }

    @Test
    fun `onSave success calls onSaved`() = runTest {
        var saved = false
        val component = createComponent(onSaved = { saved = true })
        component.onSave("New Title", null, null, LocalDateTime(2026, 10, 1, 18, 0), null)
        assertTrue(saved)
    }

    @Test
    fun `onSave failure sets error on Loaded state`() = runTest {
        val component = createComponent(
            updateEventResult = Result.failure(Exception("Access denied"))
        )
        component.onSave("Title", null, null, LocalDateTime(2026, 10, 1, 18, 0), null)
        val state = component.state.value
        assertTrue(state is EditEventState.Loaded)
        assertEquals("Access denied", state.error)
    }

    @Test
    fun `onBack calls back callback`() = runTest {
        var backCalled = false
        val component = createComponent(onBack = { backCalled = true })
        component.onBack()
        assertTrue(backCalled)
    }
}
