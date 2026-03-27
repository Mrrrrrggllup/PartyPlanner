package com.partyplanner.services

import com.partyplanner.db.tables.*
import com.partyplanner.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

class CarpoolService {

    private fun checkAccess(eventId: Int, userId: Int) {
        val event = EventEntity.findById(eventId) ?: error("Event not found")
        val isOwner   = event.owner.id.value == userId
        val isInvited = InvitationEntity.find {
            (Invitations.eventId eq eventId) and (Invitations.userId eq userId)
        }.firstOrNull() != null
        require(isOwner || isInvited) { "Access denied" }
    }

    suspend fun getOffers(eventId: Int, userId: Int): List<CarpoolOfferResponse> =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                CarpoolOfferEntity.find { CarpoolOffers.eventId eq eventId }
                    .sortedBy { it.id.value }
                    .map { it.toResponse() }
            }
        }

    suspend fun createOffer(eventId: Int, userId: Int, dto: CreateCarpoolOfferDto): CarpoolOfferResponse =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                require(dto.seatsAvailable >= 1) { "Au moins 1 place requise" }
                val event  = EventEntity.findById(eventId)!!
                val driver = UserEntity.findById(userId)!!
                CarpoolOfferEntity.new {
                    this.event     = event
                    this.driver    = driver
                    seatsAvailable = dto.seatsAvailable
                    departurePoint = dto.departurePoint?.trim()
                    departureTime  = dto.departureTime
                    notes          = dto.notes?.trim()
                }.toResponse()
            }
        }

    suspend fun deleteOffer(eventId: Int, offerId: Int, userId: Int): Unit =
        withContext(Dispatchers.IO) {
            transaction {
                val offer = CarpoolOfferEntity.findById(offerId) ?: error("Offre introuvable")
                require(offer.event.id.value == eventId) { "Offre introuvable" }
                val isDriver     = offer.driver.id.value == userId
                val isEventOwner = offer.event.owner.id.value == userId
                require(isDriver || isEventOwner) { "Access denied" }
                // Cascade — supprimer les passagers d'abord
                CarpoolPassengerEntity.find { CarpoolPassengers.offerId eq offerId }.forEach { it.delete() }
                offer.delete()
            }
        }

    suspend fun joinOffer(eventId: Int, offerId: Int, userId: Int, dto: JoinCarpoolDto): CarpoolOfferResponse =
        withContext(Dispatchers.IO) {
            transaction {
                checkAccess(eventId, userId)
                val offer = CarpoolOfferEntity.findById(offerId) ?: error("Offre introuvable")
                require(offer.event.id.value == eventId) { "Offre introuvable" }
                require(offer.driver.id.value != userId) { "Le conducteur ne peut pas rejoindre sa propre offre" }

                val alreadyIn = CarpoolPassengerEntity.find {
                    (CarpoolPassengers.offerId eq offerId) and
                    (CarpoolPassengers.passengerId eq userId) and
                    (CarpoolPassengers.status eq CarpoolPassengerStatus.MATCHED)
                }.firstOrNull()
                require(alreadyIn == null) { "Déjà inscrit" }

                val activeCount = CarpoolPassengerEntity.find {
                    (CarpoolPassengers.offerId eq offerId) and
                    (CarpoolPassengers.status eq CarpoolPassengerStatus.MATCHED)
                }.count()
                require(activeCount < offer.seatsAvailable) { "Plus de places disponibles" }

                val passenger = UserEntity.findById(userId)!!
                // Upsert: si une entrée CANCELLED existe on la remet à MATCHED
                val existing = CarpoolPassengerEntity.find {
                    (CarpoolPassengers.offerId eq offerId) and
                    (CarpoolPassengers.passengerId eq userId)
                }.firstOrNull()
                if (existing != null) {
                    existing.status      = CarpoolPassengerStatus.MATCHED
                    existing.pickupPoint = dto.pickupPoint?.trim()
                } else {
                    CarpoolPassengerEntity.new {
                        this.offer       = offer
                        this.passenger   = passenger
                        status           = CarpoolPassengerStatus.MATCHED
                        pickupPoint      = dto.pickupPoint?.trim()
                    }
                }
                offer.toResponse()
            }
        }

    suspend fun leaveOffer(eventId: Int, offerId: Int, userId: Int): CarpoolOfferResponse =
        withContext(Dispatchers.IO) {
            transaction {
                val offer = CarpoolOfferEntity.findById(offerId) ?: error("Offre introuvable")
                require(offer.event.id.value == eventId) { "Offre introuvable" }
                val entry = CarpoolPassengerEntity.find {
                    (CarpoolPassengers.offerId eq offerId) and
                    (CarpoolPassengers.passengerId eq userId)
                }.firstOrNull() ?: error("Tu n'es pas passager de cette offre")
                entry.status = CarpoolPassengerStatus.CANCELLED
                offer.toResponse()
            }
        }

    private fun CarpoolOfferEntity.toResponse(): CarpoolOfferResponse {
        val activePassengers = CarpoolPassengerEntity.find {
            (CarpoolPassengers.offerId eq id.value) and
            (CarpoolPassengers.status eq CarpoolPassengerStatus.MATCHED)
        }.map {
            CarpoolPassengerResponse(
                id            = it.id.value,
                passengerId   = it.passenger.id.value,
                passengerName = it.passenger.displayName,
                pickupPoint   = it.pickupPoint,
            )
        }
        return CarpoolOfferResponse(
            id             = id.value,
            driverId       = driver.id.value,
            driverName     = driver.displayName,
            seatsAvailable = seatsAvailable,
            seatsRemaining = seatsAvailable - activePassengers.size,
            departurePoint = departurePoint,
            departureTime  = departureTime,
            notes          = notes,
            passengers     = activePassengers,
        )
    }
}
