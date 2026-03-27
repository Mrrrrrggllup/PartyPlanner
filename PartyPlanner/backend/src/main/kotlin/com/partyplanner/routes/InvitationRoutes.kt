package com.partyplanner.routes

import com.partyplanner.dto.RsvpRequest
import com.partyplanner.services.InvitationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.invitationRoutes(invitationService: InvitationService) {
    authenticate("auth-jwt") {
        route("/invite/{token}") {
            get {
                val token = call.parameters["token"]!!
                runCatching { invitationService.getInviteInfo(token, call.invUserId()) }
                    .onSuccess  { call.respond(it) }
                    .onFailure  { call.respond(HttpStatusCode.NotFound, mapOf("error" to it.message)) }
            }
            post("/rsvp") {
                val token   = call.parameters["token"]!!
                val request = call.receive<RsvpRequest>()
                runCatching { invitationService.rsvp(token, call.invUserId(), request.status) }
                    .onSuccess  { call.respond(it) }
                    .onFailure  { call.respond(HttpStatusCode.BadRequest, mapOf("error" to it.message)) }
            }
        }
        get("/events/{id}/invitations") {
            val id = call.parameters["id"]!!.toInt()
            runCatching { invitationService.getEventInvitations(id, call.invUserId()) }
                .onSuccess  { call.respond(it) }
                .onFailure  { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
        }
    }
}

private fun ApplicationCall.invUserId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
