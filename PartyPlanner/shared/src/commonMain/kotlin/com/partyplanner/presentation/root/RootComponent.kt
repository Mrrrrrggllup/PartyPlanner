package com.partyplanner.presentation.root

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.partyplanner.presentation.auth.AuthComponent
import com.partyplanner.presentation.main.MainComponent

interface RootComponent {
    val childStack: Value<ChildStack<*, Child>>

    sealed class Child {
        class AuthChild(val component: AuthComponent) : Child()
        class MainChild(val component: MainComponent) : Child()
    }
}