package com.evcharging.evchargingapp.data.model.api

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("nic") 
    val nic: String,

    @SerializedName("FullName")
    val FullName: String,

    @SerializedName("contactNo") 
    val contactNo: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("role")
    val role: String?
)

