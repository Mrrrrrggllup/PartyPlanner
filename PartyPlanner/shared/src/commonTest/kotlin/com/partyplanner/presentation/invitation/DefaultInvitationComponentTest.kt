package com.partyplanner.presentation.invitation

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.usecase.invitation.GetInviteInfoUseCase
import com.partyplanner.domain.usecase.invitation.RsvpToInvitationUseCase
import com.partyplanner.fake.FakeInvitationRepository
import com.partyplanner.fake.testInviteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInvitationComponentTest {

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
        getInviteInfoResult: Result<com.partyplanner.domain.model.InviteInfo> = Result.success(testInviteInfo),
        rsvpResult: Result<com.partyplanner.domain.model.InviteInfo> = Result.success(testInviteInfo),
        onBack: () -> Unit = {},
        onRsvpSuccess: () -> Unit = {},
    ): DefaultInvitationComponent {
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle = lifecycle)
        val repo = FakeInvitationRepository(
            getInviteInfoResult = getInviteInfoResult,
            rsvpResult = rsvpResult,
        )
        return DefaultInvitationComponent(
            componentContext = context,
            token = "test-token",
            getInviteInfoUseCase = GetInviteInfoUseCase(repo),
            rsvpToInvitationUseCase = RsvpToInvitationUseCase(repo),
            onBack = onBack,
            onRsvpSuccess = onRsvpSuccess,
        )
    }

    @Test
    fun `init loads invite info into Success state`() = runTest {
        val component = createComponent()
        val state = component.state.value
        assertTrue(state is InvitationState.Success)
        assertEquals(testInviteInfo, state.info)
    }

    @Test
    fun `init failure transitions to Error`() = runTest {
        val component = createComponent(
            getInviteInfoResult = Result.failure(Exception("Invitation introuvable"))
        )
        val state = component.state.value
        assertTrue(state is InvitationState.Error)
        assertEquals("Invitation introuvable", state.message)
    }

    @Test
    fun `onRsvp success calls onRsvpSuccess`() = runTest {
        var rsvpSuccessCalled = false
        val component = createComponent(onRsvpSuccess = { rsvpSuccessCalled = true })
        component.onRsvp(InvitationStatus.ACCEPTED)
        assertTrue(rsvpSuccessCalled)
    }

    @Test
    fun `onRsvp failure sets Error state`() = runTest {
        val component = createComponent(
            rsvpResult = Result.failure(Exception("Invitation introuvable"))
        )
        component.onRsvp(InvitationStatus.DECLINED)
        val state = component.state.value
        assertTrue(state is InvitationState.Error)
        assertEquals("Invitation introuvable", state.message)
    }

    @Test
    fun `onBack calls back callback`() = runTest {
        var backCalled = false
        val component = createComponent(onBack = { backCalled = true })
        component.onBack()
        assertTrue(backCalled)
    }
}
