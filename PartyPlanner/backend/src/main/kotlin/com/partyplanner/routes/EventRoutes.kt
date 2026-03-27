package com.partyplanner.routes

import com.partyplanner.dto.CreateEventRequest
import com.partyplanner.dto.UpdateEventRequest
import com.partyplanner.services.EventService
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.eventRoutes(eventService: EventService) {
    authenticate("auth-jwt") {
        route("/events") {
            get {
                val userId = call.userId()
                call.respond(eventService.getEventsForUser(userId))
            }

            post {
                val userId = call.userId()
                val request = call.receive<CreateEventRequest>()
                val event = eventService.createEvent(userId, request)
                call.respond(HttpStatusCode.Created, event)
            }

            route("/{id}") {
                get {
                    val id = call.parameters["id"]!!.toInt()
                    runCatching { eventService.getEvent(id, call.userId()) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respond(HttpStatusCode.NotFound, mapOf("error" to it.message)) }
                }

                put {
                    val id = call.parameters["id"]!!.toInt()
                    val request = call.receive<UpdateEventRequest>()
                    runCatching { eventService.updateEvent(id, call.userId(), request) }
                        .onSuccess { call.respond(it) }
                        .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                }

                delete {
                    val id = call.parameters["id"]!!.toInt()
                    runCatching { eventService.deleteEvent(id, call.userId()) }
                        .onSuccess { call.respond(HttpStatusCode.NoContent) }
                        .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                }
            }
        }
    }
}

private fun ApplicationCall.userId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
