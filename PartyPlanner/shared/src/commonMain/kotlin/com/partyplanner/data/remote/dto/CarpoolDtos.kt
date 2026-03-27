package com.partyplanner.data.remote.dto

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class CreateCarpoolOfferDto(
    val seatsAvailable: Int,
    val departurePoint: String? = null,
    val departureTime: LocalDateTime? = null,
    val notes: String? = null,
)

@Serializable
data class JoinCarpoolDto(
    val pickupPoint: String? = null,
)

@Serializable
data class CarpoolPassengerResponse(
    val id: Int,
    val passengerId: Int,
    val passengerName: String,
    val pickupPoint: String?,
)

@Serializable
data class CarpoolOfferResponse(
    val id: Int,
    val driverId: Int,
    val driverName: String,
    val seatsAvailable: Int,
    val seatsRemaining: Int,
    val departurePoint: String?,
    val departureTime: LocalDateTime?,
    val notes: String?,
    val passengers: List<CarpoolPassengerResponse>,
)
