package com.partyplanner.routes

import com.partyplanner.dto.InviteByEmailRequest
import com.partyplanner.dto.InviteByUserIdRequest
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
        get("/events/{id}/invite-suggestions") {
            val eventId = call.parameters["id"]!!.toInt()
            runCatching { invitationService.getInviteSuggestions(eventId, call.invUserId()) }
                .onSuccess { call.respond(it) }
                .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
        }
        post("/events/{id}/invite-user") {
            val eventId = call.parameters["id"]!!.toInt()
            val request = call.receive<InviteByUserIdRequest>()
            runCatching { invitationService.inviteByUserId(eventId, call.invUserId(), request.userId) }
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure {
                    val status = when {
                        it.message?.contains("Accès refusé") == true  -> HttpStatusCode.Forbidden
                        it.message?.contains("déjà invité") == true   -> HttpStatusCode.Conflict
                        it.message?.contains("introuvable") == true   -> HttpStatusCode.NotFound
                        else                                           -> HttpStatusCode.BadRequest
                    }
                    call.respond(status, mapOf("error" to it.message))
                }
        }
        post("/events/{id}/invite") {
            val eventId = call.parameters["id"]!!.toInt()
            val request = call.receive<InviteByEmailRequest>()
            runCatching { invitationService.inviteByEmail(eventId, call.invUserId(), request.email.trim()) }
                .onSuccess { call.respond(HttpStatusCode.Created, it) }
                .onFailure {
                    val status = when {
                        it.message?.contains("Accès refusé") == true        -> HttpStatusCode.Forbidden
                        it.message?.contains("déjà invité") == true         -> HttpStatusCode.Conflict
                        it.message?.contains("Aucun utilisateur") == true   -> HttpStatusCode.NotFound
                        else                                                 -> HttpStatusCode.BadRequest
                    }
                    call.respond(status, mapOf("error" to it.message))
                }
        }
    }
}

private fun ApplicationCall.invUserId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
