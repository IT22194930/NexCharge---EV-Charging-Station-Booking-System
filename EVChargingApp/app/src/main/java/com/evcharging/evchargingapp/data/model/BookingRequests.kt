package com.evcharging.evchargingapp.data.model

import com.google.gson.annotations.SerializedName

data class BookingCreateRequest(
    @SerializedName("ownerNic")
    val ownerNic: String,
    @SerializedName("stationId")
    val stationId: String,
    @SerializedName("reservationDate")
    val reservationDate: String
)

data class BookingUpdateRequest(
    @SerializedName("reservationDate")
    val reservationDate: String
)