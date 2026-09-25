package com.partyplanner.routes

import com.partyplanner.dto.DeviceTokenRequest
import com.partyplanner.services.NotificationService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(notificationService: NotificationService) {
    authenticate("auth-jwt") {
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
    }
}
