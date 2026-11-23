package com.example.time_controll.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.time_controll.data.AlarmRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> {
                Log.d(TAG, "System time/zone event (${intent.action}), rescheduling all alarms...")
                AlarmRepository.getInstance(context).rescheduleAllAlarms()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
