package com.partyplanner.fake

import com.partyplanner.domain.model.CarpoolOffer
import com.partyplanner.domain.model.Event
import com.partyplanner.domain.model.ItemRequest
import com.partyplanner.domain.model.User
import kotlinx.datetime.LocalDateTime

internal val testUser = User(id = 1, email = "test@example.com", displayName = "Test User")

internal val testEvent = Event(
    id = 1,
    title = "Test Party",
    description = null,
    location = null,
    startDate = LocalDateTime(2026, 10, 1, 18, 0),
    endDate = null,
    ownerId = 1,
    ownerName = "Test User",
    inviteToken = "test-token",
    createdAt = LocalDateTime(2026, 9, 1, 10, 0),
    currentUserInvitationStatus = null,
)

internal val testItemRequest = ItemRequest(
    id = 1,
    label = "Chips",
    quantity = 2,
    isFulfilled = false,
    assignedToName = null,
    requestedById = 1,
    categoryId = null,
    categoryLabel = null,
    categoryIcon = null,
)

internal val testInviteInfo = com.partyplanner.domain.model.InviteInfo(
    eventId = 1,
    title = "Test Party",
    startDate = LocalDateTime(2026, 10, 1, 18, 0),
    endDate = null,
    location = null,
    organizerName = "Test User",
    isOwner = false,
    currentStatus = null,
)

internal val testInvitation = com.partyplanner.domain.model.Invitation(
    id = 1,
    userId = 2,
    userDisplayName = "Bob",
    status = com.partyplanner.domain.model.InvitationStatus.PENDING,
    respondedAt = null,
)

internal val testCarpoolOffer = CarpoolOffer(
    id = 1,
    driverId = 1,
    driverName = "Test User",
    seatsAvailable = 3,
    seatsRemaining = 3,
    departurePoint = null,
    departureTime = null,
    notes = null,
    passengers = emptyList(),
)
