package com.partyplanner.domain.usecase.auth

import com.partyplanner.domain.repository.AuthRepository

class ResetPasswordUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> =
        authRepository.resetPassword(token, newPassword)
}
