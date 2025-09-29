package com.evcharging.evchargingapp.ui.evowner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evcharging.evchargingapp.data.model.Booking
import com.evcharging.evchargingapp.databinding.ItemRecentBookingBinding

class RecentBookingAdapter(
    private val onBookingClick: (Booking) -> Unit,
    private val onViewQRClick: (Booking) -> Unit,
    private val onDeleteClick: (Booking) -> Unit,
    private val getStationName: (String?) -> String
) : ListAdapter<Booking, RecentBookingAdapter.RecentBookingViewHolder>(RecentBookingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentBookingViewHolder {
        val binding = ItemRecentBookingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentBookingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentBookingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentBookingViewHolder(
        private val binding: ItemRecentBookingBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.apply {
                // Set booking details
                textViewStationName.text = getStationName(booking.stationId)
                textViewStatus.text = booking.status.uppercase()
                textViewDateTime.text = booking.reservationDate.split(" ").firstOrNull() ?: booking.reservationDate

                // Set status color
                val statusColor = when (booking.status.lowercase()) {
                    "confirmed", "approved" -> android.graphics.Color.parseColor("#4CAF50")
                    "pending" -> android.graphics.Color.parseColor("#FF9800")
                    "cancelled" -> android.graphics.Color.parseColor("#F44336")
                    "completed" -> android.graphics.Color.parseColor("#2196F3")
                    else -> android.graphics.Color.parseColor("#757575")
                }
                textViewStatus.setBackgroundColor(statusColor)

                // Set click listeners
                root.setOnClickListener { onBookingClick(booking) }
                buttonViewQR.setOnClickListener { onViewQRClick(booking) }
                buttonDelete.setOnClickListener { onDeleteClick(booking) }

                // Show/hide View QR button based on QR code availability
                buttonViewQR.visibility = if (!booking.qrBase64.isNullOrEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

    private class RecentBookingDiffCallback : DiffUtil.ItemCallback<Booking>() {
        override fun areItemsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Booking, newItem: Booking): Boolean {
            return oldItem == newItem
        }
    }
}