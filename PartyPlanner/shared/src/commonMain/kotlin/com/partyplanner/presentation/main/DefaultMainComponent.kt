package com.partyplanner.presentation.main

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.partyplanner.domain.usecase.event.CreateEventUseCase
import com.partyplanner.domain.usecase.event.DeleteEventUseCase
import com.partyplanner.domain.usecase.event.GetEventUseCase
import com.partyplanner.domain.usecase.event.GetEventsUseCase
import com.partyplanner.domain.usecase.invitation.GetInviteInfoUseCase
import com.partyplanner.domain.usecase.invitation.RsvpToInvitationUseCase
import com.partyplanner.presentation.event.DefaultCreateEventComponent
import com.partyplanner.presentation.event.DefaultEventDetailComponent
import com.partyplanner.presentation.home.DefaultHomeComponent
import com.partyplanner.presentation.invitation.DefaultInvitationComponent
import com.partyplanner.presentation.profile.DefaultProfileComponent
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DefaultMainComponent(
    componentContext: ComponentContext,
    private val initialInviteToken: String? = null,
    private val onLogout: () -> Unit = {},
) : MainComponent, ComponentContext by componentContext, KoinComponent {

    private val getEventsUseCase: GetEventsUseCase by inject()
    private val getEventUseCase: GetEventUseCase by inject()
    private val createEventUseCase: CreateEventUseCase by inject()
    private val deleteEventUseCase: DeleteEventUseCase by inject()
    private val getInviteInfoUseCase: GetInviteInfoUseCase by inject()
    private val rsvpToInvitationUseCase: RsvpToInvitationUseCase by inject()

    private val navigation = StackNavigation<Config>()

    override val childStack: Value<ChildStack<*, MainComponent.Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Home,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    init {
        initialInviteToken?.let { navigation.push(Config.Invitation(it)) }
    }

    private fun createChild(config: Config, context: ComponentContext): MainComponent.Child =
        when (config) {
            Config.Home -> MainComponent.Child.HomeChild(
                DefaultHomeComponent(
                    componentContext = context,
                    getEventsUseCase = getEventsUseCase,
                    onEventClick    = { navigation.push(Config.EventDetail(it)) },
                    onCreateEvent   = { navigation.push(Config.CreateEvent) },
                    onProfileClick  = { navigation.push(Config.Profile) },
                )
            )
            is Config.EventDetail -> MainComponent.Child.EventDetailChild(
                DefaultEventDetailComponent(
                    componentContext   = context,
                    eventId            = config.eventId,
                    getEventUseCase    = getEventUseCase,
                    deleteEventUseCase = deleteEventUseCase,
                    onBack             = { navigation.pop() },
                )
            )
            Config.CreateEvent -> MainComponent.Child.CreateEventChild(
                DefaultCreateEventComponent(
                    componentContext  = context,
                    createEventUseCase = createEventUseCase,
                    onBack            = { navigation.pop() },
                    onCreated         = { navigation.pop() },
                )
            )
            is Config.Invitation -> MainComponent.Child.InvitationChild(
                DefaultInvitationComponent(
                    componentContext        = context,
                    token                   = config.token,
                    getInviteInfoUseCase    = getInviteInfoUseCase,
                    rsvpToInvitationUseCase = rsvpToInvitationUseCase,
                    onBack                  = { navigation.pop() },
                    onRsvpSuccess           = { navigation.pop() },
                )
            )
            Config.Profile -> MainComponent.Child.ProfileChild(
                DefaultProfileComponent(
                    componentContext = context,
                    onBack           = { navigation.pop() },
                    onLogout         = onLogout,
                )
            )
        }

    @Serializable
    private sealed interface Config {
        @Serializable data object Home : Config
        @Serializable data class  EventDetail(val eventId: Int) : Config
        @Serializable data object CreateEvent : Config
        @Serializable data class  Invitation(val token: String) : Config
        @Serializable data object Profile : Config
    }
}
