package com.partyplanner.data.local

import com.partyplanner.db.PartyPlannerDatabase
import com.partyplanner.db.Session

class SessionStorage(driverFactory: DriverFactory) {
    private val database = PartyPlannerDatabase(driverFactory.createDriver())
    private val queries = database.sessionQueries

    fun saveSession(token: String, userId: Long, displayName: String) {
        queries.clearSession()
        queries.insertSession(token, userId, displayName)
    }

    fun getSession(): Session? =
        queries.selectSession().executeAsOneOrNull()

    fun clearSession() = queries.clearSession()
}