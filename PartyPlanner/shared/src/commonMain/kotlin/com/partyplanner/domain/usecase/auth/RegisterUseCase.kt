package com.partyplanner.domain.usecase.auth

import com.partyplanner.domain.model.User
import com.partyplanner.domain.repository.AuthRepository

class RegisterUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
        phone: String? = null
    ): Result<User> = authRepository.register(email, password, displayName, phone)
}