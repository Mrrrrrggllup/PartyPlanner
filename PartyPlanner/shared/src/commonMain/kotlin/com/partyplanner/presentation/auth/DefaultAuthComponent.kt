package com.partyplanner.presentation.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DefaultAuthComponent(
    componentContext: ComponentContext,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val onAuthSuccess: () -> Unit = {}
) : AuthComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    override val state: StateFlow<AuthState> = _state.asStateFlow()

    override fun login(email: String, password: String) {
        scope.launch {
            _state.value = AuthState.Loading
            val result = loginUseCase(email, password)
            _state.value = result.fold(
                onSuccess = { onAuthSuccess(); AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    override fun register(email: String, password: String, displayName: String, phone: String?) {
        scope.launch {
            _state.value = AuthState.Loading
            val result = registerUseCase(email, password, displayName, phone)
            _state.value = result.fold(
                onSuccess = { onAuthSuccess(); AuthState.Success(it) },
                onFailure = { AuthState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    override fun resetState() {
        _state.value = AuthState.Idle
    }
}