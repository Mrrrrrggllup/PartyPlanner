package com.partyplanner.routes

import com.partyplanner.dto.CreateCarpoolOfferDto
import com.partyplanner.dto.JoinCarpoolDto
import com.partyplanner.services.CarpoolService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.carpoolRoutes(carpoolService: CarpoolService) {
    authenticate("auth-jwt") {
        route("/events/{id}/carpool") {
            get {
                val eventId = call.parameters["id"]!!.toInt()
                runCatching { carpoolService.getOffers(eventId, call.carpoolUserId()) }
                    .onSuccess { call.respond(it) }
                    .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
            }

            post {
                val eventId = call.parameters["id"]!!.toInt()
                val dto = call.receive<CreateCarpoolOfferDto>()
                runCatching { carpoolService.createOffer(eventId, call.carpoolUserId(), dto) }
                    .onSuccess { call.respond(HttpStatusCode.Created, it) }
                    .onFailure { call.respond(HttpStatusCode.BadRequest, mapOf("error" to it.message)) }
            }

            route("/{offerId}") {
                delete {
                    val eventId = call.parameters["id"]!!.toInt()
                    val offerId = call.parameters["offerId"]!!.toInt()
                    runCatching { carpoolService.deleteOffer(eventId, offerId, call.carpoolUserId()) }
                        .onSuccess { call.respond(HttpStatusCode.NoContent) }
                        .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                }

                post("/join") {
                    val eventId = call.parameters["id"]!!.toInt()
                    val offerId = call.parameters["offerId"]!!.toInt()
                    val dto = runCatching { call.receive<JoinCarpoolDto>() }.getOrDefault(JoinCarpoolDto())
                    runCatching { carpoolService.joinOffer(eventId, offerId, call.carpoolUserId(), dto) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respond(HttpStatusCode.BadRequest, mapOf("error" to it.message)) }
                }

                post("/leave") {
                    val eventId = call.parameters["id"]!!.toInt()
                    val offerId = call.parameters["offerId"]!!.toInt()
                    runCatching { carpoolService.leaveOffer(eventId, offerId, call.carpoolUserId()) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respond(HttpStatusCode.BadRequest, mapOf("error" to it.message)) }
                }
            }
        }
    }
}

private fun ApplicationCall.carpoolUserId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
