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
import com.evcharging.evchargingapp.databinding.FragmentHistoryBookingsBinding
import com.evcharging.evchargingapp.ui.evowner.adapters.BookingAdapter
import com.evcharging.evchargingapp.ui.evowner.fragments.EVOwnerBookingsFragment
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.DateTimeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.*

class HistoryBookingsTabFragment : Fragment() {
    
    private var _binding: FragmentHistoryBookingsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var bookingAdapter: BookingAdapter
    private var allBookings = listOf<Booking>()
    private var allStations = listOf<Station>()
    private var searchQuery = ""
    private var selectedYear: Int? = null
    private var selectedMonth: Int? = null

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
        setupFilterTrigger()
        loadStationsAndBookings()
    }

    private fun setupFilterTrigger() {
        // Setup the FAB filter button
        binding.fabFilter.setOnClickListener {
            showMonthYearFilterDialog()
        }
        
        // Setup clear filter button
        binding.imageViewClearFilter.setOnClickListener {
            clearDateFilter()
        }
        
        // Add long press on the RecyclerView container to show filter (backup)
        binding.recyclerViewHistoryBookings.setOnLongClickListener {
            showMonthYearFilterDialog()
            true
        }
        
        // Also add a touch listener to show filter info
        binding.root.setOnLongClickListener {
            if (hasActiveFilter()) {
                showError("Current filter: ${getCurrentFilterStatus()}")
            } else {
                showMonthYearFilterDialog()
            }
            true
        }
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
                        }.sortedByDescending { booking ->
                            // Sort by reservation date, most recent first
                            try {
                                // Use the reservation date string for sorting (ISO format should sort correctly)
                                booking.reservationDate
                            } catch (e: Exception) {
                                // If any issues, use a default old date to put it at the end
                                "2000-01-01"
                            }
                        }
                        updateBookingsUI()
                        updateFilterStatusCard(getCurrentFilterStatus())
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
        searchQuery = query.trim()
        updateBookingsUI()
    }

    fun showMonthYearFilter() {
        showMonthYearFilterDialog()
    }

    fun clearDateFilter() {
        selectedYear = null
        selectedMonth = null
        updateBookingsUI()
        updateFilterStatusCard("All History")
        showError("Date filter cleared")
    }

    private fun showMonthYearFilterDialog() {
        Log.d("HistoryBookings", "showMonthYearFilterDialog called")
        
        // Use the proper layout file instead of programmatic creation
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_year_filter, null)
        
        val yearSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerYear)
        val monthSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.spinnerMonth)
        
        // Setup year spinner
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear).map { it.toString() }.toTypedArray()
        val yearAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, years)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
        
        // Setup month spinner
        val months = arrayOf("All Months", "January", "February", "March", "April", "May", "June", 
                           "July", "August", "September", "October", "November", "December")
        val monthAdapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, months)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        monthSpinner.adapter = monthAdapter
        
        // Set current selections or defaults to current date
        val calendar = Calendar.getInstance()
        val currentYearValue = calendar.get(Calendar.YEAR)
        val currentMonthValue = calendar.get(Calendar.MONTH) // 0-based month
        
        // Set year selection (default to current year if no filter is set)
        val yearToSelect = selectedYear ?: currentYearValue
        val yearIndex = years.indexOf(yearToSelect.toString())
        if (yearIndex >= 0) {
            yearSpinner.setSelection(yearIndex)
        }
        
        // Set month selection (default to current month if no filter is set)
        val monthToSelect = selectedMonth ?: currentMonthValue
        monthSpinner.setSelection(monthToSelect + 1) // +1 because index 0 is "All Months"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Month & Year")
            .setView(dialogView)
            .setPositiveButton("Apply") { _, _ ->
                val selectedYearStr = yearSpinner.selectedItem.toString()
                val selectedMonthIndex = monthSpinner.selectedItemPosition
                
                selectedYear = selectedYearStr.toInt()
                selectedMonth = if (selectedMonthIndex == 0) null else selectedMonthIndex - 1 // -1 because index 0 is "All Months"
                
                updateBookingsUI()
                showFilterStatus()
            }
            .setNegativeButton("Clear Filter") { _, _ ->
                clearDateFilter()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }
    
    private fun showFilterStatus() {
        val status = when {
            selectedYear != null && selectedMonth != null -> {
                val monthNames = arrayOf("January", "February", "March", "April", "May", "June", 
                                       "July", "August", "September", "October", "November", "December")
                "Filtered: ${monthNames[selectedMonth!!]} $selectedYear"
            }
            selectedYear != null -> "Filtered: Year $selectedYear"
            else -> "All History"
        }
        
        // Update filter status card
        updateFilterStatusCard(status)
    }
    
    private fun updateFilterStatusCard(status: String) {
        if (!isAdded || _binding == null) return
        
        if (hasActiveFilter()) {
            binding.cardFilterStatus.visibility = View.VISIBLE
            binding.textViewFilterStatus.text = status
        } else {
            binding.cardFilterStatus.visibility = View.GONE
        }
    }

    private fun updateBookingsUI() {
        var filteredBookings = allBookings
        
        // Apply month/year filter first
        if (selectedYear != null || selectedMonth != null) {
            filteredBookings = filteredBookings.filter { booking ->
                try {
                    val reservationDate = booking.reservationDate
                    if (reservationDate.isNullOrEmpty()) return@filter false
                    
                    // Parse the date string (assuming ISO format like "2024-01-15T14:30" or "2024-01-15")
                    val dateStr = reservationDate.split("T")[0] // Get just the date part
                    val dateParts = dateStr.split("-")
                    if (dateParts.size >= 3) {
                        val bookingYear = dateParts[0].toInt()
                        val bookingMonth = dateParts[1].toInt() - 1 // Convert to 0-based month
                        
                        val yearMatches = selectedYear == null || bookingYear == selectedYear
                        val monthMatches = selectedMonth == null || bookingMonth == selectedMonth
                        
                        yearMatches && monthMatches
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    Log.e("HistoryBookings", "Error parsing date for filtering: ${booking.reservationDate}", e)
                    false
                }
            }
        }
        
        // Apply search query filter
        if (searchQuery.isNotEmpty()) {
            filteredBookings = filteredBookings.filter { booking ->
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
            val emptyMessage = when {
                selectedYear != null || selectedMonth != null -> {
                    val monthNames = arrayOf("January", "February", "March", "April", "May", "June", 
                                           "July", "August", "September", "October", "November", "December")
                    val filterDesc = when {
                        selectedYear != null && selectedMonth != null -> "${monthNames[selectedMonth!!]} $selectedYear"
                        selectedYear != null -> "year $selectedYear"
                        else -> "selected period"
                    }
                    "No bookings found for $filterDesc"
                }
                searchQuery.isNotEmpty() -> "No bookings match \"$searchQuery\""
                else -> "No booking history yet"
            }
            showEmptyState(emptyMessage)
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
        
        // Add Update button for pending bookings only
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
            Log.e("HistoryBookings", "Error setting current booking values", e)
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
                        reservationHour = 0, // TODO: Update to support hour selection
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
                Log.e("HistoryBookings", "Error updating booking", e)
                showError("Error updating booking: ${e.message}")
            }
        }
    }
    
    private fun convertToApiFormat(dateTimeString: String): String {
        try {
            Log.d("HistoryBookings", "convertToApiFormat input: $dateTimeString")
            
            // Input format: "2024-01-15 14:30"
            // Output format: "2024-01-15T14:30" (exactly like web datetime-local input)
            // This matches the web version format to prevent timezone conversion issues
            val parts = dateTimeString.split(" ")
            if (parts.size == 2) {
                val datePart = parts[0] // "2024-01-15"
                val timePart = parts[1] // "14:30"
                val result = "${datePart}T${timePart}"  // NO seconds, exactly like web
                
                Log.d("HistoryBookings", "convertToApiFormat output: $result")
                return result
            }
            Log.w("HistoryBookings", "convertToApiFormat: Invalid format, returning original: $dateTimeString")
            return dateTimeString
        } catch (e: Exception) {
            Log.e("HistoryBookings", "Error converting date format", e)
            return dateTimeString
        }
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

    fun getCurrentFilterStatus(): String {
        return when {
            selectedYear != null && selectedMonth != null -> {
                val monthNames = arrayOf("January", "February", "March", "April", "May", "June", 
                                       "July", "August", "September", "October", "November", "December")
                "Filtered: ${monthNames[selectedMonth!!]} $selectedYear"
            }
            selectedYear != null -> "Filtered: Year $selectedYear"
            else -> "All History"
        }
    }

    fun hasActiveFilter(): Boolean {
        return selectedYear != null || selectedMonth != null
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