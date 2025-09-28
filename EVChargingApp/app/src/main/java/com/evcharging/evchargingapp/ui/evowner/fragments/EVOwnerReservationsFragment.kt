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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.*

class EVOwnerReservationsFragment : Fragment() {

    private var _binding: FragmentEvownerReservationsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var recentBookingAdapter: RecentBookingAdapter
    private var allStations = listOf<Station>()
    private var recentBookings = listOf<Booking>()

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
            showCreateBookingDialog()
        }
        
        binding.fabCreateReservation.setOnClickListener {
            showCreateBookingDialog()
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
                        // Show only the 5 most recent bookings
                        recentBookings = allBookings.sortedByDescending { it.createdAt ?: "" }.take(5)
                        recentBookingAdapter.submitList(recentBookings)
                        Log.d("EVOwnerReservations", "Loaded ${recentBookings.size} recent bookings")
                    } else {
                        Log.w("EVOwnerReservations", "Failed to load recent bookings: ${response.code()}")
                    }
                } else {
                    Log.w("EVOwnerReservations", "User NIC not found")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error loading recent bookings", e)
            }
        }
    }

    private fun loadStations() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = apiService.getAllStations()
                
                if (response.isSuccessful && response.body() != null) {
                    allStations = response.body()!!
                    updateStationsUI()
                    Log.d("EVOwnerReservations", "Loaded ${allStations.size} stations")
                } else {
                    Log.w("EVOwnerReservations", "Failed to load stations: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error loading stations", e)
            }
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
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_booking, null)
        
        val stationSpinner = dialogView.findViewById<androidx.appcompat.widget.AppCompatSpinner>(R.id.spinnerStation)
        val dateEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextDate)
        val timeEditText = dialogView.findViewById<TextInputEditText>(R.id.editTextTime)
        
        // Setup station spinner
        val stationNames = allStations.map { "${it.name} - ${it.location}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, stationNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stationSpinner.adapter = adapter
        
        // Setup date picker
        dateEditText.setOnClickListener {
            showDatePicker { date ->
                dateEditText.setText(date)
            }
        }
        
        // Setup time picker
        timeEditText.setOnClickListener {
            showTimePicker { time ->
                timeEditText.setText(time)
            }
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Create New Reservation")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val selectedStationIndex = stationSpinner.selectedItemPosition
                val date = dateEditText.text.toString()
                val time = timeEditText.text.toString()
                
                if (selectedStationIndex >= 0 && date.isNotEmpty() && time.isNotEmpty()) {
                    val stationId = allStations[selectedStationIndex].id
                    createBooking(stationId, "$date $time")
                } else {
                    showError("Please fill in all fields")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
            showError("User not authenticated")
            return
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val request = BookingCreateRequest(
                    stationId = stationId,
                    ownerNic = userNic,
                    reservationDate = reservationDateTime
                )
                
                val response = apiService.createBooking(request)
                
                if (response.isSuccessful) {
                    showSuccess("Reservation created successfully!")
                    loadRecentBookings() // Refresh recent bookings
                } else {
                    showError("Failed to create reservation: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("EVOwnerReservations", "Error creating booking", e)
                showError("Network error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showBookingDetails(booking: Booking) {
        val stationName = getStationName(booking.stationId)
        val statusMessage = when (booking.status.lowercase()) {
            "pending" -> "Your reservation is waiting for station operator approval."
            "confirmed" -> "Your reservation is confirmed! You can start charging."
            "completed" -> "Charging session completed successfully."
            "cancelled" -> "This reservation has been cancelled."
            else -> "Reservation status: ${booking.status}"
        }
        
        val message = """
            Reservation Details:
            
            Station: $stationName
            Date & Time: ${booking.reservationDate}
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
                    
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Booking QR Code")
                        .setView(dialogView)
                        .setMessage("Show this QR code at the charging station")
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

    private fun getStationName(stationId: String): String {
        return allStations.find { it.id == stationId }?.name ?: "Unknown Station"
    }

    private fun showLoading(isLoading: Boolean) {
        if (!isAdded || _binding == null) return
        
        binding.buttonCreateReservation.isEnabled = !isLoading
        binding.fabCreateReservation.isEnabled = !isLoading
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