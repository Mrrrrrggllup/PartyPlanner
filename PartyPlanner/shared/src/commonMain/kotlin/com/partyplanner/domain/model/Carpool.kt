package com.partyplanner.domain.model

import kotlinx.datetime.LocalDateTime

data class CarpoolPassenger(
    val id: Int,
    val passengerId: Int,
    val passengerName: String,
    val pickupPoint: String?,
)

data class CarpoolOffer(
    val id: Int,
    val driverId: Int,
    val driverName: String,
    val seatsAvailable: Int,
    val seatsRemaining: Int,
    val departurePoint: String?,
    val departureTime: LocalDateTime?,
    val notes: String?,
    val passengers: List<CarpoolPassenger>,
)
