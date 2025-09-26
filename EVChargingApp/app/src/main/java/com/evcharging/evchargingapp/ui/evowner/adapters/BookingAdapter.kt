package com.evcharging.evchargingapp.ui.evowner.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.databinding.ItemBookingBinding

class BookingAdapter(
    private val onBookingClick: (Booking) -> Unit,
    private val onApproveClick: (Booking) -> Unit,
    private val onCancelClick: (Booking) -> Unit
) : ListAdapter<Booking, BookingAdapter.BookingViewHolder>(BookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val binding = ItemBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookingViewHolder(
        private val binding: ItemBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.apply {
                textViewStationName.text = booking.stationName ?: "Station ${booking.stationId}"
                textViewCustomerName.text = "Customer: ${booking.ownerNic}"
                textViewDateTime.text = formatDateTime(booking.reservationDate)
                textViewDuration.text = "Duration: 1 hour" // You can calculate this from your data
                textViewAmount.text = "$${booking.amount ?: 25.00}"
                
                // Set status with appropriate styling
                textViewStatus.text = booking.status
                setStatusBackground(booking.status)
                
                // Set click listeners
                root.setOnClickListener { onBookingClick(booking) }
                
                // Show action buttons based on status and context
                // This can be customized based on your business logic
            }
        }

        private fun formatDateTime(dateTime: String): String {
            // Format the datetime string for display
            // You should implement proper date formatting here
            return try {
                // Assuming the format is ISO string, format it nicely
                dateTime.replace("T", " ").substringBefore(".")
            } catch (e: Exception) {
                dateTime
            }
        }

        private fun setStatusBackground(status: String) {
            // Set background color based on status
            // You can customize colors in your colors.xml
            val context = binding.root.context
            binding.textViewStatus.setBackgroundResource(
                when (status) {
                    "Pending" -> android.R.color.holo_orange_light
                    "Approved" -> android.R.color.holo_green_light
                    "Cancelled" -> android.R.color.holo_red_light
                    "Completed" -> android.R.color.holo_blue_light
                    else -> android.R.color.darker_gray
                }
            )
        }
    }

    class BookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}