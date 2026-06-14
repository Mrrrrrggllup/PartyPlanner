package com.partyplanner.presentation.auth

import kotlinx.coroutines.flow.StateFlow

sealed class ResetPasswordState {
    data object Idle : ResetPasswordState()
    data object Loading : ResetPasswordState()
    data object Success : ResetPasswordState()
    data class Error(val message: String) : ResetPasswordState()
}

interface ResetPasswordComponent {
    val state: StateFlow<ResetPasswordState>
    fun onBack()
    fun onSubmit(newPassword: String)
}
