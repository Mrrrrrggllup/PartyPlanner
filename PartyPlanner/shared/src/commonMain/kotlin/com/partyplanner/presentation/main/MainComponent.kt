package com.partyplanner.presentation.main

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.partyplanner.presentation.event.CreateEventComponent
import com.partyplanner.presentation.event.EventDetailComponent
import com.partyplanner.presentation.home.HomeComponent
import com.partyplanner.presentation.invitation.InvitationComponent
import com.partyplanner.presentation.profile.ProfileComponent

interface MainComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        class HomeChild(val component: HomeComponent) : Child()
        class EventDetailChild(val component: EventDetailComponent) : Child()
        class CreateEventChild(val component: CreateEventComponent) : Child()
        class InvitationChild(val component: InvitationComponent) : Child()
        class ProfileChild(val component: ProfileComponent) : Child()
    }
}
