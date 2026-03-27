package com.partyplanner.presentation.home

import com.partyplanner.domain.model.Event
import kotlinx.coroutines.flow.StateFlow

interface HomeComponent {
    val state: StateFlow<HomeState>
    fun refresh()
    fun onEventClick(eventId: Int)
    fun onCreateEvent()
    fun onProfileClick()
}
