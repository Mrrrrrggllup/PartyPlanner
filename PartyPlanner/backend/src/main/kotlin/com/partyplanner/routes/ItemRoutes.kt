package com.partyplanner.routes

import com.partyplanner.dto.AddItemBroughtDto
import com.partyplanner.dto.AddItemRequestDto
import com.partyplanner.services.ItemService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.itemRoutes(itemService: ItemService) {
    authenticate("auth-jwt") {
        get("/items/categories") {
            call.respond(itemService.getCategories())
        }

        route("/events/{id}/items") {
            get {
                val eventId = call.parameters["id"]!!.toInt()
                runCatching { itemService.getItems(eventId, call.itemUserId()) }
                    .onSuccess { call.respond(it) }
                    .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
            }

            route("/requests") {
                post {
                    val eventId = call.parameters["id"]!!.toInt()
                    val dto = call.receive<AddItemRequestDto>()
                    runCatching { itemService.addItemRequest(eventId, call.itemUserId(), dto) }
                        .onSuccess { call.respond(HttpStatusCode.Created, it) }
                        .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                }

                route("/{rid}") {
                    delete {
                        val eventId = call.parameters["id"]!!.toInt()
                        val rid     = call.parameters["rid"]!!.toInt()
                        runCatching { itemService.deleteItemRequest(eventId, rid, call.itemUserId()) }
                            .onSuccess { call.respond(HttpStatusCode.NoContent) }
                            .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                    }

                    post("/fulfill") {
                        val eventId = call.parameters["id"]!!.toInt()
                        val rid     = call.parameters["rid"]!!.toInt()
                        runCatching { itemService.fulfillRequest(eventId, rid, call.itemUserId()) }
                            .onSuccess { call.respond(it) }
                            .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                    }
                }
            }

            route("/brought") {
                post {
                    val eventId = call.parameters["id"]!!.toInt()
                    val dto = call.receive<AddItemBroughtDto>()
                    runCatching { itemService.addItemBrought(eventId, call.itemUserId(), dto) }
                        .onSuccess { call.respond(HttpStatusCode.Created, it) }
                        .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                }

                delete("/{bid}") {
                    val eventId = call.parameters["id"]!!.toInt()
                    val bid     = call.parameters["bid"]!!.toInt()
                    runCatching { itemService.deleteItemBrought(eventId, bid, call.itemUserId()) }
                        .onSuccess { call.respond(HttpStatusCode.NoContent) }
                        .onFailure { call.respond(HttpStatusCode.Forbidden, mapOf("error" to it.message)) }
                }
            }
        }
    }
}

private fun ApplicationCall.itemUserId(): Int =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asInt()
