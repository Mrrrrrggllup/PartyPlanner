package com.partyplanner.presentation.auth

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.usecase.auth.ResetPasswordUseCase
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

class DefaultResetPasswordComponent(
    componentContext: ComponentContext,
    private val token: String,
    private val onBack: () -> Unit,
    private val onSuccess: () -> Unit,
) : ResetPasswordComponent, ComponentContext by componentContext, KoinComponent {

    private val resetPasswordUseCase: ResetPasswordUseCase by inject()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<ResetPasswordState>(ResetPasswordState.Idle)
    override val state: StateFlow<ResetPasswordState> = _state.asStateFlow()

    override fun onBack() = onBack.invoke()

    override fun onSubmit(newPassword: String) {
        scope.launch {
            _state.value = ResetPasswordState.Loading
            resetPasswordUseCase(token, newPassword).fold(
                onSuccess = { _state.value = ResetPasswordState.Success; onSuccess() },
                onFailure = { _state.value = ResetPasswordState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }
}
