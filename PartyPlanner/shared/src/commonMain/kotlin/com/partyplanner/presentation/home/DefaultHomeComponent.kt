package com.partyplanner.presentation.home

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.arkivanov.essenty.lifecycle.doOnResume
import com.partyplanner.data.local.SessionStorage
import com.partyplanner.domain.usecase.event.GetEventsUseCase
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

class DefaultHomeComponent(
    componentContext: ComponentContext,
    private val getEventsUseCase: GetEventsUseCase,
    private val onEventClick: (Int) -> Unit,
    private val onCreateEvent: () -> Unit,
    private val onProfileClick: () -> Unit,
) : HomeComponent, ComponentContext by componentContext, KoinComponent {

    private val sessionStorage: SessionStorage by inject()

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    override val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadEvents()
        lifecycle.doOnResume { loadEvents() }
    }

    override fun refresh() { loadEvents() }

    override fun onEventClick(eventId: Int) = onEventClick.invoke(eventId)

    override fun onCreateEvent() = onCreateEvent.invoke()

    override fun onProfileClick() = onProfileClick.invoke()

    private fun loadEvents() {
        scope.launch {
            _state.value = HomeState.Loading
            val session = sessionStorage.getSession()
            val currentUserId = session?.userId?.toInt() ?: 0
            val displayName = session?.displayName ?: ""
            getEventsUseCase().fold(
                onSuccess = { _state.value = HomeState.Success(it, currentUserId, displayName) },
                onFailure = { _state.value = HomeState.Error(it.message ?: "Erreur inconnue") }
            )
        }
    }
}
