package com.partyplanner.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.partyplanner.db.PartyPlannerDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(PartyPlannerDatabase.Schema, context, "partyplanner.db")
}