package com.template.calenderproject.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.template.calenderproject.ChangeCalender
import com.template.calenderproject.NewEventActivity
import com.template.calenderproject.R
import com.template.calenderproject.Utils
import com.template.calenderproject.adabter.EventAdapter
import com.template.calenderproject.adabter.GridAdapter
import com.template.calenderproject.database.DBHelper
import com.template.calenderproject.database.DBTables
import com.template.calenderproject.databinding.FragmentCalendarBinding
import com.template.calenderproject.model.Event
import com.template.calenderproject.model.RecurringPattern
import saman.zamani.persiandate.PersianDate
import java.util.*
import kotlin.collections.ArrayList


class CalendarFragment : Fragment(R.layout.fragment_calendar) {


    private val TAG = this.javaClass.simpleName
    private val dates: MutableList<Date> = ArrayList()
    private val events: MutableList<Event> = ArrayList<Event>()
    lateinit var binding: FragmentCalendarBinding

    // AlertDialog components
    var savedEventsRecyclerView: RecyclerView? = null
    private var addNewEventButton: Button? = null
    private var noEventTextView: TextView? = null
    private var alertDialog: AlertDialog? = null
    private var dbHelper: DBHelper? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentCalendarBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DBHelper(activity)
        defineListeners()
        setUpCalendar()
    }



    private fun defineListeners() {
        binding.CalenderFragmentButtonNext!!.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            setUpCalendar()
        }

        binding.CalenderFragmentButtonPrev!!.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            setUpCalendar()
        }

        binding.CalenderFragmentGridViewDates!!.onItemClickListener =
            OnItemClickListener { parent, view, position, id -> // Avoid clicking on non-activate dates
                val viewDate = dates[position]
                val viewCalendar = Calendar.getInstance()
                viewCalendar.time = viewDate
                if (viewCalendar[Calendar.YEAR] != calendar[Calendar.YEAR] || viewCalendar[Calendar.MONTH] != calendar[Calendar.MONTH]) {
                    return@OnItemClickListener
                }

                // Show events alert dialog
                val builder = AlertDialog.Builder(
                    activity
                )
                builder.setCancelable(true)
                val dialogView: View = LayoutInflater.from(requireContext())
                    .inflate(R.layout.layout_alert_dialog, parent, false)
                builder.setView(dialogView)
                alertDialog = builder.create()
                alertDialog!!.show()
                alertDialog!!.setOnCancelListener(DialogInterface.OnCancelListener { setUpCalendar() })
                savedEventsRecyclerView =
                    dialogView.findViewById<View>(R.id.AlertDialog_RecyclerView_ListEvents) as RecyclerView
                addNewEventButton =
                    dialogView.findViewById<View>(R.id.AlertDialog_Button_AddEvent) as Button
                noEventTextView =
                    dialogView.findViewById<View>(R.id.AlertDialog_TextView_NoEvent) as TextView
                val date: String = Utils.eventDateFormat.format(dates[position])
                val eventsByDate: ArrayList<Event> = collectEventsByDate(dates[position])
                if (eventsByDate.isEmpty()) {
                    savedEventsRecyclerView!!.visibility = View.INVISIBLE
                    noEventTextView!!.visibility = View.VISIBLE
                    addNewEventButton!!.text = "CREATE EVENT"
                } else {
                    savedEventsRecyclerView!!.visibility = View.VISIBLE
                    noEventTextView!!.visibility = View.GONE
                    savedEventsRecyclerView!!.setHasFixedSize(true)
                    val layoutManager: RecyclerView.LayoutManager =
                        LinearLayoutManager(view.context)
                    savedEventsRecyclerView!!.layoutManager = layoutManager
                    val eventAdapter =
                        EventAdapter(
                            requireContext(),
                            eventsByDate,
                            alertDialog!!,
                            this@CalendarFragment
                        )
                    savedEventsRecyclerView!!.adapter = eventAdapter
                    eventAdapter.notifyDataSetChanged()
                    addNewEventButton!!.text = "ADD EVENT"
                }
                addNewEventButton!!.setOnClickListener {
                    val intent = Intent(context, NewEventActivity::class.java)
                    intent.putExtra("date", date)
                    startActivityForResult(
                        intent,
                        ADD_NEW_EVENT_ACTIVITY_REQUEST_CODE
                    )
                    alertDialog!!.dismiss()
                }
            }
    }

    fun setUpCalendar() {
        val dateString : String =ChangeCalender().changeToPersian(Utils.monthFormat.format(calendar.time))
        val pdate = PersianDate(calendar.time)
      //  pdate.setShDay(Utils.dayFormat.format(PersianDate(calendar.time)).toInt())
        binding.CalenderFragmentTextViewCurrentDate!!.text = "$dateString"

        Log.e("TAGM", "setUpCalendar: ${pdate.getShDay()} ")
        dates.clear()
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar[Calendar.DAY_OF_MONTH] = 1 // start from Monday
        val firstDayOfMonth = monthCalendar[Calendar.DAY_OF_WEEK] - 2
        monthCalendar.add(Calendar.DAY_OF_MONTH, -firstDayOfMonth)
        collectEventsByMonth(
            Utils.yearFormat.format(calendar.time), Utils.monthFormat.format(
                calendar.time
            )
        )
        while (dates.size < Utils.MAX_CALENDAR_DAYS) {
            dates.add(monthCalendar.time)
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        val gridAdapter = GridAdapter(requireContext(), dates, calendar, events)

        binding.CalenderFragmentGridViewDates!!.adapter = gridAdapter


    }

    private fun collectEventsByMonth(year: String, month: String) {
        events.clear()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.readableDatabase
        val cursor: Cursor = dbHelper!!.readEventsByMonth(sqLiteDatabase, year, month)
        while (cursor.moveToNext()) {
            events.add(
                dbHelper!!.readEvent(
                    sqLiteDatabase,
                    cursor.getInt(cursor.getColumnIndex(DBTables.EVENT_ID))
                )
            )
        }
        cursor.close()
        sqLiteDatabase.close()
    }

    private fun collectEventsByDate(date: Date): ArrayList<Event> {
        val eventList: ArrayList<Event> = ArrayList<Event>()
        val calendar = Calendar.getInstance()
        calendar.time = date
        val month = calendar[Calendar.MONTH]
        val dayOfMonth = calendar[Calendar.DAY_OF_MONTH]
        val dayOfWeek = calendar[Calendar.DAY_OF_WEEK]

        // Add recurring events
        var mEvent = Event()
        val recurringPatterns: List<RecurringPattern> = readRecurringPatterns()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.readableDatabase
        for (recurringPattern in recurringPatterns) {
            when (recurringPattern.pattern) {
                Utils.DAILY -> {
                    mEvent = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    mEvent.date = Utils.eventDateFormat.format(date)
                    eventList.add(mEvent)
                }
                Utils.WEEKLY -> if (dayOfWeek == recurringPattern.dayOfWeek) {
                    mEvent = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    mEvent.date = Utils.eventDateFormat.format(date)
                    eventList.add(mEvent)
                }
                Utils.MONTHLY -> if (dayOfMonth == recurringPattern.dayOfMonth) {
                    mEvent = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    mEvent.date = Utils.eventDateFormat.format(date)
                    eventList.add(mEvent)
                }
                Utils.YEARLY -> if (month == recurringPattern.monthOfYear && dayOfMonth == recurringPattern.monthOfYear) {
                    mEvent = dbHelper!!.readEvent(sqLiteDatabase, recurringPattern.eventId!!)
                    mEvent.date = Utils.eventDateFormat.format(date)
                    eventList.add(mEvent)
                }
            }
        }


        // Add non-recurring events
        val cursor: Cursor =
            dbHelper!!.readEventsByDate(sqLiteDatabase, Utils.eventDateFormat.format(date))
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

    private fun readRecurringPatterns(): List<RecurringPattern> {
        val recurringPatterns: MutableList<RecurringPattern> = ArrayList<RecurringPattern>()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.readableDatabase
        val cursor: Cursor = dbHelper!!.readAllRecurringPatterns(sqLiteDatabase)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADD_NEW_EVENT_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setUpCalendar()
                Toast.makeText(activity, "Event created!", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == EDIT_EVENT_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                setUpCalendar()
                Toast.makeText(activity, "Event edited!", Toast.LENGTH_SHORT).show()
                alertDialog!!.dismiss()
            }
        }
    }

    companion object {
        private const val ADD_NEW_EVENT_ACTIVITY_REQUEST_CODE = 0
        private const val EDIT_EVENT_ACTIVITY_REQUEST_CODE = 1
        val calendar = Calendar.getInstance(Locale.ENGLISH)
    }
}
