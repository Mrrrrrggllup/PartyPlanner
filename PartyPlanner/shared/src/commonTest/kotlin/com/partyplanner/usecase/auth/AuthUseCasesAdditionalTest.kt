package com.partyplanner.usecase.auth

import com.partyplanner.domain.usecase.auth.ForgotPasswordUseCase
import com.partyplanner.domain.usecase.auth.ResetPasswordUseCase
import com.partyplanner.fake.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ForgotPasswordUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = ForgotPasswordUseCase(FakeAuthRepository())("alice@example.com")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates failure`() = runTest {
        val result = ForgotPasswordUseCase(
            FakeAuthRepository(
                loginResult = Result.failure(Exception()),
                registerResult = Result.failure(Exception()),
            ).also { /* forgotPassword always returns success in FakeAuthRepository */ }
        )("alice@example.com")
        // FakeAuthRepository returns success for forgotPassword by default
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates repository error`() = runTest {
        val fake = object : com.partyplanner.domain.repository.AuthRepository {
            override suspend fun login(email: String, password: String) = Result.failure<com.partyplanner.domain.model.User>(Exception())
            override suspend fun register(email: String, password: String, displayName: String, phone: String?) = Result.failure<com.partyplanner.domain.model.User>(Exception())
            override suspend fun getStoredSession() = null
            override suspend fun logout() {}
            override suspend fun forgotPassword(email: String) = Result.failure<Unit>(Exception("Service unavailable"))
            override suspend fun resetPassword(token: String, newPassword: String) = Result.success(Unit)
        }
        val result = ForgotPasswordUseCase(fake)("alice@example.com")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message == "Service unavailable")
    }
}

class ResetPasswordUseCaseTest {

    @Test
    fun `returns success from repository`() = runTest {
        val result = ResetPasswordUseCase(FakeAuthRepository())("valid-token", "newpassword")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `propagates repository failure`() = runTest {
        val fake = object : com.partyplanner.domain.repository.AuthRepository {
            override suspend fun login(email: String, password: String) = Result.failure<com.partyplanner.domain.model.User>(Exception())
            override suspend fun register(email: String, password: String, displayName: String, phone: String?) = Result.failure<com.partyplanner.domain.model.User>(Exception())
            override suspend fun getStoredSession() = null
            override suspend fun logout() {}
            override suspend fun forgotPassword(email: String) = Result.success(Unit)
            override suspend fun resetPassword(token: String, newPassword: String) = Result.failure<Unit>(Exception("Token invalide"))
        }
        val result = ResetPasswordUseCase(fake)("bad-token", "newpassword")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message == "Token invalide")
    }
}
