package com.partyplanner.presentation.auth

import kotlinx.coroutines.flow.StateFlow

interface AuthComponent {
    val state: StateFlow<AuthState>
    fun login(email: String, password: String)
    fun register(email: String, password: String, displayName: String, phone: String? = null)
    fun resetState()
}