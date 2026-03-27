package com.partyplanner.presentation.invitation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.model.InvitationStatus
import com.partyplanner.domain.usecase.invitation.GetInviteInfoUseCase
import com.partyplanner.domain.usecase.invitation.RsvpToInvitationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DefaultInvitationComponent(
    componentContext: ComponentContext,
    private val token: String,
    private val getInviteInfoUseCase: GetInviteInfoUseCase,
    private val rsvpToInvitationUseCase: RsvpToInvitationUseCase,
    private val onBack: () -> Unit,
    private val onRsvpSuccess: () -> Unit,
) : InvitationComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<InvitationState>(InvitationState.Loading)
    override val state: StateFlow<InvitationState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        scope.launch {
            _state.value = InvitationState.Loading
            getInviteInfoUseCase(token).fold(
                onSuccess = { _state.value = InvitationState.Success(it) },
                onFailure = { _state.value = InvitationState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }

    override fun onRsvp(status: InvitationStatus) {
        val current = _state.value as? InvitationState.Success ?: return
        scope.launch {
            _state.value = current.copy(isSubmitting = true)
            rsvpToInvitationUseCase(token, status).fold(
                onSuccess = { onRsvpSuccess() },
                onFailure = { _state.value = InvitationState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }

    override fun onBack() = onBack.invoke()
}
