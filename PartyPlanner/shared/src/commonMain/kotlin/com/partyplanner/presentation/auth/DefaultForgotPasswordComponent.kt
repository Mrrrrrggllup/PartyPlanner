package com.partyplanner.presentation.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.usecase.auth.ForgotPasswordUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DefaultForgotPasswordComponent(
    componentContext: ComponentContext,
    private val onBack: () -> Unit,
) : ForgotPasswordComponent, ComponentContext by componentContext, KoinComponent {

    private val forgotPasswordUseCase: ForgotPasswordUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    override val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    override fun onBack() = onBack.invoke()

    override fun onSubmit(email: String) {
        scope.launch {
            _state.value = ForgotPasswordState.Loading
            forgotPasswordUseCase(email).fold(
                onSuccess = { _state.value = ForgotPasswordState.Success },
                onFailure = { _state.value = ForgotPasswordState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }
}
