package com.partyplanner.routes

import com.partyplanner.dto.LoginRequest
import com.partyplanner.dto.RegisterRequest
import com.partyplanner.services.AuthService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            runCatching { authService.register(request) }
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure { call.respond(HttpStatusCode.Conflict, mapOf("error" to it.message)) }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            runCatching { authService.login(request) }
                .onSuccess { call.respond(HttpStatusCode.OK, it) }
                .onFailure { call.respond(HttpStatusCode.Unauthorized, mapOf("error" to it.message)) }
        }
    }
}