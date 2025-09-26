package com.evcharging.evchargingapp.ui.evowner.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.evcharging.evchargingapp.data.model.Station
import com.evcharging.evchargingapp.databinding.ItemReservationBinding

class ReservationAdapter(
    private val onReservationClick: (Station) -> Unit,
    private val onModifyClick: (Station) -> Unit,
    private val onDeleteClick: (Station) -> Unit
) : ListAdapter<Station, ReservationAdapter.ReservationViewHolder>(ReservationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val binding = ItemReservationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReservationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReservationViewHolder(
        private val binding: ItemReservationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(station: Station) {
            binding.apply {
                textViewStationName.text = station.name
                textViewDateTime.text = "${station.location} • ${station.chargerType}"
                textViewStatus.text = station.status
                textViewPrice.text = "$${station.pricePerHour}/hour"
                
                // Set status styling
                setStatusBackground(station.status)
                
                // Set click listeners
                root.setOnClickListener { onReservationClick(station) }
                buttonAction.setOnClickListener { onModifyClick(station) }
                
                // Customize button text based on status
                buttonAction.text = when (station.status) {
                    "Active" -> "Modify"
                    "Inactive" -> "Activate"
                    "Maintenance" -> "Fix"
                    else -> "Action"
                }
            }
        }

        private fun setStatusBackground(status: String) {
            binding.textViewStatus.setBackgroundResource(
                when (status) {
                    "Active" -> android.R.color.holo_green_light
                    "Inactive" -> android.R.color.holo_orange_light
                    "Maintenance" -> android.R.color.holo_red_light
                    else -> android.R.color.darker_gray
                }
            )
        }
    }

    class ReservationDiffCallback : DiffUtil.ItemCallback<Station>() {
        override fun areItemsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Station, newItem: Station): Boolean {
            return oldItem == newItem
        }
    }
}