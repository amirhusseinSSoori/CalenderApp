package com.template.calenderproject

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.template.calenderproject.ui.CalendarFragment
import com.template.calenderproject.ui.UpcomingEventsFragment
import com.template.calenderproject.ui.UserSettingsFragment

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private val TAG = this.javaClass.simpleName
    private var bottomNavigationView: BottomNavigationView? = null
    private val calendarFragment: Fragment = CalendarFragment()
    private val upcomingEventsFragment: Fragment = UpcomingEventsFragment()
    private val userSettingsFragment: Fragment = UserSettingsFragment()
    private val fragmentManager = supportFragmentManager
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomNavigationView =
            findViewById<View>(R.id.MainActivity_BottomNavigation) as BottomNavigationView
        bottomNavigationView!!.setOnNavigationItemSelectedListener(this)
        fragmentManager.beginTransaction()
            .add(R.id.MainActivity_FrameLayout_Container, userSettingsFragment)
            .hide(userSettingsFragment).commit()
        fragmentManager.beginTransaction()
            .add(R.id.MainActivity_FrameLayout_Container, upcomingEventsFragment)
            .hide(upcomingEventsFragment).commit()
        fragmentManager.beginTransaction()
            .add(R.id.MainActivity_FrameLayout_Container, calendarFragment).commit()
        if (getFlag("isChanged")) {
            bottomNavigationView!!.selectedItemId = R.id.BottomNavigation_Item_Settings
            bottomNavigationView!!.performClick()
            saveFlag("isChanged", false)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.BottomNavigation_Item_Calendar -> {
                (calendarFragment as CalendarFragment).setUpCalendar()
                fragmentManager.beginTransaction()
                    .hide(userSettingsFragment)
                    .hide(upcomingEventsFragment)
                    .show(calendarFragment)
                    .commit()
            }
            R.id.BottomNavigation_Item_UpcomingEvents -> {
                (upcomingEventsFragment as UpcomingEventsFragment).setUpRecyclerView()
                fragmentManager.beginTransaction()
                    .hide(calendarFragment)
                    .hide(userSettingsFragment)
                    .show(upcomingEventsFragment)
                    .commit()
            }
            R.id.BottomNavigation_Item_Settings -> fragmentManager.beginTransaction()
                .hide(calendarFragment)
                .hide(upcomingEventsFragment)
                .show(userSettingsFragment)
                .commit()
        }
        return true
    }

    private val appTheme: Int
        private get() {
            when (getString("theme")) {
                "Dark" -> return R.style.DarkTheme
                "Indigo" -> return R.style.DarkIndigoTheme
            }
            return R.style.DarkIndigoTheme
        }

    private fun saveFlag(key: String, flag: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, flag)
        editor.apply()
    }

    private fun getFlag(key: String): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getBoolean(key, false)
    }

    private fun getString(key: String): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getString(key, "Indigo")
    }
}