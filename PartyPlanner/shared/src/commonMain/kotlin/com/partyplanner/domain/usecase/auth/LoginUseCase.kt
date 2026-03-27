package com.partyplanner.domain.usecase.auth

import com.partyplanner.domain.model.User
import com.partyplanner.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<User> =
        authRepository.login(email, password)
}