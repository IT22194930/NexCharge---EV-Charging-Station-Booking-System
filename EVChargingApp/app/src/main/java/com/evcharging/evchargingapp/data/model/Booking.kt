package com.evcharging.evchargingapp.data.model

data class Booking(
    val id: String,
    val ownerNic: String,
    val stationId: String,
    val stationName: String? = null,
    val reservationDate: String,
    val status: String, // Pending, Approved, Cancelled, Completed
    val createdAt: String,
    val amount: Double? = null
)

data class BookingCreateRequest(
    val ownerNic: String,
    val stationId: String,
    val reservationDate: String
)

data class BookingUpdateRequest(
    val ownerNic: String,
    val stationId: String,
    val reservationDate: String
)

data class BookingStatusUpdateRequest(
    val status: String
)