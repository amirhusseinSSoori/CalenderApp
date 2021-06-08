package com.template.calenderproject.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.template.calenderproject.MainActivity
import com.template.calenderproject.R
import com.template.calenderproject.databinding.FragmentUserSettingsBinding


class UserSettingsFragment : Fragment(R.layout.fragment_user_settings) {
    private val TAG = this.javaClass.simpleName
    private var ringtoneAlertDialog: AlertDialog? = null
    private var reminderTimeAlertDialog: AlertDialog? = null
    private var reminderFrequencyAlertDialog: AlertDialog? = null
    private var appThemeAlertDialog: AlertDialog? = null
    private var isChanged = false


    lateinit var binding:FragmentUserSettingsBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding= FragmentUserSettingsBinding.bind(view)
        super.onViewCreated(view, savedInstanceState)
        initViews()
        createAlertDialogs()
        defineListeners()
    }

    private fun initViews() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            activity
        )
        binding.UserSettingsFragmentTextViewDefaultRingtone!!.text = sharedPreferences.getString("ringtone", "Consequence")
        binding.UserSettingsFragmentTextViewDefaultReminderTime!!.text = sharedPreferences.getString(
            "reminder",
            resources.getString(R.string.at_the_time_of_event)
        )
        binding.UserSettingsFragmentTextViewDefaultReminderFrequency!!.text = sharedPreferences.getString("frequency", "One-Time")
        binding.UserSettingsFragmentTextViewDefaultReminderFrequency!!.text = sharedPreferences.getString("theme", "Indigo")
    }

    private fun createAlertDialogs() {
        val builder = AlertDialog.Builder(activity)
        builder.setCancelable(true)

        // Ringtone AlertDialog
        val ringtoneDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.layout_alert_dialog_ringtone, null, false)
        val ringToneRadioGroup =
            ringtoneDialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        ringToneRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            binding.UserSettingsFragmentTextViewDefaultRingtone!!.text =
                (ringtoneDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            save(
                "ringtone",
                (ringtoneDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            )
            ringtoneAlertDialog!!.dismiss()
        }
        builder.setView(ringtoneDialogView)
        ringtoneAlertDialog = builder.create()
        (ringtoneDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { ringtoneAlertDialog!!.dismiss() }

        // Reminder time AlertDialog
        val reminderTimeDialogView: View = LayoutInflater.from(activity)
            .inflate(R.layout.layout_alert_dialog_notification, null, false)
        val reminderTimeRadioGroup =
            reminderTimeDialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        reminderTimeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            binding.UserSettingsFragmentTextViewDefaultReminderTime!!.text =
                (reminderTimeDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            save(
                "reminder",
                (reminderTimeDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            )
            reminderTimeAlertDialog!!.dismiss()
        }
        builder.setView(reminderTimeDialogView)
        reminderTimeAlertDialog = builder.create()
        (reminderTimeDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { reminderTimeAlertDialog!!.dismiss() }

        // Reminder frequency Alert Dialog
        val reminderFrequencyDialogView: View =
            LayoutInflater.from(activity).inflate(R.layout.layout_alert_dialog_repeat, null, false)
        val reminderFrequencyRadioGroup =
            reminderFrequencyDialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        reminderFrequencyRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            binding.UserSettingsFragmentTextViewDefaultReminderFrequency!!.text =
                (reminderFrequencyDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            save(
                "frequency",
                "Repeat " + (reminderFrequencyDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            )
            reminderFrequencyAlertDialog!!.dismiss()
        }
        builder.setView(reminderFrequencyDialogView)
        reminderFrequencyAlertDialog = builder.create()
        (reminderFrequencyDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { reminderFrequencyAlertDialog!!.dismiss() }

        //
        val appThemeDialogView: View = LayoutInflater.from(activity)
            .inflate(R.layout.layout_alert_dialog_apptheme, null, false)
        val appThemeRadioGroup =
            appThemeDialogView.findViewById<View>(R.id.AlertDialogLayout_RadioGroup) as RadioGroup
        appThemeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            if (!binding.UserSettingsFragmentTextViewDefaultReminderFrequency!!.text.toString().equals(
                    (appThemeDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString(),
                    ignoreCase = true
                )
            ) {
                save(
                    "theme",
                    (appThemeDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
                )
                saveFlag("isChanged", true)
                isChanged = true
            }
            binding.UserSettingsFragmentTextViewDefaultReminderFrequency!!.text =
                (appThemeDialogView.findViewById<View>(group.checkedRadioButtonId) as RadioButton).text.toString()
            appThemeAlertDialog!!.dismiss()
        }
        builder.setView(appThemeDialogView)
        appThemeAlertDialog = builder.create()
        (appThemeDialogView.findViewById<View>(R.id.AlertDialogLayout_Button_Back) as Button).setOnClickListener { appThemeAlertDialog!!.dismiss() }
    }

    private fun defineListeners() {
        binding.UserSettingsFragmentCardViewRingTone!!.setOnClickListener { ringtoneAlertDialog!!.show() }
        binding.UserSettingsFragmentCardViewReminderTime!!.setOnClickListener { reminderTimeAlertDialog!!.show() }
        binding.UserSettingsFragmentCardViewReminderFrequency!!.setOnClickListener { reminderFrequencyAlertDialog!!.show() }
        binding.UserSettingsFragmentCardViewAppTheme!!.setOnClickListener {
            appThemeAlertDialog!!.show()
            appThemeAlertDialog!!.setOnDismissListener { changeTheme() }
        }
    }

    private fun changeTheme() {
        val s = getString("theme")
        if (isChanged) {
            restartApp()
        }
    }

    private fun save(key: String, value: String) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            activity
        )
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    private fun saveFlag(key: String, flag: Boolean) {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            activity
        )
        val editor = sharedPreferences.edit()
        editor.putBoolean(key, flag)
        editor.apply()
    }

    private fun getString(key: String): String? {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            activity
        )
        return sharedPreferences.getString(key, "")
    }

    private fun restartApp() {
        startActivity(Intent(activity, MainActivity::class.java))
        requireActivity()!!.finish()
    }
}
