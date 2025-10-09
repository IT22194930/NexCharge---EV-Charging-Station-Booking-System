package com.evcharging.evchargingapp.data.network

import com.evcharging.evchargingapp.data.model.api.LoginRequest
import com.evcharging.evchargingapp.data.model.api.LoginResponse
import com.evcharging.evchargingapp.data.model.api.RegisterRequest
import com.evcharging.evchargingapp.data.model.api.RegisterApiResponse
import com.evcharging.evchargingapp.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<RegisterApiResponse>

    // Dashboard APIs
    @GET("dashboard/stats")
    suspend fun getDashboardStats(): Response<DashboardStats>

    // Station APIs
    @GET("stations")
    suspend fun getAllStations(): Response<List<Station>>

    @GET("stations/{id}")
    suspend fun getStationById(@Path("id") id: String): Response<Station>

    @GET("stations/owner/{ownerNic}")
    suspend fun getStationsByOwner(@Path("ownerNic") ownerNic: String): Response<List<Station>>

    @POST("stations")
    suspend fun createStation(@Body request: StationCreateRequest): Response<Station>

    @PUT("stations/{id}")
    suspend fun updateStation(@Path("id") id: String, @Body request: StationUpdateRequest): Response<Station>

    @DELETE("stations/{id}")
    suspend fun deleteStation(@Path("id") id: String): Response<Unit>

    // Booking APIs
    @GET("bookings")
    suspend fun getAllBookings(): Response<List<Booking>>

    @GET("bookings/owner/{ownerNic}")
    suspend fun getBookingsByOwner(@Path("ownerNic") ownerNic: String): Response<List<Booking>>

    @GET("bookings/{id}")
    suspend fun getBookingById(@Path("id") id: String): Response<Booking>

    @POST("bookings")
    suspend fun createBooking(@Body request: BookingCreateRequest): Response<Booking>

    @PUT("bookings/{id}")
    suspend fun updateBooking(@Path("id") id: String, @Body request: BookingUpdateRequest): Response<Booking>

    @POST("bookings/approve/{id}")
    suspend fun approveBooking(@Path("id") id: String): Response<Booking>

    @POST("bookings/confirm/{id}")
    suspend fun confirmBooking(@Path("id") id: String): Response<Booking>

    @POST("bookings/complete/{id}")
    suspend fun completeBooking(@Path("id") id: String): Response<Booking>

    @DELETE("bookings/{id}")
    suspend fun deleteBooking(@Path("id") id: String): Response<Unit>

    // Station availability APIs
    @GET("bookings/availability/{stationId}")
    suspend fun getStationAvailability(@Path("stationId") stationId: String, @Query("date") date: String): Response<StationAvailability>

    @GET("bookings/available-hours/{stationId}")
    suspend fun getAvailableHours(@Path("stationId") stationId: String, @Query("date") date: String): Response<List<Int>>

    // EV Owner Account Management APIs
    @POST("evowner/register")
    suspend fun registerEVOwner(@Body request: EVOwnerCreateRequest): Response<EVOwner>

    @GET("evowner/profile")
    suspend fun getOwnProfile(): Response<EVOwner>

    @PUT("evowner/profile")
    suspend fun updateOwnProfile(@Body request: EVOwnerUpdateRequest): Response<EVOwnerUpdateResponse>

    @PUT("evowner/profile/change-password")
    suspend fun changePassword(@Body request: EVOwnerChangePasswordRequest): Response<MessageResponse>

    @PUT("evowner/profile/deactivate")
    suspend fun deactivateOwnAccount(): Response<MessageResponse>

    // General auth profile endpoint for all user types
    @GET("auth/profile")
    suspend fun getCurrentUserProfile(): Response<UserProfile>

    @GET("users/{nic}")
    suspend fun getOwnerByNic(@Path("nic") nic: String): Response<EVOwner>
}
