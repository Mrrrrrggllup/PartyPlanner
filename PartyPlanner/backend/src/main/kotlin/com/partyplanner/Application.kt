package com.partyplanner

import com.partyplanner.di.backendModule
import com.partyplanner.plugins.*
import io.ktor.server.application.*import io.ktor.server.netty.*
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    install(Koin) {
        modules(backendModule())
    }
    configureLogging()
    configureSerialization()
    configureSockets()
    configureDatabases()
    configureSecurity()
    configureRouting()
}