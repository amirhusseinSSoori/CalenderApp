package com.template.calenderproject

//import android.app.*
import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.template.calenderproject.adabter.NotificationAdapter
import com.template.calenderproject.database.DBHelper
import com.template.calenderproject.database.DBTables
import com.template.calenderproject.databinding.ActivityNewEventBinding
import com.template.calenderproject.model.Event
import com.template.calenderproject.model.Notification
import com.template.calenderproject.service.ServiceAutoLauncher
import petrov.kristiyan.colorpicker.ColorPicker
import petrov.kristiyan.colorpicker.ColorPicker.OnChooseColorListener
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class EditEventActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private val MAPS_ACTIVITY_REQUEST = 1

    private var notificationAlertDialog: AlertDialog? = null
    private var repetitionAlertDialog: AlertDialog? = null
    private var alarmYear = 0
    private var alarmMonth = 0
    private var alarmDay = 0
    private var alarmHour = 0
    private var alarmMinute = 0
    private var mLocationPermissionGranted = false
    private var notColor = 0
    private var dbHelper: DBHelper? = null
    private var currentNotifications: MutableList<Notification>? = null
    private var oldEventId = 0
    private var notificationAdapter: NotificationAdapter? = null
    private var mEvent: Event? = null
    lateinit var binding:ActivityNewEventBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appTheme)
        super.onCreate(savedInstanceState)
        binding= ActivityNewEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        dbHelper = DBHelper(this)
        initViews()
        initVariables()
        createAlertDialogs()
        defineListeners()
        setSupportActionBar(binding.AddNewEventActivityToolbar)
    }


    @SuppressLint("ResourceType")
    private fun initViews() {
        val intent = intent
        val eventId = intent.getIntExtra("eventId", 0)
        mEvent = readEvent(eventId)
        oldEventId = mEvent!!.id!!
        binding.AddNewEventActivityTextInputLayoutEventTitle!!.editText!!.setText(mEvent!!.title)

        binding.AddNewEventActivityTexViewSetDate.text = intent.getStringExtra("eventDate")
        if (mEvent!!.isAllDay!!) {
            binding.AddNewEventActivitySwitchAllDayEvent.isChecked = true
//            AddNewEventActivity_Imgte_SetDate
            binding.AddNewEventActivityFrameSetTime.visibility = View.GONE
        } else {
            binding.AddNewEventActivitySwitchAllDayEvent.isChecked = false
            binding.AddNewEventActivityTexViewSetTime.setText(mEvent!!.time)
        }
        binding.AddNewEventActivityButtonDuration.text = mEvent!!.duration

        currentNotifications = ArrayList(readNotifications(mEvent!!.id!!))
        setUpRecyclerView()
        binding.AddNewEventActivityTextViewRepeat.setText(mEvent!!.recurringPeriod)
        binding.AddNewEventActivityTextInputLayoutNote.editText!!.setText(mEvent!!.note)
        val bgShape = binding.AddNewEventActivityTextViewPickNoteColor.background as GradientDrawable
        bgShape.setColor(mEvent!!.color!!)
        binding.AddNewEventActivityTextInputLayoutPhoneNumber.editText!!.setText(mEvent!!.phoneNumber)

    }

    private fun initVariables() {
        val mCal = Calendar.getInstance()
        mCal.timeZone = TimeZone.getDefault()
        try {
            mCal.time = Utils.eventDateFormat.parse( binding.AddNewEventActivityTexViewSetDate.text.toString())
            alarmYear = mCal[Calendar.YEAR]
            alarmMonth = mCal[Calendar.MONTH]
            alarmDay = mCal[Calendar.DAY_OF_MONTH]
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mLocationPermissionGranted = true
        }
    }

    private fun createAlertDialogs() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)

        // Notification AlertDialog
        val notificationDialogView = LayoutInflater.from(this)
            .inflate(R.layout.layout_alert_dialog_notification, null, false)
        val notificationRadioGroup =
            notificationDialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        notificationRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            currentNotifications!!.add(
                Notification(
                    null,
                    null,null,
                    (notificationDialogView.findViewById<View>(
                        checkedId
                    ) as RadioButton).text.toString()
                )
            )
            notificationAlertDialog!!.dismiss()
            setUpRecyclerView()
        }
        builder.setView(notificationDialogView)
        notificationAlertDialog = builder.create()
        (notificationDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { notificationAlertDialog!!.dismiss() }

        // Event repetition AlertDialog
        val eventRepetitionDialogView =
            LayoutInflater.from(this).inflate(R.layout.layout_alert_dialog_repeat, null, false)
        val eventRepetitionRadioGroup =
            eventRepetitionDialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        eventRepetitionRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            binding.AddNewEventActivityTextViewRepeat.text =
                "Repeat " + (eventRepetitionDialogView.findViewById<View>(checkedId) as RadioButton).text.toString()
            repetitionAlertDialog!!.dismiss()
        }
        builder.setView(eventRepetitionDialogView)
        repetitionAlertDialog = builder.create()
        (eventRepetitionDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { repetitionAlertDialog!!.dismiss() }
    }

    private fun defineListeners() {
        binding.AddNewEventActivitySwitchAllDayEvent.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                binding.AddNewEventActivityFrameSetTime.visibility = View.GONE
            } else {
                binding.AddNewEventActivityFrameSetTime.visibility = View.VISIBLE
            }
        }
        binding.AddNewEventActivityFrameSetDate.setOnClickListener { view -> setDate(view) }
        binding.AddNewEventActivityFrameSetTime.setOnClickListener { view -> setTime(view) }
        binding.AddNewEventActivityButtonDuration.setOnClickListener { view -> setDuration(view) }
        binding.AddNewEventActivityTextViewAddNotification.setOnClickListener { notificationAlertDialog!!.show() }
        binding.AddNewEventActivityTextViewRepeat.setOnClickListener { repetitionAlertDialog!!.show() }
        binding.AddNewEventActivityTextViewPickNoteColor.setOnClickListener { view -> pickNoteColor(view) }

    }

    private fun readEvent(eventId: Int): Event {
        val sqLiteDatabase = dbHelper!!.readableDatabase
        val event = dbHelper!!.readEvent(sqLiteDatabase, eventId)
        event.recurringPeriod=dbHelper!!.readRecurringPeriod(sqLiteDatabase, event!!.id!!)
        sqLiteDatabase.close()
        return event
    }

    private fun readNotifications(eventId: Int): ArrayList<Notification?> {
        val notifications = ArrayList<Notification?>()
        val sqLiteDatabase = dbHelper!!.readableDatabase
        val cursor = dbHelper!!.readEventNotifications(sqLiteDatabase, eventId)
        while (cursor.moveToNext()) {
            val notification = Notification()
            notification.id=cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_ID))
            notification.eventId=cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_EVENT_ID))
            notification.time=cursor.getString(cursor.getColumnIndex(DBTables.NOTIFICATION_TIME))
            notification.channelId=cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_CHANNEL_ID))
            notifications.add(notification)
        }
        sqLiteDatabase.close()
        return notifications
    }

    private fun setDuration(view: View) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, R.style.DurationPickerTheme,
            { view, hourOfDay, minute ->
                binding.AddNewEventActivityButtonDuration.text =
                    "DURATION: " + Integer.toString(hourOfDay) + " HOURS " + Integer.toString(minute) + " MINUTES"
            }, 0, 0, true
        )
        timePickerDialog.setTitle("Duration")
        timePickerDialog.show()
    }

    fun setTime(view: View?) {
        val calendar = Calendar.getInstance()
        val hour = calendar[Calendar.HOUR_OF_DAY]
        val minute = calendar[Calendar.MINUTE]
        val timePickerDialog = TimePickerDialog(this,
            { view, hourOfDay, minute ->
                val aCal = Calendar.getInstance()
                aCal[Calendar.HOUR_OF_DAY] = hourOfDay
                aCal[Calendar.MINUTE] = minute
                aCal.timeZone = TimeZone.getDefault()
                val simpleDateFormat = SimpleDateFormat("K:mm a", Locale.ENGLISH)
                val eventTime = simpleDateFormat.format(aCal.time)
                alarmHour = hourOfDay
                alarmMinute = minute
                binding.AddNewEventActivityTexViewSetTime.text = eventTime
            }, hour, minute, false
        )
        timePickerDialog.show()
    }

    fun setDate(view: View?) {
        val calendar = Calendar.getInstance()
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]
        val datePickerDialog = DatePickerDialog(this,
            { view, year, month, dayOfMonth ->
                val aCal = Calendar.getInstance()
                aCal[Calendar.YEAR] = year
                aCal[Calendar.MONTH] = month
                aCal[Calendar.DAY_OF_MONTH] = dayOfMonth
                aCal.timeZone = TimeZone.getDefault()
                val simpleDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
                val eventTime = simpleDateFormat.format(aCal.time)
                alarmYear = year
                alarmMonth = month
                alarmDay = dayOfMonth
                binding.AddNewEventActivityTexViewSetDate.text = eventTime
            }, year, month, day
        )
        datePickerDialog.show()
    }

    fun pickNoteColor(view: View?) {
        val colors = Utils.getColors(this)
        val colorPicker = ColorPicker(this)
        colorPicker
            .setColors(colors)
            .setColumns(5)
            .setDefaultColorButton(R.color.blue)
            .setOnChooseColorListener(object : OnChooseColorListener {
                override fun onChooseColor(position: Int, color: Int) {
                    notColor = color
                    val bgShape = binding.AddNewEventActivityTextViewPickNoteColor.background as GradientDrawable
                    bgShape.setColor(color)
                }

                override fun onCancel() {}
            }).show()
    }

    private fun setUpRecyclerView() {
        binding.AddNewEventActivityRecyclerViewNotifications.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        layoutManager.isMeasurementCacheEnabled = false
        binding.AddNewEventActivityRecyclerViewNotifications.layoutManager = layoutManager
        notificationAdapter = NotificationAdapter(this, currentNotifications!!)
        binding.AddNewEventActivityRecyclerViewNotifications.adapter = notificationAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.ToolBar_Item_Save -> if (confirmInputs()) {
                if (mEvent!!.isRecurring!!) {
                    AlertDialog.Builder(this)
                        .setTitle("Editing a Recurring Event")
                        .setMessage("Are you sure you want to edit this recurring event? All occurrences of this event will also be edited.")
                        .setPositiveButton(
                           "yes"
                        ) { dialog, which ->
                            viewValues
                            UpdateAsyncTask().execute()
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(R.drawable.ic_warning)
                        .show()
                } else {
                    viewValues
                    UpdateAsyncTask().execute()
                }
            }
        }
        return true
    }

    private fun cancelAlarms(notifications: List<Notification?>) {
        for (notification in notifications) {
            cancelAlarm(notification!!.id!!)
            dbHelper!!.deleteNotificationById(dbHelper!!.writableDatabase, notification.id!!)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        Log.d(TAG, "cancelAlarm: $requestCode")
        val intent = Intent(applicationContext, ServiceAutoLauncher::class.java)
        val pendingIntent = PendingIntent.getBroadcast(applicationContext, requestCode, intent, 0)
        val alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        pendingIntent.cancel()
    }

    private fun setAlarms(notifications: ArrayList<Notification?>) {
        val calendar = Calendar.getInstance()
        calendar[alarmYear, alarmMonth] = alarmDay
        calendar[Calendar.HOUR_OF_DAY] = alarmHour
        calendar[Calendar.MINUTE] = alarmMinute
        calendar[Calendar.SECOND] = 0
        for (notification in notifications) {
            val aCal = calendar.clone() as Calendar
            val notificationPreference: String = notification!!.time!!
            if (notificationPreference == getString(R.string._10_minutes_before)) {
                aCal.add(Calendar.MINUTE, -10)
            } else if (notificationPreference == getString(R.string._1_hour_before)) {
                aCal.add(Calendar.HOUR_OF_DAY, -1)
            } else if (notificationPreference == getString(R.string._1_day_before)) {
                aCal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                // At the time of the event
            }
            setAlarm(notification, aCal.timeInMillis)
        }
    }

    private fun setAlarm(notification: Notification?, triggerAtMillis: Long) {
        Log.d(TAG, "setAlarm: " + notification!!.id!!)
        val intent = Intent(this, ServiceAutoLauncher::class.java)
        intent.putExtra("eventTitle", mEvent!!.title!!)
        intent.putExtra("eventNote", mEvent!!.note!!)
        intent.putExtra("eventColor", mEvent!!.color!!)
        intent.putExtra("eventTimeStamp", mEvent!!.date!!.toString() + ", " + mEvent!!.time!!)
        intent.putExtra("interval", interval)
        intent.putExtra("soundName", getString("ringtone"))
        val asd = interval
        intent.putExtra("notificationId", notification!!.channelId!!)
        val alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        val pendingIntent =
            PendingIntent.getBroadcast(applicationContext, notification.id!!, intent, 0)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private val interval: String
        private get() {
            var interval = getString(R.string.one_time)
            val repeatingPeriod =  binding.AddNewEventActivityTextViewRepeat.text.toString()
            if (repeatingPeriod == getString(R.string.daily)) {
                interval = getString(R.string.daily)
            } else if (repeatingPeriod == getString(R.string.weekly)) {
                interval = getString(R.string.weekly)
            } else if (repeatingPeriod == getString(R.string.monthly)) {
                interval = getString(R.string.monthly)
            } else if (repeatingPeriod == getString(R.string.yearly)) {
                interval = getString(R.string.yearly)
            }
            return interval
        }

    @get:SuppressLint("ResourceType")
    private val viewValues: Unit
        private get() {
            var aDate: Date? = null
            try {
                aDate = Utils.eventDateFormat.parse( binding.AddNewEventActivityTexViewSetDate.text as String)
            } catch (e: ParseException) {
                e.printStackTrace()
                Log.e(TAG, "An error has occurred while parsing the date string")
            }
            mEvent!!.title= binding.AddNewEventActivityTextInputLayoutEventTitle.editText!!.text.toString().trim { it <= ' ' }
            mEvent!!.isAllDay=  binding.AddNewEventActivitySwitchAllDayEvent.isChecked
            mEvent!!.date = Utils.eventDateFormat.format(aDate)
            mEvent!!.month = Utils.monthFormat.format(aDate)
            mEvent!!.year = Utils.yearFormat.format(aDate)
            mEvent!!.time = binding.AddNewEventActivityTexViewSetTime.text.toString()
            mEvent!!.duration =  binding.AddNewEventActivityButtonDuration.text.toString()
            mEvent!!.isNotify =!notificationAdapter!!.getNotifications().isEmpty()
            mEvent!!.isRecurring = isRecurring( binding.AddNewEventActivityTextViewRepeat.text.toString())
            mEvent!!.recurringPeriod = binding.AddNewEventActivityTextViewRepeat.text.toString()
            mEvent!!.note = binding.AddNewEventActivityTextInputLayoutNote.editText!!.text.toString().trim { it <= ' ' }
            if (notColor == 0) {
                notColor = resources.getInteger(R.color.red)
            } else {
                mEvent!!.color=notColor
            }
            mEvent!!.phoneNumber = binding.AddNewEventActivityTextInputLayoutPhoneNumber.editText!!.text.toString().trim { it <= ' ' }

        }

    private fun isRecurring(toString: String): Boolean {
        return toString != resources.getString(R.string.one_time)
    }

    private fun confirmInputs(): Boolean {
        if (!validateEventTitle()) {
            return false
        }
        if (!validateNotifications()) {
            Snackbar.make(
                binding.AddNewEventActivityTextViewAddNotification,
                "You cannot set a notification to the past.",
                BaseTransientBottomBar.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun validateEventTitle(): Boolean {
        val eventTitleString =
            binding.AddNewEventActivityTextInputLayoutEventTitle.editText!!.text.toString().trim { it <= ' ' }
        return if (eventTitleString.isEmpty()) {
            binding.AddNewEventActivityTextInputLayoutEventTitle.error = "Field can't be empty!"
            false
        } else {
            binding.AddNewEventActivityTextInputLayoutEventTitle.error = null
            true
        }
    }

    private fun validateNotifications(): Boolean {
        val calendar = Calendar.getInstance()
        calendar[alarmYear, alarmMonth] = alarmDay
        calendar[Calendar.HOUR_OF_DAY] = alarmHour
        calendar[Calendar.MINUTE] = alarmMinute
        calendar[Calendar.SECOND] = 0
        for (notification in notificationAdapter!!.getNotifications()) {
            val aCal = calendar.clone() as Calendar
            val notificationPreference: String = notification.time!!
            if (notificationPreference == getString(R.string._10_minutes_before)) {
                aCal.add(Calendar.MINUTE, -10)
            } else if (notificationPreference == getString(R.string._1_hour_before)) {
                aCal.add(Calendar.HOUR_OF_DAY, -1)
            } else if (notificationPreference == getString(R.string._1_day_before)) {
                aCal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                // At the time of the event
            }
            if (aCal.before(Calendar.getInstance())) {
                return false
            }
        }
        return true
    }

    private inner class UpdateAsyncTask :
        AsyncTask<Void?, Void?, Void?>() {
        override fun onPreExecute() {
            super.onPreExecute()
            binding.AddNewEventActivityProgressBar!!.visibility = View.VISIBLE
        }



        override fun onPostExecute(aVoid: Void?) {
            super.onPostExecute(aVoid)
            dbHelper!!.close()
            setResult(RESULT_OK)
            finish()
        }

        override fun doInBackground(vararg params: Void?): Void? {
            cancelAlarms(readNotifications(mEvent!!.id!!))
            dbHelper!!.updateEvent(dbHelper!!.writableDatabase, oldEventId, mEvent)
            for (notification in notificationAdapter!!.getNotifications()) {
                notification.eventId=mEvent!!.id!!
                dbHelper!!.saveNotification(dbHelper!!.writableDatabase, notification)
            }
            if (mEvent!!.isNotify!!) {
                setAlarms(readNotifications(mEvent!!.id!!))
            }
            return null
        }
    }

    private val appTheme: Int
        private get() {
            when (getString("theme")) {
                "Dark" -> return R.style.DarkTheme
                "Indigo" -> return R.style.DarkIndigoTheme
            }
            return R.style.DarkIndigoTheme
        }

    private fun getString(key: String): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPreferences.getString(key, "Indigo")
    }

    private val locationPermission: Unit
        private get() {
            mLocationPermissionGranted = false
            if (ContextCompat.checkSelfPermission(
                    this.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mLocationPermissionGranted = true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MAPS_ACTIVITY_REQUEST) {
            if (resultCode == RESULT_OK) {
               // eventLocationTextInputLayout!!.editText!!.setText(data!!.getStringExtra("address"))
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }
}