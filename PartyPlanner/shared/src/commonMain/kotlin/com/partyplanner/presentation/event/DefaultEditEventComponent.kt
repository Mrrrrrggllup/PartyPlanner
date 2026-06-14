package com.partyplanner.presentation.event

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.usecase.event.GetEventUseCase
import com.partyplanner.domain.usecase.event.UpdateEventUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class DefaultEditEventComponent(
    componentContext: ComponentContext,
    private val eventId: Int,
    private val getEventUseCase: GetEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val onBack: () -> Unit,
    private val onSaved: () -> Unit,
) : EditEventComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<EditEventState>(EditEventState.Loading)
    override val state: StateFlow<EditEventState> = _state.asStateFlow()

    init {
        scope.launch {
            getEventUseCase(eventId).fold(
                onSuccess = { _state.value = EditEventState.Loaded(it) },
                onFailure = { _state.value = EditEventState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }

    override fun onBack() = onBack.invoke()

    override fun onSave(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    ) {
        val current = _state.value as? EditEventState.Loaded ?: return
        _state.value = current.copy(isSaving = true, error = null)
        scope.launch {
            updateEventUseCase(eventId, title, description, location, startDate, endDate).fold(
                onSuccess = { onSaved() },
                onFailure = {
                    _state.value = current.copy(isSaving = false, error = it.message ?: "Erreur inconnue")
                }
            )
        }
    }
}
