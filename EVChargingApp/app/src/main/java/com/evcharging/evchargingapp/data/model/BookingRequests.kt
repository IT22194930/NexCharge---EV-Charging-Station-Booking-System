package com.evcharging.evchargingapp.data.model

import com.google.gson.annotations.SerializedName

data class BookingCreateRequest(
    @SerializedName("OwnerNic")
    val ownerNic: String,
    @SerializedName("StationId")
    val stationId: String,
    @SerializedName("ReservationDate")
    val reservationDate: String,
    @SerializedName("ReservationHour")
    val reservationHour: Int
)

data class BookingUpdateRequest(
    @SerializedName("ReservationDate")
    val reservationDate: String,
    @SerializedName("ReservationHour")
    val reservationHour: Int,
    @SerializedName("StationId")
    val stationId: String
)

// New DTOs for availability checking
data class AvailableSlot(
    val hour: Int,
    val availableSlots: Int,
    val totalSlots: Int
)

data class StationAvailability(
    val stationId: String,
    val stationName: String,
    val date: String,
    val availableHours: List<AvailableSlot>
)