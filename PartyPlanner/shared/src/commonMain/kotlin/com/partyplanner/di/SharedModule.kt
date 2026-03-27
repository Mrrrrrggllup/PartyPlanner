package com.partyplanner.di

import com.partyplanner.data.local.SessionStorage
import com.partyplanner.data.remote.AuthApi
import com.partyplanner.data.remote.ChatApi
import com.partyplanner.data.remote.CarpoolApi
import com.partyplanner.data.remote.EventApi
import com.partyplanner.data.remote.InvitationApi
import com.partyplanner.data.remote.ItemApi
import com.partyplanner.data.repository.AuthRepositoryImpl
import com.partyplanner.data.repository.CarpoolRepositoryImpl
import com.partyplanner.data.repository.ChatRepositoryImpl
import com.partyplanner.data.repository.EventRepositoryImpl
import com.partyplanner.data.repository.InvitationRepositoryImpl
import com.partyplanner.data.repository.ItemRepositoryImpl
import com.partyplanner.domain.repository.AuthRepository
import com.partyplanner.domain.repository.CarpoolRepository
import com.partyplanner.domain.repository.ChatRepository
import com.partyplanner.domain.repository.EventRepository
import com.partyplanner.domain.repository.InvitationRepository
import com.partyplanner.domain.repository.ItemRepository
import com.partyplanner.domain.usecase.carpool.CreateCarpoolOfferUseCase
import com.partyplanner.domain.usecase.carpool.DeleteCarpoolOfferUseCase
import com.partyplanner.domain.usecase.carpool.GetCarpoolOffersUseCase
import com.partyplanner.domain.usecase.carpool.JoinCarpoolUseCase
import com.partyplanner.domain.usecase.carpool.LeaveCarpoolUseCase
import com.partyplanner.domain.usecase.auth.LoginUseCase
import com.partyplanner.domain.usecase.auth.LogoutUseCase
import com.partyplanner.domain.usecase.auth.RegisterUseCase
import com.partyplanner.domain.usecase.event.CreateEventUseCase
import com.partyplanner.domain.usecase.event.DeleteEventUseCase
import com.partyplanner.domain.usecase.event.GetEventUseCase
import com.partyplanner.domain.usecase.event.GetEventsUseCase
import com.partyplanner.domain.usecase.event.UpdateEventUseCase
import com.partyplanner.domain.usecase.invitation.GetEventInvitationsUseCase
import com.partyplanner.domain.usecase.invitation.GetInviteInfoUseCase
import com.partyplanner.domain.usecase.invitation.RsvpToInvitationUseCase
import com.partyplanner.domain.usecase.item.AddItemBroughtUseCase
import com.partyplanner.domain.usecase.item.AddItemRequestUseCase
import com.partyplanner.domain.usecase.item.DeleteItemBroughtUseCase
import com.partyplanner.domain.usecase.item.DeleteItemRequestUseCase
import com.partyplanner.domain.usecase.item.FulfillItemRequestUseCase
import com.partyplanner.domain.usecase.item.GetCategoriesUseCase
import com.partyplanner.domain.usecase.item.GetItemsUseCase
import com.partyplanner.util.BASE_URL
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val sharedModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(WebSockets)
        }
    }

    single { AuthApi(get(), BASE_URL) }
    single { SessionStorage(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }

    factory { LoginUseCase(get()) }
    factory { RegisterUseCase(get()) }
    factory { LogoutUseCase(get()) }

    single { EventApi(get(), BASE_URL, get()) }
    single<EventRepository> { EventRepositoryImpl(get()) }

    factory { GetEventsUseCase(get()) }
    factory { GetEventUseCase(get()) }
    factory { CreateEventUseCase(get()) }
    factory { UpdateEventUseCase(get()) }
    factory { DeleteEventUseCase(get()) }

    single { InvitationApi(get(), BASE_URL, get()) }
    single<InvitationRepository> { InvitationRepositoryImpl(get()) }

    factory { GetInviteInfoUseCase(get()) }
    factory { RsvpToInvitationUseCase(get()) }
    factory { GetEventInvitationsUseCase(get()) }

    single { ItemApi(get(), BASE_URL, get()) }
    single<ItemRepository> { ItemRepositoryImpl(get()) }

    factory { GetCategoriesUseCase(get()) }
    factory { GetItemsUseCase(get()) }
    factory { AddItemRequestUseCase(get()) }
    factory { FulfillItemRequestUseCase(get()) }
    factory { DeleteItemRequestUseCase(get()) }
    factory { AddItemBroughtUseCase(get()) }
    factory { DeleteItemBroughtUseCase(get()) }

    single { CarpoolApi(get(), BASE_URL, get()) }
    single<CarpoolRepository> { CarpoolRepositoryImpl(get()) }

    factory { ChatApi(get(), BASE_URL, get()) }
    factory<ChatRepository> { ChatRepositoryImpl(get()) }

    factory { GetCarpoolOffersUseCase(get()) }
    factory { CreateCarpoolOfferUseCase(get()) }
    factory { DeleteCarpoolOfferUseCase(get()) }
    factory { JoinCarpoolUseCase(get()) }
    factory { LeaveCarpoolUseCase(get()) }
}