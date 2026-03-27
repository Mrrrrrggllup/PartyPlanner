package com.partyplanner.db

import com.partyplanner.db.tables.*
import com.partyplanner.db.tables.CarpoolOffers
import com.partyplanner.db.tables.CarpoolPassengers
import io.ktor.server.config.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init(config: ApplicationConfig) {
        val url      = config.property("database.url").getString()
        val user     = config.property("database.user").getString()
        val password = config.property("database.password").getString()

        Database.connect(
            url    = url,
            driver = "org.postgresql.Driver",
            user   = user,
            password = password
        )

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Users, Events, Invitations,
                ItemCategories, ItemRequests, ItemsBrought,
                CarpoolOffers, CarpoolPassengers,
                ChatMessages,
            )
            seedCategories()
        }
    }

    private fun seedCategories() {
        if (ItemCategoryEntity.all().empty()) {
            listOf(
                "Nourriture"  to "🍕",
                "Boissons"    to "🥤",
                "Desserts"    to "🍰",
                "Matériel"    to "🛠️",
                "Autre"       to "📦",
            ).forEach { (label, icon) ->
                ItemCategoryEntity.new {
                    this.label = label
                    this.icon  = icon
                    parentId   = null
                }
            }
        }
    }
}
