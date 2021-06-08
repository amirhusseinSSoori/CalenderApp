package com.template.calenderproject

import android.Manifest
import android.annotation.SuppressLint
import android.app.*

import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.graphics.drawable.GradientDrawable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
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


class NewEventActivity : AppCompatActivity() {
    private val TAG = this.javaClass.simpleName
    private val MAPS_ACTIVITY_REQUEST = 1
//    private var toolbar: Toolbar? = null

   // private var eventTitleTextInputLayout: TextInputLayout? = null
  //  private var allDayEventSwitch: Switch? = null
   // private var setDateLinearLayout: LinearLayout? = null
   // private var setDateTextView: TextView? = null
    //private var setTimeLinearLayout: LinearLayout? = null
   // private var setTimeTextView: TextView? = null
   // private var setDurationButton: Button? = null
   // private var notificationsRecyclerView: RecyclerView? = null
   // private var addNotificationTextView: TextView? = null
   // private var repeatTextView: TextView? = null
   // private var eventNoteTextInputLayout: TextInputLayout? = null
   // private var pickNoteColorTextView: TextView? = null
  //  private var eventLocationTextInputLayout: TextInputLayout? = null
   // private var locationImageButton: ImageButton? = null
  //  private var phoneNumberTextInputLayout: TextInputLayout? = null
//    private var mailTextInputLayout: TextInputLayout? = null
  //  private var mailTextInputEditText: TextInputEditText? = null
//    private var mailSwitch: Switch? = null
    private var mLocationPermissionGranted = false
    private var notificationAlertDialog: AlertDialog? = null
    private var repetitionAlertDialog: AlertDialog? = null
    private var alarmYear = 0
    private var alarmMonth = 0
    private var alarmDay = 0
    private var alarmHour = 0
    private var alarmMinute = 0
    private var notColor = 0
    private var dbHelper: DBHelper? = null
    private var notifications: MutableList<Notification>? = null
    private var event: Event? = null

    lateinit var binding:ActivityNewEventBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(appTheme)
        super.onCreate(savedInstanceState)
        binding=ActivityNewEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        event = Event()
        notifications = ArrayList()
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
        binding.AddNewEventActivityTexViewSetDate!!.text = intent.getStringExtra("date")
        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getDefault()
        binding.AddNewEventActivityTexViewSetTime!!.text = SimpleDateFormat("K:mm a", Locale.ENGLISH).format(calendar.time)
        val bgShape = binding.AddNewEventActivityTextViewPickNoteColor!!.background as GradientDrawable
        bgShape.setColor(resources.getInteger(R.color.red))
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        binding.AddNewEventActivityTextViewRepeat!!.text = sharedPreferences.getString("frequency", "Repeat One-Time")
        notifications!!.add(
            Notification(
                null,null,null,
                sharedPreferences.getString(
                    "reminder",
                    resources.getString(R.string.at_the_time_of_event)
                )
            )
        )
        setUpRecyclerView()
    }

    private fun initVariables() {
        val mCal = Calendar.getInstance()
        mCal.timeZone = TimeZone.getDefault()
        alarmHour = mCal[Calendar.HOUR_OF_DAY]
        alarmMinute = mCal[Calendar.MINUTE]
        try {
            mCal.time = Utils.eventDateFormat.parse(intent.getStringExtra("date"))
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        alarmYear = mCal[Calendar.YEAR]
        alarmMonth = mCal[Calendar.MONTH]
        alarmDay = mCal[Calendar.DAY_OF_MONTH]
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
            notifications!!.add(
                Notification(null,null,null,
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
            binding.AddNewEventActivityTextViewRepeat!!.text =
                "Repeat " + (eventRepetitionDialogView.findViewById<View>(checkedId) as RadioButton).text.toString()
            repetitionAlertDialog!!.dismiss()
        }
        builder.setView(eventRepetitionDialogView)
        repetitionAlertDialog = builder.create()
        (eventRepetitionDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { repetitionAlertDialog!!.dismiss() }
    }

    private fun defineListeners() {
        binding.AddNewEventActivitySwitchAllDayEvent!!.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                binding.AddNewEventActivityLinearLayoutSetTime!!.visibility = View.GONE
            } else {
                binding.AddNewEventActivityLinearLayoutSetTime!!.visibility = View.VISIBLE
            }
        }
        binding.AddNewEventActivityLinearLayoutSetDate!!.setOnClickListener { view -> setDate(view) }
        binding.AddNewEventActivityLinearLayoutSetTime!!.setOnClickListener { view -> setTime(view) }
        binding.AddNewEventActivityButtonDuration!!.setOnClickListener { view -> setDuration(view) }
        binding.AddNewEventActivityTextViewAddNotification!!.setOnClickListener { notificationAlertDialog!!.show() }
        binding.AddNewEventActivityTextViewRepeat!!.setOnClickListener { repetitionAlertDialog!!.show() }
        binding.AddNewEventActivityTextViewPickNoteColor!!.setOnClickListener { view -> pickNoteColor(view) }

//        locationImageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (!mLocationPermissionGranted) {
//                    getLocationPermission();
//                } else {
//                    startActivityForResult(new Intent(getApplicationContext(), MapsActivity.class), MAPS_ACTIVITY_REQUEST);
//                }
//            }
//        });
        binding.AddNewEventActivitySwitchMail!!.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked) {
                binding.AddNewEventActivityTextInputEditTextMail!!.isEnabled = true
                binding.AddNewEventActivityTextInputLayoutMail!!.isEnabled = true
            } else {
                binding.AddNewEventActivityTextInputEditTextMail!!.setText("")
                binding.AddNewEventActivityTextInputEditTextMail!!.isEnabled = false
                binding.AddNewEventActivityTextInputLayoutMail!!.isEnabled = false
            }
        }
    }

    private fun setDuration(view: View) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(this, R.style.DurationPickerTheme,
            { view, hourOfDay, minute ->
                binding.AddNewEventActivityButtonDuration!!.text =
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
                aCal.timeZone = TimeZone.getDefault()
                aCal[Calendar.HOUR_OF_DAY] = hourOfDay
                aCal[Calendar.MINUTE] = minute
                val simpleDateFormat = SimpleDateFormat("K:mm a", Locale.ENGLISH)
                val eventTime = simpleDateFormat.format(aCal.time)
                alarmHour = hourOfDay
                alarmMinute = minute
                binding.AddNewEventActivityTexViewSetTime!!.text = eventTime
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
                aCal.timeZone = TimeZone.getDefault()
                aCal[Calendar.YEAR] = year
                aCal[Calendar.MONTH] = month
                aCal[Calendar.DAY_OF_MONTH] = dayOfMonth
                val simpleDateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH)
                val eventTime = simpleDateFormat.format(aCal.time)
                alarmYear = year
                alarmMonth = month
                alarmDay = dayOfMonth
                binding.AddNewEventActivityTexViewSetDate!!.text = eventTime
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
                    val bgShape = binding.AddNewEventActivityTextViewPickNoteColor!!.background as GradientDrawable
                    bgShape.setColor(color)
                }

                override fun onCancel() {}
            }).show()
    }

    private fun setUpRecyclerView() {
        binding.AddNewEventActivityRecyclerViewNotifications!!.setHasFixedSize(true)
        val layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this)
        layoutManager.isMeasurementCacheEnabled = false
        binding.AddNewEventActivityRecyclerViewNotifications!!.layoutManager = layoutManager
        val notificationAdapter = NotificationAdapter(this, notifications!!)
        binding.AddNewEventActivityRecyclerViewNotifications!!.adapter = notificationAdapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.ToolBar_Item_Save -> if (confirmInputs()) {
                viewValues
                SaveAsyncTask().execute()
            }
        }
        return true
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun setAlarms() {
        val calendar = Calendar.getInstance()
        calendar[alarmYear, alarmMonth] = alarmDay
        calendar[Calendar.HOUR_OF_DAY] = alarmHour
        calendar[Calendar.MINUTE] = alarmMinute
        calendar[Calendar.SECOND] = 0
        for (notification in notifications!!) {
            val aCal = calendar.clone() as Calendar
            val notificationPreference: String = notification.time!!
            if (notificationPreference == getString(R.string._10_minutes_before)) {
                aCal.add(Calendar.MINUTE, -10)
            } else if (notificationPreference == getString(R.string._1_hour_before)) {
                aCal.add(Calendar.HOUR_OF_DAY, -1)
            } else if (notificationPreference == getString(R.string._1_day_before)) {
                aCal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                Log.i(TAG, "setAlarms: ")
            }
            setAlarm(notification, aCal.timeInMillis)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun setAlarm(notification: Notification, triggerAtMillis: Long) {
        val intent = Intent(applicationContext, ServiceAutoLauncher::class.java)
        intent.putExtra("eventTitle", event!!.title!!)
        intent.putExtra("eventNote", event!!.note!!)
        intent.putExtra("eventColor", event!!.color!!)
        intent.putExtra("eventTimeStamp", event!!.date.toString() + ", " + event!!.time!!)
        intent.putExtra("interval", interval)
        intent.putExtra("notificationId", notification.channelId)
        intent.putExtra("soundName", getString("ringtone"))
        Log.d(TAG, "setAlarm: " + notification.id!!)
        val pendingIntent =
            PendingIntent.getBroadcast(applicationContext, notification.id!!, intent, 0)
        val alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    private val interval: String
        private get() {
            var interval = getString(R.string.one_time)
            val repeatingPeriod =   binding.AddNewEventActivityTextViewRepeat!!.text.toString()
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
                aDate = Utils.eventDateFormat.parse( binding.AddNewEventActivityTexViewSetDate!!.text as String)
            } catch (e: ParseException) {
                e.printStackTrace()
                Log.e(TAG, "An error has occurred while parsing the date string")
            }
            event!!.title =  binding.AddNewEventActivityTextInputLayoutEventTitle!!.editText!!.text.toString().trim { it <= ' ' }
            event!!.isAllDay =  binding.AddNewEventActivitySwitchAllDayEvent!!.isChecked
            event!!.date = Utils.eventDateFormat.format(aDate)
            event!!.month = Utils.monthFormat.format(aDate)
            event!!.year = Utils.yearFormat.format(aDate)
            event!!.time = binding.AddNewEventActivityTexViewSetTime!!.text.toString()
            event!!.duration =  binding.AddNewEventActivityButtonDuration!!.text.toString()
            event!!.isNotify = !notifications!!.isEmpty()
            event!!.isRecurring = isRecurring(  binding.AddNewEventActivityTextViewRepeat!!.text.toString())
            event!!.recurringPeriod=  binding.AddNewEventActivityTextViewRepeat!!.text.toString()
            event!!.note = binding.AddNewEventActivityTextInputLayoutNote!!.editText!!.text.toString().trim { it <= ' ' }
            if (notColor == 0) {
                notColor = resources.getInteger(R.color.red)
                event!!.color = notColor
            } else {
                event!!.color= notColor
            }
            event!!.location= binding.AddNewEventActivityTextInputLayoutLocation!!.editText!!.text.toString().trim { it <= ' ' }
            event!!.phoneNumber=binding.AddNewEventActivityTextInputLayoutPhoneNumber!!.editText!!.text.toString().trim { it <= ' ' }
            event!!.mail = binding.AddNewEventActivityTextInputLayoutMail!!.editText!!.text.toString().trim { it <= ' ' }
        }

    private fun isRecurring(toString: String): Boolean {
        return toString != "Repeat One-Time"
    }

    private fun confirmInputs(): Boolean {
        if (!validateEventTitle()) {
            return false
        }
        if (!validateNotifications()) {
            Snackbar.make(
                binding.AddNewEventActivityTextViewAddNotification!!,
                "You cannot set a notification to the past.",
                BaseTransientBottomBar.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun validateEventTitle(): Boolean {
        val eventTitleString =
            binding.AddNewEventActivityTextInputLayoutEventTitle!!.editText!!.text.toString().trim { it <= ' ' }
        return if (eventTitleString.isEmpty()) {
            binding.AddNewEventActivityTextInputLayoutEventTitle!!.error = "Field can't be empty!"
            false
        } else {
            binding.AddNewEventActivityTextInputLayoutEventTitle!!.error = null
            true
        }
    }

    private fun validateNotifications(): Boolean {
        val calendar = Calendar.getInstance()
        calendar[alarmYear, alarmMonth] = alarmDay
        calendar[Calendar.HOUR_OF_DAY] = alarmHour
        calendar[Calendar.MINUTE] = alarmMinute
        calendar[Calendar.SECOND] = 0
        for (notification in notifications!!) {
            val aCal = calendar.clone() as Calendar
            val notificationPreference: String = notification.time!!
            if (notificationPreference == getString(R.string._10_minutes_before)) {
                aCal.add(Calendar.MINUTE, -10)
            } else if (notificationPreference == getString(R.string._1_hour_before)) {
                aCal.add(Calendar.HOUR_OF_DAY, -1)
            } else if (notificationPreference == getString(R.string._1_day_before)) {
                aCal.add(Calendar.DAY_OF_MONTH, -1)
            } else {
                Log.i(TAG, "setAlarms: ")
            }
            if (aCal.before(Calendar.getInstance())) {
                return false
            }
        }
        return true
    }

    private inner class SaveAsyncTask :
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
            dbHelper!!.saveEvent(dbHelper!!.writableDatabase, event)
            val event_id = getEventId(event!!.title!!, event!!.date!!, event!!.time!!)
            for (notification in notifications!!) {
                notification.eventId=event_id
                dbHelper!!.saveNotification(dbHelper!!.getWritableDatabase(), notification)
            }
            notifications = readNotifications(event_id)
            if (event!!.isNotify!!) {
                setAlarms()
            }
            return null
        }
    }

    private fun readNotifications(eventId: Int): ArrayList<Notification> {
        val notifications = ArrayList<Notification>()
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.readableDatabase
        val cursor: Cursor = dbHelper!!.readEventNotifications(sqLiteDatabase, eventId)
        while (cursor.moveToNext()) {
            val notification = Notification()
            notification.id = cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_ID))
            notification.eventId = cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_EVENT_ID))
            notification.time = cursor.getString(cursor.getColumnIndex(DBTables.NOTIFICATION_TIME))
            notification.channelId = cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_CHANNEL_ID))
            notifications.add(notification)
        }
        return notifications
    }

    private fun getEventId(eventTitle: String, eventDate: String, eventTime: String): Int {
        val sqLiteDatabase: SQLiteDatabase = dbHelper!!.getReadableDatabase()
        val event: Event =
            dbHelper!!.readEventByTimestamp(sqLiteDatabase, eventTitle, eventDate, eventTime)
        sqLiteDatabase.close()
        return event!!.id!!
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
                binding.AddNewEventActivityTextInputLayoutLocation!!.editText!!.setText(data!!.getStringExtra("address"))
            }
        }
    }

    companion object {
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    }
}