package com.evcharging.evchargingapp.ui.stationoperator.fragments.tabs

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.model.UserProfile
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.databinding.FragmentApprovedBookingsBinding
import com.evcharging.evchargingapp.ui.stationoperator.adapters.OperatorBookingAdapter
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ApprovedBookingsTabFragment : Fragment() {
    
    private var _binding: FragmentApprovedBookingsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var bookingAdapter: OperatorBookingAdapter
    private var allBookings = listOf<Booking>()
    private var allStations = listOf<Station>()
    private var searchQuery = ""
    private var operatorStationId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApprovedBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadStationsAndBookings()
    }

    private fun setupRecyclerView() {
        bookingAdapter = OperatorBookingAdapter(
            onBookingClick = { booking ->
                showBookingDetails(booking)
            },
            onCompleteClick = { booking ->
                confirmCompleteBooking(booking)
            },
            getStationName = { stationId ->
                getStationName(stationId)
            }
        )
        
        binding.recyclerViewApprovedBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun loadStationsAndBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                
                // First get current user profile to get assigned station
                getCurrentUserProfile()
                
                // Load stations first
                val stationsResponse = apiService.getAllStations()
                if (stationsResponse.isSuccessful && stationsResponse.body() != null) {
                    allStations = stationsResponse.body()!!
                }
                
                // Then load approved bookings
                loadApprovedBookings()
                
            } catch (e: Exception) {
                Log.e("ApprovedBookingsTab", "Error loading data", e)
                showError("Failed to load data")
                showLoading(false)
            }
        }
    }

    private suspend fun getCurrentUserProfile() {
        try {
            val response = apiService.getCurrentUserProfile()
            if (response.isSuccessful && response.body() != null) {
                val userProfile = response.body()!!
                operatorStationId = userProfile.assignedStationId
                Log.d("ApprovedBookingsTab", "Operator assigned to station: $operatorStationId")
            } else {
                Log.e("ApprovedBookingsTab", "Failed to load user profile: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ApprovedBookingsTab", "Error loading user profile", e)
        }
    }

    private fun loadApprovedBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.getAllBookings()
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful && response.body() != null) {
                    val allBookingsFromApi = response.body()!!
                    
                    // Filter for approved bookings only and by assigned station
                    allBookings = allBookingsFromApi.filter { booking ->
                        val isApproved = booking.status.equals("Approved", ignoreCase = true)
                        val isAssignedStation = operatorStationId == null || booking.stationId == operatorStationId
                        isApproved && isAssignedStation
                    }
                    
                    Log.d("ApprovedBookingsTab", "Total approved bookings for station $operatorStationId: ${allBookings.size}")
                    
                    // Apply search filter if exists
                    filterBookings(searchQuery)
                    
                } else {
                    Log.e("ApprovedBookingsTab", "Failed to load bookings: ${response.code()}")
                    showError("Failed to load approved bookings")
                }
                
            } catch (e: Exception) {
                Log.e("ApprovedBookingsTab", "Error loading approved bookings", e)
                showError("Error loading approved bookings: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    fun filterBookings(query: String) {
        searchQuery = query
        if (!isAdded || _binding == null) return
        
        val filteredBookings = if (query.isEmpty()) {
            allBookings
        } else {
            allBookings.filter { booking ->
                val stationName = getStationName(booking.stationId)
                booking.id.contains(query, ignoreCase = true) ||
                booking.status.contains(query, ignoreCase = true) ||
                booking.ownerNic.contains(query, ignoreCase = true) ||
                stationName.contains(query, ignoreCase = true) ||
                DateTimeUtils.formatDateTime(booking.reservationDate).contains(query, ignoreCase = true)
            }
        }
        
        bookingAdapter.updateBookings(filteredBookings)
        
        if (filteredBookings.isEmpty()) {
            val message = if (query.isEmpty()) {
                "No approved bookings found"
            } else {
                "No approved bookings match your search"
            }
            showEmptyState(message)
        } else {
            hideEmptyState()
        }
    }

    private fun showBookingDetails(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        
        val message = """
            📋 Booking Details
            
            🆔 Booking ID: ${booking.id}
            👤 Customer NIC: ${booking.ownerNic}
            ⚡ Station: $stationName
            📅 Date & Time: ${DateTimeUtils.formatDateTimeWithHour(booking.reservationDate, booking.reservationHour)}
            ⏱️ Duration: ${booking.duration} hour(s)
            📊 Status: ${booking.status}
            
            This booking is approved and ready for the customer to arrive.
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Approved Booking Details")
            .setMessage(message)
            .setIcon(R.drawable.ic_check_circle)
            .setPositiveButton("Mark Complete") { _, _ ->
                confirmCompleteBooking(booking)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun confirmCompleteBooking(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        
        val message = """
            Are you sure you want to mark this booking as completed?
            
            📋 Booking: ${booking.id}
            ⚡ Station: $stationName
            📅 Date: ${DateTimeUtils.formatDateTimeWithHour(booking.reservationDate, booking.reservationHour)}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Booking")
            .setMessage(message)
            .setIcon(R.drawable.ic_done_all)
            .setPositiveButton("Mark Complete") { _, _ ->
                completeBooking(booking.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun completeBooking(bookingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Completing booking...")
                
                val response = apiService.completeBooking(bookingId)
                
                LoadingManager.dismiss()
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Booking completed successfully!", Toast.LENGTH_SHORT).show()
                    
                    // Refresh the bookings list
                    loadApprovedBookings()
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Failed to complete booking"
                    Log.e("ApprovedBookingsTab", "Failed to complete booking: ${response.code()} - $errorMessage")
                    showError("Failed to complete booking: ${response.code()}")
                }
                
            } catch (e: Exception) {
                LoadingManager.dismiss()
                Log.e("ApprovedBookingsTab", "Error completing booking", e)
                showError("Error completing booking: ${e.message}")
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewApprovedBookings.visibility = if (isLoading) View.GONE else View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
    }

    private fun showError(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getStationName(stationId: String?): String {
        if (stationId.isNullOrEmpty()) return "Unknown Station"
        
        val station = allStations.find { it.id == stationId }
        return if (station != null) {
            "${station.name} - ${station.location}"
        } else {
            "Unknown Station"
        }
    }

    private fun showEmptyState(message: String) {
        if (!isAdded || _binding == null) return
        
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewApprovedBookings.visibility = View.GONE
        binding.textViewNoBookings.text = message
    }

    private fun hideEmptyState() {
        if (!isAdded || _binding == null) return
        
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewApprovedBookings.visibility = View.VISIBLE
    }

    fun refreshBookings() {
        loadStationsAndBookings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ApprovedBookingsTabFragment()
    }
}