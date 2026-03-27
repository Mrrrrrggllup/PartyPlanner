package com.partyplanner.plugins

import io.ktor.server.application.*
import io.ktor.server.websocket.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = null
        timeout = 60.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
}
