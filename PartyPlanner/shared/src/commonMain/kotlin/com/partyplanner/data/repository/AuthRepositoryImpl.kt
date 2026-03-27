package com.partyplanner.data.repository

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.AuthApi
import com.partyplanner.data.remote.dto.LoginRequest
import com.partyplanner.data.remote.dto.RegisterRequest
import com.partyplanner.domain.model.User
import com.partyplanner.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionStorage: SessionStorage
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> =
        runCatching {
            val response = authApi.login(LoginRequest(email, password))
            sessionStorage.saveSession(response.token, response.userId.toLong(), response.displayName)
            User(response.userId, email, response.displayName)
        }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        phone: String?
    ): Result<User> = runCatching {
        val response = authApi.register(RegisterRequest(email, password, displayName, phone))
        sessionStorage.saveSession(response.token, response.userId.toLong(), response.displayName)
        User(response.userId, email, response.displayName)
    }

    override suspend fun getStoredSession(): User? {
        val session = sessionStorage.getSession() ?: return null
        return User(session.userId.toInt(), "", session.displayName)
    }

    override suspend fun logout() = sessionStorage.clearSession()
}