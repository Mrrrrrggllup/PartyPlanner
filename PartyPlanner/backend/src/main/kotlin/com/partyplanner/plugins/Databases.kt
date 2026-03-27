package com.partyplanner.plugins

import com.partyplanner.db.DatabaseFactory
import io.ktor.server.application.*

fun Application.configureDatabases() {
    DatabaseFactory.init(environment.config)
}