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
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.databinding.FragmentCompletedBookingsBinding
import com.evcharging.evchargingapp.ui.stationoperator.adapters.OperatorBookingAdapter
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CompletedBookingsTabFragment : Fragment() {
    
    private var _binding: FragmentCompletedBookingsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var bookingAdapter: OperatorBookingAdapter
    private var allBookings = listOf<Booking>()
    private var allStations = listOf<Station>()
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCompletedBookingsBinding.inflate(inflater, container, false)
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
            onCompleteClick = null, // No complete action for completed bookings
            getStationName = { stationId ->
                getStationName(stationId)
            }
        )
        
        binding.recyclerViewCompletedBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun loadStationsAndBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                
                // Load stations first
                val stationsResponse = apiService.getAllStations()
                if (stationsResponse.isSuccessful && stationsResponse.body() != null) {
                    allStations = stationsResponse.body()!!
                }
                
                // Then load completed bookings
                loadCompletedBookings()
                
            } catch (e: Exception) {
                Log.e("CompletedBookingsTab", "Error loading data", e)
                showError("Failed to load data")
                showLoading(false)
            }
        }
    }

    private fun loadCompletedBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.getAllBookings()
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful && response.body() != null) {
                    val allBookingsFromApi = response.body()!!
                    
                    // Filter for completed bookings only
                    allBookings = allBookingsFromApi.filter { 
                        it.status.equals("Completed", ignoreCase = true) 
                    }
                    
                    Log.d("CompletedBookingsTab", "Total completed bookings loaded: ${allBookings.size}")
                    
                    // Apply search filter if exists
                    filterBookings(searchQuery)
                    
                } else {
                    Log.e("CompletedBookingsTab", "Failed to load bookings: ${response.code()}")
                    showError("Failed to load completed bookings")
                }
                
            } catch (e: Exception) {
                Log.e("CompletedBookingsTab", "Error loading completed bookings", e)
                showError("Error loading completed bookings: ${e.message}")
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
                "No completed bookings found"
            } else {
                "No completed bookings match your search"
            }
            showEmptyState(message)
        } else {
            hideEmptyState()
        }
    }

    private fun showBookingDetails(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        
        val message = """
            📋 Completed Booking Details
            
            🆔 Booking ID: ${booking.id}
            👤 Customer NIC: ${booking.ownerNic}
            ⚡ Station: $stationName
            📅 Date & Time: ${DateTimeUtils.formatDateTimeWithHour(booking.reservationDate, booking.reservationHour)}
            ⏱️ Duration: ${booking.duration} hour(s)
            📊 Status: ${booking.status}
            ✅ Completed Successfully
            
            This booking has been completed successfully.
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Completed Booking Details")
            .setMessage(message)
            .setIcon(R.drawable.ic_done_all)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewCompletedBookings.visibility = if (isLoading) View.GONE else View.VISIBLE
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
        binding.recyclerViewCompletedBookings.visibility = View.GONE
        binding.textViewNoBookings.text = message
    }

    private fun hideEmptyState() {
        if (!isAdded || _binding == null) return
        
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewCompletedBookings.visibility = View.VISIBLE
    }

    fun refreshBookings() {
        loadStationsAndBookings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CompletedBookingsTabFragment()
    }
}