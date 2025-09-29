package com.evcharging.evchargingapp.ui.evowner.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.FragmentEvownerReservationsBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.BookingCreateRequest
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.ui.evowner.adapters.RecentBookingAdapter
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.*

class EVOwnerReservationsFragment : Fragment() {

    private var _binding: FragmentEvownerReservationsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var recentBookingAdapter: RecentBookingAdapter
    private val allStations = mutableListOf<Station>()
    private var recentBookings = listOf<Booking>()
    private var loadingCounter = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerReservationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Show loading screen while setting up
        LoadingManager.show(requireContext(), "Loading reservations...")
        loadingCounter = 2 // We have 2 async operations to complete
        
        setupRecyclerView()
        setupSearchFunctionality()
        setupClickListeners()
        loadStations()
        loadRecentBookings()
    }

    private fun setupRecyclerView() {
        recentBookingAdapter = RecentBookingAdapter(
            onBookingClick = { booking ->
                showBookingDetails(booking)
            },
            onViewQRClick = { booking ->
                showQRCode(booking)
            },
            onDeleteClick = { booking ->
                confirmDeleteBooking(booking)
            },
            getStationName = { stationId ->
                getStationName(stationId)
            }
        )
        
        binding.recyclerViewRecentBookings.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentBookingAdapter
        }
    }

    private fun setupSearchFunctionality() {
        binding.editTextStationSearch.addTextChangedListener { text ->
            filterStations(text.toString())
        }
    }

    private fun setupClickListeners() {
        binding.buttonCreateReservation.setOnClickListener {
            // Ensure stations are loaded before showing dialog
            if (allStations.isEmpty()) {
                // Show loading message and load stations
                showError("Loading stations... Please try again.")
                loadStations()
            } else {
                showCreateBookingDialog()
            }
        }
        
        binding.buttonQuickBookNearby.setOnClickListener {
            showNearbyStationsDialog()
        }
    }

    private fun loadRecentBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                
                if (userNic != null) {
                    val response = apiService.getBookingsByOwner(userNic)
                    
                    if (!isAdded || _binding == null) return@launch
                    
                    if (response.isSuccessful && response.body() != null) {
                        val allBookings = response.body()!!
                        // Filter out completed bookings and show only the 5 most recent active bookings
                        recentBookings = allBookings
                            .filter { it.status != "Completed" }
                            .sortedByDescending { it.createdAt ?: "" }
                            .take(5)
                        recentBookingAdapter.submitList(recentBookings)
                        Log.d("EVOwnerReservations", "Loaded ${recentBookings.size} recent active bookings (excluding completed)")
                    } else {
                        Log.w("EVOwnerReservations", "Failed to load recent bookings: ${response.code()}")
                        showError("Failed to load recent bookings")
                    }
                } else {
                    Log.w("EVOwnerReservations", "User NIC not found")
                    showError("User authentication error")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error loading recent bookings", e)
                if (isAdded && _binding != null) {
                    showError("Network error loading bookings")
                }
            } finally {
                checkLoadingComplete()
            }
        }
    }

    private fun loadStations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.getAllStations()
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful && response.body() != null) {
                    allStations.clear()
                    allStations.addAll(response.body()!!)
                    Log.d("EVOwnerReservations", "Loaded ${allStations.size} stations")
                    allStations.forEach { station ->
                        Log.d("EVOwnerReservations", "Station: ${station.name} - ${station.location} (${station.type}, ${station.availableSlots} slots)")
                    }
                } else {
                    Log.w("EVOwnerReservations", "Failed to load stations: ${response.code()}")
                    showError("Failed to load stations: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error loading stations", e)
                if (isAdded && _binding != null) {
                    showError("Error loading stations: ${e.message}")
                }
            } finally {
                checkLoadingComplete()
            }
        }
    }

    private fun checkLoadingComplete() {
        loadingCounter--
        if (loadingCounter <= 0 && isAdded && _binding != null) {
            LoadingManager.dismiss()
        }
    }

    private fun filterStations(query: String) {
        val filteredStations = if (query.isEmpty()) {
            allStations
        } else {
            allStations.filter { station ->
                station.name.contains(query, ignoreCase = true) ||
                station.location.contains(query, ignoreCase = true)
            }
        }
        updateStationsUI(filteredStations)
    }

    private fun updateStationsUI(stations: List<Station> = allStations) {
        // This would update a stations list view if we had one
        // For now, we'll just update the available stations for booking
    }

    private fun showCreateBookingDialog() {
        // Check if stations are loaded
        if (allStations.isEmpty()) {
            showError("Loading stations... Please try again in a moment.")
            loadStations() // Retry loading stations
            return
        }
        
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_booking, null)
        
        val textViewUserInfo = dialogView.findViewById<TextView>(R.id.textViewUserInfo)
        val autoCompleteStation = dialogView.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.autoCompleteStation)
        val dateEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDate)
        val timeEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextTime)
        val buttonCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCancel)
        val buttonCreate = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.buttonCreate)
        
        // Set user info
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        textViewUserInfo.text = "NIC: ${userNic ?: "Unknown"}"
        
        // Setup station dropdown with enhanced information
        val stationDisplayNames = allStations.map { station ->
            "🔌 ${station.name} - ${station.location}\n   ${station.type} | ${station.availableSlots} slots available"
        }
        
        Log.d("EVOwnerReservations", "Setting up dropdown with ${stationDisplayNames.size} stations")
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, stationDisplayNames)
        autoCompleteStation.setAdapter(adapter)
        
        // Configure the dropdown behavior
        autoCompleteStation.threshold = 0  // Show all items immediately
        autoCompleteStation.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                autoCompleteStation.showDropDown()
            }
        }
        autoCompleteStation.setOnClickListener {
            autoCompleteStation.showDropDown()
        }
        
        // Force adapter refresh
        adapter.notifyDataSetChanged()
        
        // Handle station selection
        var selectedStationIndex = -1
        autoCompleteStation.setOnItemClickListener { parent, view, position, id ->
            selectedStationIndex = position
            Log.d("EVOwnerReservations", "Selected station index: $position")
        }
        
        // Setup date picker with validation
        dateEditText.setOnClickListener {
            showEnhancedDatePicker { date ->
                dateEditText.setText(date)
            }
        }
        
        // Setup time picker with validation
        timeEditText.setOnClickListener {
            showEnhancedTimePicker { time ->
                timeEditText.setText(time)
            }
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
        
        // Handle button clicks
        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }
        
        buttonCreate.setOnClickListener {
            val date = dateEditText.text.toString()
            val time = timeEditText.text.toString()
            
            when {
                selectedStationIndex < 0 -> {
                    showError("Please select a charging station")
                }
                date.isEmpty() -> {
                    showError("Please select a reservation date")
                }
                time.isEmpty() -> {
                    showError("Please select a reservation time")
                }
                !isValidReservationDateTime(date, time) -> {
                    showError("Reservations must be made at least 1 hour in advance and within 7 days")
                }
                else -> {
                    val stationId = allStations[selectedStationIndex].id
                    val stationName = getStationName(stationId)
                    
                    // Show confirmation before creating
                    showBookingConfirmation(stationName, date, time) {
                        createBooking(stationId, "$date $time")
                        dialog.dismiss()
                    }
                }
            }
        }
        
        dialog.show()
        
        // Make dialog responsive to theme
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun showNearbyStationsDialog() {
        // Show nearby stations (simplified - you could add location logic here)
        val nearbyStations = allStations.take(5) // Just take first 5 for demo
        
        val stationNames = nearbyStations.map { "${it.name} - ${it.location}" }.toTypedArray()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Quick Book Nearby Station")
            .setItems(stationNames) { _, which ->
                val selectedStation = nearbyStations[which]
                quickBookStation(selectedStation)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun quickBookStation(station: Station) {
        // Quick book with next available slot (simplified)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR, 2) // Book for 2 hours from now
        
        val dateTime = String.format(
            "%04d-%02d-%02d %02d:%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH),
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
        
        createBooking(station.id, dateTime)
    }

    private fun createBooking(stationId: String, reservationDateTime: String) {
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        if (userNic == null) {
            showError("User not authenticated. Please log in again.")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                Log.d("EVOwnerReservations", "Creating booking for station: $stationId, dateTime: $reservationDateTime")
                
                // Convert the date format to ISO format that the API expects
                val formattedDateTime = convertToApiFormat(reservationDateTime)
                Log.d("EVOwnerReservations", "Formatted dateTime for API: $formattedDateTime")
                
                val request = BookingCreateRequest(
                    stationId = stationId,
                    ownerNic = userNic,
                    reservationDate = formattedDateTime
                )
                
                val response = apiService.createBooking(request)
                
                if (!isAdded || _binding == null) return@launch
                
                if (response.isSuccessful && response.body() != null) {
                    val createdBooking = response.body()!!
                    val stationName = getStationName(stationId)
                    
                    showBookingSuccessDialog(createdBooking, stationName)
                    loadRecentBookings() // Refresh recent bookings
                    
                    Log.d("EVOwnerReservations", "Successfully created booking: ${createdBooking.id}")
                } else {
                    val errorMsg = when (response.code()) {
                        400 -> "Invalid booking request. Please check your details."
                        404 -> "Selected charging station not found."
                        409 -> "Time slot already booked. Please select a different time."
                        500 -> "Server error. Please try again later."
                        else -> "Failed to create reservation: ${response.message()}"
                    }
                    showError(errorMsg)
                    Log.w("EVOwnerReservations", "Booking creation failed: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error creating booking", e)
                showError("Network error: Unable to create reservation. Please check your internet connection.")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun convertToApiFormat(dateTimeString: String): String {
        try {
            // Input format: "2024-01-15 14:30"
            // Output format: "2024-01-15T14:30:00" (ISO 8601 format without timezone)
            val parts = dateTimeString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0] // "2024-01-15"
                val timePart = parts[1] // "14:30"
                return "${datePart}T${timePart}:00"
            }
            return dateTimeString
        } catch (e: Exception) {
            Log.e("EVOwnerReservations", "Error converting date format", e)
            return dateTimeString
        }
    }

    private fun showBookingSuccessDialog(booking: Booking, stationName: String) {
        val message = """
            🎉 Reservation Created Successfully!
            
            Your booking has been submitted and is now pending approval from the station operator.
            
            📍 Station: $stationName
            📅 Date & Time: ${DateTimeUtils.formatToUserFriendly(booking.reservationDate)}
            🆔 Booking ID: ${booking.id}
            📊 Status: ${booking.status.uppercase()}
            
            You'll receive a QR code once your booking is approved. You can track your reservation status in the bookings section.
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Booking Confirmed")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("View My Bookings") { _, _ ->
                // Could navigate to bookings fragment if needed
            }
            .setNeutralButton("Create Another", null)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showBookingDetails(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> "Your reservation is waiting for station operator approval."
            "approved" -> "Your reservation is confirmed! You can start charging."
            "completed" -> "Charging session completed successfully."
            "cancelled" -> "This reservation has been cancelled."
            else -> "Reservation status: ${booking.status}"
        }
        
        val message = """
            Reservation Details:
            
            Station: $stationName
            Date & Time: ${DateTimeUtils.formatToUserFriendly(booking.reservationDate)}
            Status: ${booking.status.uppercase()}
            
            $statusMessage
            
            Booking ID: ${booking.id}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reservation Information")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showQRCode(booking: Booking) {
        try {
            Log.d("EVOwnerReservations", "Attempting to show QR for booking: ${booking.id}, status: ${booking.status}")
            Log.d("EVOwnerReservations", "QR Base64 available: ${!booking.qrBase64.isNullOrEmpty()}")
            
            // Check if booking has QR code from backend
            if (!booking.qrBase64.isNullOrEmpty()) {
                Log.d("EVOwnerReservations", "QR Base64 length: ${booking.qrBase64!!.length}")
                
                // Use backend-provided QR code
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
                    
                    Log.d("EVOwnerReservations", "QR Code dialog displayed successfully")
                } else {
                    Log.e("EVOwnerReservations", "Failed to decode QR code bitmap")
                    showError("QR code format is invalid")
                }
            } else {
                // Show message if QR code is not yet available
                Log.d("EVOwnerReservations", "No QR code available for booking ${booking.id}")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("QR Code Unavailable")
                    .setMessage("QR code will be available once your booking is approved by the station operator.")
                    .setPositiveButton("OK", null)
                    .show()
            }
                
        } catch (e: Exception) {
            Log.e("EVOwnerReservations", "Error displaying QR code", e)
            showError("Failed to display QR code: ${e.message}")
        }
    }

    private fun confirmDeleteBooking(booking: Booking) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Reservation")
            .setMessage("Are you sure you want to permanently delete this reservation? This action cannot be undone.")
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
                    showSuccess("Reservation deleted successfully!")
                    loadRecentBookings() // Refresh the recent bookings list
                } else {
                    showError("Failed to delete reservation: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error deleting booking", e)
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showEnhancedDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(selectedDate)
        }, year, month, day).apply {
            // Set minimum date to tomorrow (to ensure at least 1 hour advance booking)
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.DAY_OF_YEAR, 1)
            datePicker.minDate = minCalendar.timeInMillis
            
            // Set maximum date to 7 days from now
            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.DAY_OF_YEAR, 7)
            datePicker.maxDate = maxCalendar.timeInMillis
            
            show()
        }
    }

    private fun showEnhancedTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(selectedTime)
        }, hour, minute, true).show()
    }

    private fun isValidReservationDateTime(date: String, time: String): Boolean {
        try {
            val dateTimeString = "$date $time"
            val reservationDateTime = Calendar.getInstance().apply {
                val parts = date.split("-")
                val timeParts = time.split(":")
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
            }
            
            val now = Calendar.getInstance()
            val oneHourLater = Calendar.getInstance().apply {
                add(Calendar.HOUR, 1)
            }
            
            val sevenDaysLater = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
            }
            
            // Check if reservation is at least 1 hour in advance
            if (reservationDateTime.before(oneHourLater)) {
                return false
            }
            
            // Check if reservation is within 7 days
            if (reservationDateTime.after(sevenDaysLater)) {
                return false
            }
            
            return true
        } catch (e: Exception) {
            Log.e("EVOwnerReservations", "Error validating date time", e)
            return false
        }
    }

    private fun showBookingConfirmation(stationName: String, date: String, time: String, onConfirm: () -> Unit) {
        val message = """
            Confirm your reservation details:
            
            🔌 Station: $stationName
            📅 Date: $date
            ⏰ Time: $time
            
            Please review the information before proceeding.
        """.trimIndent()
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Reservation")
            .setMessage(message)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton("Confirm Booking") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("Edit Details", null)
            .show()
    }

    private fun getStationName(stationId: String?): String {
        if (stationId.isNullOrEmpty()) return "Unknown Station"
        
        // Log for debugging
        Log.d("EVOwnerReservations", "Looking for station with ID: $stationId")
        Log.d("EVOwnerReservations", "Available stations: ${allStations.map { "${it.id} -> ${it.name}" }}")
        
        val station = allStations.find { it.id == stationId }
        return if (station != null) {
            "${station.name} - ${station.location}"
        } else {
            "Unknown Station"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.buttonCreateReservation.isEnabled = !isLoading
        binding.buttonQuickBookNearby.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showSuccess(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerReservationsFragment()
    }
}