package com.partyplanner.di

import com.partyplanner.services.AuthService
import com.partyplanner.services.CarpoolService
import com.partyplanner.services.ChatService
import com.partyplanner.services.EventService
import com.partyplanner.services.InvitationService
import com.partyplanner.services.ItemService
import io.ktor.server.application.*
import org.koin.dsl.module

fun Application.backendModule() = module {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()

    single { AuthService(jwtSecret, jwtIssuer, jwtAudience) }
    single { EventService() }
    single { InvitationService() }
    single { ItemService() }
    single { CarpoolService() }
    single { ChatService() }
}