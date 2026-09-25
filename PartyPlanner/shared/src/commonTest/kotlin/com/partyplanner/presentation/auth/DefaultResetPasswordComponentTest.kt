package com.partyplanner.presentation.auth

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.partyplanner.domain.usecase.auth.ResetPasswordUseCase
import com.partyplanner.fake.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultResetPasswordComponentTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        runCatching { stopKoin() }
        Dispatchers.resetMain()
    }

    private fun createComponent(
        resetPasswordResult: Result<Unit> = Result.success(Unit),
        onBack: () -> Unit = {},
        onSuccess: () -> Unit = {},
    ): DefaultResetPasswordComponent {
        runCatching { stopKoin() }
        startKoin {
            modules(module {
                factory { ResetPasswordUseCase(FakeAuthRepository(resetPasswordResult = resetPasswordResult)) }
            })
        }
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle = lifecycle)
        return DefaultResetPasswordComponent(
            componentContext = context,
            token = "reset-token-123",
            onBack = onBack,
            onSuccess = onSuccess,
        )
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val component = createComponent()
        assertEquals(ResetPasswordState.Idle, component.state.value)
    }

    @Test
    fun `onSubmit success transitions to Success and calls onSuccess`() = runTest {
        var successCalled = false
        val component = createComponent(onSuccess = { successCalled = true })
        component.onSubmit("newPassword123")
        assertEquals(ResetPasswordState.Success, component.state.value)
        assertTrue(successCalled)
    }

    @Test
    fun `onSubmit failure transitions to Error`() = runTest {
        val component = createComponent(
            resetPasswordResult = Result.failure(Exception("Token invalide"))
        )
        component.onSubmit("newPassword123")
        val state = component.state.value
        assertTrue(state is ResetPasswordState.Error)
        assertEquals("Token invalide", state.message)
    }

    @Test
    fun `onSubmit failure does not call onSuccess`() = runTest {
        var successCalled = false
        val component = createComponent(
            resetPasswordResult = Result.failure(Exception("Token expiré")),
            onSuccess = { successCalled = true },
        )
        component.onSubmit("newPassword123")
        assertFalse(successCalled)
    }

    @Test
    fun `onBack calls back callback`() = runTest {
        var backCalled = false
        val component = createComponent(onBack = { backCalled = true })
        component.onBack()
        assertTrue(backCalled)
    }
}
