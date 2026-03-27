package com.partyplanner.presentation.event

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.usecase.event.CreateEventUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime

class DefaultCreateEventComponent(
    componentContext: ComponentContext,
    private val createEventUseCase: CreateEventUseCase,
    private val onBack: () -> Unit,
    private val onCreated: () -> Unit,
) : CreateEventComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<CreateEventState>(CreateEventState.Idle)
    override val state: StateFlow<CreateEventState> = _state.asStateFlow()

    override fun onBack() = onBack.invoke()

    override fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    ) {
        scope.launch {
            _state.value = CreateEventState.Loading
            createEventUseCase(title, description, location, startDate, endDate).fold(
                onSuccess = { _state.value = CreateEventState.Success; onCreated() },
                onFailure = { _state.value = CreateEventState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }
}
