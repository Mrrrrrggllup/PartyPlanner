package com.partyplanner.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.partyplanner.db.PartyPlannerDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(PartyPlannerDatabase.Schema, "partyplanner.db")
}