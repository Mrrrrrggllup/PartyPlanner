package com.partyplanner.ui.main

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.partyplanner.presentation.main.MainComponent
import com.partyplanner.ui.event.CreateEventScreen
import com.partyplanner.ui.event.EventDetailScreen
import com.partyplanner.ui.home.HomeScreen
import com.partyplanner.ui.invitation.InvitationScreen
import com.partyplanner.ui.profile.ProfileScreen

@Composable
fun MainScreen(component: MainComponent) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(slide())
    ) {
        when (val child = it.instance) {
            is MainComponent.Child.HomeChild        -> HomeScreen(child.component)
            is MainComponent.Child.EventDetailChild -> EventDetailScreen(child.component)
            is MainComponent.Child.CreateEventChild -> CreateEventScreen(child.component)
            is MainComponent.Child.InvitationChild  -> InvitationScreen(child.component)
            is MainComponent.Child.ProfileChild     -> ProfileScreen(child.component)
        }
    }
}
