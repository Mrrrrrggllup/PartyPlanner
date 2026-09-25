package com.partyplanner.fake

import com.partyplanner.domain.model.Event
import com.partyplanner.domain.repository.EventRepository
import kotlinx.datetime.LocalDateTime

internal class FakeEventRepository(
    private val getEventsResult: Result<List<Event>> = Result.success(listOf(testEvent)),
    private val getEventResult: Result<Event> = Result.success(testEvent),
    private val createEventResult: Result<Event> = Result.success(testEvent),
    private val updateEventResult: Result<Event> = Result.success(testEvent),
    private val deleteEventResult: Result<Unit> = Result.success(Unit),
) : EventRepository {

    var lastCreatedTitle: String? = null
    var deleteCalledWithId: Int? = null

    override suspend fun getEvents(): Result<List<Event>> = getEventsResult

    override suspend fun getEvent(id: Int): Result<Event> = getEventResult

    override suspend fun createEvent(
        title: String,
        description: String?,
        location: String?,
        startDate: LocalDateTime,
        endDate: LocalDateTime?,
    ): Result<Event> {
        lastCreatedTitle = title
        return createEventResult
    }

    override suspend fun updateEvent(
        id: Int,
        title: String?,
        description: String?,
        location: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
    ): Result<Event> = updateEventResult

    override suspend fun deleteEvent(id: Int): Result<Unit> {
        deleteCalledWithId = id
        return deleteEventResult
    }
}
