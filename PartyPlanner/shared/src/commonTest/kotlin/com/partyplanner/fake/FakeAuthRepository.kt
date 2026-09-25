package com.partyplanner.fake

import com.partyplanner.domain.model.User
import com.partyplanner.domain.repository.AuthRepository

internal class FakeAuthRepository(
    private val loginResult: Result<User> = Result.success(testUser),
    private val registerResult: Result<User> = Result.success(testUser),
    private val forgotPasswordResult: Result<Unit> = Result.success(Unit),
    private val resetPasswordResult: Result<Unit> = Result.success(Unit),
) : AuthRepository {

    var lastLoginEmail: String? = null
    var lastLoginPassword: String? = null
    var logoutCalled = false

    override suspend fun login(email: String, password: String): Result<User> {
        lastLoginEmail = email
        lastLoginPassword = password
        return loginResult
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        phone: String?,
    ): Result<User> = registerResult

    override suspend fun getStoredSession(): User? = null

    override suspend fun logout() {
        logoutCalled = true
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = forgotPasswordResult

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = resetPasswordResult
}
