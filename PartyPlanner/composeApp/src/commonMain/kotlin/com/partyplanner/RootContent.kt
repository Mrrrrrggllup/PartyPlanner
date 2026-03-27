package com.partyplanner

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.partyplanner.presentation.root.RootComponent
import com.partyplanner.ui.auth.AuthScreen
import com.partyplanner.ui.main.MainScreen

@Composable
fun RootContent(component: RootComponent) {
    Children(
        stack = component.childStack,
        animation = stackAnimation(slide())
    ) {
        when (val child = it.instance) {
            is RootComponent.Child.AuthChild -> AuthScreen(child.component)
            is RootComponent.Child.MainChild -> MainScreen(child.component)
        }
    }
}