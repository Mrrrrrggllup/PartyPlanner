package com.partyplanner.presentation.root

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.partyplanner.domain.repository.AuthRepository
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.presentation.auth.DefaultAuthComponent
import com.partyplanner.presentation.main.DefaultMainComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val authRepository: AuthRepository,
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val initialInviteToken: String? = null,
) : RootComponent, ComponentContext by componentContext {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).also {
        lifecycle.doOnDestroy(it::cancel)
    }
    private val navigation = StackNavigation<Config>()

    // Stored so we can hand it off after auth when the user wasn't logged in
    private var pendingInviteToken: String? = initialInviteToken

    override val childStack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Auth,
        handleBackButton = true,
        childFactory = ::createChild
    )

    init {
        scope.launch {
            if (authRepository.getStoredSession() != null) {
                navigation.replaceAll(Config.Main(pendingInviteToken))
            }
        }
    }

    private fun createChild(config: Config, context: ComponentContext): RootComponent.Child =
        when (config) {
            Config.Auth -> RootComponent.Child.AuthChild(
                DefaultAuthComponent(
                    componentContext = context,
                    loginUseCase = loginUseCase,
                    registerUseCase = registerUseCase,
                    onAuthSuccess = {
                        val token = pendingInviteToken
                        pendingInviteToken = null
                        navigation.replaceAll(Config.Main(token))
                    }
                )
            )
            is Config.Main -> RootComponent.Child.MainChild(
                DefaultMainComponent(
                    componentContext = context,
                    initialInviteToken = config.inviteToken,
                    onLogout = { navigation.replaceAll(Config.Auth) },
                )
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable data object Auth : Config
        @Serializable data class Main(val inviteToken: String? = null) : Config
    }
}
