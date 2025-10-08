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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.FragmentEvownerReservationsBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.BookingCreateRequest
import com.evcharging.evchargingapp.data.model.BookingUpdateRequest
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.ui.evowner.adapters.RecentBookingAdapter
import com.evcharging.evchargingapp.ui.evowner.adapters.StationsAdapter
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.*

class EVOwnerReservationsFragment : Fragment() {

    private var _binding: FragmentEvownerReservationsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var recentBookingAdapter: RecentBookingAdapter
    private lateinit var stationsAdapter: StationsAdapter
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
            onBookingClick = { booking -> showBookingDetails(booking) },
            onViewQRClick = { booking -> showQRCode(booking) },
            onDeleteClick = { booking -> confirmDeleteBooking(booking) },
            onUpdateClick = { booking -> showUpdateBookingDialog(booking) },
            getStationName = { stationId -> getStationName(stationId) }
        )
        
        stationsAdapter = StationsAdapter { station ->
            showCreateBookingDialogForStation(station)
        }
        
        binding.recyclerViewRecentBookings.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = recentBookingAdapter
        }
        
        binding.recyclerViewStations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stationsAdapter
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
                    updateStationsUI(allStations) // Update the stations list
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
        // Update the stations adapter with filtered stations
        if (::stationsAdapter.isInitialized) {
            stationsAdapter.submitList(stations.toList()) // Create a new list to trigger update
        }
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
            val dailyCapacity = station.availableSlots * 24 // Total EVs that can be charged per day
            "🔌 ${station.name} - ${station.location}\n   ${station.type} | ${station.availableSlots} machines/hour | Up to ${dailyCapacity} EVs/day"
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
                // Clear hour selection when date changes
                timeEditText.setText("")
                timeEditText.tag = null
            }
        }
        
        // Setup hour slot picker with availability
        timeEditText.setOnClickListener {
            val selectedDate = dateEditText.text.toString()
            if (selectedDate.isEmpty()) {
                showError("Please select a date first")
                return@setOnClickListener
            }
            
            if (selectedStationIndex < 0) {
                showError("Please select a station first")
                return@setOnClickListener
            }
            
            val stationId = allStations[selectedStationIndex].id
            showAvailableHoursPicker(stationId, selectedDate) { hour ->
                timeEditText.setText("${hour}:00 - ${hour + 1}:00")
                timeEditText.tag = hour // Store the actual hour value
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
            val selectedHour = timeEditText.tag as? Int
            
            when {
                selectedStationIndex < 0 -> {
                    showError("Please select a charging station")
                }
                date.isEmpty() -> {
                    showError("Please select a reservation date")
                }
                time.isEmpty() || selectedHour == null -> {
                    showError("Please select an available hour slot")
                }
                !isValidReservationDateTimeWithHour(date, selectedHour) -> {
                    showError("Reservations must be made at least 1 hour in advance and within 7 days")
                }
                else -> {
                    val stationId = allStations[selectedStationIndex].id
                    val stationName = getStationName(stationId)
                    
                    Log.d("EVOwnerReservations", "User selected - date: $date, hour: $selectedHour")
                    
                    // Show confirmation before creating
                    showBookingConfirmation(stationName, date, time) {
                        createBookingWithHour(stationId, date, selectedHour)
                        dialog.dismiss()
                    }
                }
            }
        }
        
        dialog.show()
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    private fun showCreateBookingDialogForStation(selectedStation: Station) {
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
        
        // Pre-select the station and disable the dropdown
        val dailyCapacity = selectedStation.availableSlots * 24
        val stationDisplayName = "🔌 ${selectedStation.name} - ${selectedStation.location}\n   ${selectedStation.type} | ${selectedStation.availableSlots} machines/hour | Up to ${dailyCapacity} EVs/day"
        autoCompleteStation.setText(stationDisplayName)
        autoCompleteStation.isEnabled = false // Disable editing since station is pre-selected
        
        // Setup date picker with validation
        dateEditText.setOnClickListener {
            showEnhancedDatePicker { date ->
                dateEditText.setText(date)
                // Clear hour selection when date changes
                timeEditText.setText("")
                timeEditText.tag = null
            }
        }
        
        // Setup hour slot picker with availability
        timeEditText.setOnClickListener {
            val selectedDate = dateEditText.text.toString()
            if (selectedDate.isEmpty()) {
                showError("Please select a date first")
                return@setOnClickListener
            }
            
            showAvailableHoursPicker(selectedStation.id, selectedDate) { hour ->
                timeEditText.setText("${hour}:00 - ${hour + 1}:00")
                timeEditText.tag = hour // Store the actual hour value
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
            val selectedHour = timeEditText.tag as? Int
            
            when {
                date.isEmpty() -> {
                    showError("Please select a reservation date")
                }
                time.isEmpty() || selectedHour == null -> {
                    showError("Please select an available hour slot")
                }
                !isValidReservationDateTimeWithHour(date, selectedHour) -> {
                    showError("Reservations must be made at least 1 hour in advance and within 7 days")
                }
                else -> {
                    Log.d("EVOwnerReservations", "User selected - date: $date, hour: $selectedHour")
                    
                    // Show confirmation before creating
                    showBookingConfirmation(selectedStation.name, date, time) {
                        createBookingWithHour(selectedStation.id, date, selectedHour)
                        dialog.dismiss()
                    }
                }
            }
        }
        
        dialog.show()
        
        // Make dialog background transparent
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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
        
        val date = String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        // Try to book the calculated hour, or show available hours if not available
        createBookingWithHour(station.id, date, hour)
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
                    reservationDate = formattedDateTime,
                    reservationHour = 0
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
            Log.d("EVOwnerReservations", "convertToApiFormat input: $dateTimeString")
            
            // Input format: "2024-01-15 14:30"
            // Output format: "2024-01-15T14:30" (exactly like web datetime-local input)
            // This matches the web version format to prevent timezone conversion issues
            val parts = dateTimeString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0] // "2024-01-15"
                val timePart = parts[1] // "14:30"
                val result = "${datePart}T${timePart}"  // NO seconds, exactly like web
                
                Log.d("EVOwnerReservations", "convertToApiFormat output: $result")
                return result
            }
            Log.w("EVOwnerReservations", "convertToApiFormat: Invalid format, returning original: $dateTimeString")
            return dateTimeString
        } catch (e: Exception) {
            Log.e("EVOwnerReservations", "Error converting date format", e)
            return dateTimeString
        }
    }

    private fun showBookingSuccessDialog(booking: Booking, stationName: String) {
        // Use professional layout instead of simple message dialog
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_booking_success, null)
        
        // Find views and set data
        val bookingId = dialogView.findViewById<TextView>(R.id.textViewBookingId)
        val stationNameView = dialogView.findViewById<TextView>(R.id.textViewStationName)
        val bookingDate = dialogView.findViewById<TextView>(R.id.textViewBookingDate)
        val timeSlot = dialogView.findViewById<TextView>(R.id.textViewTimeSlot)
        val bookingStatus = dialogView.findViewById<TextView>(R.id.textViewBookingStatus)
        
        bookingId.text = booking.id
        stationNameView.text = stationName
        bookingDate.text = booking.reservationDate
        timeSlot.text = "${booking.reservationHour}:00 - ${booking.reservationHour + 1}:00"
        
        // Enhanced status styling with proper visibility and colors
        bookingStatus?.let { status ->
            Log.d("ReservationsBookingSuccess", "Setting booking status: ${booking.status}")
            
            // Ensure visibility and proper styling
            status.visibility = View.VISIBLE
            status.textSize = 14f
            status.setPadding(16, 8, 16, 8)
            
            val statusText = booking.status.uppercase()
            status.text = statusText
            
            // Apply appropriate colors based on booking status
            when (statusText) {
                "PENDING" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_pending_background)
                    Log.d("ReservationsBookingSuccess", "Applied PENDING styling")
                }
                "APPROVED", "CONFIRMED" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_active_background)
                    Log.d("ReservationsBookingSuccess", "Applied APPROVED/CONFIRMED styling")
                }
                "CANCELLED" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_cancelled_background)
                    Log.d("ReservationsBookingSuccess", "Applied CANCELLED styling")
                }
                "COMPLETED" -> {
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_completed_background)
                    Log.d("ReservationsBookingSuccess", "Applied COMPLETED styling")
                }
                else -> {
                    // Default styling for unknown status
                    status.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    status.setBackgroundResource(R.drawable.status_active_background)
                    Log.d("ReservationsBookingSuccess", "Applied default styling for status: $statusText")
                }
            }
            
            // Force layout update
            status.requestLayout()
            status.invalidate()
            
            Log.d("ReservationsBookingSuccess", "Final booking status: '${status.text}', visibility: ${status.visibility}")
        } ?: Log.e("ReservationsBookingSuccess", "bookingStatus TextView is null!")
        
        MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("View My Bookings") { dialog, _ ->
                dialog.dismiss()
                // Refresh the bookings list to show the new booking
                loadRecentBookings()
            }
            .setNeutralButton("Create Another") { dialog, _ ->
                dialog.dismiss()
                showCreateBookingDialog()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
            .apply {
                // Apply NexCharge theme background instead of transparent
                window?.setBackgroundDrawableResource(R.color.nexcharge_surface)
            }
    }

    private fun showBookingDetails(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> {
                if (isBookingOlderThan12Hours(booking)) {
                    "Your reservation is waiting for station operator approval.\n\nNote: This booking can no longer be updated as it was created more than 12 hours ago."
                } else {
                    "Your reservation is waiting for station operator approval."
                }
            }
            "approved" -> "Your reservation is confirmed! You can start charging."
            "completed" -> "Charging session completed successfully."
            "cancelled" -> "This reservation has been cancelled."
            else -> "Reservation status: ${booking.status}"
        }
        
        val message = """
            Reservation Details:
            
            Station: $stationName
            Time Slot: ${DateTimeUtils.formatBookingTimeRange(booking.reservationDate, booking.reservationHour)}
            Status: ${booking.status.uppercase()}
            
            $statusMessage
            
            Booking ID: ${booking.id}
        """.trimIndent()
        
        val alertDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reservation Information")
            .setMessage(message)
            .setPositiveButton("Close", null)
        
        // Add Update button for pending bookings that are not older than 12 hours
        if (booking.status.lowercase() == "pending" && !isBookingOlderThan12Hours(booking)) {
            alertDialog.setNeutralButton("Update") { _, _ ->
                showUpdateBookingDialog(booking)
            }
        }
        
        alertDialog.show()
    }

    private fun isBookingOlderThan12Hours(booking: Booking): Boolean {
        try {
            // Parse the booking creation time
            val createdAt = booking.createdAt
            if (createdAt.isNullOrEmpty()) {
                Log.w("EVOwnerReservations", "Booking createdAt is null or empty, allowing update")
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
            
            Log.d("EVOwnerReservations", "Checking booking age - Created: ${bookingCreationTime.time}, 12h ago: ${twelveHoursAgo.time}")
            
            // Return true if booking was created more than 12 hours ago
            return bookingCreationTime.before(twelveHoursAgo)
            
        } catch (e: Exception) {
            Log.e("EVOwnerReservations", "Error parsing booking creation time: ${booking.createdAt}", e)
            return false // If parsing fails, allow update for safety
        }
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
            Log.e("EVOwnerReservations", "Error setting current booking values", e)
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
            showEnhancedDatePicker { selectedDate ->
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
            
            showAvailableHoursPicker(booking.stationId, selectedDate) { hour ->
                editTextHour.setText("${hour}:00 - ${hour + 1}:00")
                editTextHour.tag = hour // Store the actual hour value
            }
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
                !isValidReservationDateTimeWithHour(date, selectedHour) -> {
                    showError("Selected date and time must be at least 1 hour in advance and within 7 days")
                }
                else -> {
                    dialog.dismiss()
                    updateBookingWithHour(booking, date, selectedHour)
                }
            }
        }
        
        dialog.show()
    }
    
    private fun updateBooking(booking: Booking, newDate: String, newTime: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Updating booking...")
                
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
                
                LoadingManager.dismiss()
                
                if (response.isSuccessful) {
                    val stationName = getStationName(booking.stationId)
                    showSuccess("Booking updated successfully!")
                    loadRecentBookings() // Refresh the list
                } else {
                    showError("Failed to update booking: ${response.message()}")
                }
                
            } catch (e: Exception) {
                LoadingManager.dismiss()
                Log.e("EVOwnerReservations", "Error updating booking", e)
                showError("Error updating booking: ${e.message}")
            }
        }
    }

    private fun updateBookingWithHour(booking: Booking, newDate: String, newHour: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Updating booking...")
                
                val response = apiService.updateBooking(
                    booking.id,
                    BookingUpdateRequest(
                        reservationDate = newDate,
                        reservationHour = newHour,
                        stationId = booking.stationId
                    )
                )
                
                LoadingManager.dismiss()
                
                if (response.isSuccessful) {
                    val stationName = getStationName(booking.stationId)
                    showSuccess("Booking updated successfully!")
                    loadRecentBookings() // Refresh the list
                } else {
                    val errorMsg = when (response.code()) {
                        409 -> "Hour slot already fully booked. Please select a different time."
                        400 -> "Invalid booking update request."
                        404 -> "Booking not found."
                        else -> "Failed to update booking: ${response.message()}"
                    }
                    showError(errorMsg)
                }
                
            } catch (e: Exception) {
                LoadingManager.dismiss()
                Log.e("EVOwnerReservations", "Error updating booking", e)
                showError("Error updating booking: ${e.message}")
            }
        }
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
                LoadingManager.show(requireContext(), "Deleting reservation...")
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
                LoadingManager.dismiss()
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
            Log.d("EVOwnerReservations", "Date selected: $selectedDate")
            onDateSelected(selectedDate)
        }, year, month, day).apply {
            // Set minimum date to today (allow same-day booking if more than 1 hour in advance)
            val minCalendar = Calendar.getInstance()
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
            Log.d("EVOwnerReservations", "Time picker selected: hour=$selectedHour, minute=$selectedMinute, formatted=$selectedTime")
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

    private fun isValidReservationDateTimeWithHour(date: String, hour: Int): Boolean {
        try {
            val reservationDateTime = Calendar.getInstance().apply {
                val parts = date.split("-")
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }
            
            val now = Calendar.getInstance()
            val oneHourLater = Calendar.getInstance().apply {
                add(Calendar.HOUR, 1)
            }
            
            val sevenDaysLater = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
            }
            
            Log.d("EVOwnerReservations", "Validating reservation: $date $hour:00")
            Log.d("EVOwnerReservations", "Current time: ${now.time}")
            Log.d("EVOwnerReservations", "One hour later: ${oneHourLater.time}")
            Log.d("EVOwnerReservations", "Reservation time: ${reservationDateTime.time}")
            Log.d("EVOwnerReservations", "Seven days later: ${sevenDaysLater.time}")
            
            // Check if reservation is at least 1 hour in advance
            if (reservationDateTime.before(oneHourLater)) {
                Log.d("EVOwnerReservations", "Validation failed: Reservation is not at least 1 hour in advance")
                return false
            }
            
            // Check if reservation is within 7 days
            if (reservationDateTime.after(sevenDaysLater)) {
                Log.d("EVOwnerReservations", "Validation failed: Reservation is more than 7 days in advance")
                return false
            }
            
            Log.d("EVOwnerReservations", "Validation passed")
            return true
        } catch (e: Exception) {
            Log.e("EVOwnerReservations", "Error validating date time with hour", e)
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

    private fun showAvailableHoursPicker(stationId: String, date: String, onHourSelected: (Int) -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("EVOwnerReservations", "Loading available hours for station: $stationId, date: $date")
                LoadingManager.show(requireContext(), "Loading available hours...")
                
                val response = apiService.getAvailableHours(stationId, date)
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                
                Log.d("EVOwnerReservations", "API response code: ${response.code()}")
                Log.d("EVOwnerReservations", "API response success: ${response.isSuccessful}")
                
                if (response.isSuccessful) {
                    val availableHours = response.body() ?: emptyList()
                    Log.d("EVOwnerReservations", "Available hours received: $availableHours")
                    
                    if (availableHours.isEmpty()) {
                        Toast.makeText(requireContext(), "No available slots for this date. Please select another date.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    
                    // Create hour selection dialog - simple version like dashboard
                    val hourOptions = availableHours.map { hour ->
                        "${hour}:00 - ${hour + 1}:00"
                    }.toTypedArray()
                    
                    Log.d("EVOwnerReservations", "Hour options created: ${hourOptions.contentToString()}")
                    
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Available Hour Slots")
                        .setSingleChoiceItems(hourOptions, -1) { dialog, which ->
                            onHourSelected(availableHours[which])
                            dialog.dismiss()
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                        
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("EVOwnerReservations", "API call failed: ${response.code()} - $errorBody")
                    Toast.makeText(requireContext(), "Failed to load available hours: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                Log.e("BookingHours", "Error loading available hours", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createBookingWithHour(stationId: String, date: String, hour: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                LoadingManager.show(requireContext(), "Creating booking...")
                
                val userNic = TokenUtils.getCurrentUserNic(requireContext()) ?: throw Exception("User not logged in")
                
                val bookingRequest = BookingCreateRequest(
                    ownerNic = userNic,
                    stationId = stationId,
                    reservationDate = date,
                    reservationHour = hour
                )
                
                val response = apiService.createBooking(bookingRequest)
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                
                if (response.isSuccessful) {
                    val booking = response.body()
                    if (booking != null) {
                        val stationName = getStationName(stationId)
                        showBookingSuccessDialog(booking, stationName)
                        loadRecentBookings() // Refresh recent bookings
                    } else {
                        Toast.makeText(requireContext(), "Booking created but no details returned", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Failed to create booking"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                if (LoadingManager.isShowing()) LoadingManager.dismiss()
                Log.e("CreateBooking", "Error creating booking", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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