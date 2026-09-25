package com.partyplanner.presentation.auth

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.partyplanner.domain.usecase.auth.ForgotPasswordUseCase
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultForgotPasswordComponentTest {

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
        forgotPasswordResult: Result<Unit> = Result.success(Unit),
        onBack: () -> Unit = {},
    ): DefaultForgotPasswordComponent {
        runCatching { stopKoin() }
        startKoin {
            modules(module {
                factory { ForgotPasswordUseCase(FakeAuthRepository(forgotPasswordResult = forgotPasswordResult)) }
            })
        }
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle = lifecycle)
        return DefaultForgotPasswordComponent(
            componentContext = context,
            onBack = onBack,
        )
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val component = createComponent()
        assertEquals(ForgotPasswordState.Idle, component.state.value)
    }

    @Test
    fun `onSubmit success transitions to Success`() = runTest {
        val component = createComponent()
        component.onSubmit("alice@example.com")
        assertEquals(ForgotPasswordState.Success, component.state.value)
    }

    @Test
    fun `onSubmit failure transitions to Error`() = runTest {
        val component = createComponent(
            forgotPasswordResult = Result.failure(Exception("Email non trouvé"))
        )
        component.onSubmit("unknown@example.com")
        val state = component.state.value
        assertTrue(state is ForgotPasswordState.Error)
        assertEquals("Email non trouvé", state.message)
    }

    @Test
    fun `onBack calls back callback`() = runTest {
        var backCalled = false
        val component = createComponent(onBack = { backCalled = true })
        component.onBack()
        assertTrue(backCalled)
    }
}
