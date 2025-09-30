package com.evcharging.evchargingapp.ui.evowner.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.evcharging.evchargingapp.ui.evowner.fragments.tabs.ActiveBookingsTabFragment
import com.evcharging.evchargingapp.ui.evowner.fragments.tabs.HistoryBookingsTabFragment

class BookingsPagerAdapter(private val fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ActiveBookingsTabFragment.newInstance()
            1 -> HistoryBookingsTabFragment.newInstance()
            else -> ActiveBookingsTabFragment.newInstance()
        }
    }

    fun getActiveFragment(position: Int): Fragment? {
        return fragmentActivity.supportFragmentManager.findFragmentByTag("f$position")
    }
    
    fun filterBookingsInAllTabs(query: String) {
        // Filter in active tab
        val activeFragment = getActiveFragment(0) as? ActiveBookingsTabFragment
        activeFragment?.filterBookings(query)
        
        // Filter in history tab
        val historyFragment = getActiveFragment(1) as? HistoryBookingsTabFragment
        historyFragment?.filterBookings(query)
    }
}