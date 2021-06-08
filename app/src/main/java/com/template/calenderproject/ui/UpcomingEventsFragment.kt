package com.template.calenderproject.ui

import android.app.Activity
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Binder
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.template.calenderproject.R
import com.template.calenderproject.Utils
import com.template.calenderproject.adabter.UpcomingEventAdapter
import com.template.calenderproject.database.DBHelper
import com.template.calenderproject.database.DBTables
import com.template.calenderproject.databinding.FragmentUpcomingEventsBinding
import com.template.calenderproject.model.Event
import com.template.calenderproject.model.RecurringPattern
import java.text.ParseException
import java.util.*
import kotlin.collections.ArrayList


class UpcomingEventsFragment : Fragment(R.layout.fragment_upcoming_events) {
    private val TAG = this.javaClass.simpleName
    private var dbHelper: DBHelper? = null

    //public String period;
    private val todayDate: String? = null

    lateinit var binding:FragmentUpcomingEventsBinding


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentUpcomingEventsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(activity)
        //        period = Utils.CURRENT_FILTER;
        initViews()
        defineListeners()
    }

    private fun initViews() {
        binding.UpcomingEventsFragmentTextViewPeriod!!.text = Utils.CURRENT_FILTER
        setUpRecyclerView()
    }

    private fun defineListeners() {
        binding.UpcomingEventsFragmentImageButtonPeriod!!.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                // inflate menu
                val popup = PopupMenu(activity, view)
                val inflater = popup.menuInflater
                inflater.inflate(R.menu.popup_period, popup.menu)
                popup.setOnMenuItemClickListener(MyMenuItemClickListener())
                popup.show()
                setUpRecyclerView()
            }

            inner class MyMenuItemClickListener : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.PopupPeriod_Item_Today -> {
                            Utils.CURRENT_FILTER = Utils.TODAY
                            binding.UpcomingEventsFragmentTextViewPeriod!!.text = Utils.CURRENT_FILTER
                        }
                        R.id.PopupPeriod_Item_Next7Days -> {
                            Utils.CURRENT_FILTER = Utils.NEXT_7_DAYS
                            binding.UpcomingEventsFragmentTextViewPeriod!!.text = Utils.CURRENT_FILTER
                        }
                        R.id.PopupPeriod_Item_Next30Days -> {
                            Utils.CURRENT_FILTER = Utils.NEXT_30_DAYS
                            binding.UpcomingEventsFragmentTextViewPeriod!!.text = Utils.CURRENT_FILTER
                        }
                    }
                    setUpRecyclerView()
                    return true
                }
            }
        })
    }

    fun setUpRecyclerView() {
        binding.UpcomingEventsFragmentRecyclerViewEvents!!.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(activity)
        layoutManager.isMeasurementCacheEnabled = false
        binding.UpcomingEventsFragmentRecyclerViewEvents!!.layoutManager = layoutManager
        val upcomingEventAdapter =
            UpcomingEventAdapter(requireContext(), collectEvents(Calendar.getInstance().time)!!, this)
        binding.UpcomingEventsFragmentRecyclerViewEvents!!.adapter = upcomingEventAdapter
    }

    private fun collectEvents(today: Date): ArrayList<Event>? {
        var events: ArrayList<Event>? = null
        try {
            when (Utils.CURRENT_FILTER) {
                Utils.TODAY -> events = collectTodayEvents(today)
                Utils.NEXT_7_DAYS -> events = collectNext7DaysEvents(today)
                Utils.NEXT_30_DAYS -> events = collectNext30DaysEvents(today)
            }
        } catch (e: ParseException) {
            Log.e(TAG, "An error has occurred while parsing the date string")
        }
        return events
    }

    private fun collectTodayEvents(today: Date): ArrayList<Event> {
        val eventList: ArrayList<Event> = ArrayList<Event>()
        val calendar = Calendar.getInstance()
        calendar.time = today
        val month = calendar[Calendar.MONTH]
        val dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
        val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]

        // Add recurring events
        val recurringPatterns: List<RecurringPattern> = readRecurringPatterns()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.readableDatabase
        var event = Event()
        for (recurringPattern in recurringPatterns) {
            when (recurringPattern.pattern) {
                Utils.DAILY -> {
                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    event.date=Utils.eventDateFormat.format(today)
                    eventList.add(event)
                }
                Utils.WEEKLY -> if (dayOfWeek == recurringPattern.dayOfWeek) {
                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    event.date = Utils.eventDateFormat.format(today)
                    eventList.add(event)
                }
                Utils.MONTHLY -> if (dayOfMonth == recurringPattern.dayOfMonth) {
                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    event.date=Utils.eventDateFormat.format(today)
                    eventList.add(event)
                }
                Utils.YEARLY -> if (month == recurringPattern.monthOfYear && dayOfMonth == recurringPattern.dayOfMonth) {
                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    event.date=Utils.eventDateFormat.format(today)
                    eventList.add(event)
                }
            }
        }


        // Add non-recurring events
        val cursor: Cursor =
            dbHelper!!.readEventsByDate(sqLiteDatabase, Utils.eventDateFormat.format(today))
        while (cursor.moveToNext()) {
            val eventID = cursor.getInt(cursor.getColumnIndex(DBTables.EVENT_ID))
            if (!isContains(eventList, eventID)) {
                eventList.add(dbHelper!!.readEvent(sqLiteDatabase, eventID))
            }
        }
        cursor.close()
        sqLiteDatabase.close()
        return eventList
    }

    @Throws(ParseException::class)
    private fun collectNext7DaysEvents(today: Date): ArrayList<Event> {
        val fromCalendar = Calendar.getInstance()
        fromCalendar.time = today
        val toCalendar = fromCalendar.clone() as Calendar
        toCalendar.add(Calendar.DAY_OF_MONTH, 8)
        val fromDate = fromCalendar.time
        val toDate = toCalendar.time
        val eventList: ArrayList<Event> = ArrayList<Event>()
        // Add recurring events
        val recurringPatterns: List<RecurringPattern> = readRecurringPatterns()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.getReadableDatabase()
        var event = Event()
        var mCalendar = fromCalendar.clone() as Calendar
        for (recurringPattern in recurringPatterns) {
            when (recurringPattern.pattern) {
                Utils.DAILY -> {

                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    var i = 0
                    while (i < 7) {
                        mCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        event.date=Utils.eventDateFormat.format(mCalendar.time)
                        eventList.add(event)
                        event = dbHelper!!.readEvent(
                            sqLiteDatabase,
                            recurringPattern.eventId!!
                        ) // TODO: clone the object
                        i++
                    }
                }
                Utils.WEEKLY -> {
                    mCalendar = fromCalendar.clone() as Calendar
                    mCalendar.add(Calendar.DAY_OF_MONTH, 7)
                    mCalendar.set(Calendar.DAY_OF_WEEK, recurringPattern.dayOfWeek!!)
                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    event.date=Utils.eventDateFormat.format(mCalendar.time)
                    eventList.add(event)
                }
                Utils.MONTHLY -> {
                    mCalendar = fromCalendar.clone() as Calendar
                    mCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    if (recurringPattern.dayOfMonth!! >= mCalendar.get(Calendar.DAY_OF_MONTH)) {
                        mCalendar.set(Calendar.DAY_OF_MONTH, recurringPattern.dayOfMonth!!)
                        event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                        event.date = Utils.eventDateFormat.format(mCalendar.getTime())
                        eventList.add(event)
                    }
                }
                Utils.THIS_YEAR -> {
                    mCalendar = fromCalendar.clone() as Calendar
                    mCalendar.set(Calendar.MONTH, recurringPattern.monthOfYear!!)
                    mCalendar.set(Calendar.DAY_OF_MONTH, recurringPattern.dayOfMonth!!)
                    event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    event.date=Utils.eventDateFormat.format(today)
                    eventList.add(event)
                }
            }
        }
        val allEvents: List<Event> = dbHelper!!.readAllEvents(sqLiteDatabase)
        for (mEvent in allEvents) {
            val currentDate: Date = Utils.eventDateFormat.parse(mEvent.date)
            if (currentDate.after(fromDate) && currentDate.before(toDate) && !isContains(
                    eventList,
                    mEvent.id!!
                )
            ) {
                eventList.add(mEvent)
            }
        }
        sqLiteDatabase.close()
        return eventList
    }

    @Throws(ParseException::class)
    private fun collectNext30DaysEvents(today: Date): ArrayList<Event> {
        val fromCalendar = Calendar.getInstance()
        fromCalendar.time = today
        val toCalendar = fromCalendar.clone() as Calendar
        toCalendar.add(Calendar.DAY_OF_MONTH, 31)
        val fromDate = fromCalendar.time
        val toDate = toCalendar.time
        val eventList: ArrayList<Event> = ArrayList<Event>()
        // Add recurring events
        val recurringPatterns: List<RecurringPattern> = readRecurringPatterns()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.getReadableDatabase()
        var event = Event()
        var mCalendar = fromCalendar.clone() as Calendar
        for (recurringPattern in recurringPatterns) {
            when (recurringPattern.pattern) {
                Utils.DAILY -> {

                    var i = 0
                    while (i < 30) {
                        event = dbHelper!!.readEvent(
                            sqLiteDatabase,
                            recurringPattern.eventId!!
                        ) // TODO: clone the object
                        mCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        event.date = Utils.eventDateFormat.format(mCalendar.time)
                        eventList.add(event)
                        i++
                    }
                }
                Utils.WEEKLY -> {
                    mCalendar = fromCalendar.clone() as Calendar
                    var i = 0
                    while (i < 4) {
                        event = dbHelper!!.readEvent(
                            sqLiteDatabase,
                            recurringPattern.eventId!!
                        ) // TODO: clone the object
                        mCalendar.add(Calendar.DAY_OF_MONTH, 7)
                        mCalendar.set(Calendar.DAY_OF_WEEK, recurringPattern.dayOfWeek!!)
                        if (mCalendar.getTime().before(toDate)) {
                            event.date = Utils.eventDateFormat.format(mCalendar.getTime())
                            eventList.add(event)
                        }
                        i++
                    }
                }
                Utils.MONTHLY -> {
                    mCalendar = fromCalendar.clone() as Calendar
                    mCalendar.set(Calendar.DAY_OF_MONTH, recurringPattern.dayOfMonth!!)
                    mCalendar.add(Calendar.DAY_OF_MONTH, 1)
                    if (mCalendar.getTime().before(toDate) && mCalendar.getTime().after(fromDate)) {
                        mCalendar.set(Calendar.DAY_OF_MONTH, recurringPattern.dayOfMonth!!)
                        event = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                        event.date= Utils.eventDateFormat.format(mCalendar.getTime())
                        eventList.add(event)
                    }
                }
            }
        }
        val allEvents: List<Event> = dbHelper!!.readAllEvents(sqLiteDatabase)
        for (mEvent in allEvents) {
            val currentDate: Date = Utils.eventDateFormat.parse(mEvent.date)
            if (currentDate.after(fromDate) && currentDate.before(toDate) && !isContains(
                    eventList,
                    mEvent.id!!
                )
            ) {
                eventList.add(mEvent)
            }
        }
        sqLiteDatabase.close()
        return eventList
    }

    private fun collectAllEvents(today: Date): List<Event> {
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.getReadableDatabase()
        return dbHelper!!.readAllEvents(sqLiteDatabase)
    }

    private fun readRecurringPatterns(): List<RecurringPattern> {
        val recurringPatterns: MutableList<RecurringPattern> = ArrayList<RecurringPattern>()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.getReadableDatabase()
        val cursor: Cursor = dbHelper!!.readAllRecurringPatterns(sqLiteDatabase)
        while (cursor.moveToNext()) {
            val recurringPattern = RecurringPattern()
            recurringPattern.eventId = cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_EVENT_ID))
            recurringPattern.pattern = cursor.getString(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_TYPE))
            recurringPattern.monthOfYear = cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_MONTH_OF_YEAR))
            recurringPattern.dayOfMonth = cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_DAY_OF_MONTH))
            recurringPattern.dayOfWeek = cursor.getInt(cursor.getColumnIndex(DBTables.RECURRING_PATTERN_DAY_OF_WEEK))
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            setUpRecyclerView()
            Toast.makeText(activity, "Event edited!", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EDIT_EVENT_ACTIVITY_REQUEST_CODE = 1
    }
}
