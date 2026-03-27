package com.partyplanner.domain.repository

import com.partyplanner.domain.model.Event
import kotlinx.datetime.LocalDateTime

interface EventRepository {
    suspend fun getEvents(): Result<List<Event>>
    suspend fun getEvent(id: Int): Result<Event>
    suspend fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    ): Result<Event>
    suspend fun updateEvent(
        id: Int,
        title: String?,
        description: String?,
        location: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ): Result<Event>
    suspend fun deleteEvent(id: Int): Result<Unit>
}
