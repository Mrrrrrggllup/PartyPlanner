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
import com.partyplanner.services.PasswordResetService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.getKoin

fun Application.configureRouting() {
    val authService          = getKoin().get<AuthService>()
    val passwordResetService = getKoin().get<PasswordResetService>()
    val eventService         = getKoin().get<EventService>()
    val invitationService    = getKoin().get<InvitationService>()
    val itemService          = getKoin().get<ItemService>()
    val carpoolService       = getKoin().get<CarpoolService>()
    val chatService          = getKoin().get<ChatService>()

    routing {
        get("/health") {
            call.respondText("OK")
        }
        // Public redirect page for invite deep links (partyplanner://invite/{token})
        // Shared as http://server/i/{token} — clickable in SMS/WhatsApp unlike custom schemes
        get("/i/{token}") {
            val token = call.parameters["token"] ?: return@get call.respondText("Lien invalide")
            val deepLink = "partyplanner://invite/$token"
            call.respondText(
                contentType = io.ktor.http.ContentType.Text.Html,
                text = """
                    <!DOCTYPE html>
                    <html lang="fr">
                    <head>
                      <meta charset="utf-8">
                      <meta name="viewport" content="width=device-width,initial-scale=1">
                      <title>Invitation PartyPlanner</title>
                      <script>window.location.replace("$deepLink");</script>
                      <style>
                        body{font-family:sans-serif;display:flex;flex-direction:column;align-items:center;
                             justify-content:center;min-height:100vh;margin:0;background:#f5f5f5;color:#333}
                        a{margin-top:24px;padding:14px 28px;background:#7c3aed;color:#fff;border-radius:50px;
                          text-decoration:none;font-size:16px;font-weight:600}
                      </style>
                    </head>
                    <body>
                      <p>🎉 Vous avez été invité à un événement !</p>
                      <a href="$deepLink">Ouvrir dans PartyPlanner</a>
                    </body>
                    </html>
                """.trimIndent()
            )
        }
        authRoutes(authService, passwordResetService)
        eventRoutes(eventService)
        invitationRoutes(invitationService)
        itemRoutes(itemService)
        carpoolRoutes(carpoolService)
        chatRoutes(chatService, authService)
    }
}