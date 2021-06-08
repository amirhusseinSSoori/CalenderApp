package com.template.calenderproject.adabter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.template.calenderproject.R
import com.template.calenderproject.model.Notification

class NotificationAdapter(private val context: Context, notifications: MutableList<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    private var notifications: MutableList<Notification>
    fun getNotifications(): List<Notification> {
        return notifications
    }

    fun setNotifications(notifications: MutableList<Notification>) {
        this.notifications = notifications
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.layout_remainder_list_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification: Notification = notifications[position]
        holder.cardView.visibility = View.VISIBLE
        holder.reminderTimeTextView.setText(notification.time)
        holder.cancelNotificationImageButton.setOnClickListener {
            notifications.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notifications.size)
            notifyDataSetChanged()
            holder.cardView.visibility = View.GONE
        }
        val builder = AlertDialog.Builder(context)
        builder.setCancelable(true)
        val dialogView: View = LayoutInflater.from(context)
            .inflate(R.layout.layout_alert_dialog_notification, null, false)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        val notificationPreferenceRadioGroup =
            dialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        val backButton = dialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button
        backButton.setOnClickListener { alertDialog.dismiss() }
        holder.reminderTimeTextView.setOnClickListener {
            notificationPreferenceRadioGroup.check(getIdOfRadioButton(holder.reminderTimeTextView.text.toString()))
            alertDialog.show()
        }
        (dialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup).setOnCheckedChangeListener { radioGroup, buttonId ->
            val selectedPreferenceRadioButton =
                dialogView.findViewById<View>(buttonId) as RadioButton
            notifications[position] = Notification(
                notification.id,
                notification.channelId,
                null,
                selectedPreferenceRadioButton.text.toString()
            )
            notifyDataSetChanged()
            alertDialog.dismiss()
        }
    }

    private fun getIdOfRadioButton(text: String): Int {
        when (text) {
            "10 minutes before" -> return R.id.AlertDialogLayout_Notification_RadioButton_10minBefore
            "1 hour before" -> return R.id.AlertDialogLayout_Notification_RadioButton_1hourBefore
            "1 day before" -> return R.id.AlertDialogLayout_Notification_RadioButton_1dayBefore
            "At the time of event" -> return R.id.AlertDialogLayout_Notification_RadioButton_AtTheTimeOfEvent
        }
        return R.id.AlertDialogLayout_Notification_RadioButton_AtTheTimeOfEvent
    }

    override fun getItemCount(): Int {
        return notifications.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
         val reminderTimeTextView: TextView
         val cancelNotificationImageButton: ImageButton
    val rootLinearLayout: LinearLayout
        val cardView: CardView

        init {
            reminderTimeTextView =
                itemView.findViewById<View>(R.id.ReminderListLayout_TextView_Notification) as TextView
            cancelNotificationImageButton =
                itemView.findViewById<View>(R.id.ReminderListLayout_ImageButton_Cancel) as ImageButton
            rootLinearLayout =
                itemView.findViewById<View>(R.id.ReminderListLayout_LinearLayout_Root) as LinearLayout
            cardView = itemView.findViewById<View>(R.id.ReminderListLayout_CardView) as CardView
        }
    }

    init {
        this.notifications = notifications
    }
}
