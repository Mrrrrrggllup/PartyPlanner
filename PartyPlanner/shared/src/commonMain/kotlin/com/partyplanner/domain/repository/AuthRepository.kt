package com.partyplanner.domain.repository

import com.partyplanner.domain.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(email: String, password: String, displayName: String, phone: String? = null): Result<User>
    suspend fun getStoredSession(): User?
    suspend fun logout()
}