package com.evcharging.evchargingapp.ui.evowner.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.evcharging.evchargingapp.databinding.FragmentEvownerBookingsBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.data.model.BookingStatusUpdateRequest
import com.evcharging.evchargingapp.ui.evowner.adapters.BookingAdapter
import com.evcharging.evchargingapp.utils.TokenUtils
import kotlinx.coroutines.launch

class EVOwnerBookingsFragment : Fragment() {

    private var _binding: FragmentEvownerBookingsBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private lateinit var bookingAdapter: BookingAdapter
    private var allBookings = listOf<Booking>()
    private var currentBookings = listOf<Booking>()

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
        setupRecyclerView()
        setupTabs()
        setupSwipeRefresh()
        loadBookings()
    }

    private fun setupRecyclerView() {
        bookingAdapter = BookingAdapter(
            onBookingClick = { booking ->
                showBookingDetails(booking)
            },
            onApproveClick = { booking ->
                updateBookingStatus(booking.id, "Approved")
            },
            onCancelClick = { booking ->
                updateBookingStatus(booking.id, "Cancelled")
            }
        )
        
        binding.recyclerViewBookings.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = bookingAdapter
        }
    }

    private fun setupTabs() {
        binding.tabLayoutBookings.addTab(binding.tabLayoutBookings.newTab().setText("Upcoming"))
        binding.tabLayoutBookings.addTab(binding.tabLayoutBookings.newTab().setText("Past"))
        
        binding.tabLayoutBookings.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadUpcomingBookings()
                    1 -> loadPastBookings()
                }
            }
            
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadBookings()
        }
    }

    private fun loadBookings() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                showLoading(true)
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                
                if (userNic != null) {
                    val response = apiService.getBookingsByOwner(userNic)
                    
                    // Check if fragment is still attached before updating UI
                    if (!isAdded || _binding == null) return@launch
                    
                    if (response.isSuccessful && response.body() != null) {
                        allBookings = response.body()!!
                        loadUpcomingBookings() // Load upcoming by default
                    } else {
                        showError("Failed to load bookings")
                        showEmptyState("Failed to load bookings")
                    }
                } else {
                    showError("User not authenticated")
                    showEmptyState("User not authenticated")
                }
            } catch (e: Exception) {
                if (isAdded && _binding != null) {
                    showError("Network error: ${e.message}")
                    showEmptyState("Network error occurred")
                }
            } finally {
                if (isAdded && _binding != null) {
                    showLoading(false)
                    binding.swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private fun loadUpcomingBookings() {
        currentBookings = allBookings.filter { booking ->
            booking.status in listOf("Pending", "Approved") && 
            isUpcomingDate(booking.reservationDate)
        }
        
        if (currentBookings.isEmpty()) {
            showEmptyState("No upcoming bookings")
        } else {
            hideEmptyState()
            bookingAdapter.submitList(currentBookings)
        }
    }

    private fun loadPastBookings() {
        currentBookings = allBookings.filter { booking ->
            booking.status in listOf("Completed", "Cancelled") || 
            !isUpcomingDate(booking.reservationDate)
        }
        
        if (currentBookings.isEmpty()) {
            showEmptyState("No past bookings")
        } else {
            hideEmptyState()
            bookingAdapter.submitList(currentBookings)
        }
    }

    private fun updateBookingStatus(bookingId: String, status: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = BookingStatusUpdateRequest(status)
                val response = apiService.updateBookingStatus(bookingId, request)
                
                if (response.isSuccessful) {
                    Toast.makeText(requireContext(), "Booking $status successfully", Toast.LENGTH_SHORT).show()
                    loadBookings() // Refresh the list
                } else {
                    showError("Failed to update booking status")
                }
            } catch (e: Exception) {
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun showBookingDetails(booking: Booking) {
        // TODO: Show booking details dialog or navigate to details screen
        Toast.makeText(requireContext(), 
            "Booking Details:\nStation: ${booking.stationName}\nDate: ${booking.reservationDate}\nStatus: ${booking.status}", 
            Toast.LENGTH_LONG).show()
    }

    private fun isUpcomingDate(dateString: String): Boolean {
        return try {
            // Simple check - you should implement proper date comparison
            val currentTime = System.currentTimeMillis()
            // For now, we'll consider all bookings as upcoming if they're not completed/cancelled
            true
        } catch (e: Exception) {
            true
        }
    }

    private fun showLoading(isLoading: Boolean) {
        // Implement loading indicator
        binding.swipeRefreshLayout.isRefreshing = isLoading
    }

    private fun showError(message: String) {
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun showEmptyState(message: String) {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.recyclerViewBookings.visibility = View.GONE
        binding.textViewNoBookings.text = message
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.recyclerViewBookings.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerBookingsFragment()
    }
}