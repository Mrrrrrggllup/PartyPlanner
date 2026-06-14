package com.partyplanner.domain.usecase.auth

import com.partyplanner.domain.repository.AuthRepository

class ForgotPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String): Result<Unit> =
        authRepository.forgotPassword(email)
}
