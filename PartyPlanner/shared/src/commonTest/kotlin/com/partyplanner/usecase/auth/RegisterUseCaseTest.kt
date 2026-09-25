package com.partyplanner.usecase.auth

import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.fake.FakeAuthRepository
import com.partyplanner.fake.testUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegisterUseCaseTest {

    @Test
    fun `returns user on success`() = runTest {
        val result = RegisterUseCase(FakeAuthRepository(registerResult = Result.success(testUser)))(
            email = "alice@example.com",
            password = "secret",
            displayName = "Alice",
        )
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val result = RegisterUseCase(
            FakeAuthRepository(registerResult = Result.failure(Exception("Email already in use")))
        )("alice@example.com", "secret", "Alice")
        assertTrue(result.isFailure)
        assertEquals("Email already in use", result.exceptionOrNull()?.message)
    }
}
