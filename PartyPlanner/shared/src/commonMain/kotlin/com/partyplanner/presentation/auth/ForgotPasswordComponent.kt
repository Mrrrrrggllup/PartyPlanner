package com.partyplanner.presentation.auth

import kotlinx.coroutines.flow.StateFlow

sealed class ForgotPasswordState {
    data object Idle : ForgotPasswordState()
    data object Loading : ForgotPasswordState()
    data object Success : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

interface ForgotPasswordComponent {
    val state: StateFlow<ForgotPasswordState>
    fun onBack()
    fun onSubmit(email: String)
}
