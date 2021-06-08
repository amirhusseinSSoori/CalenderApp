package com.template.calenderproject.adabter

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import com.template.calenderproject.R
import com.template.calenderproject.Utils
import com.template.calenderproject.database.DBHelper
import com.template.calenderproject.database.DBTables
import com.template.calenderproject.model.Event
import com.template.calenderproject.model.RecurringPattern
import java.util.ArrayList

import java.util.Calendar

import java.util.Date


class GridAdapter(
    context: Context,
    private val dates: List<Date>,
    private val selectedCalendar: Calendar,
    events: List<Event>
) :
    ArrayAdapter<Any?>(context, R.layout.layout_cell) {
    private val DAILY = "Repeat Daily"
    private val WEEKLY = "Repeat Weekly"
    private val MONTHLY = "Repeat Monthly"
    private val YEARLY = "Repeat Yearly"
    private val appTheme: Utils.AppTheme
    private val events: List<Event>
    private val layoutInflater: LayoutInflater
    private var dayTextView: TextView? = null
    private var eventCountTextView: TextView? = null
    private val colors: ArrayList<Int>
    private val dbHelper: DBHelper

    @SuppressLint("ResourceAsColor", "SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val viewDate = dates[position]
        val viewCalendar = Calendar.getInstance()
        viewCalendar.time = viewDate
        val viewMonth = viewCalendar[Calendar.MONTH]
        val viewYear = viewCalendar[Calendar.YEAR]
        val viewDayOfMonth = viewCalendar[Calendar.DAY_OF_MONTH]
        val viewDayOfWeek = viewCalendar[Calendar.DAY_OF_WEEK]
        val selectedMonth = selectedCalendar[Calendar.MONTH]
        val selectedYear = selectedCalendar[Calendar.YEAR]
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.layout_cell, parent, false)
            dayTextView =
                convertView!!.findViewById<View>(R.id.LayoutCell_TextView_Day) as TextView?
            eventCountTextView =
                convertView.findViewById<View>(R.id.LayoutCell_TextView_EventCount) as TextView?
            dayTextView!!.text = viewDayOfMonth.toString()
        }
        val dayTextView = convertView!!.findViewById<TextView>(R.id.LayoutCell_TextView_Day)
        val eventCountTextView =
            convertView.findViewById<TextView>(R.id.LayoutCell_TextView_EventCount)
        val bgLinearLayout = convertView.findViewById<LinearLayout>(R.id.LayoutCell_LinearLayout)
        if (viewYear == selectedYear && viewMonth == selectedMonth) {
            // Active dates
            convertView.setBackgroundColor(context.resources.getColor(colors[2]))
            dayTextView.setTextColor(context.resources.getColor(colors[3]))
            eventCountTextView.setTextColor(context.resources.getColor(colors[6]))
        } else {
            // Inactive dates
            dayTextView.setTextColor(context.resources.getColor(colors[1]))
            eventCountTextView.visibility = View.GONE
        }

        // Highlight current day on the calendar
        var mCalendar = Calendar.getInstance()
        if (viewYear == mCalendar[Calendar.YEAR] && viewMonth == mCalendar[Calendar.MONTH] && viewDayOfMonth == mCalendar[Calendar.DAY_OF_MONTH]) {
            bgLinearLayout.setBackgroundColor(context.resources.getColor(colors[4]))
            dayTextView.setTextColor(context.resources.getColor(colors[5]))
            eventCountTextView.setTextColor(context.resources.getColor(colors[5]))
        }


        // Find event count
        val eventIDs: MutableList<Int> = ArrayList()
        val recurringPatterns: List<RecurringPattern> = readRecurringPatterns()
        for (recurringPattern in recurringPatterns) {
            when (recurringPattern.pattern) {
                DAILY -> eventIDs.add(recurringPattern.eventId!!)
                WEEKLY -> if (viewDayOfWeek == recurringPattern.dayOfWeek) {
                    eventIDs.add(recurringPattern.eventId!!)
                }
                MONTHLY -> if (viewDayOfMonth == recurringPattern.dayOfMonth) {
                    eventIDs.add(recurringPattern.eventId!!)
                }
                YEARLY -> if (viewMonth == recurringPattern.monthOfYear && viewDayOfMonth == recurringPattern.dayOfMonth) {
                    eventIDs.add(recurringPattern.eventId!!)
                }
            }
        }
        mCalendar = Calendar.getInstance()
        for (event in events) {
            if (event.date != null) {
                mCalendar.time = Utils.convertStringToDate(event.date)
                if (viewDayOfMonth == mCalendar[Calendar.DAY_OF_MONTH] && viewMonth == mCalendar[Calendar.MONTH] && viewYear == mCalendar[Calendar.YEAR] && !eventIDs.contains(
                        event.id!!
                    )
                ) {
                    eventIDs.add(event.id!!)
                }
            }
        }
        if (eventIDs.size > 0) {
            eventCountTextView.text = Integer.toString(eventIDs.size)
        }
        return convertView
    }

    private fun readRecurringPatterns(): List<RecurringPattern> {
        val recurringPatterns: MutableList<RecurringPattern> = ArrayList<RecurringPattern>()
        val sqLiteDatabase: SQLiteDatabase = dbHelper.getReadableDatabase()
        val cursor: Cursor = dbHelper.readAllRecurringPatterns(sqLiteDatabase)
        while (cursor.moveToNext()) {
            val recurringPattern = RecurringPattern()
            recurringPattern.eventId =
                cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_EVENT_ID))
            recurringPattern.pattern =
                cursor.getString(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_TYPE))
            recurringPattern.monthOfYear =
                cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_MONTH_OF_YEAR))
            recurringPattern.dayOfMonth =
                cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_DAY_OF_MONTH))
            recurringPattern.dayOfWeek =
                cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_DAY_OF_WEEK))
            recurringPatterns.add(recurringPattern)
        }
        return recurringPatterns
    }

    private fun isContains(events: List<Event>, eventId: Int): Boolean {
        for (event in events) {
            if (event.id === eventId) {
                return true
            }
        }
        return false
    }

    override fun getCount(): Int {
        return dates.size
    }

    override fun getPosition(item: Any?): Int {
        return dates.indexOf(item)
    }

    override fun getItem(position: Int): Any? {
        return dates[position]
    }

    private fun getAppTheme(): Utils.AppTheme {
        var theme: Utils.AppTheme = Utils.AppTheme.INDIGO
        when (string) {
            "Dark" -> theme = Utils.AppTheme.DARK
            "Indigo" -> theme = Utils.AppTheme.INDIGO
        }
        return theme
    }

    private val string: String?
        private get() {
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
                context
            )
            return sharedPreferences.getString("theme", "Indigo")
        }

    private fun getColors(): ArrayList<Int> {
        val colors = ArrayList<Int>()
        when (appTheme) {
            Utils.AppTheme.INDIGO -> {
                colors.add(R.color.white) // disabled date backgroundColor
                colors.add(R.color.lightGrey) // disabled date textColor
                colors.add(R.color.lightIndigo) // active date backgroundColor
                colors.add(R.color.darkIndigo) // active date textColor
                colors.add(R.color.darkIndigo) // current date backgroundColor
                colors.add(R.color.white) // current date textColor
                colors.add(R.color.darkIndigo) // event count textColor
            }
            Utils.AppTheme.DARK -> {
                colors.add(R.color.darkGrey) // disabled date backgroundColor
                colors.add(R.color.lightGrey2) // disabled date textColor
                colors.add(R.color.Grey800) // active date backgroundColor
                colors.add(R.color.white) // active date textColor
                colors.add(R.color.black) // current date backgroundColor
                colors.add(R.color.white) // current date textColor
                colors.add(R.color.white) // event count textColor
            }
        }
        return colors
    }

    init {
        this.events = events
        layoutInflater = LayoutInflater.from(context)
        dbHelper = DBHelper(context)
        appTheme = getAppTheme()
        colors = getColors()
    }
}
