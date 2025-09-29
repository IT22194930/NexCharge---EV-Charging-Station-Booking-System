package com.evcharging.evchargingapp.ui.evowner.fragments

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import java.util.*
import com.evcharging.evchargingapp.R
import com.evcharging.evchargingapp.databinding.FragmentEvownerDashboardBinding
import com.evcharging.evchargingapp.data.network.RetrofitInstance
import com.evcharging.evchargingapp.data.model.DashboardStats
import com.evcharging.evchargingapp.data.repository.UserRepository
import com.evcharging.evchargingapp.utils.TokenUtils
import com.evcharging.evchargingapp.utils.LoadingManager
import kotlinx.coroutines.launch

class EVOwnerDashboardFragment : Fragment() {

    private var _binding: FragmentEvownerDashboardBinding? = null
    private val binding get() = _binding!!
    private val apiService by lazy { RetrofitInstance.createApiService(requireContext()) }
    private val userRepository by lazy { UserRepository(requireContext()) }
    private var userName: String = "EV Owner" // Store user's full name

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEvownerDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userNic = TokenUtils.getCurrentUserNic(requireContext())
        val userRole = TokenUtils.getUserRole(requireContext())
        val tokenValid = TokenUtils.isTokenValid(requireContext())
        
        if (userNic == null) {
            Log.e("EVOwnerDashboardFragment", "User NIC is null - authentication issue")
            Toast.makeText(requireContext(), "Authentication issue: Please login again", Toast.LENGTH_LONG).show()
            return
        }
        
        setupDashboard()
        setupFontSizes()
        
        LoadingManager.show(requireContext(), "Loading your dashboard...")
        loadUserProfileAndDashboard()
    }

    private fun setupDashboard() {
        // Set initial loading message with professional greeting
        val greeting = getTimeBasedGreeting()
        binding.textViewWelcome.text = "$greeting\nLoading your dashboard..."
        
        // Setup click listeners for quick actions
        binding.buttonAddStation.setOnClickListener {
            // Navigate to My Bookings tab
            if (isAdded && context != null) {
                navigateToBookingsTab()
            }
        }
        
        binding.buttonViewReports.setOnClickListener {
            // Navigate to Reservations tab
            if (isAdded && context != null) {
                navigateToReservationsTab()
            }
        }
    }

    private fun setupFontSizes() {
        // Set appropriate font sizes for different text elements
        binding.apply {
            // Welcome message - Large and prominent
            textViewWelcome.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            
            // Dashboard stats - Only My Bookings and Active Bookings
            textViewTotalStations.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // Will show My Bookings
            textViewActiveBookings.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f) // Will show Active Bookings
            

            
            // Button text - Rename buttons for new functionality
            buttonAddStation.text = "My Bookings"
            buttonAddStation.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            buttonViewReports.text = "Book Now"
            buttonViewReports.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        }
    }

    private fun loadUserProfileAndDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // First load user profile
                LoadingManager.updateMessage("Loading profile...")
                loadUserProfileSync()
                
                // Then load dashboard data
                LoadingManager.updateMessage("Loading dashboard data...")
                loadDashboardDataSync()
                
            } catch (e: Exception) {
                Log.e("EVOwnerDashboardFragment", "Error loading data", e)
                if (isAdded && _binding != null) {
                    showError("Error loading data: ${e.message}")
                }
            } finally {
                // Dismiss loading only after both operations complete
                if (isAdded && _binding != null) {
                    LoadingManager.dismiss()
                }
            }
        }
    }

    private suspend fun loadUserProfileSync() {
        try {
            // Use repository which handles local/remote data
            val owner = userRepository.getProfile()
            
            if (!isAdded || _binding == null) return
            
            if (owner != null) {
                userName = when {
                    owner.FullName.isNotBlank() -> owner.FullName
                    owner.actualNic.isNotBlank() -> owner.actualNic
                    else -> "EV Owner"
                }
                
                val greeting = getTimeBasedGreeting()
                val welcomeMessage = getCreativeWelcomeMessage(userName, greeting)
                binding.textViewWelcome.text = welcomeMessage
            } else {
                val userNic = TokenUtils.getCurrentUserNic(requireContext())
                userName = userNic ?: "EV Owner"
                val greeting = getTimeBasedGreeting()
                val welcomeMessage = getCreativeWelcomeMessage(userName, greeting)
                binding.textViewWelcome.text = welcomeMessage
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboardFragment", "Error loading user profile", e)
            
            if (isAdded && _binding != null) {
                val userNic = try {
                    TokenUtils.getCurrentUserNic(requireContext())
                } catch (contextError: Exception) {
                    Log.w("EVOwnerDashboardFragment", "Context not available for fallback", contextError)
                    null
                }
                userName = userNic ?: "EV Owner"
                val greeting = getTimeBasedGreeting()
                val welcomeMessage = getCreativeWelcomeMessage(userName, greeting)
                binding.textViewWelcome.text = welcomeMessage
            }
        }
    }

    private suspend fun loadDashboardDataSync() {
        try {
            val response = apiService.getDashboardStats()
            
            if (!isAdded || _binding == null) return
            
            if (response.isSuccessful && response.body() != null) {
                val stats = response.body()!!
                updateDashboardUI(stats)
            } else {
                val errorMsg = "Failed to load dashboard data. Response code: ${response.code()}"
                Log.e("EVOwnerDashboardFragment", errorMsg)
                showError(errorMsg)
                updateDashboardUI(DashboardStats())
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.w("EVOwnerDashboardFragment", "Dashboard data request was cancelled")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("EVOwnerDashboardFragment", "Request timed out", e)
            if (isAdded && _binding != null) {
                showError("Request timed out. Please check your internet connection.")
                updateDashboardUI(DashboardStats())
            }
        } catch (e: java.net.ConnectException) {
            Log.e("EVOwnerDashboardFragment", "Connection failed", e)
            if (isAdded && _binding != null) {
                showError("Cannot connect to server. Please check if the API server is running.")
                updateDashboardUI(DashboardStats())
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("EVOwnerDashboardFragment", "Unknown host", e)
            if (isAdded && _binding != null) {
                showError("Cannot reach server. Please check your network connection.")
                updateDashboardUI(DashboardStats())
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboardFragment", "Unexpected error loading dashboard data", e)
            if (isAdded && _binding != null) {
                showError("Network error: ${e.message}")
                // Show default values on error
                updateDashboardUI(DashboardStats())
            }
        }
    }

    private fun loadDashboardData() {
        
    }

    private fun loadUserProfile() {
        // This method is kept for backward compatibility but not used in the new flow
        // All loading is now handled by loadUserProfileAndDashboard()
    }

    private fun updateDashboardUI(stats: DashboardStats) {
        binding.textViewTotalStations.text = "My Bookings\n${stats.totalBookings}"
        binding.textViewActiveBookings.text = "Active Bookings\n${stats.activeBookings}"
    }

    private fun calculateRevenue(stats: DashboardStats): String {
        val estimatedRevenue = stats.totalBookings * 25.0
        return String.format("%.2f", estimatedRevenue)
    }

    private fun showLoading(isLoading: Boolean) {
        if (isAdded && _binding != null) {
            binding.buttonAddStation.isEnabled = !isLoading
            binding.buttonViewReports.isEnabled = !isLoading
        }
    }

    private fun navigateToBookingsTab() {
        try {
            val activity = requireActivity()
            if (activity is com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity) {
                activity.switchToTab(R.id.nav_bookings)
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error navigating to bookings tab", e)
            Toast.makeText(requireContext(), "Error navigating to bookings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToReservationsTab() {
        // Navigate to Reservations tab in the bottom navigation
        try {
            val activity = requireActivity()
            if (activity is com.evcharging.evchargingapp.ui.evowner.EVOwnerHomeActivity) {
                activity.switchToTab(R.id.nav_reservations)
            }
        } catch (e: Exception) {
            Log.e("EVOwnerDashboard", "Error navigating to reservations tab", e)
            Toast.makeText(requireContext(), "Error navigating to reservations", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        // Check if fragment is still attached before showing toast
        if (isAdded && context != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getTimeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Welcome Back"
        }
    }

    private fun getCreativeWelcomeMessage(userName: String, greeting: String): String {
        return "$greeting\n$userName"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EVOwnerDashboardFragment()
    }
}