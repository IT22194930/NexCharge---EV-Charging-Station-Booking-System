package com.evcharging.evchargingapp.ui.evowner.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.FragmentEvownerBookingsBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.ui.evowner.adapters.BookingsPagerAdapter
import com.evcharging.evchargingapp.ui.evowner.fragments.tabs.ActiveBookingsTabFragment
import com.evcharging.evchargingapp.ui.evowner.fragments.tabs.HistoryBookingsTabFragment
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class EVOwnerBookingsFragment : Fragment() {

    private var _binding: FragmentEvownerBookingsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var pagerAdapter: BookingsPagerAdapter
    private var allStations = listOf<Station>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Show loading screen while setting up
        LoadingManager.show(requireContext(), "Loading your bookings...")
        
        setupViewPager()
        setupSearchFunctionality()
        setupClickListeners()
        loadStations()
    }

    private fun setupViewPager() {
        pagerAdapter = BookingsPagerAdapter(requireActivity())
        binding.viewPager.adapter = pagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Active"
                1 -> "History"
                else -> ""
            }
        }.attach()
    }

    private fun setupSearchFunctionality() {
        binding.editTextSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            
            // Update search hint based on input
            if (query.isNotEmpty()) {
                binding.textInputLayoutSearch.hint = "Searching for \"$query\"..."
            } else {
                binding.textInputLayoutSearch.hint = "Search by station, status, or booking ID..."
            }
            
            // Filter bookings with improved search
            filterBookingsInTabs(query)
        }
    }

    private fun filterBookingsInTabs(query: String) {
        // Use the adapter's improved filtering method
        pagerAdapter.filterBookingsInAllTabs(query)
    }

    private fun setupClickListeners() {
        // This fragment only shows existing bookings
        // New booking creation is handled in EVOwnerReservationsFragment
    }



    private fun loadStations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.getAllStations()
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful && response.body() != null) {
                    allStations = response.body()!!
                    Log.d("EVOwnerBookings", "Loaded ${allStations.size} stations")
                } else {
                    Log.w("EVOwnerBookings", "Failed to load stations: ${response.code()}")
                    showError("Failed to load stations")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerBookings", "Error loading stations", e)
                if (isAdded && _binding != null) {
                    showError("Network error loading stations")
                }
            } finally {
                if (isAdded && _binding != null) {
                    LoadingManager.dismiss()
                }
            }
        }
    }



    fun confirmDeleteBooking(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Booking")
            .setMessage("Are you sure you want to permanently delete this booking? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteBooking(booking.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBooking(bookingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val response = apiService.deleteBooking(bookingId)
                
                if (response.isSuccessful) {
                    showSuccess("Booking deleted successfully!")
                    refreshTabData()
                } else {
                    showError("Failed to delete booking: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerBookings", "Error deleting booking", e)
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    fun showQRCode(booking: Booking) {
        try {
            Log.d("EVOwnerBookings", "Attempting to show QR for booking: ${booking.id}, status: ${booking.status}")
            Log.d("EVOwnerBookings", "QR Base64 available: ${!booking.qrBase64.isNullOrEmpty()}")
            
            val stationName = getStationName(booking.stationId)
            
            // Check if booking has QR code from backend (like web version)
            if (!booking.qrBase64.isNullOrEmpty()) {
                Log.d("EVOwnerBookings", "QR Base64 length: ${booking.qrBase64!!.length}")
                // Use backend-provided QR code (similar to web version)
                val qrBytes = Base64.decode(booking.qrBase64, Base64.DEFAULT)
                val qrBitmap = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
                
                if (qrBitmap != null) {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_code, null)
                    val imageView = dialogView.findViewById<ImageView>(R.id.imageViewQRCode)
                    imageView.setImageBitmap(qrBitmap)
                    
                    val timeSlot = DateTimeUtils.formatBookingTimeRange(booking.reservationDate, booking.reservationHour)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Booking QR Code")
                        .setView(dialogView)
                        .setMessage("Show this QR code at $stationName\n📅 $timeSlot\n🆔 Booking: ${booking.id}")
                        .setPositiveButton("Close", null)
                        .show()
                    
                    Log.d("EVOwnerBookings", "QR Code dialog displayed successfully")
                } else {
                    Log.e("EVOwnerBookings", "Failed to decode QR code bitmap")
                    showError("QR code format is invalid")
                }
            } else {
                // Show message if QR code is not yet available
                Log.d("EVOwnerBookings", "No QR code available for booking ${booking.id}")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("QR Code Unavailable")
                    .setMessage("QR code will be available once your booking is approved by the station operator.\n\nStation: $stationName")
                    .setPositiveButton("OK", null)
                    .show()
            }
                
        } catch (e: Exception) {
            Log.e("EVOwnerBookings", "Error displaying QR code", e)
            showError("Failed to display QR code")
        }
    }

    private fun refreshTabData() {
        // Refresh data in both tabs using the improved method
        try {
            val activeFragment = pagerAdapter.getActiveFragment(0) as? ActiveBookingsTabFragment
            val historyFragment = pagerAdapter.getActiveFragment(1) as? HistoryBookingsTabFragment
            
            activeFragment?.refreshBookings()
            historyFragment?.refreshBookings()
        } catch (e: Exception) {
            Log.e("EVOwnerBookings", "Error refreshing tab data", e)
        }
    }

    private fun showSuccess(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(selectedDate)
        }, year, month, day).apply {
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(selectedTime)
        }, hour, minute, true).show()
    }

    fun showBookingDetails(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> "Your booking is waiting for station operator approval."
            "approved" -> "Your booking is confirmed! You can start charging."
            "completed" -> "Charging session completed successfully."
            "cancelled" -> "This booking has been cancelled."
            else -> "Booking status: ${booking.status}"
        }
        
        val message = """
            Booking Details:
            
            Station: $stationName
            Time Slot: ${DateTimeUtils.formatBookingTimeRange(booking.reservationDate, booking.reservationHour)}
            Status: ${booking.status.uppercase()}
            
            $statusMessage
            
            Booking ID: ${booking.id}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Booking Information")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .setNeutralButton("View QR") { _, _ ->
                showQRCode(booking)
            }
            .show()
    }

    private fun getStationName(stationId: String?): String {
        if (stationId.isNullOrEmpty()) return "Unknown Station"
        
        // Log for debugging
        Log.d("EVOwnerBookings", "Looking for station with ID: $stationId")
        Log.d("EVOwnerBookings", "Available stations: ${allStations.map { "${it.id} -> ${it.name}" }}")
        
        val station = allStations.find { it.id == stationId }
        return if (station != null) {
            "${station.name} - ${station.location}"
        } else {
            "Unknown Station"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        // Loading state handling for bookings fragment
        // No create booking buttons in this fragment
    }

    private fun showError(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerBookingsFragment()
    }
}