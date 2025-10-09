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
import com.evcharging.evchargingapp.utils.LoadingManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
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
                        
                        allBookings = allBookingsFromApi.filter { booking ->
                            booking.status.lowercase() in listOf("pending", "approved", "confirmed")
                        }.sortedWith(compareBy<Booking> { booking ->
                            // First sort criteria: Status priority (approved/confirmed first, then pending)
                            when (booking.status.lowercase()) {
                                "approved", "confirmed" -> 0 // Highest priority
                                "pending" -> 1 // Lower priority
                                else -> 2 // Fallback for any other status
                            }
                        }.thenByDescending { booking ->
                            // Second sort criteria: Within each status group, sort by date (most recent first)
                            try {
                                booking.reservationDate ?: "1970-01-01"
                            } catch (e: Exception) {
                                "1970-01-01"
                            }
                        })
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
        searchQuery = query.trim()
        updateBookingsUI()
    }

    private fun updateBookingsUI() {
        val filteredBookings = if (searchQuery.isEmpty()) {
            allBookings
        } else {
            allBookings.filter { booking ->
                val stationName = getStationName(booking.stationId)
                val bookingDate = booking.reservationDate?.let { DateTimeUtils.formatToUserFriendly(it) } ?: ""
                
                booking.stationId?.contains(searchQuery, ignoreCase = true) == true ||
                booking.status?.contains(searchQuery, ignoreCase = true) == true ||
                booking.ownerNic?.contains(searchQuery, ignoreCase = true) == true ||
                booking.id?.contains(searchQuery, ignoreCase = true) == true ||
                stationName.contains(searchQuery, ignoreCase = true) ||
                bookingDate.contains(searchQuery, ignoreCase = true)
            }
        }
        
        if (filteredBookings.isEmpty()) {
            showEmptyState(if (searchQuery.isEmpty()) "No active bookings yet" else "No bookings match \"$searchQuery\"")
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
        val stationName = getStationName(booking.stationId)
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> {
                if (isBookingOlderThan12Hours(booking)) {
                    "Your booking is waiting for station operator approval.\n\nNote: This booking can no longer be updated as it was created more than 12 hours ago."
                } else {
                    "Your booking is waiting for station operator approval."
                }
            }
            "confirmed", "approved" -> "Your booking is confirmed! You can start charging."
            "completed" -> "Charging session completed successfully."
            "cancelled" -> "This booking has been cancelled."
            else -> "Booking status: ${booking.status}"
        }
        
        val message = """
            Station: $stationName
            Time Slot: ${DateTimeUtils.formatBookingTimeRange(booking.reservationDate, booking.reservationHour)}
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
        
        // Add Update button for pending bookings that are not older than 12 hours
        if (booking.status.lowercase() == "pending" && !isBookingOlderThan12Hours(booking)) {
            alertDialog.setNegativeButton("Update") { _, _ ->
                showUpdateBookingDialog(booking)
            }
        }
        
        alertDialog.show()
    }

    private fun showUpdateBookingDialog(booking: Booking) {
        // Check if booking is older than 12 hours and prevent update
        if (isBookingOlderThan12Hours(booking)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Update Not Allowed")
                .setMessage("This booking cannot be updated because it was created more than 12 hours ago. Bookings can only be modified within 12 hours of creation.")
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
            return
        }
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_booking, null)
        val editTextDate = dialogView.findViewById<TextInputEditText>(R.id.editTextDate)
        val editTextHour = dialogView.findViewById<TextInputEditText>(R.id.editTextHour)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.buttonCancel)
        val buttonUpdate = dialogView.findViewById<MaterialButton>(R.id.buttonUpdate)
        
        // Current booking info views
        val textViewCurrentStation = dialogView.findViewById<TextView>(R.id.textViewCurrentStation)
        val textViewCurrentDateTime = dialogView.findViewById<TextView>(R.id.textViewCurrentDateTime)
        val textViewBookingStatus = dialogView.findViewById<TextView>(R.id.textViewBookingStatus)
        
        // Display current booking information
        val stationName = getStationName(booking.stationId)
        textViewCurrentStation.text = "Station: $stationName"
        textViewCurrentDateTime.text = "Time Slot: ${DateTimeUtils.formatBookingTimeRange(booking.reservationDate, booking.reservationHour)}"
        textViewBookingStatus.text = "Status: ${booking.status.uppercase()}"
        
        // Initialize with current booking date and hour
        try {
            // Set the date from reservation date (extract only the date part)
            editTextDate.setText(DateTimeUtils.extractDateOnly(booking.reservationDate))
            
            // Set the hour from reservation hour
            val hour = booking.reservationHour
            editTextHour.setText("${hour}:00 - ${hour + 1}:00")
            editTextHour.tag = hour // Store the current hour value
            
        } catch (e: Exception) {
            Log.e("ActiveBookings", "Error setting current booking values", e)
            // Set default values if parsing fails
            editTextDate.setText("")
            editTextHour.setText("")
            editTextHour.tag = null
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
                // Clear hour selection when date changes
                editTextHour.setText("")
                editTextHour.tag = null
            }
        }
        
        // Hour slot picker
        editTextHour.setOnClickListener {
            val selectedDate = editTextDate.text.toString()
            if (selectedDate.isEmpty()) {
                showError("Please select a date first")
                return@setOnClickListener
            }
            
            // Basic hour picker (0-23)
            val hours = (0..23).map { hour -> "${hour}:00 - ${hour + 1}:00" }.toTypedArray()
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Hour Slot")
                .setItems(hours) { _, which ->
                    editTextHour.setText(hours[which])
                    editTextHour.tag = which // Store the actual hour value
                }
                .show()
        }
        
        // Cancel button
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        // Update button
        buttonUpdate.setOnClickListener {
            val date = editTextDate.text.toString().trim()
            val time = editTextHour.text.toString().trim()
            val selectedHour = editTextHour.tag as? Int
            
            when {
                date.isEmpty() -> {
                    showError("Please select a date")
                }
                time.isEmpty() || selectedHour == null -> {
                    showError("Please select an available hour slot")
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

    private fun isBookingOlderThan12Hours(booking: Booking): Boolean {
        try {
            // Parse the booking creation time
            val createdAt = booking.createdAt
            if (createdAt.isNullOrEmpty()) {
                Log.w("ActiveBookingsTab", "Booking createdAt is null or empty, allowing update")
                return false // If no creation time, allow update for safety
            }
            
            // Parse the creation date (assuming ISO format like "2024-10-08T10:30:00")
            val bookingCreationTime = Calendar.getInstance().apply {
                val isoDate = createdAt.replace("T", " ").substring(0, 16) 
                val parts = isoDate.split(" ")
                val dateParts = parts[0].split("-")
                val timeParts = parts[1].split(":")
                
                set(Calendar.YEAR, dateParts[0].toInt())
                set(Calendar.MONTH, dateParts[1].toInt() - 1) // Month is 0-based
                set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            val currentTime = Calendar.getInstance()
            val twelveHoursAgo = Calendar.getInstance().apply {
                add(Calendar.HOUR_OF_DAY, -12)
            }
            
            Log.d("ActiveBookingsTab", "Checking booking age - Created: ${bookingCreationTime.time}, 12h ago: ${twelveHoursAgo.time}")
            
            // Return true if booking was created more than 12 hours ago
            return bookingCreationTime.before(twelveHoursAgo)
            
        } catch (e: Exception) {
            Log.e("ActiveBookingsTab", "Error parsing booking creation time: ${booking.createdAt}", e)
            return false // If parsing fails, allow update for safety
        }
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
                        reservationHour = 0,
                        stationId = booking.stationId
                    )
                )
                
                showLoading(false)
                
                if (response.isSuccessful) {
                    val stationName = getStationName(booking.stationId)
                    Toast.makeText(requireContext(), 
                        "Booking updated successfully!", 
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
                LoadingManager.show(requireContext(), "Deleting booking...")
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
                LoadingManager.dismiss()
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