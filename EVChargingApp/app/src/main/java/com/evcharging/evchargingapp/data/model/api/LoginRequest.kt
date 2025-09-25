package com.evcharging.evchargingapp.data.model.api

data class LoginRequest(
    val nic: String,
    val password: String
)