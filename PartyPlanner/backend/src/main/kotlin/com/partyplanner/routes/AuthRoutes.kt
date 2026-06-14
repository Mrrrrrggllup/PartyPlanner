package com.partyplanner.routes

import com.partyplanner.dto.ForgotPasswordRequest
import com.partyplanner.dto.LoginRequest
import com.partyplanner.dto.RegisterRequest
import com.partyplanner.dto.ResetPasswordRequest
import com.partyplanner.services.AuthService
import com.partyplanner.services.PasswordResetService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes(authService: AuthService, passwordResetService: PasswordResetService) {
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

        post("/forgot-password") {
            val request = call.receive<ForgotPasswordRequest>()
            runCatching { passwordResetService.requestReset(request.email) }
            // Always respond OK to avoid email enumeration
            call.respond(HttpStatusCode.OK, mapOf("message" to "Si cet email existe, un lien de réinitialisation a été envoyé."))
        }

        post("/reset-password") {
            val request = call.receive<ResetPasswordRequest>()
            runCatching { passwordResetService.resetPassword(request.token, request.newPassword) }
                .onSuccess { call.respond(HttpStatusCode.OK, mapOf("message" to "Mot de passe mis à jour")) }
                .onFailure { call.respond(HttpStatusCode.BadRequest, mapOf("error" to it.message)) }
        }
    }
}