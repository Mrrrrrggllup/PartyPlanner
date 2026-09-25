package com.partyplanner.presentation.event

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.partyplanner.domain.usecase.event.CreateEventUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCreateEventComponentTest {

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
        createEventResult: Result<com.partyplanner.domain.model.Event> = Result.success(testEvent),
        onBack: () -> Unit = {},
        onCreated: () -> Unit = {},
    ): DefaultCreateEventComponent {
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle = lifecycle)
        return DefaultCreateEventComponent(
            componentContext = context,
            createEventUseCase = CreateEventUseCase(FakeEventRepository(createEventResult = createEventResult)),
            onBack = onBack,
            onCreated = onCreated,
        )
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val component = createComponent()
        assertTrue(component.state.value is CreateEventState.Idle)
    }

    @Test
    fun `create event success transitions to Success and calls onCreated`() = runTest {
        var created = false
        val component = createComponent(onCreated = { created = true })
        component.createEvent("Party", null, null, LocalDateTime(2026, 10, 1, 18, 0), null)
        assertTrue(component.state.value is CreateEventState.Success)
        assertTrue(created)
    }

    @Test
    fun `create event failure transitions to Error`() = runTest {
        val component = createComponent(
            createEventResult = Result.failure(Exception("Titre requis"))
        )
        component.createEvent("", null, null, LocalDateTime(2026, 10, 1, 18, 0), null)
        val state = component.state.value
        assertTrue(state is CreateEventState.Error)
        assertEquals("Titre requis", state.message)
    }

    @Test
    fun `create event does not call onCreated on failure`() = runTest {
        var created = false
        val component = createComponent(
            createEventResult = Result.failure(Exception("Error")),
            onCreated = { created = true },
        )
        component.createEvent("", null, null, LocalDateTime(2026, 10, 1, 18, 0), null)
        assertFalse(created)
    }

    @Test
    fun `onBack calls back callback`() = runTest {
        var backCalled = false
        val component = createComponent(onBack = { backCalled = true })
        component.onBack()
        assertTrue(backCalled)
    }
}
