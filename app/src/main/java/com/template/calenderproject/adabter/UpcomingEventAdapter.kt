package com.template.calenderproject.adabter

import android.app.AlarmManager
import com.template.calenderproject.EditEventActivity
import com.template.calenderproject.R
import com.template.calenderproject.database.DBTables
import com.template.calenderproject.model.Notification
import com.template.calenderproject.service.ServiceAutoLauncher
import kotlin.collections.ArrayList


import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.RecyclerView
import com.template.calenderproject.database.DBHelper
import com.template.calenderproject.model.Event
import com.template.calenderproject.ui.UpcomingEventsFragment
import java.util.*

class UpcomingEventAdapter(
    private val context: Context,
    events: ArrayList<Event>,
    upcomingEventsFragment: UpcomingEventsFragment
) :
    RecyclerView.Adapter<UpcomingEventAdapter.ViewHolder>() {
    private val TAG = this.javaClass.simpleName
    private val events: ArrayList<Event>
    private var dbHelper: DBHelper
    private val upcomingEventsFragment: UpcomingEventsFragment




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_upcoming_events_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event: Event = events[position]
        holder.eventColorImageView.setBackgroundColor(event.color!!)
        holder.eventTitleTextView.setText(event.title)
        holder.eventDateTextView.setText(event.date)
        holder.eventTimeTextView.setText(event.time)
        holder.eventNoteTextView.setText(event.note)
        holder.optionsImageButton.setOnClickListener {
            showPopupMenu(
                holder.optionsImageButton,
                position
            )
        }
        if (!event.isNotify!!) {
            holder.notificationImageButton.visibility = View.GONE
        }
        if (event.isAllDay!!) {
            holder.eventTimeLinearLayout.visibility = View.GONE
        }
        if (!event.isRecurring!!) {
            holder.recurringEventLinearLayout.visibility = View.GONE
        }
        holder.upcomingEventLinearLayout.setOnClickListener {
            val intent = Intent(context, EditEventActivity::class.java)
            intent.putExtra("eventId", event.id)
            intent.putExtra("eventDate", event.date)
            upcomingEventsFragment.startActivityForResult(
                intent,
                EDIT_EVENT_ACTIVITY_REQUEST_CODE
            )
        }
    }

    private fun showPopupMenu(view: View, position: Int) {
        // inflate menu
        val popup = PopupMenu(view.context, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.popup, popup.menu)
        popup.setOnMenuItemClickListener(MyMenuItemClickListener(position))
        popup.show()
    }

    override fun getItemCount(): Int {
        return events.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
         val eventColorImageView: ImageView
         val eventTitleTextView: TextView
         val eventDateTextView: TextView
         val eventTimeTextView: TextView
         val eventNoteTextView: TextView
         val optionsImageButton: ImageButton
         val notificationImageButton: ImageButton
         val eventTimeLinearLayout: LinearLayout
         val recurringEventLinearLayout: LinearLayout
         val upcomingEventLinearLayout: LinearLayout

        init {
            eventColorImageView =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_ImageView_EventColor) as ImageView
            eventTitleTextView =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_TextView_EventTitle) as TextView
            eventDateTextView =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_TextView_EventDate) as TextView
            eventTimeTextView =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_TextView_EventTime) as TextView
            eventNoteTextView =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_TextView_EventNote) as TextView
            optionsImageButton =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_ImageButton_Options) as ImageButton
            notificationImageButton =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_ImageButton_Notification) as ImageButton
            eventTimeLinearLayout =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_LinearLayout_Time) as LinearLayout
            recurringEventLinearLayout =
                itemView.findViewById<View>(R.id.UpcomingLayoutCell_LinearLayout_Loop) as LinearLayout
            upcomingEventLinearLayout =
                itemView.findViewById<View>(R.id.UpcomingEventsFragment_LinearLayout) as LinearLayout
        }
    }

    private inner class MyMenuItemClickListener(private val position: Int) :
        PopupMenu.OnMenuItemClickListener {
        private val mEvent: Event
        override fun onMenuItemClick(menuItem: MenuItem): Boolean {
            var intent: Intent? = null
            when (menuItem.itemId) {
                R.id.Popup_Item_Edit -> {
                    intent = Intent(context, EditEventActivity::class.java)
                    intent.putExtra("eventId", mEvent.id!!)
                    intent.putExtra("eventDate", mEvent.date!!)
                    upcomingEventsFragment.startActivityForResult(
                        intent,
                        EDIT_EVENT_ACTIVITY_REQUEST_CODE
                    )
                    return true
                }
                R.id.Popup_Item_Delete -> {
                    if (mEvent.isRecurring!!) {
                        AlertDialog.Builder(context)
                            .setTitle("Deleting a Recurring Event")
                            .setMessage("Are you sure you want to delete this recurring event? All occurrences of this event will also be deleted.")
                            .setPositiveButton(
                                "yes"
                            ) { dialog, which ->
                                DeleteAsyncTask().execute(mEvent.id)
                                notifyDataSetChanged()
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, events.size)
                                events.removeAt(position)
                                upcomingEventsFragment.setUpRecyclerView()
                                upcomingEventsFragment.getFragmentManager()?.beginTransaction()
                                    ?.detach(upcomingEventsFragment)?.commit()
                                upcomingEventsFragment.getFragmentManager()?.beginTransaction()
                                   ?.attach(upcomingEventsFragment)?.commit()
                                Toast.makeText(context, "Event removed!", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("no", null)
                            .setIcon(R.drawable.ic_warning)
                            .show()
                    } else {
                        DeleteAsyncTask().execute(mEvent.id!!)
                        events.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, events.size)
                        notifyDataSetChanged()
                        upcomingEventsFragment.getFragmentManager()?.beginTransaction()
                            ?.detach(upcomingEventsFragment)?.commit()
                        upcomingEventsFragment.getFragmentManager()?.beginTransaction()
                           ?.attach(upcomingEventsFragment)?.commit()
                        Toast.makeText(context, "Event removed!", Toast.LENGTH_SHORT).show()
                        upcomingEventsFragment.setUpRecyclerView()
                    }
                    return true
                }
                R.id.Popup_Item_Share -> {
                    intent = Intent()
                    intent.action = Intent.ACTION_SEND
                    intent.putExtra(Intent.EXTRA_TEXT, mEvent.toString())
                    intent.type = "text/plain"
                    upcomingEventsFragment.startActivity(Intent.createChooser(intent, null))
                    return true
                }
                R.id.Popup_Item_Mail -> {
                    // String receiver_email = receiver_editText.getText().toString();
                    val subject: String = mEvent.title!!
                    val message: String = mEvent.toString()

                    // String[] addresses = receiver_email.split(", ");
                    intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    // intent.putExtra(Intent.EXTRA_EMAIL, addresses);
                    intent.putExtra(Intent.EXTRA_SUBJECT, subject)
                    intent.putExtra(Intent.EXTRA_TEXT, message)
                    upcomingEventsFragment.startActivity(Intent.createChooser(intent, "Send Email"))
                    return true
                }
            }
            return false
        } //        private void removeAllOccurrences(int id) {

        //            int size = events.size();
        //            if (size > 0) {
        //                for (int i = 0; i < size; i++) {
        //                    if (events.get(i).getId() == id) {
        //
        //                        try {
        //                            events.remove(i);
        //                            notifyDataSetChanged();
        //                        }catch (Exception e){
        //                            return;
        //                        }
        //
        //
        //                    }
        //                }
        //
        //                //notifyItemRangeRemoved(0, size);
        //            }
        //
        //        }
        init {
            mEvent = events[position]
        }
    }

    private inner class DeleteAsyncTask : AsyncTask<Int?, Void?, Void?>() {


        override fun doInBackground(vararg params: Int?): Void? {
            cancelAllNotifications(params[0]!!)
            deleteEvent(params[0]!!)
            return null
        }
    }

    private fun cancelAllNotifications(integer: Int) {
        cancelAlarms(readNotifications(integer))
    }

    private fun cancelAlarms(notifications: List<Notification>) {
        for (notification in notifications) {
            cancelAlarm(notification.id!!)
            dbHelper.deleteNotificationById(dbHelper.getWritableDatabase(), notification.id!!)
        }
    }

    private fun cancelAlarm(requestCode: Int) {
        Log.d(TAG, "cancelAlarm: $requestCode")
        val intent = Intent(context.applicationContext, ServiceAutoLauncher::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context.applicationContext, requestCode, intent, 0)
        val alarmManager =
            context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent.cancel()
    }

    private fun deleteEvent(eventId: Int) {
        dbHelper = DBHelper(context)
        dbHelper.deleteEvent(dbHelper.getWritableDatabase(), eventId)
        dbHelper.deleteRecurringPattern(dbHelper.getWritableDatabase(), eventId)
        dbHelper.deleteEventInstanceException(dbHelper.getWritableDatabase(), eventId)
        dbHelper.deleteNotificationsByEventId(dbHelper.getWritableDatabase(), eventId)
        dbHelper.close()
    }

    private fun readNotifications(eventId: Int): ArrayList<Notification> {
        val notifications: ArrayList<Notification> = ArrayList<Notification>()
        val sqLiteDatabase: SQLiteDatabase = dbHelper.getReadableDatabase()
        val cursor: Cursor = dbHelper.readEventNotifications(sqLiteDatabase, eventId)
        while (cursor.moveToNext()) {
            val notification = Notification()
            notification.id = cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_ID))
            notification.eventId = cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_EVENT_ID))
            notification.time = cursor.getString(cursor.getColumnIndex(DBTables.NOTIFICATION_TIME))
            notification.channelId = cursor.getInt(cursor.getColumnIndex(DBTables.NOTIFICATION_CHANNEL_ID))
            notifications.add(notification)
        }
        sqLiteDatabase.close()
        return notifications
    }

    companion object {
        private const val EDIT_EVENT_ACTIVITY_REQUEST_CODE = 1
    }

    init {
        this.events = events
        this.upcomingEventsFragment = upcomingEventsFragment
        dbHelper = DBHelper(context)
    }
}
