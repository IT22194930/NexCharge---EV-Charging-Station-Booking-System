package com.evcharging.evchargingapp.data.model.api

import com.google.gson.annotations.SerializedName

data class RegisterApiResponse(
    @SerializedName("nic")
    val nic: String?,

    @SerializedName("fullName")
    val fullName: String?,

    @SerializedName("role")
    val role: String?,

    @SerializedName("token")
    val token: String?,

    @SerializedName("message")
    val message: String?
)
