package com.evcharging.evchargingapp.ui.evowner.fragments.tabs

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.databinding.FragmentHistoryBookingsBinding
import com.evcharging.evchargingapp.ui.evowner.adapters.BookingAdapter
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerBookingsFragment
import com.evcharging.evchargingapp.utils.TokenUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class HistoryBookingsTabFragment : Fragment() {
    
    private var _binding: FragmentHistoryBookingsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var bookingAdapter: BookingAdapter
    private var allBookings = listOf<Booking>()
    private var allStations = listOf<Station>()
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadStationsAndBookings()
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            onBookingClick = { booking ->
                showBookingDetails(booking)
            },
            onDeleteClick = { booking ->
                confirmDeleteBooking(booking)
            },
            onViewQRClick = { booking ->
                showQRCode(booking)
            },
            getStationName = { stationId ->
                getStationName(stationId)
            }
        )
        
        binding.recyclerViewHistoryBookings.apply {
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
                
                // Then load bookings
                loadHistoryBookings()
                
            } catch (e: Exception) {
                Log.e("HistoryBookingsTab", "Error loading data", e)
                showError("Failed to load data")
                showLoading(false)
            }
        }
    }

    private fun loadHistoryBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                
                if (userNic != null) {
                    val response = apiService.getBookingsByOwner(userNic)
                    
                    if (!isAdded || _binding == null) return@launch
                    
                    if (response.isSuccessful && response.body() != null) {
                        allBookings = response.body()!!.filter { booking ->
                            booking.status.lowercase() in listOf("completed", "cancelled")
                        }
                        updateBookingsUI()
                        Log.d("HistoryBookings", "Loaded ${allBookings.size} history bookings")
                    } else {
                        showError("Failed to load bookings: ${response.message()}")
                        showEmptyState("No booking history found")
                    }
                } else {
                    showError("User not authenticated")
                    showEmptyState("Authentication required")
                }
            } catch (e: Exception) {
                Log.e("HistoryBookings", "Error loading bookings", e)
                showError("Network error: ${e.message}")
                showEmptyState("Failed to load booking history")
            } finally {
                showLoading(false)
            }
        }
    }

    fun filterBookings(query: String) {
        searchQuery = query
        updateBookingsUI()
    }

    private fun updateBookingsUI() {
        val filteredBookings = if (searchQuery.isEmpty()) {
            allBookings
        } else {
            allBookings.filter { booking ->
                booking.stationId.contains(searchQuery, ignoreCase = true) ||
                booking.status.contains(searchQuery, ignoreCase = true) ||
                booking.ownerNic.contains(searchQuery, ignoreCase = true)
            }
        }
        
        if (filteredBookings.isEmpty()) {
            showEmptyState(if (searchQuery.isEmpty()) "No booking history yet" else "No bookings match your search")
        } else {
            hideEmptyState()
            bookingAdapter.submitList(filteredBookings)
        }
    }

    private fun confirmDeleteBooking(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Booking")
            .setMessage("Are you sure you want to permanently delete this booking? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteBooking(booking.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQRCode(booking: Booking) {
        try {
            Log.d("HistoryBookings", "Attempting to show QR for booking: ${booking.id}, status: ${booking.status}")
            Log.d("HistoryBookings", "QR Base64 available: ${!booking.qrBase64.isNullOrEmpty()}")
            
            // Check if booking has QR code from backend
            if (!booking.qrBase64.isNullOrEmpty()) {
                Log.d("HistoryBookings", "QR Base64 length: ${booking.qrBase64!!.length}")
                
                // Use backend-provided QR code (similar to web version)
                val qrBytes = Base64.decode(booking.qrBase64, Base64.DEFAULT)
                val qrBitmap = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
                
                if (qrBitmap != null) {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_code, null)
                    val imageView = dialogView.findViewById<ImageView>(R.id.imageViewQRCode)
                    imageView.setImageBitmap(qrBitmap)
                    
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Booking QR Code")
                        .setView(dialogView)
                        .setMessage("This QR code was used for charging")
                        .setPositiveButton("Close", null)
                        .show()
                    
                    Log.d("HistoryBookings", "QR Code dialog displayed successfully")
                } else {
                    Log.e("HistoryBookings", "Failed to decode QR code bitmap")
                    showError("QR code format is invalid")
                }
            } else {
                // Show message if QR code is not available
                Log.d("HistoryBookings", "No QR code available for booking ${booking.id}")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("QR Code Unavailable")
                    .setMessage("This booking did not have a QR code generated.")
                    .setPositiveButton("OK", null)
                    .show()
            }
                
        } catch (e: Exception) {
            Log.e("HistoryBookings", "Error displaying QR code", e)
            showError("Failed to display QR code: ${e.message}")
        }
    }

    private fun showBookingDetails(booking: Booking) {
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> "This booking was waiting for approval."
            "confirmed", "approved" -> "This booking was confirmed."
            "completed" -> "Charging session was completed successfully."
            "cancelled" -> "This booking was cancelled."
            else -> "Booking status: ${booking.status}"
        }
        
        val stationName = getStationName(booking.stationId)
        val message = """
            Station: $stationName
            Reservation Date: ${booking.reservationDate}
            Status: ${booking.status.uppercase()}
            
            $statusMessage
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Booking Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("View QR") { _, _ ->
                showQRCode(booking)
            }
            .show()
    }

    private fun deleteBooking(bookingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val response = apiService.deleteBooking(bookingId)
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful) {
                    showError("Booking deleted successfully!")
                    loadHistoryBookings() // Refresh the list
                } else {
                    showError("Failed to delete booking: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("HistoryBookings", "Error deleting booking", e)
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewHistoryBookings.visibility = if (isLoading) View.GONE else View.VISIBLE
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

    fun refreshBookings() {
        loadStationsAndBookings()
    }

    private fun showEmptyState(message: String) {
        if (!isAdded || _binding == null) return
        
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewHistoryBookings.visibility = View.GONE
        binding.textViewNoBookings.text = message
    }

    private fun hideEmptyState() {
        if (!isAdded || _binding == null) return
        
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewHistoryBookings.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = HistoryBookingsTabFragment()
    }
}