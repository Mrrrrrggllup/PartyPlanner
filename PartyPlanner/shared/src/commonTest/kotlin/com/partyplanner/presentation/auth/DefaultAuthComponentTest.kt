package com.partyplanner.presentation.auth

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.fake.FakeAuthRepository
import com.partyplanner.fake.testUser
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultAuthComponentTest {

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
        loginResult: Result<com.partyplanner.domain.model.User> = Result.success(testUser),
        registerResult: Result<com.partyplanner.domain.model.User> = Result.success(testUser),
        onAuthSuccess: () -> Unit = {},
        onForgotPasswordNav: () -> Unit = {},
    ): DefaultAuthComponent {
        val lifecycle = LifecycleRegistry()
        val context = DefaultComponentContext(lifecycle = lifecycle)
        return DefaultAuthComponent(
            componentContext = context,
            loginUseCase = LoginUseCase(FakeAuthRepository(loginResult = loginResult)),
            registerUseCase = RegisterUseCase(FakeAuthRepository(registerResult = registerResult)),
            onAuthSuccess = onAuthSuccess,
            onForgotPasswordNav = onForgotPasswordNav,
        )
    }

    @Test
    fun `initial state is Idle`() = runTest(testDispatcher) {
        val component = createComponent()
        assertEquals(AuthState.Idle, component.state.value)
    }

    @Test
    fun `login success sets Success state`() = runTest(testDispatcher) {
        val component = createComponent(loginResult = Result.success(testUser))
        component.login("alice@example.com", "password")
        assertEquals(AuthState.Success(testUser), component.state.value)
    }

    @Test
    fun `login success calls onAuthSuccess`() = runTest(testDispatcher) {
        var called = false
        val component = createComponent(onAuthSuccess = { called = true })
        component.login("alice@example.com", "password")
        assertTrue(called)
    }

    @Test
    fun `login failure sets Error state with message`() = runTest(testDispatcher) {
        val component = createComponent(loginResult = Result.failure(Exception("Invalid credentials")))
        component.login("alice@example.com", "wrong")
        assertEquals(AuthState.Error("Invalid credentials"), component.state.value)
    }

    @Test
    fun `login failure does not call onAuthSuccess`() = runTest(testDispatcher) {
        var called = false
        val component = createComponent(
            loginResult = Result.failure(Exception("Error")),
            onAuthSuccess = { called = true },
        )
        component.login("alice@example.com", "wrong")
        assertFalse(called)
    }

    @Test
    fun `register success sets Success state`() = runTest(testDispatcher) {
        val component = createComponent(registerResult = Result.success(testUser))
        component.register("alice@example.com", "password", "Alice")
        assertEquals(AuthState.Success(testUser), component.state.value)
    }

    @Test
    fun `register failure sets Error state`() = runTest(testDispatcher) {
        val component = createComponent(registerResult = Result.failure(Exception("Email already in use")))
        component.register("alice@example.com", "password", "Alice")
        assertEquals(AuthState.Error("Email already in use"), component.state.value)
    }

    @Test
    fun `resetState returns to Idle after error`() = runTest(testDispatcher) {
        val component = createComponent(loginResult = Result.failure(Exception("Error")))
        component.login("alice@example.com", "wrong")
        assertEquals(AuthState.Error("Error"), component.state.value)

        component.resetState()
        assertEquals(AuthState.Idle, component.state.value)
    }

    @Test
    fun `onForgotPassword triggers navigation callback`() = runTest(testDispatcher) {
        var navigated = false
        val component = createComponent(onForgotPasswordNav = { navigated = true })
        component.onForgotPassword()
        assertTrue(navigated)
    }
}
