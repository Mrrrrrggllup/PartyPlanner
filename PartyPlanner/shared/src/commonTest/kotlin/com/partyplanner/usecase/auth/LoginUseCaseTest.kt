package com.partyplanner.usecase.auth

import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.fake.FakeAuthRepository
import com.partyplanner.fake.testUser
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginUseCaseTest {

    @Test
    fun `delegates email and password to repository`() = runTest {
        val fake = FakeAuthRepository()
        LoginUseCase(fake)("alice@example.com", "secret")
        assertEquals("alice@example.com", fake.lastLoginEmail)
        assertEquals("secret", fake.lastLoginPassword)
    }

    @Test
    fun `returns user on success`() = runTest {
        val result = LoginUseCase(FakeAuthRepository(loginResult = Result.success(testUser)))(
            "alice@example.com", "secret"
        )
        assertTrue(result.isSuccess)
        assertEquals(testUser, result.getOrNull())
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val result = LoginUseCase(
            FakeAuthRepository(loginResult = Result.failure(Exception("Invalid credentials")))
        )("alice@example.com", "wrong")
        assertTrue(result.isFailure)
        assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
    }
}
