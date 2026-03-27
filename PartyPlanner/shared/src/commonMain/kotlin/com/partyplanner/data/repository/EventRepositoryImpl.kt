package com.partyplanner.data.repository

import com.partyplanner.data.remote.EventApi
import com.partyplanner.data.remote.dto.CreateEventRequest
import com.partyplanner.data.remote.dto.EventResponse
import com.partyplanner.data.remote.dto.UpdateEventRequest
import com.partyplanner.domain.model.Event
import com.partyplanner.domain.repository.EventRepository
import kotlinx.datetime.LocalDateTime

class EventRepositoryImpl(private val eventApi: EventApi) : EventRepository {

    override suspend fun getEvents(): Result<List<Event>> = runCatching {
        eventApi.getEvents().map { it.toDomain() }
    }

    override suspend fun getEvent(id: Int): Result<Event> = runCatching {
        eventApi.getEvent(id).toDomain()
    }

    override suspend fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    ): Result<Event> = runCatching {
        eventApi.createEvent(
            CreateEventRequest(title, description, location, startDate, endDate)
        ).toDomain()
    }

    override suspend fun updateEvent(
        id: Int,
        title: String?,
        description: String?,
        location: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ): Result<Event> = runCatching {
        eventApi.updateEvent(
            id, UpdateEventRequest(title, description, location, startDate, endDate)
        ).toDomain()
    }

    override suspend fun deleteEvent(id: Int): Result<Unit> = runCatching {
        eventApi.deleteEvent(id)
    }

    private fun EventResponse.toDomain() = Event(
        id          = id,
        title       = title,
        description = description,
        location    = location,
        startDate   = startDate,
        endDate     = endDate,
        ownerId     = ownerId,
        inviteToken = inviteToken,
        createdAt   = createdAt,
    )
}
