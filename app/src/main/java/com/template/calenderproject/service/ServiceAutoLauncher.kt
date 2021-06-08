package com.template.calenderproject.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ServiceAutoLauncher : BroadcastReceiver() {
    private val TAG = this.javaClass.simpleName
    override fun onReceive(context: Context, intent: Intent) {
        val mIntent = Intent(context, AlarmService::class.java)
        mIntent.putExtras(intent.extras!!)
        Log.d("APP_TEST", "onReceive: " + "Calling AlarmService...")
        context.startService(mIntent)
    }
}