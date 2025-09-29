package com.evcharging.evchargingapp.data.model

import com.google.gson.annotations.SerializedName

data class BookingCreateRequest(
    @SerializedName("OwnerNic")
    val ownerNic: String,
    @SerializedName("StationId")
    val stationId: String,
    @SerializedName("ReservationDate")
    val reservationDate: String
)

data class BookingUpdateRequest(
    @SerializedName("ReservationDate")
    val reservationDate: String,
    @SerializedName("StationId")
    val stationId: String
)