package com.partyplanner.domain.usecase.auth

import com.partyplanner.data.local.SessionStorage

class LogoutUseCase(private val sessionStorage: SessionStorage) {
    operator fun invoke() = sessionStorage.clearSession()
}
