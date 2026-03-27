package com.partyplanner.db.tables

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

enum class CarpoolPassengerStatus { MATCHED, CANCELLED }

object CarpoolOffers : IntIdTable("carpool_offers") {
    val eventId        = reference("event_id", Events)
    val driverId       = reference("driver_id", Users)
    val seatsAvailable = integer("seats_available")
    val departurePoint = varchar("departure_point", 300).nullable()
    val departureTime  = datetime("departure_time").nullable()
    val notes          = text("notes").nullable()
}

class CarpoolOfferEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CarpoolOfferEntity>(CarpoolOffers)

    var event          by EventEntity referencedOn CarpoolOffers.eventId
    var driver         by UserEntity  referencedOn CarpoolOffers.driverId
    var seatsAvailable by CarpoolOffers.seatsAvailable
    var departurePoint by CarpoolOffers.departurePoint
    var departureTime  by CarpoolOffers.departureTime
    var notes          by CarpoolOffers.notes
}

object CarpoolPassengers : IntIdTable("carpool_passengers") {
    val offerId     = reference("offer_id", CarpoolOffers)
    val passengerId = reference("passenger_id", Users)
    val status      = enumerationByName("status", 20, CarpoolPassengerStatus::class)
    val pickupPoint = varchar("pickup_point", 300).nullable()

    init { uniqueIndex(offerId, passengerId) }
}

class CarpoolPassengerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<CarpoolPassengerEntity>(CarpoolPassengers)

    var offer       by CarpoolOfferEntity referencedOn CarpoolPassengers.offerId
    var passenger   by UserEntity         referencedOn CarpoolPassengers.passengerId
    var status      by CarpoolPassengers.status
    var pickupPoint by CarpoolPassengers.pickupPoint
}
