package com.partyplanner.routes

import com.partyplanner.db.tables.UserEntity
import com.partyplanner.dto.DeviceTokenRequest
import com.partyplanner.dto.MeResponse
import com.partyplanner.services.NotificationService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.userRoutes(notificationService: NotificationService) {
    authenticate("auth-jwt") {
        get("/users/me") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
            val canExport = transaction { UserEntity.findById(userId)?.canExport ?: false }
            call.respond(HttpStatusCode.OK, MeResponse(canExport = canExport))
        }

        // Register / refresh FCM token for this device
        post("/users/me/device-token") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
            val body   = call.receive<DeviceTokenRequest>()
            notificationService.registerToken(userId, body.token, body.platform)
            call.respond(HttpStatusCode.NoContent)
        }

        // App came to foreground — reset the pending notification flag
        delete("/users/me/notification") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
            notificationService.markActive(userId)
            call.respond(HttpStatusCode.NoContent)
        }

        // User opened an event screen — suppress notifications for this event while viewing
        put("/users/me/event-presence/{eventId}") {
            val userId  = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
            val eventId = call.parameters["eventId"]?.toIntOrNull()
                ?: return@put call.respond(HttpStatusCode.BadRequest)
            notificationService.enterEvent(userId, eventId)
            call.respond(HttpStatusCode.NoContent)
        }

        // User left the event screen — resume notifications for this event
        delete("/users/me/event-presence") {
            val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
            notificationService.leaveEvent(userId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
