package com.example.time_controll.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.time_controll.data.AlarmRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d(TAG, "Boot completed, rescheduling all alarms...")
            AlarmRepository.getInstance(context).rescheduleAllAlarms()
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
