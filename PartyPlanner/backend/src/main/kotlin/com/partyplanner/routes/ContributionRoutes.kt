package com.partyplanner.routes

import com.partyplanner.dto.AddContributionDto
import com.partyplanner.services.ContributionService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.contributionRoutes(contributionService: ContributionService) {
    authenticate("auth-jwt") {
        route("/events/{id}/contributions") {
            get {
                val eventId = call.parameters["id"]!!.toInt()
                runCatching { contributionService.getContributions(eventId, call.contribUserId()) }
                    .onSuccess  { call.respond(it) }
                    .onFailure  {
                        val status = if (it.message?.contains("Accès refusé") == true) HttpStatusCode.Forbidden
                                     else HttpStatusCode.BadRequest
                        call.respond(status, mapOf("error" to it.message))
                    }
            }
            post {
                val eventId = call.parameters["id"]!!.toInt()
                val dto     = call.receive<AddContributionDto>()
                runCatching { contributionService.addContribution(eventId, call.contribUserId(), dto) }
                    .onSuccess  { call.respond(HttpStatusCode.Created, it) }
                    .onFailure  {
                        val status = if (it.message?.contains("Accès refusé") == true) HttpStatusCode.Forbidden
                                     else HttpStatusCode.BadRequest
                        call.respond(status, mapOf("error" to it.message))
                    }
            }
            delete("/{contributionId}") {
                val eventId        = call.parameters["id"]!!.toInt()
                val contributionId = call.parameters["contributionId"]!!.toInt()
                runCatching { contributionService.deleteContribution(eventId, contributionId, call.contribUserId()) }
                    .onSuccess { call.respond(HttpStatusCode.NoContent) }
                    .onFailure {
                        val status = if (it.message?.contains("Accès refusé") == true ||
                                         it.message?.contains("Seul") == true) HttpStatusCode.Forbidden
                                     else HttpStatusCode.BadRequest
                        call.respond(status, mapOf("error" to it.message))
                    }
            }
        }
    }
}

private fun ApplicationCall.contribUserId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
