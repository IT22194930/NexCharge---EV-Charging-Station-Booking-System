package com.evcharging.evchargingapp.ui.evowner.fragments.tabs

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.BookingUpdateRequest
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.databinding.FragmentActiveBookingsBinding
import com.evcharging.evchargingapp.ui.evowner.adapters.BookingAdapter
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerBookingsFragment
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.*
import java.util.*

class ActiveBookingsTabFragment : Fragment() {
    
    private var _binding: FragmentActiveBookingsBinding? = null
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
        _binding = FragmentActiveBookingsBinding.inflate(inflater, container, false)
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
            onUpdateClick = { booking ->
                showUpdateBookingDialog(booking)
            },
            getStationName = { stationId ->
                getStationName(stationId)
            }
        )
        
        binding.recyclerViewActiveBookings.apply {
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
                loadActiveBookings()
                
            } catch (e: Exception) {
                Log.e("ActiveBookingsTab", "Error loading data", e)
                showError("Failed to load data")
                showLoading(false)
            }
        }
    }

    private fun loadActiveBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                
                if (userNic != null) {
                    val response = apiService.getBookingsByOwner(userNic)
                    
                    if (!isAdded || _binding == null) return@launch
                    
                    if (response.isSuccessful && response.body() != null) {
                        val allBookingsFromApi = response.body()!!
                        Log.d("ActiveBookings", "Total bookings from API: ${allBookingsFromApi.size}")
                        
                        // Log all booking statuses to debug
                        allBookingsFromApi.forEach { booking ->
                            Log.d("ActiveBookings", "Booking ${booking.id}: status='${booking.status}', hasQR=${!booking.qrBase64.isNullOrEmpty()}")
                        }
                        
                        allBookings = allBookingsFromApi.filter { booking ->
                            booking.status.lowercase() in listOf("pending", "approved", "confirmed")
                        }
                        Log.d("ActiveBookings", "Filtered active bookings: ${allBookings.size}")
                        updateBookingsUI()
                        Log.d("ActiveBookings", "Loaded ${allBookings.size} active bookings")
                    } else {
                        showError("Failed to load bookings: ${response.message()}")
                        showEmptyState("No active bookings found")
                    }
                } else {
                    showError("User not authenticated")
                    showEmptyState("Authentication required")
                }
            } catch (e: Exception) {
                Log.e("ActiveBookings", "Error loading bookings", e)
                showError("Network error: ${e.message}")
                showEmptyState("Failed to load bookings")
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
            showEmptyState(if (searchQuery.isEmpty()) "No active bookings yet" else "No bookings match your search")
        } else {
            hideEmptyState()
            bookingAdapter.submitList(filteredBookings)
        }
    }

    private fun showQRCode(booking: Booking) {
        try {
            Log.d("ActiveBookings", "Attempting to show QR for booking: ${booking.id}, status: ${booking.status}")
            Log.d("ActiveBookings", "QR Base64 available: ${!booking.qrBase64.isNullOrEmpty()}")
            
            // Check if booking has QR code from backend
            if (!booking.qrBase64.isNullOrEmpty()) {
                Log.d("ActiveBookings", "QR Base64 length: ${booking.qrBase64!!.length}")
                
                // Use backend-provided QR code (similar to web version)
                val qrBytes = Base64.decode(booking.qrBase64, Base64.DEFAULT)
                val qrBitmap = BitmapFactory.decodeByteArray(qrBytes, 0, qrBytes.size)
                
                if (qrBitmap != null) {
                    val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_qr_code, null)
                    val imageView = dialogView.findViewById<ImageView>(R.id.imageViewQRCode)
                    imageView.setImageBitmap(qrBitmap)
                    
                    val stationName = getStationName(booking.stationId)
                    val userFriendlyDate = DateTimeUtils.formatToUserFriendly(booking.reservationDate)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Booking QR Code")
                        .setView(dialogView)
                        .setMessage("Show this QR code at $stationName\n📅 $userFriendlyDate")
                        .setPositiveButton("Close", null)
                        .show()
                    
                    Log.d("ActiveBookings", "QR Code dialog displayed successfully")
                } else {
                    Log.e("ActiveBookings", "Failed to decode QR code bitmap")
                    showError("QR code format is invalid")
                }
            } else {
                // Show message if QR code is not yet available
                Log.d("ActiveBookings", "No QR code available for booking ${booking.id}")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("QR Code Unavailable")
                    .setMessage("QR code will be available once your booking is approved by the station operator.")
                    .setPositiveButton("OK", null)
                    .show()
            }
                
        } catch (e: Exception) {
            Log.e("ActiveBookings", "Error displaying QR code", e)
            showError("Failed to display QR code: ${e.message}")
        }
    }

    private fun showBookingDetails(booking: Booking) {
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> "Your booking is waiting for station operator approval."
            "confirmed", "approved" -> "Your booking is confirmed! You can start charging."
            "completed" -> "Charging session completed successfully."
            "cancelled" -> "This booking has been cancelled."
            else -> "Booking status: ${booking.status}"
        }
        
        val stationName = getStationName(booking.stationId)
        val message = """
            Station: $stationName
            Reservation Date: ${DateTimeUtils.formatToUserFriendly(booking.reservationDate)}
            Status: ${booking.status.uppercase()}
            
            $statusMessage
        """.trimIndent()
        
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Booking Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .setNeutralButton("View QR") { _, _ ->
                showQRCode(booking)
            }
        
        // Add Update button for pending bookings
        if (booking.status.lowercase() == "pending") {
            alertDialog.setNegativeButton("Update") { _, _ ->
                showUpdateBookingDialog(booking)
            }
        }
        
        alertDialog.show()
    }

    private fun showUpdateBookingDialog(booking: Booking) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_booking, null)
        val editTextDate = dialogView.findViewById<TextInputEditText>(R.id.editTextDate)
        val editTextTime = dialogView.findViewById<TextInputEditText>(R.id.editTextTime)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonUpdate = dialogView.findViewById<MaterialButton>(R.id.buttonUpdate)
        
        // Current booking info views
        val textViewCurrentStation = dialogView.findViewById<TextView>(R.id.textViewCurrentStation)
        val textViewCurrentDateTime = dialogView.findViewById<TextView>(R.id.textViewCurrentDateTime)
        val textViewBookingStatus = dialogView.findViewById<TextView>(R.id.textViewBookingStatus)
        
        // Display current booking information
        val stationName = getStationName(booking.stationId)
        textViewCurrentStation.text = "Station: $stationName"
        textViewCurrentDateTime.text = "Current Date & Time: ${DateTimeUtils.formatToUserFriendly(booking.reservationDate)}"
        textViewBookingStatus.text = "Status: ${booking.status.uppercase()}"
        
        // Initialize with current booking date/time
        try {
            val currentDateTime = booking.reservationDate
            // Parse the current date/time and populate the fields
            if (currentDateTime.contains("T")) {
                // ISO format: "2024-01-15T14:30:00"
                val parts = currentDateTime.split("T")
                if (parts.size >= 2) {
                    editTextDate.setText(parts[0]) // "2024-01-15"
                    val timePart = parts[1].split(":") // ["14", "30", "00"]
                    if (timePart.size >= 2) {
                        editTextTime.setText("${timePart[0]}:${timePart[1]}") // "14:30"
                    }
                }
            } else if (currentDateTime.contains(" ")) {
                // Space format: "2024-01-15 14:30:00"
                val parts = currentDateTime.split(" ")
                if (parts.size >= 2) {
                    editTextDate.setText(parts[0]) // "2024-01-15"
                    val timePart = parts[1].split(":") // ["14", "30", "00"]
                    if (timePart.size >= 2) {
                        editTextTime.setText("${timePart[0]}:${timePart[1]}") // "14:30"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ActiveBookings", "Error parsing current date/time", e)
            // Set default values if parsing fails
            editTextDate.setText("")
            editTextTime.setText("")
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
            
        dialog.window?.setBackgroundDrawable(null)
        
        // Date picker
        editTextDate.setOnClickListener {
            showDatePicker { selectedDate ->
                editTextDate.setText(selectedDate)
            }
        }
        
        // Time picker
        editTextTime.setOnClickListener {
            showTimePicker { selectedTime ->
                editTextTime.setText(selectedTime)
            }
        }
        
        // Cancel button
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Update button
        buttonUpdate.setOnClickListener {
            val date = editTextDate.text.toString().trim()
            val time = editTextTime.text.toString().trim()
            
            when {
                date.isEmpty() -> {
                    showError("Please select a date")
                }
                time.isEmpty() -> {
                    showError("Please select a time")
                }
                !isValidReservationDateTime(date, time) -> {
                    showError("Selected date and time must be in the future")
                }
                else -> {
                    dialog.dismiss()
                    updateBooking(booking, date, time)
                }
            }
        }
        
        dialog.show()
    }
    
    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                onDateSelected(formattedDate)
            },
            year, month, day
        )
        
        // Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }
    
    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour, minute, true
        )
        
        timePickerDialog.show()
    }
    
    private fun isValidReservationDateTime(date: String, time: String): Boolean {
        return try {
            val dateTimeString = "$date $time"
            val reservationDateTime = Calendar.getInstance().apply {
                val parts = date.split("-")
                val timeParts = time.split(":")
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), timeParts[0].toInt(), timeParts[1].toInt())
            }
            
            val currentTime = Calendar.getInstance()
            reservationDateTime.after(currentTime)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun updateBooking(booking: Booking, newDate: String, newTime: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                
                val newDateTime = "$newDate $newTime"
                val formattedDateTime = convertToApiFormat(newDateTime)
                
                val response = apiService.updateBooking(
                    booking.id,
                    BookingUpdateRequest(
                        reservationDate = formattedDateTime,
                        stationId = booking.stationId
                    )
                )
                
                showLoading(false)
                
                if (response.isSuccessful) {
                    val stationName = getStationName(booking.stationId)
                    Toast.makeText(requireContext(), 
                        "Booking updated successfully!\n\nStation: $stationName\nNew Date & Time: ${DateTimeUtils.formatToUserFriendly(formattedDateTime)}", 
                        Toast.LENGTH_LONG).show()
                    refreshBookings() // Refresh the list
                } else {
                    showError("Failed to update booking: ${response.message()}")
                }
                
            } catch (e: Exception) {
                showLoading(false)
                Log.e("ActiveBookings", "Error updating booking", e)
                showError("Error updating booking: ${e.message}")
            }
        }
    }
    
    private fun convertToApiFormat(dateTimeString: String): String {
        try {
            Log.d("ActiveBookings", "convertToApiFormat input: $dateTimeString")
            
            // Input format: "2024-01-15 14:30"
            // Output format: "2024-01-15T14:30" (exactly like web datetime-local input)
            // This matches the web version format to prevent timezone conversion issues
            val parts = dateTimeString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0] // "2024-01-15"
                val timePart = parts[1] // "14:30"
                val result = "${datePart}T${timePart}"  // NO seconds, exactly like web
                
                Log.d("ActiveBookings", "convertToApiFormat output: $result")
                return result
            }
            Log.w("ActiveBookings", "convertToApiFormat: Invalid format, returning original: $dateTimeString")
            return dateTimeString
        } catch (e: Exception) {
            Log.e("ActiveBookings", "Error converting date format", e)
            return dateTimeString
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

    private fun deleteBooking(bookingId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val response = apiService.deleteBooking(bookingId)
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful) {
                    showError("Booking deleted successfully!")
                    loadActiveBookings() // Refresh the list
                } else {
                    showError("Failed to delete booking: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("ActiveBookings", "Error deleting booking", e)
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.recyclerViewActiveBookings.visibility = if (isLoading) View.GONE else View.VISIBLE
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
        binding.recyclerViewActiveBookings.visibility = View.GONE
        binding.textViewNoBookings.text = message
    }

    private fun hideEmptyState() {
        if (!isAdded || _binding == null) return
        
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewActiveBookings.visibility = View.VISIBLE
    }

    fun refreshBookings() {
        loadStationsAndBookings()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ActiveBookingsTabFragment()
    }
}