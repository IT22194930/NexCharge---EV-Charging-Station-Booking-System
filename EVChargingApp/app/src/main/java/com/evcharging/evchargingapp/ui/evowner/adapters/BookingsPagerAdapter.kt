package com.evcharging.evchargingapp.ui.evowner.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.evcharging.evchargingapp.ui.evowner.fragments.tabs.ActiveBookingsTabFragment
import com.evcharging.evchargingapp.ui.evowner.fragments.tabs.HistoryBookingsTabFragment

class BookingsPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val fragments = listOf(
        ActiveBookingsTabFragment.newInstance(),
        HistoryBookingsTabFragment.newInstance()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment = fragments[position]

    fun getFragment(position: Int): Fragment = fragments[position]
}