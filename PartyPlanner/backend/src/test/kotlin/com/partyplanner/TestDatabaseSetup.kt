package com.partyplanner

import com.partyplanner.db.tables.CarpoolOffers
import com.partyplanner.db.tables.CarpoolPassengers
import com.partyplanner.db.tables.ChatMessages
import com.partyplanner.db.tables.DeviceTokens
import com.partyplanner.db.tables.EventCarpoolViews
import com.partyplanner.db.tables.EventItemViews
import com.partyplanner.db.tables.Events
import com.partyplanner.db.tables.Invitations
import com.partyplanner.db.tables.ItemCategories
import com.partyplanner.db.tables.ItemRequests
import com.partyplanner.db.tables.ItemsBrought
import com.partyplanner.db.tables.PasswordResetTokens
import com.partyplanner.db.tables.Users
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

private val ALL_TABLES_ORDERED = listOf(
    "password_reset_tokens",
    "chat_messages",
    "event_carpool_views",
    "carpool_passengers",
    "carpool_offers",
    "event_item_views",
    "items_brought",
    "item_requests",
    "item_categories",
    "invitations",
    "events",
    "device_tokens",
    "users",
)

object TestDatabaseSetup {

    private var initialized = false

    fun init() {
        if (initialized) return
        Database.connect(
            url = "jdbc:h2:mem:partyplanner_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
            driver = "org.h2.Driver",
        )
        transaction {
            SchemaUtils.create(
                Users,
                Events,
                Invitations,
                ItemCategories,
                ItemRequests,
                ItemsBrought,
                EventItemViews,
                CarpoolOffers,
                CarpoolPassengers,
                EventCarpoolViews,
                ChatMessages,
                PasswordResetTokens,
                DeviceTokens,
            )
        }
        initialized = true
    }

    fun clearAll() {
        transaction {
            exec("SET REFERENTIAL_INTEGRITY FALSE")
            ALL_TABLES_ORDERED.forEach { exec("DELETE FROM $it") }
            exec("SET REFERENTIAL_INTEGRITY TRUE")
        }
    }
}
