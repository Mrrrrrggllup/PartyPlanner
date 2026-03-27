package com.partyplanner.plugins

import com.partyplanner.routes.authRoutes
import com.partyplanner.routes.carpoolRoutes
import com.partyplanner.routes.chatRoutes
import com.partyplanner.routes.eventRoutes
import com.partyplanner.routes.invitationRoutes
import com.partyplanner.routes.itemRoutes
import com.partyplanner.services.AuthService
import com.partyplanner.services.CarpoolService
import com.partyplanner.services.ChatService
import com.partyplanner.services.EventService
import com.partyplanner.services.InvitationService
import com.partyplanner.services.ItemService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.getKoin

fun Application.configureRouting() {
    val authService       = getKoin().get<AuthService>()
    val eventService      = getKoin().get<EventService>()
    val invitationService = getKoin().get<InvitationService>()
    val itemService       = getKoin().get<ItemService>()
    val carpoolService    = getKoin().get<CarpoolService>()
    val chatService       = getKoin().get<ChatService>()

    routing {
        get("/health") {
            call.respondText("OK")
        }
        authRoutes(authService)
        eventRoutes(eventService)
        invitationRoutes(invitationService)
        itemRoutes(itemService)
        carpoolRoutes(carpoolService)
        chatRoutes(chatService, authService)
    }
}